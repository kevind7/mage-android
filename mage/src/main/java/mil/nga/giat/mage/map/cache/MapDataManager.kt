package mil.nga.giat.mage.map.cache

import android.annotation.SuppressLint
import android.app.Application
import android.arch.lifecycle.*
import android.arch.lifecycle.Observer
import android.os.AsyncTask
import android.support.annotation.MainThread
import com.google.android.gms.maps.GoogleMap
import mil.nga.giat.mage.data.BasicResource
import mil.nga.giat.mage.data.Resource
import java.io.File
import java.net.URI
import java.util.*
import java.util.concurrent.Executor
import kotlin.collections.HashMap
import kotlin.collections.HashSet


fun <K, V> Map<K, V>.minusIf(predicate: (K, V) -> Boolean, removeTo: MutableMap<K, V>? = null): Map<K, V> {
    val result = HashMap<K, V>()
    forEach { key, value ->
        if (!predicate.invoke(key, value)) {
            result[key] = value
        }
        else {
            removeTo?.put(key, value)
        }
    }
    return result
}

@MainThread
class MapDataManager(config: Config) : LifecycleOwner, LiveData<Resource<Map<URI, MapDataResource>>>() {

    private val executor: Executor
    private val repositories: Array<out MapDataRepository>
    private val providers: Array<out MapDataProvider>
    private val lifecycle = LifecycleRegistry(this)
    private val nextChangeForRepository = HashMap<String, ResolveRepositoryChangeTask>()
    private val changeInProgressForRepository = HashMap<String, ResolveRepositoryChangeTask>()

    fun requireValue(): Resource<Map<URI, MapDataResource>> { return value!! }
    /**
     * Return a non-null map of resources, keyed by their [URIs][MapDataResource.uri].
     */
    var resources: Map<URI, MapDataResource> = emptyMap()
        private set
    /**
     * Return a non-null, mutable map of all layers from every [resource][resources], keyed by [layer URI][MapLayerDescriptor.layerUri].
     * Changes to the returned mutable map have no effect on this [MapDataManager]'s layers and resources.
     */
    val layers: MutableMap<URI, MapLayerDescriptor> get() = resources.values.flatMap({ it.layers.values }).associateBy({ it.layerUri }).toMutableMap()

    init {
        repositories = config.repositories!!
        providers = config.providers!!
        executor = config.executor!!
        lifecycle.markState(Lifecycle.State.RESUMED)
        value = BasicResource.success(emptyMap())
        for (repo in repositories) {
            repo.observe(this, RepositoryObserver(repo))
        }
    }

    fun resourceForLayer(layer: MapLayerDescriptor): MapDataResource? {
        return resources[layer.resourceUri]
    }

    /**
     * Attempt to import the data the given resource URI references.  If a potential path to importing the data
     * exists, begin the asynchronous import process.
     *
     * @return true if an async import operation will begin, false if the resource cannot be imported
     */
    fun tryImportResource(resourceUri: URI): Boolean {
        for (repo in repositories) {
            if (repo.ownsResource(resourceUri)) {
                repo.refreshAvailableMapData(resourcesForRepo(repo), executor)
                return true
            }
        }
        return false
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycle
    }

    /**
     * Discover new resources available in known [locations][MapDataRepository], then remove defunct resources.
     * One or more asynchronous notifications to [observers][observe] will result as data from the various
     * [repositories][MapDataRepository] loads and [resolves][MapDataProvider.resolveResource].
     */
    fun refreshMapData() {
        for (repo in repositories) {
            if (repo.value?.status != Resource.Status.Loading && !changeInProgressForRepository.containsKey(repo.id)) {
                repo.refreshAvailableMapData(resourcesForRepo(repo), executor)
            }
        }
    }

    fun createMapLayerManager(map: GoogleMap): MapLayerManager {
        return MapLayerManager(this, Arrays.asList(*providers), map)
    }

    private fun resourcesForRepo(repo: MapDataRepository): Map<URI, MapDataResource> {
        return resources.filter { it.value.repositoryId == repo.id }
    }

    private fun onMapDataChanged(resource: Resource<Set<MapDataResource>>?, source: MapDataRepository) {
        val existing = HashMap<URI, MapDataResource>()
        resources = resources.minusIf({ _, value ->
            value.repositoryId == source.id
        }, existing)
        if (resource?.status == Resource.Status.Loading && (existing.isNotEmpty() || requireValue().status != Resource.Status.Loading)) {
            value = BasicResource.loading(resources)
            return
        }
        if (resource?.content?.isEmpty() != false && existing.isEmpty() && requireValue().status == Resource.Status.Loading) {
            if (repositories.none { it.value?.status == Resource.Status.Loading }) {
                value = BasicResource(resources, resource?.status ?: Resource.Status.Success)
            }
            return
        }
        val change = resource!!.content!!
        val resolved = HashMap<URI, MapDataResource>(change.size)
        val unresolved = HashMap<URI, MapDataResource>(change.size)
        change.forEach {
            if (it.resolved != null) {
                resolved[it.uri] = it
            }
            else {
                unresolved[it.uri] = it
            }
        }
        resources += resolved
        var nextStatus = Resource.Status.Success
        if (resource.status == Resource.Status.Loading ||
            unresolved.isNotEmpty() ||
            repositories.any { it.value?.status == Resource.Status.Loading }) {
            nextStatus = Resource.Status.Loading
        }

        value = BasicResource(resources, nextStatus)

//        nextChangeForRepository[source.id] = ResolveRepositoryChangeTask(source, resource ?:
//            BasicResource(emptySet(), Resource.Status.Error, Resource.Status.Error.ordinal, "no map data for repository"))
//        val changeInProgress = changeInProgressForRepository[source.id]
//        if (changeInProgress == null) {
//            beginNextChangeForRepository(source)
//        }
//        else {
//            changeInProgress.cancel(false)
//        }
    }

    private fun beginNextChangeForRepository(source: MapDataRepository, changeInProgress: ResolveRepositoryChangeTask? = null) {
        val change = nextChangeForRepository.remove(source.id)!!
        changeInProgressForRepository[source.id] = change
        if (changeInProgress != null) {
            change.updateProgressFromCancelledChange(changeInProgress)
        }
        if (change.toResolve.isEmpty()) {
            finishChangeInProgressForRepository(source)
        }
        else {
            change.executeOnExecutor(executor)
        }
    }

    private fun onResolveFinished(change: ResolveRepositoryChangeTask) {
        val changeInProgress = changeInProgressForRepository.remove(change.repository.id)
        if (change !== changeInProgress) {
            throw IllegalStateException("finished repository change mismatch: expected\n  $changeInProgress\n  but finishing change is\n  $change")
        }
        if (change.isCancelled) {
            beginNextChangeForRepository(change.repository, change)
        }
        else {
            val result = change.get()
            result.repository.onExternallyResolved(HashSet(result.resolved.values))
        }
    }

    private fun finishChangeInProgressForRepository(repo: MapDataRepository) {
        val change = changeInProgressForRepository.remove(repo.id)!!
        val result = change.result
        val added = HashMap<URI, MapDataResource>()
        val updated = HashMap<URI, MapDataResource>()
        val removed = result.oldResources.toMutableMap()
        for (value in result.resolved.values) {
            val oldValue = removed.remove(value.uri)
            if (oldValue == null) {
                added[value.uri] = value
            }
            else if (value.contentTimestamp > oldValue.contentTimestamp) {
                updated[value.uri] = value
            }
        }
        // TODO: update resources and set LiveData value
    }

    class Config {

        var context: Application? = null
            private set
        var repositories: Array<out MapDataRepository>? = emptyArray()
            private set
        var providers: Array<out MapDataProvider>? = emptyArray()
            private set
        var executor: Executor? = AsyncTask.THREAD_POOL_EXECUTOR
            private set

        fun context(x: Application): Config {
            context = x
            return this
        }

        fun repositories(vararg x: MapDataRepository): Config {
            repositories = x
            return this
        }

        fun providers(vararg x: MapDataProvider): Config {
            providers = x
            return this
        }

        fun executor(x: Executor): Config {
            executor = x
            return this
        }
    }

    private inner class RepositoryObserver internal constructor(private val repo: MapDataRepository) : Observer<Resource<Set<MapDataResource>>> {

        override fun onChanged(data: Resource<Set<MapDataResource>>?) {
            onMapDataChanged(data, repo)
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class ResolveRepositoryChangeTask internal constructor(
            val repository: MapDataRepository,
            data: Resource<Set<MapDataResource>>)
        : AsyncTask<Void, Pair<MapDataResource, MapDataResolveException?>, ResolveRepositoryChangeResult>() {

        internal val toResolve: SortedSet<MapDataResource> = TreeSet({ a, b -> a.uri.compareTo(b.uri) })
        internal var result: ResolveRepositoryChangeResult
        internal val stringValue: String

        init {
            val changedResources = data.content ?: emptySet()
            val oldResources = resourcesForRepo(repository)
            val resolved = HashMap<MapDataResource, MapDataResource>()
            for (resource in changedResources) {
                if (resource.resolved == null) {
                    toResolve.add(resource)
                }
                else {
                    resolved[resource] = resource
                }
            }
            result = ResolveRepositoryChangeResult(repository, oldResources, resolved)
            stringValue = "${javaClass.simpleName} repo ${repository.id} resolving\n  ${toResolve.map { it.uri }}"
        }


        @Throws(MapDataResolveException::class)
        private fun resolveWithFirstCapableProvider(resource: MapDataResource): MapDataResource {
            val uri = resource.uri
            if (uri.scheme.equals("file", ignoreCase = true)) {
                val resourceFile = File(uri)
                if (!resourceFile.canRead()) {
                    throw MapDataResolveException(uri, "resource file is not readable or does not exist: ${resourceFile.name}")
                }
            }
            val oldResource = result.oldResources[uri]
            if (oldResource != null && oldResource.contentTimestamp >= resource.contentTimestamp) {
                return resource.resolve(oldResource.resolved!!)
            }
            for (provider in providers) {
                if (provider.canHandleResource(resource)) {
                    val resolvedResource = provider.resolveResource(resource)
                    if (resolvedResource != null) {
                        return resolvedResource
                    }
                }
            }
            throw MapDataResolveException(uri, "no cache provider could handle resource $resource")
        }

        override fun doInBackground(vararg nothing: Void): ResolveRepositoryChangeResult {
            toResolve.iterator().run {
                while (hasNext() && !isCancelled) {
                    val unresolved = next()
                    try {
                        val resolved = resolveWithFirstCapableProvider(unresolved)
                        publishProgress(Pair(resolved, null))
                    }
                    catch (e: MapDataResolveException) {
                        publishProgress(Pair(unresolved, e))
                    }
                    remove()
                }
                return result
            }
        }

        override fun onCancelled(cancelledResult: ResolveRepositoryChangeResult?) {
            toResolve.iterator().run {
                while (hasNext()) {
                    val resource = next()
                    result.cancelled[resource] = resource
                    remove()
                }
            }
            onResolveFinished(this)
        }

        override fun onProgressUpdate(vararg values: Pair<MapDataResource, MapDataResolveException?>?) {
            val progress: Pair<MapDataResource, MapDataResolveException?> = values[0]!!
            if (progress.second == null) {
                result.resolved[progress.first] = progress.first
            }
            else {
                result.resolved.remove(progress.first)
                result.failed[progress.first] = progress.second!!
            }
        }

        override fun onPostExecute(result: ResolveRepositoryChangeResult) {
            onResolveFinished(this)
        }

        @MainThread
        fun updateProgressFromCancelledChange(cancelledChange: MapDataManager.ResolveRepositoryChangeTask) {
            if (status != Status.PENDING) {
                throw IllegalStateException("attempt to initialize progress from cancelled change after this change already began")
            }
            toResolve.iterator().run {
                while (hasNext()) {
                    val unresolvedResource = next()
                    val resolvedResource = cancelledChange.result.resolved[unresolvedResource]
                    if (resolvedResource != null && resolvedResource.contentTimestamp >= unresolvedResource.contentTimestamp) {
                        result.resolved[unresolvedResource] = unresolvedResource.resolve(resolvedResource.resolved!!)
                        remove()
                    }
                }
            }
        }

        override fun toString(): String {
            return stringValue
        }
    }



    private class ResolveRepositoryChangeResult internal constructor(
            val repository: MapDataRepository,
            val oldResources: Map<URI, MapDataResource>,
            val resolved: MutableMap<MapDataResource, MapDataResource>) {

        val cancelled = HashMap<MapDataResource, MapDataResource>()
        // TODO: propagate failed imports to user somehow
        val failed = HashMap<MapDataResource, MapDataResolveException>()
    }

    companion object {

        @JvmStatic
        var instance: MapDataManager? = null
            private set

        @Synchronized
        @JvmStatic
        fun initialize(config: Config) {
            if (MapDataManager.instance != null) {
                throw Error("attempt to initialize " + MapDataManager::class.java + " singleton more than once")
            }
            instance = MapDataManager(config)
        }
    }
}
