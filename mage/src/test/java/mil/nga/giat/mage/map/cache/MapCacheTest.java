package mil.nga.giat.mage.map.cache;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MapCacheTest {

    @Test
    public void equalWhenNameAndTypeAreEqual() {
        MapCache c1 = new MapCache("c1", CacheProvider.class, null, null);
        MapCache c2 = new MapCache("c1", CacheProvider.class, null, null);

        assertTrue(c1.equals(c2));
    }

    @Test
    public void notEqualWhenOtherIsNull() {
        MapCache c1 = new MapCache("c1", CacheProvider.class, null, null);

        assertFalse(c1.equals(null));
    }

    @Test
    public void notEqualWhenOtherIsNotMapCache() {
        MapCache c1 = new MapCache("c1", CacheProvider.class, null, null);

        assertFalse(c1.equals(new Object()));
    }

    @Test
    public void notEqualWhenNamesAreDifferent() {
        MapCache c1 = new MapCache("c1", CacheProvider.class, null, null);
        MapCache c2 = new MapCache("c2", CacheProvider.class, null, null);

        assertFalse(c1.equals(c2));
    }
}
