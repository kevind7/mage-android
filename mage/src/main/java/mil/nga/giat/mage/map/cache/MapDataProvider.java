package mil.nga.giat.mage.map.cache;

import java.net.URI;
import java.util.Set;

/**
 * A MapDataProvider represents a specific cache data format that can put overlays on a map.
 *
 * TODO: thread-safety coniderations - {@link MapDataManager} for now only invokes these methods serially
 * across all providers, but could be otherwise
 */
public interface MapDataProvider {

    /**
     * Does this provider recognize the given file as its type of cache?
     *
     * @param cacheFile
     * @return
     */
    boolean isCacheFile(URI cacheFile);

    /**
     * Attempt to import the given file as this provider's type of cache and add
     * it to the set of available caches.
     *
     * @param cacheFile
     * @return
     * @throws CacheImportException
     */
    MapDataResource importCacheFromFile(URI cacheFile) throws CacheImportException;

    /**
     * Refresh the data in the given set of caches.  Return a new subset of the
     * given set with new {@link MapLayerDescriptor} instances for updated caches, the
     * same instances for unchanged caches, and without instances whose data is
     * no longer available, such as that on a removed SD card.
     *
     * @param existingCaches a set of caches to refresh
     * @return a subset (possibly equal) to the given cache set
     */
    Set<MapDataResource> refreshCaches(Set<MapDataResource> existingCaches);


    OverlayOnMapManager.OverlayOnMap createOverlayOnMapFromCache(MapLayerDescriptor cache, OverlayOnMapManager map);
}