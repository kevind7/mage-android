package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.GoogleMap;

import mil.nga.giat.mage.map.MapFragment;

public interface CacheOverlayOnMap {
    GoogleMap getMap();
    CacheOverlayOnMap addToMap();
    CacheOverlayOnMap removeFromMap();
    CacheOverlayOnMap zoomMapToBoundingBox();

    /**
     * Have this overlay's features been added to the {@link #getMap() map}?
     *
     * @return true if the overlay data is currently on the map
     */
    boolean isEnabled();
}
