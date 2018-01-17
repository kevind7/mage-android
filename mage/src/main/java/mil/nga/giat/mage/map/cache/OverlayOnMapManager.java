package mil.nga.giat.mage.map.cache;

import android.support.annotation.MainThread;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@MainThread
public class OverlayOnMapManager implements CacheManager.CacheOverlaysUpdateListener {

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

    private final CacheManager cacheManager;
    private final GoogleMap map;
    private final Map<Class<? extends CacheProvider>, CacheProvider> providers = new HashMap<>();
    private final Map<CacheOverlay, OverlayOnMap> overlaysOnMap = new HashMap<>();
    private final List<CacheOverlay> overlayOrder = new ArrayList<>();

    public OverlayOnMapManager(CacheManager cacheManager, List<CacheProvider> providers, GoogleMap map) {
        this.cacheManager = cacheManager;
        this.map = map;
        for (CacheProvider provider : providers) {
            this.providers.put(provider.getClass(), provider);
        }
    }

    @Override
    public void onCacheOverlaysUpdated(CacheManager.CacheOverlayUpdate update) {
        for (MapCache removed : update.removed) {
			for (CacheOverlay cacheOverlay : removed.getCacheOverlays()) {
		        overlayOrder.remove(cacheOverlay);
				OverlayOnMap onMap = overlaysOnMap.remove(cacheOverlay);
				if (onMap != null) {
					onMap.removeFromMap();
				}
			}
		}

		Set<CacheOverlay> updatedOnMap = new HashSet<>(overlaysOnMap.size());
		for (MapCache updated : update.updated) {
			for (CacheOverlay cacheOverlay : updated.getCacheOverlays()) {
				if (overlaysOnMap.containsKey(cacheOverlay)) {
					updatedOnMap.add(cacheOverlay);
				}
			}
		}
		for (CacheOverlay cacheOverlay : updatedOnMap) {
			OverlayOnMap onMap = overlaysOnMap.remove(cacheOverlay);
			boolean visible = onMap.isVisible();
			onMap.removeFromMap();
			addOverlayToMap(cacheOverlay, visible);
		}

        for (MapCache added : update.added) {
            for (CacheOverlay cacheOverlay : added.getCacheOverlays()) {
                // TODO: create by default or lazy?
                addOverlayToMap(cacheOverlay, false);
            }
        }
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

    private void addOverlayToMap(CacheOverlay cacheOverlay, boolean visible) {
        OverlayOnMap onMap = overlaysOnMap.get(cacheOverlay);
        if (onMap == null) {
            CacheProvider provider = providers.get(cacheOverlay.getCacheType());
            onMap = provider.createOverlayOnMapFromCache(cacheOverlay, this);
            overlaysOnMap.put(cacheOverlay, onMap);
        }
        if (onMap.isOnMap()) {
            onMap.show();
        }
        else {
            onMap.addToMapWithVisibility(visible);
        }
    }
}
