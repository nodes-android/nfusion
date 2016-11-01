package dk.nodes.nfusion.base;

import android.content.Context;
import android.os.Handler;
import com.google.gson.JsonSyntaxException;
import org.json.JSONException;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import dk.nodes.nfusion.EndPointError;
import dk.nodes.nfusion.EndPointRequest;
import dk.nodes.nfusion.interfaces.IErrorHandler;
import dk.nodes.nfusion.interfaces.ILog;
import dk.nodes.nfusion.nFusion;
import dk.nodes.nfusion.interfaces.IAuthentication;
import dk.nodes.nfusion.interfaces.ICache;
import dk.nodes.nfusion.interfaces.ICacheListener;
import dk.nodes.nfusion.interfaces.IEndPoint;
import dk.nodes.nfusion.interfaces.ISubscription;

/**
 * Created by bison on 05/03/16.
 */
public abstract class EndPoint<T> implements IEndPoint<T>, ICacheListener<T> {
    public static final String TAG = EndPoint.class.getSimpleName();
    protected ICache<T> cache;
    protected IAuthentication authentication;
    CountDownLatch initLatch = new CountDownLatch(1);
    protected Class modelClass;
    Context context;
    EndPointError error;
    ILog Log;

    @Override
    public void init(Context context, Class model_cls) {
        Log = nFusion.instance().log();
        modelClass = model_cls;
        this.context = context;
        cache = obtainCacheInstance(context, model_cls);
        cache.setListener(this);
        cache.load();

        authentication = obtainAuthenticationInstance(context);
    }

    @Override
    public void waitForInit() {
        try {
            initLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void clearCache(Context c, Class model_cls) {
        cache = obtainCacheInstance(c, model_cls);
        cache.clearAsync();
    }

    @Override
    public void serveRequest(EndPointRequest request) {
        boolean show_progress = false;
        // is this a cache clear request
        if(request.ICR)
        {
            cache.clear();
            request.getSubscription().onCacheCleared();
            notifyCacheCleared(request.getSubscription());
            return;
        }
        // first do we have something cached, return that immediately then
        // 06/07/12 edit chnt: included the url in the CacheId
        String cache_id = getUrl() + request.generateCacheId();
        T obj = null;
        if(request.getEnableCache()) {
            obj = cache.getById(cache_id);
            if (obj != null) {
                Log.v(TAG, "Cache hit (" + cache_id + ")");
                if (request.getEnableCache())
                    runUpdateOnMainThread(request.getSubscription(), obj, false);
                else
                    show_progress = true;

                if (!request.getForceRefresh()) {
                    if (!cache.isStale(cache_id))
                        return;
                    else
                        Log.v(TAG, "Refreshing stale cached data (" + cache_id + ") type " + modelClass.getSimpleName());
                } else
                    Log.v(TAG, "Forcing refresh! (" + cache_id + ") type " + modelClass.getSimpleName());
            } else {
                show_progress = true;
                Log.v(TAG, "Cache miss (" + cache_id + ") type " + modelClass.getSimpleName());
            }
        }
        else
        {
            show_progress = true;
        }

        if(show_progress)
        {
            notifyProgressStatus(request.getSubscription(), true);
        }

        Log.i(TAG, "Calling endpoint...");
        obj = performCall(request);

        if(show_progress) {
            notifyProgressStatus(request.getSubscription(), false);
        }

        if(obj != null) {
            cache.put(cache_id, obj);
            runUpdateOnMainThread(request.getSubscription(), obj, true);
        }
        else
        {
            if(error != null)
            {
                if(error.code == EndPointError.FAILED_AUTH_ERROR)
                {
                    Log.e(TAG, "Authentication failed detected.");
                    if(authentication != null)
                    {
                        if(authentication.obtain())
                        {
                            obj = performCall(request);
                            if(obj != null)
                            {
                                cache.put(cache_id, obj);
                                runUpdateOnMainThread(request.getSubscription(), obj, true);
                            }
                            else
                                runErrorOnMainThread(request.getSubscription(), error);     // <-- The pointy end
                        }
                        else
                        {
                            runErrorOnMainThread(request.getSubscription(), error);
                        }
                    }
                    else
                        runErrorOnMainThread(request.getSubscription(), error);
                } else {
                    runErrorOnMainThread(request.getSubscription(), error);
                }
            }
        }
    }

    private void notifyProgressStatus(final ISubscription<T> sub, boolean show_progress)
    {
        if(show_progress)
        {
            postRunnableOnUIThread(new Runnable() {
                @Override
                public void run() {
                    sub.onShowProgress();
                }
            });
        }
        else
        {
            postRunnableOnUIThread(new Runnable() {
                @Override
                public void run() {
                    sub.onHideProgress();
                }
            });
        }
    }

    private void notifyCacheCleared(final ISubscription<T> sub)
    {
        postRunnableOnUIThread(new Runnable() {
            @Override
            public void run() {
                sub.onCacheCleared();
            }
        });
    }

    private void postRunnableOnUIThread(Runnable r)
    {
        Handler mainHandler = new Handler(context.getMainLooper());
        mainHandler.post(r);
    }

    protected void runUpdateOnMainThread(final ISubscription<T> sub, final T obj, final boolean broadcast)
    {
        if(nFusion.instance().isApplicationPaused())
        {
            if(nFusion.instance().isDebug())
                Log.d(TAG, "Application is paused, aborting call to onUpdate()");
            return;
        }
        // Get a handler that can be used to post to the main thread
        Handler mainHandler = new Handler(context.getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run()
            {
                // deliver to requesting subscriber
                if(sub.isSubscribing()) {
                    sub.onUpdate(obj);
                }
                if(broadcast) {
                    ArrayList<ISubscription> subs = nFusion.instance().getSubscribersOfEndpoint(sub.getEndPointClass());
                    Log.d(TAG, "Delivering broadcast to " + subs.size() + " subscribers");

                    // deliver to all other subscribers who broadcast
                    for (ISubscription<T> s : subs) {
                        if (s.isSubscribing() && s != sub) {
                            if (s.getReceiveBroadcasts()) // if its not the calling sub, but they want broadcast
                                s.onUpdate(obj);
                        }
                    }
                }
            }
        };
        mainHandler.post(myRunnable);
    }

    protected void runErrorOnMainThread(final ISubscription<T> sub, final EndPointError ep_error)
    {
        if(nFusion.instance().isApplicationPaused())
        {
            if(nFusion.instance().isDebug())
                Log.d(TAG, "Application is paused, aborting call to onError()");
            return;
        }
        if(!sub.isSubscribing())
            return;
        // Get a handler that can be used to post to the main thread
        Handler mainHandler = new Handler(context.getMainLooper());
        // call global error handler if one is set
        final IErrorHandler globalErrorHandler = nFusion.instance().getGlobalErrorHandler();
        if(globalErrorHandler != null)
        {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    globalErrorHandler.onEndPointError(sub.getEndPointClass(), ep_error);
                }
            });
        }

        // call subscriptions on error
        Runnable myRunnable = new Runnable() {
            @Override
            public void run()
            {
                sub.onError(ep_error);
            }
        };
        mainHandler.post(myRunnable);
    }

    @Override
    public void setError(EndPointError error) {
        this.error = error;
    }

    @Override
    public EndPointError getError() {
        return error;
    }

    private void prepareError(EndPointError error, Throwable e)
    {
        error.message = e.toString();
        error.stackTrace = nFusion.stackTraceToString(e);
    }

    public T performCall(EndPointRequest request)
    {
        try {
            T obj = call(request);
            return obj;
        }
        catch(JSONException e)
        {
            EndPointError error = new EndPointError(EndPointError.REQUEST_PARAM_ERROR, 0, null, null);
            prepareError(error, e);
            setError(error);
        }
        catch(JsonSyntaxException e)
        {
            EndPointError error = new EndPointError(EndPointError.JSON_ERROR, 0, null, null);
            prepareError(error, e);
            setError(error);
        }
        catch (UnknownHostException e)
        {
            EndPointError error = new EndPointError(EndPointError.UNKNOWN_HOST_ERROR, 0, null, null);
            prepareError(error, e);
            setError(error);
        }
        catch (SocketTimeoutException e)
        {
            EndPointError error = new EndPointError(EndPointError.TIMEOUT_ERROR, 0, null, null);
            prepareError(error, e);
            setError(error);
        }
        catch (Exception e) {
            e.printStackTrace();
            EndPointError error = new EndPointError(EndPointError.UNKNOWN_ERROR, 0, null, null);
            prepareError(error, e);
            setError(error);
        }
        return null;
    }

    // CacheListener interface ---------------------------------------------------------------------

    @Override
    public void onCacheReady() {
        Log.i(TAG, "Cache type " + modelClass.getSimpleName() + " is ready");
        initLatch.countDown();
    }
}
