package mil.nga.giat.mage.map.cache;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import mil.nga.giat.mage.map.FileSystemTileProvider;

public class XYZDirectoryCacheProvider implements CacheProvider {

    static class OnMap extends OverlayOnMapManager.OverlayOnMap {

        private final GoogleMap map;
        private final XYZDirectoryCacheOverlay cache;
        private TileOverlay overlay;

        OnMap(OverlayOnMapManager manager, XYZDirectoryCacheOverlay cache) {
            manager.super();
            this.map = manager.getMap();
            this.cache = cache;
        }

        @NonNull
        @Override
        public void addToMapWithVisibility(boolean visibility) {
            TileProvider tileProvider = new FileSystemTileProvider(256, 256, cache.getDirectory().getAbsolutePath());
            TileOverlayOptions overlayOptions = new TileOverlayOptions().visible(visibility);
            overlayOptions.tileProvider(tileProvider);
            overlay = map.addTileOverlay(overlayOptions);
        }

        @NonNull
        @Override
        public void removeFromMap() {
            overlay.remove();
            overlay = null;
        }

        @NonNull
        @Override
        public void zoomMapToBoundingBox() {
            // TODO
        }

        @NonNull
        @Override
        public void show() {
            if (overlay != null) {
                overlay.setVisible(true);
            }
        }

        @NonNull
        @Override
        public void hide() {
            if (overlay != null) {
                overlay.setVisible(false);
            }
        }

        @Nullable
        @Override
        public String onMapClick(LatLng latLng, MapView mapView) {
            return null;
        }

        @Override
        public boolean isOnMap() {
            return overlay != null;
        }

        @Override
        public boolean isVisible() {
            return overlay.isVisible();
        }
    }

    @Override
    public boolean isCacheFile(File cacheFile) {
        return cacheFile.isDirectory();
    }

    @Override
    public MapCache importCacheFromFile(File cacheFile) throws CacheImportException {
        if (!cacheFile.isDirectory()) {
            throw new CacheImportException(cacheFile, "cache file is not a directory: " + cacheFile.getName());
        }
        Set<CacheOverlay> overlays = new HashSet<>();
        overlays.add(new XYZDirectoryCacheOverlay(cacheFile.getName(), cacheFile.getName(), cacheFile));
        return new MapCache(cacheFile.getName(), getClass(), cacheFile, Collections.unmodifiableSet(overlays));
    }

    @Override
    public Set<MapCache> refreshCaches(Set<MapCache> existingCaches) {
        // TODO
        return Collections.emptySet();
    }

    @Override
    public OverlayOnMapManager.OverlayOnMap createOverlayOnMapFromCache(CacheOverlay cache, OverlayOnMapManager mapManager) {
        return new OnMap(mapManager, (XYZDirectoryCacheOverlay) cache);
    }

    /**
     * TODO: this was originally in TileOverlayPreferenceActivity - delete should be function of the provider
     */
    private void deleteXYZCacheOverlay(XYZDirectoryCacheOverlay xyzCacheOverlay){

        File directory = xyzCacheOverlay.getDirectory();

        if (directory.canWrite()) {
            deleteFile(directory);
        }
    }

    private void deleteFile(File base) {
        if (base.isDirectory()) {
            for (File file : base.listFiles()) {
                deleteFile(file);
            }
        }
        base.delete();
    }
}
