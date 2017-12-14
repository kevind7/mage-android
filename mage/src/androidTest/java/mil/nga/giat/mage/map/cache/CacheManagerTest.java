package mil.nga.giat.mage.map.cache;


import android.app.Application;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CacheManagerTest {

    static class TestCacheOverlay extends CacheOverlay {

        protected TestCacheOverlay(Class<? extends CacheProvider> type, String overlayName, boolean supportsChildren) {
            super(type, overlayName, supportsChildren);
        }

        @Override
        public void removeFromMap() {

        }
    }

    @Rule
    public TemporaryFolder testRoot = new TemporaryFolder();

    @Rule
    public TestName testName = new TestName();

    Application context;
    File cacheDir1;
    File cacheDir2;
    List<File> cacheDirs;
    CacheManager cacheManager;
    CacheProvider catProvider;
    CacheProvider dogProvider;
    CacheManager.OnCacheOverlaysLoadedListener listener;

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

        listener = mock(CacheManager.OnCacheOverlaysLoadedListener.class);
        cacheManager = new CacheManager(config);
        cacheManager.registerCacheOverlayListener(listener);
    }

    @Test
    public void importsCacheWithCapableProvider() throws Exception {
        File cacheFile = new File(cacheDir1, "big_cache.dog");

        assertTrue(cacheFile.createNewFile());

        cacheManager.tryImportCacheFile(cacheFile);

        verify(dogProvider, timeout(1000)).importCacheFromFile(cacheFile);
        verify(catProvider, never()).importCacheFromFile(any(File.class));
    }

    @Test
    public void addsImportedCacheOverlayToCacheOverlaySet() throws Exception {
        TestCacheOverlay catOverlay = new TestCacheOverlay(catProvider.getClass(), testName.getMethodName(), false);
        File cacheFile = new File(cacheDir2, "data.cat");
        when(catProvider.importCacheFromFile(cacheFile)).thenReturn(catOverlay);

        assertTrue(cacheFile.createNewFile());

        cacheManager.tryImportCacheFile(cacheFile);

        ArgumentCaptor<Set<CacheOverlay>> overlaysCaptor = ArgumentCaptor.forClass(Set.class);
        verify(listener, timeout(1000)).onCacheOverlaysLoaded(overlaysCaptor.capture());
        Set<CacheOverlay> overlays = overlaysCaptor.getValue();

        assertThat(overlays.size(), is(1));
        assertThat(overlays, hasItem(catOverlay));
    }

    @Test
    public void findsCachesInProvidedLocations() throws Exception {
        File cache1File = new File(cacheDir1, "pluto.dog");
        File cache2File = new File(cacheDir2, "figaro.cat");
        TestCacheOverlay cache1 = new TestCacheOverlay(dogProvider.getClass(), cache1File.getName(), false);
        TestCacheOverlay cache2 = new TestCacheOverlay(catProvider.getClass(), cache2File.getName(), false);
        when(dogProvider.importCacheFromFile(cache1File)).thenReturn(cache1);
        when(catProvider.importCacheFromFile(cache2File)).thenReturn(cache2);

        assertTrue(cache1File.createNewFile());
        assertTrue(cache2File.createNewFile());

        cacheManager.refreshAvailableCaches();

        ArgumentCaptor<Set<CacheOverlay>> overlaysCaptor = ArgumentCaptor.forClass(Set.class);

        verify(listener, timeout(1000)).onCacheOverlaysLoaded(overlaysCaptor.capture());

        Set<CacheOverlay> overlays = overlaysCaptor.getValue();

        assertThat(overlays.size(), is(2));
        assertThat(overlays, hasItem(cache1));
        assertThat(overlays, hasItem(cache2));
    }

    @Test
    public void removesCachesWithFilesThatDoNotExist() {

    }

}
