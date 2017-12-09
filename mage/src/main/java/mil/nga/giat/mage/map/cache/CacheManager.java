package mil.nga.giat.mage.map.cache;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import mil.nga.giat.mage.R;

/**
 * Created by wnewman on 2/11/16.
 */
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

    public interface OnCacheOverlayListener {
        void onCacheOverlay(Set<CacheOverlay> cacheOverlays);
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
    private final Collection<OnCacheOverlayListener> cacheOverlayListeners = new ArrayList<>();

    public CacheManager(Config config) {
        context = config.context;
        cacheLocations = config.cacheLocations;
        providers.addAll(config.providers);
    }

    public void tryImportCacheFile(File cacheFile) {
        new ImportCacheFileTask().execute(cacheFile);
    }

    public void registerCacheOverlayListener(OnCacheOverlayListener listener) {
        cacheOverlayListeners.add(listener);
        listener.onCacheOverlay(cacheOverlays);
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

    public void unregisterCacheOverlayListener(OnCacheOverlayListener listener) {
        cacheOverlayListeners.remove(listener);
    }

    public void refreshTileOverlays() {
        FindCacheOverlaysTask task = new FindCacheOverlaysTask();
        task.execute();
    }

    private void mergeCacheOverlays(Set<CacheOverlay> update) {
        cacheOverlays.retainAll(update);
        update.removeAll(cacheOverlays);
        cacheOverlays.addAll(update);
        for (OnCacheOverlayListener listener : cacheOverlayListeners) {
            listener.onCacheOverlay(cacheOverlays);
        }
    }

    public final class ImportCacheFileTask extends AsyncTask<File, Void, Set<CacheOverlay>> {

        public ImportCacheFileTask() {
        }

        @Override
        protected Set<CacheOverlay> doInBackground(File... files) {
            Set<CacheOverlay> caches = new HashSet<>(files.length);
            for (File cacheFile : files) {
                CacheOverlay imported = importFromFirstCapableProvider(cacheFile);
                if (imported != null) {
                    caches.add(imported);
                }
            }
            return caches;
        }

        @Override
        protected void onPostExecute(Set<CacheOverlay> cacheOverlays) {
            mergeCacheOverlays(cacheOverlays);
        }

        private CacheOverlay importFromFirstCapableProvider(File cacheFile) {
            for (CacheProvider provider : providers) {
                if (cacheFile.canRead() && provider.isCacheFile(cacheFile)) {
                    try {
                        return provider.importCacheFromFile(cacheFile);
                    }
                    catch (CacheImportException e) {
                        // TODO: report back to caller
                    }
                }
            }
            return null;
        }
    }

    private final class FindCacheOverlaysTask extends AsyncTask<Void, Void, Set<CacheOverlay>> {

        FindCacheOverlaysTask() {
        }

        @Override
        protected Set<CacheOverlay> doInBackground(Void... params) {

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

            return overlays;
        }

        @Override
        protected void onPostExecute(Set<CacheOverlay> result) {
            mergeCacheOverlays(result);
        }
    }
}
