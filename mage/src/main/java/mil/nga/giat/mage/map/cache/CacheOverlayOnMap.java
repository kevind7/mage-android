package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.GoogleMap;

import mil.nga.giat.mage.map.MapFragment;

public interface CacheOverlayOnMap {
    CacheOverlay getCacheOverlay();
    GoogleMap getMap();
    abstract CacheOverlayOnMap addToMap();
    abstract CacheOverlayOnMap removeFromMap();
    abstract CacheOverlayOnMap refreshOnMap();
    abstract CacheOverlayOnMap zoomMapToBoundingBox();
}
