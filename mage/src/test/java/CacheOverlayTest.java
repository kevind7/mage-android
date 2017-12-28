import com.google.android.gms.maps.GoogleMap;

import org.junit.Test;

import java.io.File;
import java.util.Set;

import mil.nga.giat.mage.map.cache.CacheImportException;
import mil.nga.giat.mage.map.cache.CacheOverlay;
import mil.nga.giat.mage.map.cache.CacheOverlayOnMap;
import mil.nga.giat.mage.map.cache.CacheProvider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CacheOverlayTest {

    static class TestCacheProvider1 implements CacheProvider {

        @Override
        public boolean isCacheFile(File cacheFile) {
            return false;
        }

        @Override
        public CacheOverlay importCacheFromFile(File cacheFile) throws CacheImportException {
            return null;
        }

        @Override
        public Set<CacheOverlay> refreshCaches(Set<CacheOverlay> existingCaches) {
            return null;
        }
    }

    static class TestCacheProvider2 implements CacheProvider {

        @Override
        public boolean isCacheFile(File cacheFile) {
            return false;
        }

        @Override
        public CacheOverlay importCacheFromFile(File cacheFile) throws CacheImportException {
            return null;
        }

        @Override
        public Set<CacheOverlay> refreshCaches(Set<CacheOverlay> existingCaches) {
            return null;
        }
    }

    static class TestCacheOverlay1 extends CacheOverlay {

        protected TestCacheOverlay1(Class<? extends CacheProvider> type, String overlayName, boolean supportsChildren) {
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

    static class TestCacheOverlay2 extends CacheOverlay {

        protected TestCacheOverlay2(Class<? extends CacheProvider> type, String overlayName, boolean supportsChildren) {
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

    @Test
    public void equalWhenNameAndTypeEqualRegardlessOfClass() {
        TestCacheOverlay1 overlay1 = new TestCacheOverlay1(TestCacheProvider1.class, "test1", false);
        TestCacheOverlay1 overlay2 = new TestCacheOverlay1(TestCacheProvider1.class, "test1", false);
        TestCacheOverlay2 overlay3 = new TestCacheOverlay2(TestCacheProvider1.class,"test1", false);

        assertTrue(overlay1.equals(overlay2));
        assertTrue(overlay1.equals(overlay3));
    }

    @Test
    public void notEqualWhenOtherIsNull() {
        TestCacheOverlay1 overlay = new TestCacheOverlay1(TestCacheProvider1.class, "testOverlay", false);

        assertFalse(overlay.equals(null));
    }

    @Test
    public void notEqualWhenProviderTypesAreDifferent() {
        TestCacheOverlay1 overlay1 = new TestCacheOverlay1(TestCacheProvider1.class, "test1", false);
        TestCacheOverlay1 overlay2 = new TestCacheOverlay1(TestCacheProvider2.class, overlay1.getOverlayName(), false);
        TestCacheOverlay2 overlay3 = new TestCacheOverlay2(TestCacheProvider2.class, overlay1.getOverlayName(), false);

        assertFalse(overlay1.equals(overlay2));
        assertFalse(overlay1.equals(overlay3));
    }

    @Test
    public void notEqualWhenNamesAreDifferent() {
        TestCacheOverlay1 overlay1 = new TestCacheOverlay1(TestCacheProvider1.class, "test1", false);
        TestCacheOverlay1 overlay2 = new TestCacheOverlay1(TestCacheProvider1.class, "test2", false);
        TestCacheOverlay2 overlay3 = new TestCacheOverlay2(TestCacheProvider1.class, "test2", false);

        assertFalse(overlay1.equals(overlay2));
        assertFalse(overlay1.equals(overlay3));
    }

    @Test
    public void notEqualWhenNamesAndProvidersAreDifferent() {
        TestCacheOverlay1 overlay1 = new TestCacheOverlay1(TestCacheProvider1.class, "test1", false);
        TestCacheOverlay1 overlay2 = new TestCacheOverlay1(TestCacheProvider2.class, "test2", false);
        TestCacheOverlay2 overlay3 = new TestCacheOverlay2(TestCacheProvider2.class, "test2", false);

        assertFalse(overlay1.equals(overlay2));
        assertFalse(overlay1.equals(overlay3));
    }
}
