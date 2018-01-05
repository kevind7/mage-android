package mil.nga.giat.mage.map.cache;


import android.app.Application;
import android.os.AsyncTask;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.google.android.gms.maps.GoogleMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CacheManagerTest {

    // had to do this to make Mockito generate a different class for
    // two mock providers, because it uses the same class for two
    // separate mock instances of CacheProvider directly, which is
    // a collision for CacheOverlay.getCacheType()
    static abstract class CatProvider implements CacheProvider {}
    static abstract class DogProvider implements CacheProvider {}

    static class TestCacheOverlay extends CacheOverlay {

        TestCacheOverlay(Class<? extends CacheProvider> type, String overlayName, boolean supportsChildren) {
            super(type, overlayName, supportsChildren);
        }

        @Override
        public CacheOverlayOnMap createOverlayOnMap(GoogleMap map) {
            return null;
        }

        @Override
        public void removeFromMap() {

        }
    }

    private static Set<CacheOverlay> cacheSetWithCaches(CacheOverlay... caches) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(caches)));
    }

    @Rule
    public TemporaryFolder testRoot = new TemporaryFolder();

    @Rule
    public TestName testName = new TestName();

    private Application context;
    private File cacheDir1;
    private File cacheDir2;
    private List<File> cacheDirs;
    private CacheManager cacheManager;
    private Executor executor;
    private CacheProvider catProvider;
    private CacheProvider dogProvider;
    private CacheManager.CacheOverlaysUpdateListener listener;
    private ArgumentCaptor<CacheManager.CacheOverlayUpdate> updateCaptor = ArgumentCaptor.forClass(CacheManager.CacheOverlayUpdate.class);

    @Before
    public void configureCacheManager() throws Exception {

        context = Mockito.mock(Application.class);

        cacheDirs = Arrays.asList(
            cacheDir1 = testRoot.newFolder("cache1"),
            cacheDir2 = testRoot.newFolder("cache2")
        );

        assertTrue(cacheDir1.isDirectory());
        assertTrue(cacheDir2.isDirectory());

        executor = mock(Executor.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Runnable task = invocationOnMock.getArgument(0);
                AsyncTask.SERIAL_EXECUTOR.execute(task);
                return null;
            }
        }).when(executor).execute(any(Runnable.class));

        catProvider = mock(CatProvider.class);
        dogProvider = mock(DogProvider.class);

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
            .executor(executor)
            .providers(catProvider, dogProvider)
            .cacheLocations(new CacheManager.CacheLocationProvider() {
                @Override
                public List<File> getLocalSearchDirs() {
                    return cacheDirs;
                }
            });

        listener = mock(CacheManager.CacheOverlaysUpdateListener.class);
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

        verify(listener, timeout(1000)).onCacheOverlaysUpdated(updateCaptor.capture());

        CacheManager.CacheOverlayUpdate update = updateCaptor.getValue();
        Set<CacheOverlay> overlays = cacheManager.getCaches();

        assertThat(overlays.size(), is(1));
        assertThat(overlays, hasItem(catOverlay));
        assertThat(update.added.size(), is(1));
        assertThat(update.added, hasItem(catOverlay));
        assertTrue(update.updated.isEmpty());
        assertTrue(update.removed.isEmpty());
        assertThat(update.allAvailable, sameInstance(overlays));
    }

    @Test
    public void refreshingFindsCachesInProvidedLocations() throws Exception {
        File cache1File = new File(cacheDir1, "pluto.dog");
        File cache2File = new File(cacheDir2, "figaro.cat");
        CacheOverlay cache1 = new TestCacheOverlay(dogProvider.getClass(), cache1File.getName(), false);
        CacheOverlay cache2 = new TestCacheOverlay(catProvider.getClass(), cache2File.getName(), false);
        when(dogProvider.importCacheFromFile(cache1File)).thenReturn(cache1);
        when(catProvider.importCacheFromFile(cache2File)).thenReturn(cache2);

        assertTrue(cache1File.createNewFile());
        assertTrue(cache2File.createNewFile());

        cacheManager.refreshAvailableCaches();

        verify(listener, timeout(1000)).onCacheOverlaysUpdated(updateCaptor.capture());

        CacheManager.CacheOverlayUpdate update = updateCaptor.getValue();
        Set<CacheOverlay> overlays = cacheManager.getCaches();

        assertThat(overlays.size(), is(2));
        assertThat(overlays, hasItems(cache1, cache2));
        assertThat(update.added.size(), is(2));
        assertThat(update.added, hasItems(cache1, cache2));
        assertTrue(update.updated.isEmpty());
        assertTrue(update.removed.isEmpty());
        assertThat(update.allAvailable, sameInstance(overlays));
    }

    @Test
    public void refreshingGetsAvailableCachesFromProviders() throws Exception {
        CacheOverlay dogCache1 = new TestCacheOverlay(dogProvider.getClass(), "dog1", false);
        CacheOverlay dogCache2 = new TestCacheOverlay(dogProvider.getClass(), "dog2", false);
        CacheOverlay catCache = new TestCacheOverlay(catProvider.getClass(), "cat1", false);

        when(dogProvider.refreshCaches(ArgumentMatchers.<CacheOverlay>anySet())).thenReturn(cacheSetWithCaches(dogCache1, dogCache2));
        when(catProvider.refreshCaches(ArgumentMatchers.<CacheOverlay>anySet())).thenReturn(cacheSetWithCaches(catCache));

        cacheManager.refreshAvailableCaches();

        verify(listener, timeout(1000)).onCacheOverlaysUpdated(updateCaptor.capture());
        verify(dogProvider).refreshCaches(eq(Collections.<CacheOverlay>emptySet()));
        verify(catProvider).refreshCaches(eq(Collections.<CacheOverlay>emptySet()));

        CacheManager.CacheOverlayUpdate update = updateCaptor.getValue();
        Set<CacheOverlay> overlays = cacheManager.getCaches();

        assertThat(overlays.size(), is(3));
        assertThat(overlays, hasItems(dogCache1, dogCache2, catCache));
        assertThat(update.added.size(), is(3));
        assertThat(update.added, hasItems(dogCache1, dogCache2, catCache));
        assertTrue(update.updated.isEmpty());
        assertTrue(update.removed.isEmpty());
        assertThat(update.allAvailable, sameInstance(overlays));
    }

    @Test
    public void refreshingRemovesCachesNoLongerAvailable() throws Exception {
        CacheOverlay dogCache1 = new TestCacheOverlay(dogProvider.getClass(), "dog1", false);
        CacheOverlay dogCache2 = new TestCacheOverlay(dogProvider.getClass(), "dog2", false);
        CacheOverlay catCache = new TestCacheOverlay(catProvider.getClass(), "cat1", false);

        when(dogProvider.refreshCaches(ArgumentMatchers.<CacheOverlay>anySet())).thenReturn(cacheSetWithCaches(dogCache1, dogCache2));
        when(catProvider.refreshCaches(ArgumentMatchers.<CacheOverlay>anySet())).thenReturn(cacheSetWithCaches(catCache));

        cacheManager.refreshAvailableCaches();

        verify(listener, timeout(1000)).onCacheOverlaysUpdated(updateCaptor.capture());
        verify(dogProvider).refreshCaches(eq(Collections.<CacheOverlay>emptySet()));
        verify(catProvider).refreshCaches(eq(Collections.<CacheOverlay>emptySet()));

        Set<CacheOverlay> overlays = cacheManager.getCaches();

        assertThat(overlays.size(), is(3));
        assertThat(overlays, hasItems(dogCache1, dogCache2, catCache));

        when(dogProvider.refreshCaches(ArgumentMatchers.<CacheOverlay>anySet())).thenReturn(cacheSetWithCaches(dogCache2));
        when(catProvider.refreshCaches(ArgumentMatchers.<CacheOverlay>anySet())).thenReturn(Collections.<CacheOverlay>emptySet());

        cacheManager.refreshAvailableCaches();

        verify(listener, timeout(1000).times(2)).onCacheOverlaysUpdated(updateCaptor.capture());

        verify(dogProvider).refreshCaches(eq(cacheSetWithCaches(dogCache1, dogCache2)));
        verify(catProvider).refreshCaches(eq(cacheSetWithCaches(catCache)));

        overlays = cacheManager.getCaches();
        CacheManager.CacheOverlayUpdate update = updateCaptor.getValue();

        assertThat(overlays.size(), is(1));
        assertThat(overlays, hasItem(dogCache2));
        assertThat(update.added, empty());
        assertThat(update.updated, empty());
        assertThat(update.removed, hasItems(dogCache1, catCache));
        assertThat(update.allAvailable, sameInstance(overlays));
    }

    @Test
    public void refreshingUpdatesExistingCachesThatChanged() throws Exception {
        CacheOverlay dogOrig = new TestCacheOverlay(dogProvider.getClass(), "dog1", false);

        when(dogProvider.refreshCaches(ArgumentMatchers.<CacheOverlay>anySet())).thenReturn(cacheSetWithCaches(dogOrig));

        cacheManager.refreshAvailableCaches();

        verify(listener, timeout(1000)).onCacheOverlaysUpdated(updateCaptor.capture());

        Set<CacheOverlay> overlays = cacheManager.getCaches();
        CacheManager.CacheOverlayUpdate update = updateCaptor.getValue();

        assertThat(overlays.size(), is(1));
        assertThat(overlays, hasItem(dogOrig));
        assertThat(update.added.size(), is(1));
        assertThat(update.added, hasItem(dogOrig));
        assertThat(update.updated, empty());
        assertThat(update.removed, empty());

        CacheOverlay dogUpdated = new TestCacheOverlay(dogProvider.getClass(), "dog1", false);

        when(dogProvider.refreshCaches(ArgumentMatchers.<CacheOverlay>anySet())).thenReturn(cacheSetWithCaches(dogUpdated));

        cacheManager.refreshAvailableCaches();

        verify(listener, timeout(1000).times(2)).onCacheOverlaysUpdated(updateCaptor.capture());

        Set<CacheOverlay> overlaysRefreshed = cacheManager.getCaches();
        update = updateCaptor.getValue();

        assertThat(overlaysRefreshed, not(sameInstance(overlays)));
        assertThat(overlaysRefreshed.size(), is(1));
        assertThat(overlaysRefreshed, hasItem(sameInstance(dogUpdated)));
        assertThat(overlaysRefreshed, hasItem(dogOrig));
        assertThat(update.added, empty());
        assertThat(update.updated.size(), is(1));
        assertThat(update.updated, hasItem(sameInstance(dogUpdated)));
        assertThat(update.removed, empty());
    }

    @Test
    public void immediatelyBeginsRefreshOnExecutor() {
        final boolean[] overrodeMock = new boolean[]{false};
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // make sure this answer overrides the one in the setup method
                overrodeMock[0] = true;
                return null;
            }
        }).when(executor).execute(any(Runnable.class));

        cacheManager.refreshAvailableCaches();

        verify(executor).execute(any(Runnable.class));
        assertTrue(overrodeMock[0]);
    }

    @Test
    public void cannotRefreshMoreThanOnceConcurrently() throws Exception {
        final CyclicBarrier taskBegan = new CyclicBarrier(2);
        final CyclicBarrier taskCanProceed = new CyclicBarrier(2);
        final AtomicReference<Runnable> runningTask = new AtomicReference<>();

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final Runnable task = invocation.getArgument(0);
                final Runnable blocked = new Runnable() {
                    @Override
                    public void run() {
                        if (runningTask.get() == this) {
                            try {
                                taskBegan.await();
                                taskCanProceed.await();
                            }
                            catch (Exception e) {
                                fail(e.getMessage());
                                throw new IllegalStateException(e);
                            }
                        }
                        task.run();
                    }
                };
                runningTask.compareAndSet(null, blocked);
                AsyncTask.SERIAL_EXECUTOR.execute(blocked);
                return null;
            }
        }).when(executor).execute(any(Runnable.class));

        when(catProvider.refreshCaches(ArgumentMatchers.<CacheOverlay>anySet())).thenReturn(Collections.<CacheOverlay>emptySet());
        when(dogProvider.refreshCaches(ArgumentMatchers.<CacheOverlay>anySet())).thenReturn(Collections.<CacheOverlay>emptySet());

        cacheManager.refreshAvailableCaches();

        verify(executor, times(1)).execute(any(Runnable.class));

        // wait for the background task to start, then try to start another refresh
        // and verify no new tasks were submitted to executor
        taskBegan.await();

        cacheManager.refreshAvailableCaches();

        verify(executor, times(1)).execute(any(Runnable.class));

        taskCanProceed.await();

        verify(listener, timeout(1000)).onCacheOverlaysUpdated(updateCaptor.capture());
    }
}
