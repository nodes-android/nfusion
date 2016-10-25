package dk.nodes.nfusion.base;

import dk.nodes.nfusion.interfaces.ICache;
import dk.nodes.nfusion.interfaces.ICacheListener;

/**
 * Created by bison on 05/03/16.
 */
public abstract class Cache<T> implements ICache<T> {
    protected ICacheListener cacheListener;

    @Override
    public void setListener(ICacheListener listener) {
        cacheListener = listener;
    }

    public void signalReady()
    {
        cacheListener.onCacheReady();
    }
}
