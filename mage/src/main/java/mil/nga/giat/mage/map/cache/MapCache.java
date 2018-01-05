package mil.nga.giat.mage.map.cache;

import java.io.File;
import java.util.Set;


public class MapCache {

    private final String name;
    private final Class<? extends CacheProvider> type;
    private final File sourceFile;
    private final Set<CacheOverlay> overlays;
    private long refreshTimestamp;


    public MapCache(String name, Class<? extends CacheProvider> type, File sourceFile, Set<CacheOverlay> overlays) {
        this.name = name;
        this.type = type;
        this.overlays = overlays;
        this.sourceFile = sourceFile;
        updateRefreshTimestamp();
    }

    public String getName() {
        return name;
    }

    public Class<? extends CacheProvider> getType() {
        return type;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public Set<CacheOverlay> getCacheOverlays() {
        return overlays;
    }

    public long getRefreshTimestamp() {
        return refreshTimestamp;
    }

    public void updateRefreshTimestamp() {
        this.refreshTimestamp = System.currentTimeMillis();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof MapCache)) {
            return false;
        }
        MapCache other = (MapCache) obj;
        return getType().equals(other.getType()) && getName().equals(other.getName());
    }

    public int hashCode() {
        return getName().hashCode();
    }

    public String toString() {
        return getName() + ":" + getType().getSimpleName();
    }
}
