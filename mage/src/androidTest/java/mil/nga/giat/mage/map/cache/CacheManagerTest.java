package mil.nga.giat.mage.map.cache;


import android.app.Application;
import android.os.AsyncTask;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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

    static class TestCacheOverlay extends CacheOverlay {

        protected TestCacheOverlay(Class<? extends CacheProvider> type, String overlayName, boolean supportsChildren) {
            super(type, overlayName, supportsChildren);
        }

        @Override
        public void removeFromMap() {

        }
    }

    static Set<CacheOverlay> cacheSetWithCaches(CacheOverlay... caches) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(caches)));
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
    Executor executor;
    CacheProvider catProvider;
    CacheProvider dogProvider;
    CacheManager.CacheOverlaysUpdateListener listener;
    ArgumentCaptor<Set<CacheOverlay>> overlaysCaptor = ArgumentCaptor.forClass(Set.class);

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

        verify(listener, timeout(1000)).onCacheOverlaysUpdated(overlaysCaptor.capture());

        Set<CacheOverlay> overlays = overlaysCaptor.getValue();

        assertThat(overlays.size(), is(1));
        assertThat(overlays, hasItem(catOverlay));
    }

    @Test
    public void refreshingFindsCachesInProvidedLocations() throws Exception {
        File cache1File = new File(cacheDir1, "pluto.dog");
        File cache2File = new File(cacheDir2, "figaro.cat");
        TestCacheOverlay cache1 = new TestCacheOverlay(dogProvider.getClass(), cache1File.getName(), false);
        TestCacheOverlay cache2 = new TestCacheOverlay(catProvider.getClass(), cache2File.getName(), false);
        when(dogProvider.importCacheFromFile(cache1File)).thenReturn(cache1);
        when(catProvider.importCacheFromFile(cache2File)).thenReturn(cache2);

        assertTrue(cache1File.createNewFile());
        assertTrue(cache2File.createNewFile());

        cacheManager.refreshAvailableCaches();

        verify(listener, timeout(1000)).onCacheOverlaysUpdated(overlaysCaptor.capture());

        Set<CacheOverlay> overlays = overlaysCaptor.getValue();

        assertThat(overlays.size(), is(2));
        assertThat(overlays, hasItem(cache1));
        assertThat(overlays, hasItem(cache2));
    }

    @Test
    public void refreshingGetsAvailableCachesFromProviders() throws Exception {
        CacheOverlay dogCache1 = new TestCacheOverlay(dogProvider.getClass(), "dog1", false);
        CacheOverlay dogCache2 = new TestCacheOverlay(dogProvider.getClass(), "dog2", false);
        CacheOverlay catCache = new TestCacheOverlay(catProvider.getClass(), "cat1", false);

        when(dogProvider.refreshAvailableCaches()).thenReturn(cacheSetWithCaches(dogCache1, dogCache2));
        when(catProvider.refreshAvailableCaches()).thenReturn(cacheSetWithCaches(catCache));

        cacheManager.refreshAvailableCaches();

        verify(listener, timeout(1000)).onCacheOverlaysUpdated(overlaysCaptor.capture());
        verify(dogProvider).refreshAvailableCaches();
        verify(catProvider).refreshAvailableCaches();

        Set<CacheOverlay> caches = overlaysCaptor.getValue();

        assertThat(caches.size(), is(3));
        assertThat(caches, hasItems(dogCache1, dogCache2, catCache));
    }

    @Test
    public void refreshingRemovesCachesNoLongerAvailable() throws Exception {
        CacheOverlay dogCache1 = new TestCacheOverlay(dogProvider.getClass(), "dog1", false);
        CacheOverlay dogCache2 = new TestCacheOverlay(dogProvider.getClass(), "dog2", false);
        CacheOverlay catCache = new TestCacheOverlay(catProvider.getClass(), "cat1", false);

        when(dogProvider.refreshAvailableCaches()).thenReturn(cacheSetWithCaches(dogCache1, dogCache2));
        when(catProvider.refreshAvailableCaches()).thenReturn(cacheSetWithCaches(catCache));

        cacheManager.refreshAvailableCaches();

        verify(listener, timeout(1000)).onCacheOverlaysUpdated(overlaysCaptor.capture());
        verify(dogProvider).refreshAvailableCaches();
        verify(catProvider).refreshAvailableCaches();

        Set<CacheOverlay> caches = overlaysCaptor.getValue();

        assertThat(caches.size(), is(3));
        assertThat(caches, hasItems(dogCache1, dogCache2, catCache));

        when(dogProvider.refreshAvailableCaches()).thenReturn(cacheSetWithCaches(dogCache2));
        when(catProvider.refreshAvailableCaches()).thenReturn(Collections.<CacheOverlay>emptySet());

        cacheManager.refreshAvailableCaches();

        verify(listener, timeout(1000).times(2)).onCacheOverlaysUpdated(overlaysCaptor.capture());
        verify(dogProvider, times(2)).refreshAvailableCaches();
        verify(catProvider, times(2)).refreshAvailableCaches();

        caches = overlaysCaptor.getValue();

        assertThat(caches.size(), is(1));
        assertThat(caches, hasItem(dogCache2));
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
        final Lock lock = new ReentrantLock();
        final Condition go = lock.newCondition();
        final List<Runnable> executedTasks = Collections.<Runnable>synchronizedList(new ArrayList<Runnable>());

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final Runnable task = invocation.getArgument(0);
                Runnable blocked = new Runnable() {
                    @Override
                    public void run() {
                        if (executedTasks.isEmpty()) {
                            lock.lock();
                            try {
                                go.await();
                            }
                            catch (InterruptedException e) {
                                throw new IllegalStateException(e);
                            }
                            lock.unlock();
                        }
                        task.run();
                    }
                };
                executedTasks.add(blocked);
                AsyncTask.SERIAL_EXECUTOR.execute(blocked);
                return null;
            }
        }).when(executor).execute(any(Runnable.class));

        when(catProvider.refreshAvailableCaches()).thenReturn(Collections.<CacheOverlay>emptySet());
        when(dogProvider.refreshAvailableCaches()).thenReturn(Collections.<CacheOverlay>emptySet());

        cacheManager.refreshAvailableCaches();

        verify(executor).execute(any(Runnable.class));
        assertThat(executedTasks.size(), is(1));

        cacheManager.refreshAvailableCaches();
        cacheManager.refreshAvailableCaches();

        verify(executor, times(1)).execute(any(Runnable.class));
        assertThat(executedTasks.size(), is(1));

        lock.lock();
        go.signal();
        lock.unlock();

        verify(listener, timeout(1000)).onCacheOverlaysUpdated(overlaysCaptor.capture());
    }
}
