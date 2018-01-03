package mil.nga.giat.mage.map.cache;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;

import mil.nga.giat.mage.map.MapFragment;

public interface CacheOverlayOnMap {

    @Nullable
    GoogleMap getMap();

    @NonNull
    CacheOverlay getCache();

    @NonNull
    CacheOverlayOnMap addToMapWithVisibility(boolean visible);

    @NonNull
    CacheOverlayOnMap removeFromMap();

    @NonNull
    CacheOverlayOnMap zoomMapToBoundingBox();

    /**
     * Make this overlay's map UI elements {@link #isVisible() visible}.
     * @return
     */
    @NonNull
    CacheOverlayOnMap show();

    /**
     * Make this overlay's map UI elements {@link #isVisible() invisible}.
     * @return this
     */
    @NonNull
    CacheOverlayOnMap hide();

    /**
     * @param latLng  map click location
     * @param mapView the map's parent view
     * @return map click message, or null if not applicable
     */
    @Nullable
    String onMapClick(LatLng latLng, MapView mapView);

    /**
     * Have the map UI objects been created and {@link #addToMapWithVisibility(boolean) added} to the map?
     * @return true or false
     */
    boolean isOnMap();
    boolean isVisible();

}
