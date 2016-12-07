package dk.nodes.nfusion.interfaces;
/*

    ISubscription
    IEndPoint
        ICache
        IAuthentication - abstracts away authentication
            obtain() <-- can either return immediately if auth is obtained, otherwise go trough a lengthy process to obtain it
            detectFailedAuth(Response r) <-- examines a response and checks whether it failed due to auth
            prepareRequest(Request r) <-- add token to request header
            clear() <-- clears all token and account info

 */

import dk.nodes.nfusion.EndPointError;
import dk.nodes.nfusion.EndPointRequest;
import dk.nodes.nfusion.util.ProgressRequestBody;

/**
 * Created by bison on 05/03/16.
 */
public interface ISubscription<T> {
    void onUpdate(T model);
    void onError(EndPointError error);
    void onCacheCleared();
    void onShowProgress();
    /**
     * calls-out the progress of the current request
     * The <link {@link EndPointRequest} must contain a <link {@link ProgressRequestBody}
     * @param f progress, range is [0-1]
     */
    void onProgressChanged(float f);
    void onHideProgress();

    void subscribe();
    void subscribeAndRequest(EndPointRequest request);
    void subscribeAndRequestSequential(EndPointRequest request);
    void subscribeAndRequest();
    void subscribeAndRequestSequential();
    void unSubscribe();
    boolean isSubscribing();
    void request(EndPointRequest request);
    void request();
    void requestSequential(EndPointRequest request);
    void requestSequential();
    Class getEndPointClass();
    Class getModelClass();
    boolean getReceiveBroadcasts();
    void setReceiveBroadcasts(boolean receiveBroadcasts);
    void clearCacheRequest();
}
