package mil.nga.giat.mage.map.cache;

import android.app.Application;
import android.os.AsyncTask;
import android.support.annotation.MainThread;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by wnewman on 2/11/16.
 */
@MainThread
public class CacheManager {

    /**
     * Dynamically provide a list of standard locations to search for available caches.
     * This is primarily intended to support changing external SD cards and refreshing
     * caches from there, but the concept could be extended to remote URLs as well or
     * other file sources.
     */
    public interface CacheLocationProvider {
        List<File> getLocalSearchDirs();
    }

    /**
     * Implement this interface and {@link #registerCacheOverlayListener(OnCacheOverlaysLoadedListener)}
     */
    public interface OnCacheOverlaysLoadedListener {
        void onCacheOverlaysLoaded(Set<CacheOverlay> cacheOverlays);
    }

    private static final String LOG_NAME = CacheManager.class.getName();

    private static CacheManager instance = null;

    public static class Config {
        private Application context;
        private CacheLocationProvider cacheLocations;
        private List<CacheProvider> providers = new ArrayList<>();

        public Config context(Application x) {
            context = x;
            return this;
        }

        /**
         * Paths to directories to search for cache files
         * @param x
         * @return
         */
        public Config cacheLocations(CacheLocationProvider x) {
            cacheLocations = x;
            return this;
        }

        public Config providers(CacheProvider... x) {
            providers.addAll(Arrays.asList(x));
            return this;
        }
    }

    public static synchronized void initialize(Config config) {
        if (instance == null) {
            instance = new CacheManager(config);
            return;
        }
        throw new Error("attempt to initialize " + CacheManager.class + " singleton more than once");
    }

    public static CacheManager getInstance() {
        return instance;
    }

    private final Application context;
    private final CacheLocationProvider cacheLocations;
    private final List<CacheProvider> providers = new ArrayList<>();
    private final Set<CacheOverlay> cacheOverlays = new HashSet<>();
    private final Collection<OnCacheOverlaysLoadedListener> cacheOverlayListeners = new ArrayList<>();

    public CacheManager(Config config) {
        context = config.context;
        cacheLocations = config.cacheLocations;
        providers.addAll(config.providers);
    }

    public void tryImportCacheFile(File cacheFile) {
        new ImportCacheFileTask().execute(cacheFile);
    }

    public void registerCacheOverlayListener(OnCacheOverlaysLoadedListener listener) {
        cacheOverlayListeners.add(listener);
    }

    public void removeCacheOverlay(String name) {
        // TODO: remove from CacheProvider
        Iterator<CacheOverlay> iterator = cacheOverlays.iterator();
        while (iterator.hasNext()) {
            CacheOverlay cacheOverlay = iterator.next();
            if (cacheOverlay.getOverlayName().equalsIgnoreCase(name)) {
                iterator.remove();
                return;
            }
        }
    }

    public void unregisterCacheOverlayListener(OnCacheOverlaysLoadedListener listener) {
        cacheOverlayListeners.remove(listener);
    }

    public void refreshTileOverlays() {
        FindCacheOverlaysTask task = new FindCacheOverlaysTask();
        task.execute();
    }

    private void mergeCacheOverlays(CacheImportResult update) {
        cacheOverlays.addAll(update.imported);
        for (OnCacheOverlaysLoadedListener listener : cacheOverlayListeners) {
            listener.onCacheOverlaysLoaded(cacheOverlays);
        }
    }

    private static class CacheImportResult {
        private final Set<CacheOverlay> imported;
        private final List<CacheImportException> failed;

        private CacheImportResult(Set<CacheOverlay> imported, List<CacheImportException> failed) {
            this.imported = imported;
            this.failed = failed;
        }
    }

    public final class ImportCacheFileTask extends AsyncTask<File, Void, CacheImportResult> {

        public ImportCacheFileTask() {
        }

        @Override
        protected CacheImportResult doInBackground(File... files) {
            Set<CacheOverlay> caches = new HashSet<>(files.length);
            List<CacheImportException> fails = new ArrayList<>(files.length);
            for (File cacheFile : files) {
                CacheOverlay imported = null;
                try {
                    imported = importFromFirstCapableProvider(cacheFile);
                    caches.add(imported);
                }
                catch (CacheImportException e) {
                    fails.add(e);
                }
            }
            return new CacheImportResult(caches, fails);
        }

        @Override
        protected void onPostExecute(CacheImportResult result) {
            mergeCacheOverlays(result);
        }

        private CacheOverlay importFromFirstCapableProvider(File cacheFile) throws CacheImportException {
            for (CacheProvider provider : providers) {
                if (!cacheFile.canRead()) {
                    throw new CacheImportException(cacheFile, "cache file is not readable or does not exist: " + cacheFile.getName());
                }
                if (provider.isCacheFile(cacheFile)) {
                    return provider.importCacheFromFile(cacheFile);
                }
            }
            throw new CacheImportException(cacheFile, "no cache provider could handle file " + cacheFile.getName());
        }
    }

    private final class FindCacheOverlaysTask extends AsyncTask<Void, Void, Void> {

        FindCacheOverlaysTask() {
        }

        @Override
        protected Void doInBackground(Void... params) {

            Set<CacheOverlay> overlays = new HashSet<>();

            for (CacheProvider provider : providers) {
                overlays.addAll(provider.refreshAvailableCaches());
            }

            List<File> searchDirs = cacheLocations.getLocalSearchDirs();
            for (File dir : searchDirs) {
                File[] potentialCaches = dir.listFiles();
                new ImportCacheFileTask().execute(potentialCaches);
            }

            // TODO: move this to CacheOverlayMapManager, or some such map-specific linkage
            // Set what should be enabled based on preferences.
//            boolean update = false;
//            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
//            Set<String> updatedEnabledOverlays = new HashSet<>();
//            updatedEnabledOverlays.addAll(preferences.getStringSet(context.getString(R.string.tileOverlaysKey), Collections.<String>emptySet()));
//            Set<String> enabledOverlays = new HashSet<>();
//            enabledOverlays.addAll(updatedEnabledOverlays);
//
//            // Determine which caches are enabled
//            for (CacheOverlay cacheOverlay : overlays) {
//
//                // Check and enable the cache
//                String cacheName = cacheOverlay.getOverlayName();
//                if (enabledOverlays.remove(cacheName)) {
//                    cacheOverlay.setEnabled(true);
//                }
//
//                // Check the child caches
//                for (CacheOverlay childCache : cacheOverlay.getChildren()) {
//                    if (enabledOverlays.remove(childCache.getOverlayName())) {
//                        childCache.setEnabled(true);
//                        cacheOverlay.setEnabled(true);
//                    }
//                }
//
//                // Check for new caches to enable in the overlays and preferences
//                if (overlaysToEnable.contains(cacheName)) {
//
//                    update = true;
//                    cacheOverlay.setEnabled(true);
//                    cacheOverlay.setAdded(true);
//                    if (cacheOverlay.isSupportsChildren()) {
//                        for (CacheOverlay childCache : cacheOverlay.getChildren()) {
//                            childCache.setEnabled(true);
//                            updatedEnabledOverlays.add(childCache.getOverlayName());
//                        }
//                    } else {
//                        updatedEnabledOverlays.add(cacheName);
//                    }
//                }
//            }
//
//            // Remove overlays in the preferences that no longer exist
//            if (!enabledOverlays.isEmpty()) {
//                updatedEnabledOverlays.removeAll(enabledOverlays);
//                update = true;
//            }
//
//            // If new enabled cache overlays, update them in the preferences
//            if (update) {
//                SharedPreferences.Editor editor = preferences.edit();
//                editor.putStringSet(context.getString(R.string.tileOverlaysKey), updatedEnabledOverlays);
//                editor.apply();
//            }

            return null;
        }
    }
}
