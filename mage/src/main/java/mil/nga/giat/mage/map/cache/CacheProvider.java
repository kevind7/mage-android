package mil.nga.giat.mage.map.cache;

import java.io.File;
import java.util.Set;

/**
 * A CacheProvider represents a specific cache data format that can put overlays on a map.
 *
 * TODO: thread-safety coniderations - {@link CacheManager} for now only invokes these methods serially
 * across all providers, but could be otherwise
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
     *
     * TODO: assumes one cache per file; child caches can get around this, but maybe that's not always the best model
     */
    CacheOverlay importCacheFromFile(File cacheFile) throws CacheImportException;

    /**
     * Return the set of caches that have been imported and remain available.
     * Defunct caches, such as on a removed SD card, will be removed.
     * @return
     */
    Set<CacheOverlay> refreshAvailableCaches();
}
