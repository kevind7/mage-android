package mil.nga.giat.mage.map.cache;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MapLayerDescriptorTest {

    static abstract class TestMapDataProvider1 implements MapDataProvider {}

    static abstract class TestMapDataProvider2 implements MapDataProvider {}

    static class TestLayerDescriptor1 extends MapLayerDescriptor {

        protected TestLayerDescriptor1(String overlayName, String cacheName, Class<? extends MapDataProvider> cacheType) {
            super(overlayName, cacheName, cacheType);
        }
    }

    static class TestLayerDescriptor2 extends MapLayerDescriptor {

        protected TestLayerDescriptor2(String overlayName, String cacheName, Class<? extends MapDataProvider> cacheType) {
            super(overlayName, cacheName, cacheType);
        }
    }

    @Test
    public void equalWhenNamesAndTypeEqualRegardlessOfClass() {
        TestLayerDescriptor1 overlay1 = new TestLayerDescriptor1("test1", "cache1", TestMapDataProvider1.class);
        TestLayerDescriptor1 overlay2 = new TestLayerDescriptor1("test1", "cache1", TestMapDataProvider1.class);
        TestLayerDescriptor2 overlay3 = new TestLayerDescriptor2("test1", "cache1", TestMapDataProvider1.class);

        assertTrue(overlay1.equals(overlay2));
        assertTrue(overlay1.equals(overlay3));
    }

    @Test
    public void notEqualWhenOtherIsNull() {
        TestLayerDescriptor1 overlay = new TestLayerDescriptor1("testOverlay", "testCache", TestMapDataProvider1.class);

        assertFalse(overlay.equals(null));
    }

    @Test
    public void notEqualWhenProviderTypesAreDifferent() {
        TestLayerDescriptor1 overlay1 = new TestLayerDescriptor1("test1", "cache1", TestMapDataProvider1.class);
        TestLayerDescriptor1 overlay2 = new TestLayerDescriptor1(overlay1.getLayerName(), overlay1.getResourceName(), TestMapDataProvider2.class);
        TestLayerDescriptor2 overlay3 = new TestLayerDescriptor2(overlay1.getLayerName(), overlay1.getResourceName(), TestMapDataProvider2.class);

        assertFalse(overlay1.equals(overlay2));
        assertFalse(overlay1.equals(overlay3));
    }

    @Test
    public void notEqualWhenNamesAreDifferent() {
        TestLayerDescriptor1 overlay1 = new TestLayerDescriptor1("test1", "cache1", TestMapDataProvider1.class);
        TestLayerDescriptor1 overlay2 = new TestLayerDescriptor1("test2", "cache1", TestMapDataProvider1.class);
        TestLayerDescriptor1 overlay3 = new TestLayerDescriptor1("test1", "cache2", TestMapDataProvider1.class);

        assertFalse(overlay1.equals(overlay2));
        assertFalse(overlay1.equals(overlay3));
    }

    @Test
    public void notEqualWhenNamesAndProvidersAreDifferent() {
        TestLayerDescriptor1 overlay1 = new TestLayerDescriptor1("test1", "cache1", TestMapDataProvider1.class);
        TestLayerDescriptor1 overlay2 = new TestLayerDescriptor1("test2", "cache1", TestMapDataProvider2.class);
        TestLayerDescriptor2 overlay3 = new TestLayerDescriptor2("test1", "cache2", TestMapDataProvider2.class);
        TestLayerDescriptor1 overlay4 = new TestLayerDescriptor1("test2", "cache2", TestMapDataProvider2.class);

        assertFalse(overlay1.equals(overlay2));
        assertFalse(overlay1.equals(overlay3));
        assertFalse(overlay1.equals(overlay4));
    }
}
