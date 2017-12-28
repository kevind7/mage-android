package mil.nga.giat.mage.map.cache;

import java.io.File;
import java.util.Set;

public class XYZDirectoryCacheProvider implements CacheProvider {

    @Override
    public boolean isCacheFile(File cacheFile) {
        return cacheFile.isDirectory();
    }

    @Override
    public CacheOverlay importCacheFromFile(File cacheFile) throws CacheImportException {
        return new XYZDirectoryCacheOverlay(cacheFile.getName(), cacheFile);
    }

    @Override
    public Set<CacheOverlay> refreshCaches(Set<CacheOverlay> existingCaches) {
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
