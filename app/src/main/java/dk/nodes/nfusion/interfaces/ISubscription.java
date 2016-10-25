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

/**
 * Created by bison on 05/03/16.
 */
public interface ISubscription<T> {
    void onUpdate(T model);
    void onError(EndPointError error);
    void onCacheCleared();
    void onShowProgress();
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
