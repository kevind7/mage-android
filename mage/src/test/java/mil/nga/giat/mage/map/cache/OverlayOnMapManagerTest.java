package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.security.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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
        MapCache mapCache = new MapCache("test cache", Provider1.class, new File("test"), new HashSet<>(Arrays.asList(overlay1, overlay2)));
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
        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);
        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("test overlay 1", "test cache", provider1.getClass());
        CacheOverlay overlay2 = new CacheOverlayTest.TestCacheOverlay1("test overlay 2", "test cache", provider1.getClass());
        MapCache mapCache = new MapCache("test cache", Provider1.class, new File("test"), new HashSet<>(Arrays.asList(overlay1, overlay2)));
        Set<MapCache> added = Collections.singleton(mapCache);
        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(this, added, Collections.<MapCache>emptySet(), Collections.<MapCache>emptySet());

        when(provider1.createOverlayOnMapFromCache(overlay1, overlayManager)).thenReturn(new TestOverlayOnMap(overlayManager));
        when(provider1.createOverlayOnMapFromCache(overlay2, overlayManager)).thenReturn(new TestOverlayOnMap(overlayManager));

        overlayManager.onCacheOverlaysUpdated(update);

        assertThat(overlayManager.getOverlays().size(), is(2));
        assertThat(overlayManager.getOverlays(), hasItems(overlay1, overlay2));

        update = cacheManager.new CacheOverlayUpdate(this, Collections.<MapCache>emptySet(), Collections.<MapCache>emptySet(), Collections.singleton(mapCache));

        overlayManager.onCacheOverlaysUpdated(update);

        assertTrue(overlayManager.getOverlays().isEmpty());
    }

    @Test
    public void removesOverlaysFromUpdatedCaches() {
        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);
        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("test overlay 1", "test cache", Provider1.class);
        CacheOverlay overlay2 = new CacheOverlayTest.TestCacheOverlay1("test overlay 2", "test cache", Provider1.class);
        MapCache mapCache = new MapCache("test cache", Provider1.class, new File("test"), new HashSet<>(Arrays.asList(overlay1, overlay2)));
        Set<MapCache> added = Collections.singleton(mapCache);
        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(this,
            added, Collections.<MapCache>emptySet(), Collections.<MapCache>emptySet());

        when(provider1.createOverlayOnMapFromCache(overlay1, overlayManager)).thenReturn(new TestOverlayOnMap(overlayManager));
        when(provider1.createOverlayOnMapFromCache(overlay2, overlayManager)).thenReturn(new TestOverlayOnMap(overlayManager));

        overlayManager.onCacheOverlaysUpdated(update);

        List<CacheOverlay> overlays = overlayManager.getOverlays();
        assertThat(overlays.size(), is(2));
        assertThat(overlays, hasItems(overlay1, overlay2));

        overlay2 = new CacheOverlayTest.TestCacheOverlay1(overlay2.getOverlayName(), overlay2.getCacheName(), overlay2.getCacheType());
        mapCache = new MapCache(mapCache.getName(), mapCache.getType(), mapCache.getSourceFile(), Collections.singleton(overlay2));
        Set<MapCache> updated = Collections.singleton(mapCache);
        update = cacheManager.new CacheOverlayUpdate(this,
            Collections.<MapCache>emptySet(), updated, Collections.<MapCache>emptySet());

        when(provider1.createOverlayOnMapFromCache(overlay2, overlayManager)).thenReturn(new TestOverlayOnMap(overlayManager));

        overlayManager.onCacheOverlaysUpdated(update);

        overlays = overlayManager.getOverlays();
        assertThat(overlays.size(), is(1));
        assertThat(overlays, hasItem(overlay2));
    }

    @Test
    public void addsOverlaysFromUpdatedCaches() {

    }

    @Test
    public void replacesOverlaysFromUpdatedCaches() {

    }

    @Test
    public void doesNotUnnecessarilyRemoveAndRecreateOverlaysFromUpdatedCaches() {

    }

    @Test
    public void maintainsOrderOfUpdatedCacheOverlays() {

    }

    @Test
    public void behavesWhenToCachesHaveOverlaysWithTheSameName() {

    }
}
