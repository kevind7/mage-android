package mil.nga.giat.mage.map.cache;

import android.support.annotation.MainThread;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@MainThread
public class OverlayOnMapManager implements CacheManager.CacheOverlaysUpdateListener {

    public interface OverlayOnMapListener {
        void overlaysChanged();
    }

    public abstract class OverlayOnMap {

        abstract protected void addToMapWithVisibility(boolean visible);
        abstract protected void removeFromMap();
        abstract protected void show();
        abstract protected void hide();
        abstract protected void zoomMapToBoundingBox();
        abstract protected boolean isOnMap();
        abstract protected boolean isVisible();
        // TODO: this is awkward passing the map view and returning a string; probably can do better
        abstract protected String onMapClick(LatLng latLng, MapView mapView);
    }

    private static String keyForCache(MapCache cache) {
        return cache.getName() + ":" + cache.getType().getName();
    }

    private static String keyForCache(CacheOverlay overlay) {
        return overlay.getCacheName() + ":" + overlay.getCacheType().getName();
    }

    private final CacheManager cacheManager;
    private final GoogleMap map;
    private final Map<Class<? extends CacheProvider>, CacheProvider> providers = new HashMap<>();
    private final Map<CacheOverlay, OverlayOnMap> overlaysOnMap = new HashMap<>();
    private final List<CacheOverlay> overlayOrder = new ArrayList<>();
    private final List<OverlayOnMapListener> listeners = new ArrayList<>();

    public OverlayOnMapManager(CacheManager cacheManager, List<CacheProvider> providers, GoogleMap map) {
        this.cacheManager = cacheManager;
        this.map = map;
        for (CacheProvider provider : providers) {
            this.providers.put(provider.getClass(), provider);
        }
        for (MapCache cache : cacheManager.getCaches()) {
            overlayOrder.addAll(cache.getCacheOverlays().values());
        }
        cacheManager.addUpdateListener(this);
    }

    @Override
    public void onCacheOverlaysUpdated(CacheManager.CacheOverlayUpdate update) {
        Set<String> removedCacheNames = new HashSet<>(update.removed.size());
        for (MapCache removed : update.removed) {
			removedCacheNames.add(removed.getName());
		}
		Map<String, Map<String, CacheOverlay>> updatedCaches = new HashMap<>(update.updated.size());
        for (MapCache cache : update.updated) {
            Map<String, CacheOverlay> updatedOverlays = new HashMap<>(cache.getCacheOverlays());
            updatedCaches.put(keyForCache(cache), updatedOverlays);
        }

        int position = 0;
        Iterator<CacheOverlay> orderIterator = overlayOrder.iterator();
        while (orderIterator.hasNext()) {
            CacheOverlay overlay = orderIterator.next();
            if (removedCacheNames.contains(overlay.getCacheName())) {
                removeFromMapReturningVisibility(overlay);
                orderIterator.remove();
                position--;
            }
            else {
                String cacheKey = keyForCache(overlay);
                Map<String, CacheOverlay> updatedCacheOverlays = updatedCaches.get(cacheKey);
                if (updatedCacheOverlays != null) {
                    CacheOverlay updatedOverlay = updatedCacheOverlays.remove(overlay.getOverlayName());
                    if (updatedOverlay != null) {
                        refreshOverlayAtPositionFromUpdatedCache(position, updatedOverlay);
                    }
                    else {
                        removeFromMapReturningVisibility(overlay);
                        orderIterator.remove();
                        position--;
                    }
                }
            }
            position++;
        }

        for (Map<String, CacheOverlay> newOverlaysFromUpdatedCaches : updatedCaches.values()) {
            overlayOrder.addAll(newOverlaysFromUpdatedCaches.values());
        }

        for (MapCache added : update.added) {
            overlayOrder.addAll(added.getCacheOverlays().values());
        }

        for (OverlayOnMapListener listener : listeners) {
		    listener.overlaysChanged();
        }
    }

    public void addOverlayOnMapListener(OverlayOnMapListener x) {
        listeners.add(x);
    }

    public void removeOverlayOnMapListener(OverlayOnMapListener x) {
        listeners.remove(x);
    }

    public GoogleMap getMap() {
        return map;
    }

    public List<CacheOverlay> getOverlays() {
        return overlayOrder;
    }

    public void showOverlay(CacheOverlay cacheOverlay) {
        addOverlayToMap(cacheOverlay, true);
    }

    public void hideOverlay(CacheOverlay cacheOverlay) {
        OverlayOnMap onMap = overlaysOnMap.get(cacheOverlay);
        if (onMap == null || !onMap.isVisible()) {
            return;
        }
        onMap.hide();
    }

    public boolean isOverlayVisible(CacheOverlay cacheOverlay) {
        OverlayOnMap onMap = overlaysOnMap.get(cacheOverlay);
        return onMap != null && onMap.isVisible();
    }

    public void onMapClick(LatLng latLng, MapView mapView) {
        for (CacheOverlay overlay : overlayOrder) {
            OverlayOnMap onMap = overlaysOnMap.get(overlay);
            if (onMap != null) {
                onMap.onMapClick(latLng, mapView);
            }
        }
    }

    public void zoomToOverlay(CacheOverlay cacheOverlay) {
        addOverlayToMap(cacheOverlay, true);
        OverlayOnMap onMap = overlaysOnMap.get(cacheOverlay);
        onMap.zoomMapToBoundingBox();
    }

    public void dispose() {
        // TODO: remove and dispose all overlays/notify providers
        cacheManager.removeUpdateListener(this);
    }

    private boolean removeFromMapReturningVisibility(CacheOverlay overlay) {
        boolean wasVisible = false;
        OverlayOnMap onMap = overlaysOnMap.remove(overlay);
        if (onMap != null) {
            wasVisible = onMap.isVisible();
            onMap.removeFromMap();
        }
        return wasVisible;
    }

    private void refreshOverlayAtPositionFromUpdatedCache(int position, CacheOverlay updatedOverlay) {
        CacheOverlay currentOverlay = overlayOrder.get(position);
        if (currentOverlay == updatedOverlay) {
            return;
        }
        overlayOrder.set(position, updatedOverlay);
        if (removeFromMapReturningVisibility(currentOverlay)) {
            addOverlayToMap(updatedOverlay, true);
        }
    }

    private void disposeOverlay(CacheOverlay overlay) {

    }

    private void addOverlayToMap(CacheOverlay cacheOverlay, boolean visible) {
        OverlayOnMap onMap = overlaysOnMap.remove(cacheOverlay);
        if (onMap == null) {
            CacheProvider provider = providers.get(cacheOverlay.getCacheType());
            onMap = provider.createOverlayOnMapFromCache(cacheOverlay, this);
        }
        overlaysOnMap.put(cacheOverlay, onMap);

        if (onMap.isOnMap()) {
            onMap.show();
        }
        else {
            onMap.addToMapWithVisibility(visible);
        }
    }
}
