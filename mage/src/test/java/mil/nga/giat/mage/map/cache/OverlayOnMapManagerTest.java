package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.common.cache.Cache;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class OverlayOnMapManagerTest implements CacheManager.CreateUpdatePermission {

    static class TestOverlayOnMap extends OverlayOnMapManager.OverlayOnMap {

        TestOverlayOnMap(OverlayOnMapManager manager) {
            manager.super();
        }

        @Override
        protected void addToMapWithVisibility(boolean visible) {

        }

        @Override
        protected void removeFromMap() {

        }

        @Override
        protected void show() {

        }

        @Override
        protected void hide() {

        }

        @Override
        protected void zoomMapToBoundingBox() {

        }

        @Override
        protected boolean isOnMap() {
            return false;
        }

        @Override
        protected boolean isVisible() {
            return false;
        }

        @Override
        protected String onMapClick(LatLng latLng, MapView mapView) {
            return null;
        }
    }

    abstract class Provider1 implements CacheProvider {};

    CacheManager cacheManager;
    Provider1 provider1;
    List<CacheProvider> providers;

    @Before
    public void setup() {

        provider1 = mock(Provider1.class);
        cacheManager = mock(CacheManager.class, withSettings().useConstructor(new CacheManager.Config().updatePermission(this)));
        providers = Collections.<CacheProvider>singletonList(provider1);
    }

    @Test
    public void listensToCacheManagerUpdates() {

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        verify(cacheManager).addUpdateListener(overlayManager);
    }

    @Test
    public void disposeStopsListeningToCacheManager() {

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);
        overlayManager.dispose();

        verify(cacheManager).removeUpdateListener(overlayManager);
    }

    @Test
    public void addsOverlaysFromAddedCaches() {

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);
        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("test overlay 1", "test cache", provider1.getClass());
        CacheOverlay overlay2 = new CacheOverlayTest.TestCacheOverlay1("test overlay 2", "test cache", provider1.getClass());
        MapCache mapCache = new MapCache("test cache", provider1.getClass(), new File("test"), new HashSet<>(Arrays.asList(overlay1, overlay2)));
        Set<MapCache> added = Collections.singleton(mapCache);
        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(this, added, Collections.<MapCache>emptySet(), Collections.<MapCache>emptySet());

        when(provider1.createOverlayOnMapFromCache(overlay1, overlayManager)).thenReturn(new TestOverlayOnMap(overlayManager));
        when(provider1.createOverlayOnMapFromCache(overlay2, overlayManager)).thenReturn(new TestOverlayOnMap(overlayManager));

        overlayManager.onCacheOverlaysUpdated(update);

        assertThat(overlayManager.getOverlays().size(), is(2));
        assertThat(overlayManager.getOverlays(), hasItems(overlay1, overlay2));
    }

    @Test
    public void removesOverlaysFromRemovedCaches() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("test overlay 1", "test cache", provider1.getClass());
        CacheOverlay overlay2 = new CacheOverlayTest.TestCacheOverlay1("test overlay 2", "test cache", provider1.getClass());
        MapCache mapCache = new MapCache("test cache", provider1.getClass(), new File("test"), new HashSet<>(Arrays.asList(overlay1, overlay2)));

        when(cacheManager.getCaches()).thenReturn(Collections.singleton(mapCache));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        assertThat(overlayManager.getOverlays().size(), is(2));
        assertThat(overlayManager.getOverlays(), hasItems(overlay1, overlay2));

        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(this, Collections.<MapCache>emptySet(), Collections.<MapCache>emptySet(), Collections.singleton(mapCache));
        overlayManager.onCacheOverlaysUpdated(update);

        assertTrue(overlayManager.getOverlays().isEmpty());
    }

    @Test
    public void removesOverlaysFromUpdatedCaches() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("test overlay 1", "test cache", provider1.getClass());
        CacheOverlay overlay2 = new CacheOverlayTest.TestCacheOverlay1("test overlay 2", "test cache", provider1.getClass());
        MapCache mapCache = new MapCache("test cache", provider1.getClass(), new File("test"), new HashSet<>(Arrays.asList(overlay1, overlay2)));

        when(cacheManager.getCaches()).thenReturn(Collections.singleton(mapCache));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        List<CacheOverlay> overlays = overlayManager.getOverlays();
        assertThat(overlays.size(), is(2));
        assertThat(overlays, hasItems(overlay1, overlay2));

        overlay2 = new CacheOverlayTest.TestCacheOverlay1(overlay2.getOverlayName(), overlay2.getCacheName(), overlay2.getCacheType());
        mapCache = new MapCache(mapCache.getName(), mapCache.getType(), mapCache.getSourceFile(), Collections.singleton(overlay2));
        Set<MapCache> updated = Collections.singleton(mapCache);
        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(this,
            Collections.<MapCache>emptySet(), updated, Collections.<MapCache>emptySet());

        when(provider1.createOverlayOnMapFromCache(overlay2, overlayManager)).thenReturn(new TestOverlayOnMap(overlayManager));

        overlayManager.onCacheOverlaysUpdated(update);

        overlays = overlayManager.getOverlays();
        assertThat(overlays.size(), is(1));
        assertThat(overlays, hasItem(overlay2));
    }

    @Test
    public void addsOverlaysFromUpdatedCaches() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("test overlay 1", "test cache", provider1.getClass());
        MapCache cache = new MapCache("test cache", provider1.getClass(), null, Collections.singleton(overlay1));

        when(cacheManager.getCaches()).thenReturn(Collections.singleton(cache));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        assertThat(overlayManager.getOverlays().size(), is(1));
        assertThat(overlayManager.getOverlays(), hasItem(overlay1));


        CacheOverlay overlay2 = new CacheOverlayTest.TestCacheOverlay1("test overlay 2", "test cache", provider1.getClass());
        Set<CacheOverlay> overlays = new HashSet<>(Arrays.asList(overlay1, overlay2));
        cache = new MapCache(cache.getName(), cache.getType(), null, overlays);
        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(
            this, Collections.<MapCache>emptySet(), Collections.singleton(cache), Collections.<MapCache>emptySet());
        overlayManager.onCacheOverlaysUpdated(update);

        assertThat(overlayManager.getOverlays().size(), is(2));
        assertThat(overlayManager.getOverlays(), hasItems(overlay1, overlay2));
    }

    @Test
    public void addsAndRemovesOverlaysFromUpdatedCachesWhenOverlayCountIsUnchanged() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("overlay 1", "test cache", provider1.getClass());
        MapCache cache = new MapCache("test cache", provider1.getClass(), null, Collections.singleton(overlay1));

        when(cacheManager.getCaches()).thenReturn(Collections.singleton(cache));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        assertThat(overlayManager.getOverlays().size(), is(1));
        assertThat(overlayManager.getOverlays(), hasItem(overlay1));

        CacheOverlay overlay2 = new CacheOverlayTest.TestCacheOverlay1("overlay 2", "test cache", provider1.getClass());
        cache = new MapCache(cache.getName(), cache.getType(), null, Collections.singleton(overlay2));
        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(
            this, Collections.<MapCache>emptySet(), Collections.singleton(cache), Collections.<MapCache>emptySet());
        overlayManager.onCacheOverlaysUpdated(update);

        assertThat(overlayManager.getOverlays().size(), is(1));
        assertThat(overlayManager.getOverlays(), hasItems(overlay2));
    }

    @Test
    public void replacesLikeOverlaysFromUpdatedCaches() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("overlay 1", "test cache", provider1.getClass());
        MapCache cache = new MapCache("test cache", provider1.getClass(), null, Collections.singleton(overlay1));

        when(cacheManager.getCaches()).thenReturn(Collections.singleton(cache));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        assertThat(overlayManager.getOverlays().size(), is(1));
        assertThat(overlayManager.getOverlays(), hasItem(overlay1));

        CacheOverlay overlay1Updated = new CacheOverlayTest.TestCacheOverlay1(overlay1.getOverlayName(), overlay1.getCacheName(), overlay1.getCacheType());
        cache = new MapCache(cache.getName(), cache.getType(), null, Collections.singleton(overlay1Updated));
        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(
            this, Collections.<MapCache>emptySet(), Collections.singleton(cache), Collections.<MapCache>emptySet());
        overlayManager.onCacheOverlaysUpdated(update);

        assertThat(overlayManager.getOverlays().size(), is(1));
        assertThat(overlayManager.getOverlays(), hasItem(overlay1));
        assertThat(overlayManager.getOverlays(), not(hasItem(sameInstance(overlay1))));
        assertThat(overlayManager.getOverlays(), hasItem(sameInstance(overlay1Updated)));
    }

    @Test
    public void doesNotUnnecessarilyRecreateOverlaysFromUpdatedCaches() {
        fail("unimplemented");
    }

    @Test
    public void maintainsOrderOfUpdatedCacheOverlays() {
        fail("unimplemented");
    }

    @Test
    public void behavesWhenTwoCachesHaveOverlaysWithTheSameName() {
        fail("unimplemented");
    }
}
