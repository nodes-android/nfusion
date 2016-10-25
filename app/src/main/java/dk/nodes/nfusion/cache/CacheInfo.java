package dk.nodes.nfusion.cache;

import java.io.Serializable;

/**
 * Created by bison on 06/03/16.
 */
public class CacheInfo implements Serializable
{
    public long lastUpdate;
    public long timeSinceLastUpdate()
    {
        return Math.abs(lastUpdate - System.currentTimeMillis());
    }

    /*
    public boolean isStale()
    {
        if(timeSinceLastUpdate() > SimpleDiskCache.STALE_LIMIT) {
            return true;
        }
        return false;
    }
    */
}