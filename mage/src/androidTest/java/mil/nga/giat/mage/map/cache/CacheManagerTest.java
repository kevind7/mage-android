package mil.nga.giat.mage.map.cache;


import android.app.Application;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CacheManagerTest {

    static class TestCacheProvider implements CacheProvider {

        final String ext;
        final Set<CacheOverlay> availableCaches = new HashSet<>();

        TestCacheProvider(String ext) {
            this.ext = ext.toLowerCase();
        }

        @Override
        public boolean isCacheFile(File cacheFile) {
            return cacheFile.getName().toLowerCase().endsWith("." + ext);
        }

        @Override
        public CacheOverlay importCacheFromFile(File cacheFile) throws CacheImportException {
            if (!isCacheFile(cacheFile)) {
                return null;
            }
            TestCacheOverlay overlay = new TestCacheOverlay(getClass(), cacheFile.getName(), false);
            availableCaches.add(overlay);
            return overlay;
        }

        @Override
        public Set<CacheOverlay> refreshAvailableCaches() {
            return Collections.unmodifiableSet(availableCaches);
        }
    }

    static class CatProvider extends TestCacheProvider {
        CatProvider() {
            super("cat");
        }
    }

    static class DogProvider extends TestCacheProvider {
        DogProvider() {
            super("dog");
        }
    }

    static class TestCacheOverlay extends CacheOverlay {

        /**
         * Constructor
         *  @param overlayName      overlayName
         * @param supportsChildren true if cache overlay with children caches
         */
        protected TestCacheOverlay(Class<? extends CacheProvider> type, String overlayName, boolean supportsChildren) {
            super(type, overlayName, supportsChildren);
        }

        @Override
        public void removeFromMap() {

        }
    }

    @Rule
    public TemporaryFolder testRoot = new TemporaryFolder();

    Application context;
    File cacheDir1;
    File cacheDir2;
    List<File> cacheDirs;
    CacheManager cacheManager;
    CacheProvider catProvider;
    CacheProvider dogProvider;

    @Before
    public void configureCacheManager() throws Exception {

        context = Mockito.mock(Application.class);

        cacheDirs = Arrays.asList(
            cacheDir1 = testRoot.newFolder("cache1"),
            cacheDir2 = testRoot.newFolder("cache2")
        );

        assertTrue(cacheDir1.isDirectory());
        assertTrue(cacheDir2.isDirectory());

        catProvider = mock(CacheProvider.class);
        dogProvider = mock(CacheProvider.class);

        when(catProvider.isCacheFile(any(File.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                File file = invocationOnMock.getArgument(0);
                return file.getName().toLowerCase().endsWith(".cat");
            }
        });
        when(dogProvider.isCacheFile(any(File.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                File file = invocationOnMock.getArgument(0);
                return file.getName().toLowerCase().endsWith(".dog");
            }
        });

        CacheManager.Config config = new CacheManager.Config()
            .context(context)
            .cacheLocations(new CacheManager.CacheLocationProvider() {
                @Override
                public List<File> getLocalSearchDirs() {
                    return cacheDirs;
                }
            })
            .providers(catProvider, dogProvider);

        cacheManager = new CacheManager(config);
    }

    @Test
    @UiThreadTest
    public void importsCacheWithCapableProvider() throws Exception {
        File cacheFile = new File(cacheDir1, "big_cache.dog");

        assertTrue(cacheFile.createNewFile());

        cacheManager.tryImportCacheFile(cacheFile);

        verify(dogProvider, timeout(500)).importCacheFromFile(cacheFile);
        verify(catProvider, never()).importCacheFromFile(any(File.class));
    }
}
