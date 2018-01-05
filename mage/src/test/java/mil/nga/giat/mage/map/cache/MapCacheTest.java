package mil.nga.giat.mage.map.cache;

import static org.junit.Assert.assertTrue;

public class MapCacheTest {

    public void equalWhenNameAndTypeAreEqual() {
        MapCache c1 = new MapCache("c1", CacheProvider.class, null, null);
        MapCache c2 = new MapCache("c2", CacheProvider.class, null, null);

        assertTrue(c1.equals(c2));
    }
}
