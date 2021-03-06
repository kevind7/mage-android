package mil.nga.giat.mage.map;

import android.util.Log;

import com.google.android.gms.maps.model.UrlTileProvider;

import java.net.MalformedURLException;
import java.net.URL;

import mil.nga.giat.mage.map.cache.URLCacheOverlay;

public class XYZTileProvider extends UrlTileProvider {

    private static final String LOG_NAME = XYZTileProvider.class.getName();

    private final URLCacheOverlay myOverlay;

    public XYZTileProvider(int width, int height, URLCacheOverlay overlay) {
        super(width, height);

        myOverlay = overlay;
    }

    @Override
    public URL getTileUrl(int x, int y, int z) {
        String path = myOverlay.getURL().toString();
        path = path.replaceAll("\\{s\\}\\.", "");
        path = path.replaceAll("\\{x\\}", Integer.toString(x));
        path = path.replaceAll("\\{y\\}", Long.toString(y));
        path = path.replaceAll("\\{z\\}", Integer.toString(z));

        URL newPath = null;

        try{
            newPath = new URL(path);
        }catch(MalformedURLException e){
            Log.w(LOG_NAME, "Problem with URL " + path, e);
        }

        return newPath;
    }

}
