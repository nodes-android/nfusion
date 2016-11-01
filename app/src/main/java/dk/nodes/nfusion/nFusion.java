package dk.nodes.nfusion;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;


import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import dk.nodes.nfusion.interfaces.IEndPoint;
import dk.nodes.nfusion.interfaces.IErrorHandler;
import dk.nodes.nfusion.interfaces.ILog;
import dk.nodes.nfusion.interfaces.ISubscription;
import dk.nodes.nfusion.util.DefaultLog;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by bison on 05/03/16.
 */
public class nFusion implements Runnable, Application.ActivityLifecycleCallbacks {
    public static final String TAG = nFusion.class.getSimpleName();
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static nFusion instance;

    public OkHttpClient httpClient;
    boolean shouldTerminate = false;
    LinkedBlockingQueue<EndPointRequest> requestQueue;
    ConcurrentHashMap<Class, IEndPoint> endPointMap;
    ArrayList<ISubscription> subscriptions;
    Thread thread;
    Context context;
    ILog log;
    boolean debug = false;
    boolean isApplicationPaused = false;
    IErrorHandler globalErrorHandler = null;

    ExecutorService executorService = Executors.newFixedThreadPool(4);

    private nFusion()
    {

    }

    public static nFusion instance()
    {
        if(instance == null)
        {
            instance = new nFusion();
        }
        return instance;
    }

    public void init(Context context)
    {
        init(context, new DefaultLog(), true);
    }

    public void init(Context context, boolean debug)
    {
        init(context, new DefaultLog(), debug);
    }

    public void init(Context context, ILog log, boolean debug)
    {
        this.context = context;
        this.log = log;
        this.debug = debug;
        log.setDebug(debug);
        log.i(TAG, "Initializing nFusion...");

        httpClient = new OkHttpClient();
        OkHttpClient.Builder builder = httpClient.newBuilder();
        builder.sslSocketFactory(getGullibleSSLSocketFactory());
        builder.connectTimeout(60, TimeUnit.SECONDS);
        builder.writeTimeout(60, TimeUnit.SECONDS);
        builder.readTimeout(60, TimeUnit.SECONDS);
        httpClient = builder.build();

        //httpClient.interceptors().add(new LoggingInterceptor());
        if(debug) {
            setDebug(debug);
        }

        endPointMap = new ConcurrentHashMap<>();
        subscriptions = new ArrayList<>();
        requestQueue = new LinkedBlockingQueue<>();
        thread = new Thread(this);
        thread.start();
    }

    public void registerApplicationLifecycle(Application app)
    {
        app.registerActivityLifecycleCallbacks(this);
    }

    public void setTimeouts(int connect, int read, int write)
    {
        OkHttpClient.Builder builder = httpClient.newBuilder();
        builder.sslSocketFactory(getGullibleSSLSocketFactory());
        builder.connectTimeout(connect, TimeUnit.SECONDS);
        builder.writeTimeout(read, TimeUnit.SECONDS);
        builder.readTimeout(write, TimeUnit.SECONDS);
        httpClient = builder.build();
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        if(debug) {
            log.setDebug(debug);
            if(debug) {
                log.i(TAG, "Debugging enabled, installing HTTP logging interceptor");
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                // set your desired log level
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
                OkHttpClient.Builder builder = httpClient.newBuilder();
                builder.addNetworkInterceptor(logging);
                /*
                    This turns off the Accept-encoding: gzip header in debug mode
                 */
                builder.addNetworkInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request originalRequest = chain.request();
                        // if there is no accept gzip header, do nothing
                        /*
                        if(originalRequest.header("Accept-Encoding") != null) {
                            log.e("nfusion", "no accept-encoding header found");
                            return chain.proceed(originalRequest);
                        }
                        */

                        Request compressedRequest = originalRequest.newBuilder()
                                .removeHeader("Accept-Encoding")
                                .build();
                        return chain.proceed(compressedRequest);
                    }
                });
                httpClient = builder.build();
            }
        }
    }

    public void addSubscription(Class cls, ISubscription subscription) throws Exception
    {
        // end point has not yet been instantiated
        if(!endPointMap.containsKey(cls))
        {
            IEndPoint ep = (IEndPoint) cls.newInstance();
            endPointMap.put(cls, ep);
            log.i(TAG, "Creating instance of " + cls.getSimpleName());
            ep.init(context, subscription.getModelClass());

        }
        subscriptions.add(subscription);
    }

    public void removeSubscription(ISubscription subscription)
    {
        subscriptions.remove(subscription);
    }

    public void queueRequest(EndPointRequest request)
    {
        if(request.isSequential()) {
            try {
                request.state = EndPointRequest.STATE_BEGIN;
                requestQueue.put(request);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else // queuing parallel pigs
        {
            RequestCallable callable = new RequestCallable(request);
            executorService.submit(callable);
        }
    }

    public ArrayList<ISubscription> getSubscribersOfEndpoint(Class endpoint_cls) {
        ArrayList<ISubscription> subs = new ArrayList<>();
        for(ISubscription sub : subscriptions)
        {
            if(sub.getEndPointClass() == endpoint_cls)
                subs.add(sub);
        }
        return subs;
    }

    public ArrayList<ISubscription> getSubscribersOfModel(Class model_cls)
    {
        ArrayList<ISubscription> subs = new ArrayList<>();
        for(ISubscription sub : subscriptions)
        {
            if(sub.getModelClass() == model_cls)
                subs.add(sub);
        }
        return subs;
    }

    public void clearCache(Class cls, Class model_cls) throws Exception
    {
        log.i(TAG, "Clearing cache belonging to " + cls.getSimpleName());

        if(!endPointMap.containsKey(cls))
        {
            log.d(TAG, "Endpoint not instantiated, instantiating..");
            IEndPoint ep = (IEndPoint) cls.newInstance();
            ep.clearCache(context, model_cls);
        }
        else
        {
            log.d(TAG, "Using existing endpoint.");
            IEndPoint ep = endPointMap.get(cls);
            ep.clearCache(context, model_cls);
        }
    }

    public IErrorHandler getGlobalErrorHandler() {
        return globalErrorHandler;
    }

    public void setGlobalErrorHandler(IErrorHandler globalErrorHandler) {
        this.globalErrorHandler = globalErrorHandler;
    }

    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    /*
            This is the main function of the manager thread which serves end point requests
         */
    @Override
    public void run() {
        log.i(TAG, "Worker thread started, going to sleep waiting for requests... zZzz");
        while(!shouldTerminate) {
            try {
                EndPointRequest request = requestQueue.take();
                IEndPoint ep = endPointMap.get(request.subscription.getEndPointClass());
                log.i(TAG, "Got request. model: " + request.subscription.getModelClass().getSimpleName() + ", endpoint: " + request.subscription.getEndPointClass().getSimpleName());
                // wait on endpoint if not ready
                ep.waitForInit();
                log.i(TAG, "Worker thread spinning up to serve request");
                ep.serveRequest(request);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.i(TAG, "Manager thread terminating.");
    }

    // TODO THIS METHOD IS VERY VERY NAUGHTY!, So naughty infact it makes God cry. (turns off ssl cert checking)

    /**
     * Returns a socket factory which disables SSL auth checking, making man in the middle attacks possible
     * (but not all that common)
     * @return SSLSocketFactory that doesn't trow invalid cert exceptions. Use with http client
     */
    public static SSLSocketFactory getGullibleSSLSocketFactory() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
                    context.getSocketFactory());
            return context.getSocketFactory();
        } catch (Exception e) { // should never happen
            e.printStackTrace();

        }
        return null;
    }

    public ILog log() {
        return log;
    }

    public static String stackTraceToString(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    public boolean isApplicationPaused() {
        return isApplicationPaused;
    }

    // Activity lifecycle callbacks
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        if(debug)
            log.d(TAG, "detected application returning to front");
        isApplicationPaused = false;
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        if(debug)
            log.d(TAG, "detected application going to background");
        isApplicationPaused = true;
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    // Callable ------------------------------------------------------------------------------------
    public class RequestCallable implements Callable {
        EndPointRequest request;

        public RequestCallable(EndPointRequest request) {
            this.request = request;
        }

        @Override
        public Object call() throws Exception {
            try {
                IEndPoint ep = endPointMap.get(request.subscription.getEndPointClass());
                log.i(TAG, "Got request, waiting for endpoint initialization");
                // wait on endpoint if not ready
                ep.waitForInit();
                log.i(TAG, "Worker thread spinning up to serve request");
                ep.serveRequest(request);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    // HTTP utility functions ----------------------------------------------------------------------
    public Request buildGetRequest(String url) {
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        return builder.build();
    }

    public Request buildPostRequest(String url, String postData) {
        Request.Builder builder = new Request.Builder();
        builder.url(url).post(RequestBody.create(JSON, postData));
        return builder.build();
    }

    public Request buildPutRequest(String url, String putData) {
        Request.Builder builder = new Request.Builder();
        builder.url(url).put(RequestBody.create(JSON, putData));
        return builder.build();
    }

    public Request buildPostRequest(String url, RequestBody body) {
        Request.Builder builder = new Request.Builder();
        builder.url(url).post(body);
        return builder.build();
    }

    public Request buildPutRequest(String url, RequestBody body) {
        Request.Builder builder = new Request.Builder();
        builder.url(url).put(body);
        return builder.build();
    }

    public Response executeRequest(Request request) throws Exception {
        Response response = httpClient.newCall(request).execute();
        return response;
    }
}
