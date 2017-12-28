package mil.nga.giat.mage.map.cache;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A <code>CacheOverlay</code> represents cached data set which can appear on a map.
 * A {@link CacheProvider} implementation will create instances of its associated
 * <code>CacheOverlay</code> subclass.  Note that this class provides default
 * {@link #equals(Object)} and {@link #hashCode()} implementations because
 * {@link CacheManager} places <code>CacheOverlay</code> instances in sets and they
 * may also be used as {@link java.util.HashMap} keys.  Subclasses must take care
 * those methods work properly if overriding those or other methods on which
 * <code>equals()</code> and <code>hashCode()</code> depend.
 *
 * @author osbornb
 */
public abstract class CacheOverlay {


    /**
     * Build the cache overlayName of a child
     *
     * @param name      cache overlay name
     * @param childName child cache overlay name
     * @return
     */
    static String buildChildCacheName(String name, String childName) {
        return name + "-" + childName;
    }

    /**
     * Name of the cache overlay
     */
    private final String overlayName;

    /**
     * Cache type
     */
    private final Class<? extends CacheProvider> type;

    /**
     * True if the cache type supports child caches
     */
    private final boolean supportsChildren;

    private long refreshTimestamp;

    /**
     * Cache overlay parent
     */
    private CacheOverlay parent;

    /**
     * True when enabled
     */
    @Deprecated
    private boolean enabled = false;

    /**
     * True when the cache was newly added, such as a file opened with MAGE
     */
    @Deprecated
    private boolean added = false;

    /**
     * Constructor
     *
     * @param type the {@link CacheProvider provider} that creates and manages this overlay
     * @param overlayName a unique, persistent name for the overlay
     * @param supportsChildren true if this cache overlay can have child cache overlays
     */
    protected CacheOverlay(Class<? extends CacheProvider> type, String overlayName, boolean supportsChildren) {
        this.type = type;
        this.overlayName = overlayName;
        this.supportsChildren = supportsChildren;
        updateRefreshTimestamp();
    }

    /**
     * Create the {@link CacheProvider provider-specific} {@link CacheOverlayOnMap map-linkage} that
     * can draw this cache's data on the given map.
     * @param map the map to link to the {@link CacheOverlayOnMap#getMap()}
     * @return a new {@link CacheOverlayOnMap} instance
     */
    public abstract CacheOverlayOnMap createOverlayOnMap(GoogleMap map);

    @Deprecated
    public abstract void removeFromMap();

    protected void updateRefreshTimestamp() {
        refreshTimestamp = System.currentTimeMillis();
    }

    public String getOverlayName() {
        return overlayName;
    }

    public Class<? extends CacheProvider> getType() {
        return type;
    }

    /**
     * Return the last timestamp when this cache data was refreshed.  This does
     * not necessarily indicate the underlying data changed, only that the data
     * was last verified to be available at this time.
     *
     * @return a long timestamp suitable for constructing {@link java.util.Date#Date(long)}
     */
    public long getRefreshTimestamp() {
        return refreshTimestamp;
    }

    @Deprecated
    public boolean isEnabled() {
        return enabled;
    }

    @Deprecated
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Deprecated
    public boolean isAdded() {
        return added;
    }

    @Deprecated
    public void setAdded(boolean added) {
        this.added = added;
    }

    /**
     * Get the icon image resource id for the cache
     *
     * @return
     */
    @Nullable
    public Integer getIconImageResourceId() {
        return null;
    }

    /**
     * Does the cache type support children
     *
     * @return
     */
    public boolean isSupportsChildren() {
        return supportsChildren;
    }

    /**
     * Get the children cache overlays
     *
     * @return
     */
    @NonNull
    public List<CacheOverlay> getChildren() {
        return Collections.emptyList();
    }

    /**
     * Get the child's parent cache overlay
     *
     * @return parent cache overlay
     */
    @Nullable
    public CacheOverlay getParent(){
        return parent;
    }

    /**
     * Get information about the cache to display
     *
     * @return
     */
    @Nullable
    public String getInfo() {
        return null;
    }

    /**
     * On map click
     *
     * @param latLng  map click location
     * @param mapView map view
     * @param map     Google map
     * @return map click message
     */
    public String onMapClick(LatLng latLng, MapView mapView, GoogleMap map) {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CacheOverlay)) {
            return false;
        }
        CacheOverlay other = (CacheOverlay)obj;
        return getType().equals(other.getType()) && getOverlayName().equals(other.getOverlayName());
    }

    @Override
    public int hashCode() {
        return getOverlayName().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getOverlayName() + ")";
    }

    public boolean isTypeOf(Class<? extends CacheProvider> providerType) {
        return providerType.isAssignableFrom(getType());
    }

    /**
     * Set the parent cache overlay
     *
     * @param parent
     */
    protected void setParent(CacheOverlay parent) {
        this.parent = parent;
    }
}