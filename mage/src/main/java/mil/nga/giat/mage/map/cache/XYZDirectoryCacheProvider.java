package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.GoogleMap;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class XYZDirectoryCacheProvider implements CacheProvider {

    @Override
    public boolean isCacheFile(File cacheFile) {
        return cacheFile.isDirectory();
    }

    @Override
    public MapCache importCacheFromFile(File cacheFile) throws CacheImportException {
        if (!cacheFile.isDirectory()) {
            throw new CacheImportException(cacheFile, "cache file is not a directory: " + cacheFile.getName());
        }
        Set<CacheOverlay> overlays = new HashSet<>();
        overlays.add(new XYZDirectoryCacheOverlay(cacheFile.getName(), cacheFile));
        return new MapCache(cacheFile.getName(), getClass(), cacheFile, Collections.unmodifiableSet(overlays));
    }

    @Override
    public Set<MapCache> refreshCaches(Set<MapCache> existingCaches) {
        return null;
    }

    @Override
    public CacheOverlayOnMap createOverlayOnMapFromCache(CacheOverlay cache, GoogleMap map) {
        return null;
    }

    /**
     * TODO: this was originally in TileOverlayPreferenceActivity - delete should be function of the provider
     */
    private void deleteXYZCacheOverlay(XYZDirectoryCacheOverlay xyzCacheOverlay){

        File directory = xyzCacheOverlay.getDirectory();

        if(directory.canWrite()){
            deleteFile(directory);
        }

    }

    private void deleteFile(File base) {
        if (base.isDirectory()) {
            for (File file : base.listFiles()) {
                deleteFile(file);
            }
        }
        base.delete();
    }
}
