package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.TileOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.nga.geopackage.geom.map.GoogleMapShape;
import mil.nga.geopackage.geom.map.GoogleMapShapeConverter;
import mil.nga.geopackage.tiles.overlay.FeatureOverlayQuery;
import mil.nga.giat.mage.R;
import mil.nga.wkb.geom.GeometryType;

/**
 * GeoPackage Feature Table cache overlay
 *
 * @author osbornb
 */
public class GeoPackageFeatureTableCacheOverlay extends GeoPackageTableCacheOverlay {

    /**
     * Max zoom for features
     */
    static final int MAX_ZOOM = 21;

    /**
     * Indexed flag, true when the feature table is indexed
     */
    private final boolean indexed;

    /**
     * Linked tile table cache overlays
     */
    private final List<GeoPackageTileTableCacheOverlay> linkedTiles;

    /**
     * Constructor
     *
     * @param name         overlay name
     * @param geoPackage   GeoPackage name
     * @param tableName    GeoPackage table name
     * @param count        count
     * @param minZoom      min zoom level
     * @param indexed      indexed flag
     */
    GeoPackageFeatureTableCacheOverlay(String name, String geoPackage, String tableName, int count, int minZoom, boolean indexed, List<GeoPackageTileTableCacheOverlay> linkedTiles) {
        super(name, geoPackage, tableName, count, minZoom, MAX_ZOOM);
        this.indexed = indexed;
        this.linkedTiles = linkedTiles;
    }

    @Override
    public Integer getIconImageResourceId() {
        return R.drawable.ic_timeline_gray_24dp;
    }

    @Override
    public String getInfo() {
        int minZoom = getMinZoom();
        int maxZoom = getMaxZoom();
        for(GeoPackageTileTableCacheOverlay linkedTileTable: linkedTiles){
            minZoom = Math.min(minZoom, linkedTileTable.getMinZoom());
            maxZoom = Math.max(maxZoom, linkedTileTable.getMaxZoom());
        }
        return "features: " + getCount() + ", zoom: " + minZoom + " - " + maxZoom;
    }

    /**
     * Determine if the feature table is indexed
     *
     * @return true if indexed
     */
    public boolean isIndexed() {
        return indexed;
    }

    /**
     * Get the linked tile table cache overlays
     *
     * @return linked tile table cache overlays
     */
    public List<GeoPackageTileTableCacheOverlay> getLinkedTileTables(){
        return linkedTiles;
    }
}
