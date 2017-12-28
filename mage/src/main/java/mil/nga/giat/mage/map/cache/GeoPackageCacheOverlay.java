package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.GoogleMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mil.nga.giat.mage.R;

/**
 * GeoPackage file cache overlay
 *
 * @author osbornb
 */
public class GeoPackageCacheOverlay extends CacheOverlay {

    /**
     * Mapping between table cache names and the table cache overlays
     */
    private Map<String, CacheOverlay> tables = new LinkedHashMap<String, CacheOverlay>();

    /**
     * Constructor
     *
     * @param name   GeoPackage name
     * @param tables tables
     */
    public GeoPackageCacheOverlay(String name, List<GeoPackageTableCacheOverlay> tables) {
        super(GeoPackageCacheProvider.class, name, true);

        for (GeoPackageTableCacheOverlay table : tables) {
            table.setParent(this);
            if (table instanceof GeoPackageFeatureTableCacheOverlay) {
                GeoPackageFeatureTableCacheOverlay featureTable = (GeoPackageFeatureTableCacheOverlay) table;
                for(GeoPackageTileTableCacheOverlay linkedTileTable: featureTable.getLinkedTileTables()) {
                    linkedTileTable.setParent(this);
                }
            }
            this.tables.put(table.getTableName(), table);
        }
    }

    @Override
    public CacheOverlayOnMap createOverlayOnMap(GoogleMap map) {
        // TODO:
        return null;
    }

    @Override
    public void removeFromMap() {
        for (CacheOverlay tableCacheOverlay : getChildren()) {
            tableCacheOverlay.removeFromMap();
        }
    }

    @Override
    public Integer getIconImageResourceId() {
        return R.drawable.ic_geopackage;
    }

    @Override
    public List<CacheOverlay> getChildren() {
        return new ArrayList<>(tables.values());
    }

}
