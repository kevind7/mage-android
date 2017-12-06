package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;

import java.util.Collections;
import java.util.List;

/**
 * Abstract cache overlay
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
    public static String buildChildCacheName(String name, String childName) {
        return name + "-" + childName;
    }

    /**
     * Name of the cache overlay
     */
    private final String overlayName;

    /**
     * Cache type
     */
    private final CacheOverlayType type;

    /**
     * True when enabled
     */
    private boolean enabled = false;

    /**
     * True when the cache was newly added, such as a file opened with MAGE
     */
    private boolean added = false;

    /**
     * True if the cache type supports child caches
     */
    private final boolean supportsChildren;

    /**
     * Constructor
     *
     * @param overlayName             overlayName
     * @param type             cache type
     * @param supportsChildren true if cache overlay with children caches
     */
    protected CacheOverlay(String overlayName, CacheOverlayType type, boolean supportsChildren) {
        this.overlayName = overlayName;
        this.type = type;
        this.supportsChildren = supportsChildren;
    }

    /**
     * Remove the cache overlay from the map
     */
    public abstract void removeFromMap();

    public String getOverlayName() {
        return overlayName;
    }

    public CacheOverlayType getType() {
        return type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAdded() {
        return added;
    }

    public void setAdded(boolean added) {
        this.added = added;
    }

    /**
     * Get the icon image resource id for the cache
     *
     * @return
     */
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
    public List<CacheOverlay> getChildren() {
        return Collections.emptyList();
    }

    /**
     * Get the child's parent cache overlay
     *
     * @return parent cache overlay
     */
    public CacheOverlay getParent(){
        return null;
    }

    /**
     * Get information about the cache to display
     *
     * @return
     */
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
        return obj != null && getClass().equals(obj.getClass()) && getOverlayName().equals(((CacheOverlay)obj).getOverlayName());
    }

    @Override
    public int hashCode() {
        return getOverlayName().hashCode();
    }

}