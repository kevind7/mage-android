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

    static class OnMap implements CacheOverlayOnMap {

        private final GoogleMap map;
        private final XYZDirectoryCacheOverlay cache;
        private TileOverlay overlay;

        OnMap(GoogleMap map, XYZDirectoryCacheOverlay cache) {
            this.map = map;
            this.cache = cache;
        }

        @Override
        public GoogleMap getMap() {
            return map;
        }

        @NonNull
        @Override
        public CacheOverlay getCacheOverlay() {
            return cache;
        }

        @NonNull
        @Override
        public CacheOverlayOnMap addToMapWithVisibility(boolean visibility) {
            TileProvider tileProvider = new FileSystemTileProvider(256, 256, cache.getDirectory().getAbsolutePath());
            TileOverlayOptions overlayOptions = new TileOverlayOptions().visible(visibility);
            overlayOptions.tileProvider(tileProvider);
            overlay = map.addTileOverlay(overlayOptions);
            return this;
        }

        @NonNull
        @Override
        public CacheOverlayOnMap removeFromMap() {
            overlay.remove();
            overlay = null;
            return this;
        }

        @NonNull
        @Override
        public CacheOverlayOnMap zoomMapToBoundingBox() {
            // TODO
            return this;
        }

        @NonNull
        @Override
        public CacheOverlayOnMap show() {
            if (overlay != null) {
                overlay.setVisible(true);
            }
            return this;
        }

        @NonNull
        @Override
        public CacheOverlayOnMap hide() {
            if (overlay != null) {
                overlay.setVisible(false);
            }
            return this;
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
    public CacheOverlayOnMap createOverlayOnMapFromCache(CacheOverlay cache, GoogleMap map) {
        // TODO
        return null;
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
