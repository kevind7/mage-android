package mil.nga.giat.mage.map.cache;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import mil.nga.giat.mage.data.Resource;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeature;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureHelper;
import mil.nga.giat.mage.sdk.datastore.staticfeature.StaticFeatureProperty;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.exceptions.LayerException;
import mil.nga.giat.mage.sdk.exceptions.StaticFeatureException;
import mil.nga.giat.mage.sdk.http.resource.LayerResource;
import mil.nga.giat.mage.test.AsyncTesting;
import mil.nga.wkb.geom.Point;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class StaticFeatureLayerRepositoryTest {

    private static class TestLayer extends Layer {

        private Long id;

        TestLayer(String remoteId, String type, String name, Event event) {
            super(remoteId, type, name, event);
        }

        public TestLayer setId(Long x) {
            id = x;
            return this;
        }

        public Long getId() {
            return id;
        }
    }

    private static long oneSecond() {
        return 300000;
    }

    @Rule
    public TestName testName = new TestName();

    @Rule
    public TemporaryFolder iconsDirRule = new TemporaryFolder();

    @Rule
    public AsyncTesting.MainLooperAssertion onMainThread = new AsyncTesting.MainLooperAssertion();

    private Event currentEvent;
    private EventHelper eventHelper;
    private LayerHelper layerHelper;
    private StaticFeatureHelper featureHelper;
    private LayerResource layerService;
    private File iconsDir;
    private StaticFeatureLayerRepository.NetworkCondition network;
    private StaticFeatureLayerRepository repo;
    private LifecycleOwner observerLifecycle;
    private Observer<Set<MapDataResource>> observer;
    private ThreadPoolExecutor executor;

    @Before
    public void setupRepo() throws IOException {
        currentEvent = new Event(testName.getMethodName(), testName.getMethodName(), testName.getMethodName(), "", "");
        eventHelper = mock(EventHelper.class);
        layerHelper = mock(LayerHelper.class);
        featureHelper = mock(StaticFeatureHelper.class);
        layerService = mock(LayerResource.class);
        iconsDir = iconsDirRule.newFolder("icons");
        network = mock(StaticFeatureLayerRepository.NetworkCondition.class);
        repo = new StaticFeatureLayerRepository(eventHelper, layerHelper, featureHelper, layerService, iconsDir, network);
        observerLifecycle = new LifecycleOwner() {
            private Lifecycle myLifecycle = new LifecycleRegistry(this);
            @NonNull
            public Lifecycle getLifecycle() {
                return myLifecycle;
            }
        };
        observer = mock(Observer.class);
        executor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(16));

        when(network.isConnected()).thenReturn(true);
        when(eventHelper.getCurrentEvent()).thenReturn(currentEvent);
    }

    @After
    public void awaitThreadPoolTermination() throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(oneSecond(), TimeUnit.MILLISECONDS)) {
            fail("timed out waiting for thread pool to shutdown");
        }
    }

    @Test
    public void ownsTheMageStaticFeatureLayersUri() {
        URI uri = URI.create("mage:/current_event/layers");
        assertTrue(repo.ownsResource(uri));
    }

    @Test
    public void doesNotOwnOtherUris() {
        for (String uri : Arrays.asList("mage:/layers", "mage:/current_event/layer", "mage:/current-event/layers", "nage:/current_event/layers", "http://mage/current_event/layers")) {
            assertFalse(uri, repo.ownsResource(URI.create(uri)));
        }
    }

    @Test
    public void initialStatusIsSuccess() {
        assertThat(repo.getStatus(), is(Resource.Status.Success));
        assertThat(repo.getStatusCode(), is(Resource.Status.Success.ordinal()));
        assertThat(repo.getStatusMessage(), is(Resource.Status.Success.toString()));
    }

    @Test
    public void fetchesCurrentEventLayersFromServer() throws IOException, InterruptedException {

        when(layerService.getLayers(currentEvent)).thenReturn(Collections.emptySet());

        AsyncTesting.waitForMainThreadToRun(() -> {
            repo.refreshAvailableMapData(emptyMap(), executor);
            assertThat(repo.getStatus(), is(Resource.Status.Loading));
        });

        onMainThread.assertThatWithin(oneSecond(), repo::getStatus, is(Resource.Status.Success));

        verify(layerService).getLayers(currentEvent);
    }

    @Test
    public void fetchesFeaturesForFetchedLayers() throws IOException, InterruptedException {

        Layer layer = new Layer("1", "test", "test1", currentEvent);
        when(layerService.getLayers(currentEvent)).thenReturn(Collections.singleton(layer));

        AsyncTesting.waitForMainThreadToRun(() -> {
            repo.refreshAvailableMapData(emptyMap(), executor);
            assertThat(repo.getStatus(), is(Resource.Status.Loading));
        });

        onMainThread.assertThatWithin(oneSecond(), repo::getStatus, is(Resource.Status.Success));

        awaitThreadPoolTermination();

        InOrder fetchOrder = inOrder(layerService);
        fetchOrder.verify(layerService).getLayers(currentEvent);
        fetchOrder.verify(layerService).getFeatures(layer);
    }

    @Test
    public void savesFetchedLayersWithFeaturesAfterFetchingFeatures() throws IOException, InterruptedException, LayerException {

        Layer layer = new Layer("1", "test", "test1", currentEvent);
        when(layerService.getLayers(currentEvent)).thenReturn(Collections.singleton(layer));
        Collection<StaticFeature> features = Collections.singleton(
            new StaticFeature("1.1", new Point(1, 2), layer));
        when(layerService.getFeatures(layer)).thenReturn(features);
        when(layerHelper.create(layer)).thenReturn(layer);
        when(featureHelper.createAll(features, layer)).then((invocation) -> {
            layer.setLoaded(true);
            return layer;
        });

        AsyncTesting.waitForMainThreadToRun(() -> {
            repo.refreshAvailableMapData(emptyMap(), executor);
            assertThat(repo.getStatus(), is(Resource.Status.Loading));
        });

        onMainThread.assertThatWithin(oneSecond(), repo::getStatus, is(Resource.Status.Success));

        awaitThreadPoolTermination();

        InOrder saveOrder = inOrder(featureHelper, layerHelper);
        saveOrder.verify(layerHelper).delete(layer.getId());
        saveOrder.verify(layerHelper).create(layer);
        saveOrder.verify(featureHelper).createAll(features, layer);
        saveOrder.verify(layerHelper).update(layer);
        assertTrue(layer.isLoaded());

        fail("need to verify layer.isLoaded() is true, probably by doing readAll() after refresh and using that to set the value of the repo LiveData");
    }

    @Test
    public void fetchesAndSavesIconFilesForFeaturesAfterSavingFetchedFeatures() throws IOException, InterruptedException, LayerException, StaticFeatureException {

        Layer layer = new Layer("1", "test", "test1", currentEvent);
        Layer createdLayer = new TestLayer("1", "test", "test1", currentEvent).setId(1234L);
        StaticFeature feature = new StaticFeature("1.1", new Point(1, 2), layer);
        String iconUrl = "http://test.mage/icons/test/point.png";
        StaticFeatureProperty iconProperty = new StaticFeatureProperty(StaticFeatureLayerRepository.PROP_ICON_URL, iconUrl);
        feature.getProperties().add(iconProperty);
        List<StaticFeature> features = Collections.singletonList(feature);
        ByteArrayInputStream iconBytes = new ByteArrayInputStream("test icon".getBytes());

        when(layerService.getLayers(currentEvent)).thenReturn(Collections.singleton(layer));
        when(layerService.getFeatures(layer)).thenReturn(features);
        when(layerService.getFeatureIcon(iconUrl)).thenReturn(iconBytes);
        when(layerHelper.create(layer)).thenReturn(createdLayer);
        when(featureHelper.createAll(features, createdLayer)).thenReturn(createdLayer);
        when(featureHelper.readAll(createdLayer.getId())).thenReturn(features);

        AsyncTesting.waitForMainThreadToRun(() -> {
            repo.refreshAvailableMapData(emptyMap(), executor);
            assertThat(repo.getStatus(), is(Resource.Status.Loading));
        });

        onMainThread.assertThatWithin(oneSecond(), repo::getStatus, is(Resource.Status.Success));

        awaitThreadPoolTermination();

        verify(layerHelper, never()).delete(any());
        InOrder fetchOrder = inOrder(layerHelper, featureHelper, layerService);
        fetchOrder.verify(layerHelper).create(layer);
        fetchOrder.verify(featureHelper).createAll(features, createdLayer);
        fetchOrder.verify(layerService).getFeatureIcon(iconUrl);
        File iconFile = new File(iconsDir, "icons/test/point.png");
        assertTrue(iconFile.exists());
        FileReader reader = new FileReader(iconFile);
        char[] content = new char[9];
        reader.read(content);
        assertThat(reader.read(), is(-1));
        assertThat(String.valueOf(content), is("test icon"));
    }

    @Test
    public void doesNotFetchSameIconUrlTwiceAfterSavingIcon() {
        fail("unimplemented");
    }

    @Test
    public void savesFetchedIconsToTheFileSystem() {
        fail("unimplemented");
    }

    @Test
    public void doesNotAttemptToFetchWhenOffline() {
        fail("unimplemented");
    }

    @Test
    public void doesNotDeleteLayersIfLayerFetchFailed() {
        fail("unimplemented");
    }

    @Test
    public void doesNotDeleteLayersIfOffline() {
        fail("unimplemented");
    }

    @Test
    public void finishesSyncInProgressIfCurrentEventChanges() {
        fail("unimplemented");
    }

    @Test
    public void handlesRejectedExecutionOnExecutor() {
        fail("unimplemented");
    }

    @Test
    public void finishesProperlyWhenTheLastLayerSyncFinishesButIsStillResolvingIconsForPreviousLayer() {
        fail("unimplemented");
    }
}