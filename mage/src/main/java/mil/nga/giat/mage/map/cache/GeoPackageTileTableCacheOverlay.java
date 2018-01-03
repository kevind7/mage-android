package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.util.ArrayList;
import java.util.List;

import mil.nga.geopackage.extension.link.FeatureTileTableLinker;
import mil.nga.geopackage.features.index.FeatureIndexManager;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.tiles.features.FeatureTiles;
import mil.nga.geopackage.tiles.features.MapFeatureTiles;
import mil.nga.geopackage.tiles.overlay.BoundedOverlay;
import mil.nga.geopackage.tiles.overlay.FeatureOverlayQuery;
import mil.nga.geopackage.tiles.overlay.GeoPackageOverlayFactory;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.giat.mage.R;

/**
 * GeoPackage Tile Table cache overlay
 *
 * @author osbornb
 */
public class GeoPackageTileTableCacheOverlay extends GeoPackageTableCacheOverlay {

    /**
     * Constructor
     *
     * @param name       overlay name
     * @param geoPackage GeoPackage name
     * @param tableName  GeoPackage table name
     * @param count      count
     * @param minZoom    min zoom level
     * @param maxZoom    max zoom level
     */
    public GeoPackageTileTableCacheOverlay(String name, String geoPackage, String tableName, int count, int minZoom, int maxZoom) {
        super(name, geoPackage, tableName, count, minZoom, maxZoom);
    }

    @Override
    public Integer getIconImageResourceId() {
        return R.drawable.ic_layers_gray_24dp;
    }

    @Override
    public String getInfo() {
        return "tiles: " + getCount() + ", zoom: " + getMinZoom() + " - " + getMaxZoom();
    }
}
