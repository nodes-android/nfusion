package dk.nodes.nfusion.base;

import dk.nodes.nfusion.EndPointError;
import dk.nodes.nfusion.interfaces.ILog;
import dk.nodes.nfusion.nFusion;
import dk.nodes.nfusion.EndPointRequest;
import dk.nodes.nfusion.interfaces.ISubscription;

/**
 * Created by bison on 05/03/16.
 */
public abstract class Subscription<T> implements ISubscription<T> {
    public static final String TAG = Subscription.class.getSimpleName();
    private Class endPointClass;
    private Class modelClass;
    boolean subscribing;
    boolean receiveBroadcasts = true;
    ILog Log;

    public Subscription(Class cls, Class model_cls) {
        Log = nFusion.instance().log();
        endPointClass = cls;
        modelClass = model_cls;
    }

    @Override
    public void subscribe() {
        if(subscribing)
            return;
        try {
            subscribing = true;
            nFusion.instance().addSubscription(endPointClass, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unSubscribe() {
        if(!subscribing)
            return;
        subscribing = false;
        nFusion.instance().removeSubscription(this);
    }

    @Override
    public void subscribeAndRequest(EndPointRequest request) {
        subscribe();
        request(request);
    }

    @Override
    public void subscribeAndRequest() {
        EndPointRequest request = new EndPointRequest();
        subscribe();
        request(request);
    }

    @Override
    public void request(EndPointRequest request) {
        request.setSubscription(this);
        nFusion.instance().queueRequest(request);
    }

    @Override
    public void request() {
        request(new EndPointRequest());
    }

    @Override
    public void subscribeAndRequestSequential(EndPointRequest request) {
        request.setSequential(true);
        subscribeAndRequest(request);
    }

    @Override
    public void subscribeAndRequestSequential() {
        EndPointRequest request = new EndPointRequest();
        subscribeAndRequestSequential(request);
    }

    @Override
    public void requestSequential(EndPointRequest request) {
        request.setSequential(true);
        request(request);
    }

    @Override
    public void requestSequential() {
        EndPointRequest request = new EndPointRequest();
        request.setSequential(true);
        request(request);
    }

    @Override
    public void onProgressChanged(float f) {

    }

    // TODO make kickass generic error handler shaming the implementer
    @Override
    public void onError(EndPointError error) {
        StringBuilder sb = new StringBuilder();
        sb.append("Subscription received error: code " + error.code + " (" + error.errorNames[error.code] + "). httpCode " + error.httpCode + "\n");
        if(!error.message.isEmpty())
        {
            sb.append("Message: " + error.message + "\n\n");
        }
        if(!error.stackTrace.isEmpty())
        {
            sb.append("Stacktrace:\n" + error.stackTrace + "\n\n");
        }
        sb.append("Warning: I am the default error handler. Override me to react to errors.");
        String err = sb.toString();
        Log.e(TAG, err);
    }

    @Override
    public void onCacheCleared() {
        Log.d(TAG, "Cache for " + getModelClass() + " objects have been cleared.");
    }

    @Override
    public void onShowProgress() {
    }

    @Override
    public void onHideProgress() {
    }

    @Override
    public Class getEndPointClass() {
        return endPointClass;
    }

    @Override
    public Class getModelClass() {
        return modelClass;
    }

    @Override
    public boolean isSubscribing() {
        return subscribing;
    }

    public boolean getReceiveBroadcasts() {
        return receiveBroadcasts;
    }

    public void setReceiveBroadcasts(boolean receiveBroadcasts) {
        this.receiveBroadcasts = receiveBroadcasts;
    }

    public void clearCacheRequest()
    {
        EndPointRequest r = new EndPointRequest();
        r.ICR = true;
        if(subscribing)
            request(r);
        else
            subscribeAndRequest(r);
    }
}
