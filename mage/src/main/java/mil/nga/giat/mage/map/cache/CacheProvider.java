package mil.nga.giat.mage.map.cache;

import java.io.File;
import java.util.Set;

/**
 * A CacheProvider represents a specific cache data format that can put overlays on a map.
 */
public interface CacheProvider {

    /**
     * Does this provider recognize the given file as its type of cache?
     * @param cacheFile
     * @return
     */
    boolean isCacheFile(File cacheFile);

    /**
     * Attempt to import the given file as this provider's type of cache and add
     * it to the set of available caches.
     * @param cacheFile
     * @return
     * @throws CacheImportException
     */
    CacheOverlay importCacheFromFile(File cacheFile) throws CacheImportException;

    /**
     * Return the set of caches that have been imported and remain available.
     * Defunct caches, such as on a removed SD card, will be removed.
     * @return
     */
    Set<CacheOverlay> refreshAvailableCaches();
}
