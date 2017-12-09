package mil.nga.giat.mage.map.cache;

/**
 * GeoPackage Table cache overlay
 *
 * @author osbornb
 */
public abstract class GeoPackageTableCacheOverlay extends CacheOverlay {

    /**
     * GeoPackage name
     */
    private final String geoPackage;

    /**
     * Cache label
     */
    private final String tableName;

    /**
     * Count of data in the table
     */
    private final int count;

    /**
     * Min zoom level of the data
     */
    private final int minZoom;

    /**
     * Max zoom level of the data
     */
    private final int maxZoom;

    /**
     * Cache overlay parent
     */
    private CacheOverlay parent;

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
    protected GeoPackageTableCacheOverlay(String name, String geoPackage, String tableName, int count, int minZoom, Integer maxZoom) {
        super(GeoPackageCacheProvider.class, name, false);
        this.geoPackage = geoPackage;
        this.tableName = tableName;
        this.count = count;
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
    }

    @Override
    public CacheOverlay getParent(){
        return parent;
    }

    /**
     * Set the parent cache overlay
     *
     * @param parent
     */
    public void setParent(CacheOverlay parent) {
        this.parent = parent;
    }

    /**
     * Get the GeoPackage name
     *
     * @return
     */
    public String getGeoPackage() {
        return geoPackage;
    }

    public String getTableName() {
        return tableName;
    }

    /**
     * Get the count
     *
     * @return
     */
    public int getCount() {
        return count;
    }

    /**
     * Get the min zoom
     *
     * @return
     */
    public int getMinZoom() {
        return minZoom;
    }

    /**
     * Get the max zoom
     *
     * @return
     */
    public int getMaxZoom() {
        return maxZoom;
    }
}
