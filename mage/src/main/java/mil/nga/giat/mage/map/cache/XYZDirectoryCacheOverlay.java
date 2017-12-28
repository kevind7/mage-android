package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

import java.io.File;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.map.FileSystemTileProvider;

/**
 * XYZ Directory of tiles cache overlay
 *
 * @author osbornb
 */
public class XYZDirectoryCacheOverlay extends CacheOverlay {


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
            return null;
        }

        @Override
        public CacheOverlayOnMap addToMap() {
            TileProvider tileProvider = new FileSystemTileProvider(256, 256, cache.getDirectory().getAbsolutePath());
            TileOverlayOptions overlayOptions = new TileOverlayOptions();
            overlayOptions.tileProvider(tileProvider);
            // TODO: z-index
//            overlayOptions.zIndex(zIndex);
            overlay = map.addTileOverlay(overlayOptions);
            return this;
        }

        @Override
        public CacheOverlayOnMap removeFromMap() {
            // TODO: should there be setVisible()/isVisible() to retain the TileOverlay instance?
            overlay.remove();
            overlay = null;
            return this;
        }

        @Override
        public CacheOverlayOnMap zoomMapToBoundingBox() {
            return null;
        }

        @Override
        public boolean isEnabled() {
            return overlay != null;
        }
    }


    /**
     * Tile directory
     */
    private File directory;

    /**
     * Constructor
     *
     * @param name      cache name
     * @param directory tile directory
     */
    public XYZDirectoryCacheOverlay(String name, File directory) {
        super(XYZDirectoryCacheProvider.class, name, false);
        this.directory = directory;
    }

    @Override
    public CacheOverlayOnMap createOverlayOnMap(GoogleMap map) {
        return new OnMap(map, this);
    }

    @Override
    public void removeFromMap() {
    }

    @Override
    public Integer getIconImageResourceId() {
        return R.drawable.ic_layers_gray_24dp;
    }

    /**
     * Get the directory
     *
     * @return
     */
    public File getDirectory() {
        return directory;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof XYZDirectoryCacheOverlay && getDirectory().equals(((XYZDirectoryCacheOverlay) other).getDirectory());
    }

    @Override
    public int hashCode() {
        return getDirectory().hashCode();
    }

}
