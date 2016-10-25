package dk.nodes.nfusion.interfaces;

import android.content.Context;

import dk.nodes.nfusion.EndPointError;
import dk.nodes.nfusion.EndPointRequest;
import dk.nodes.nfusion.base.EndPoint;

/**
 * Abstract representation of an endpoint used by the library and client app.
 * Client apps endpoints should be derived from the abstract base class @see EndPoint {@link EndPoint} however
 *
 * @author bison
 */
public interface IEndPoint<T> {
    /**
     * Starts the initialization of the endpoint and is called when first instantiated
     * @param context android context used primarily for file IO
     * @param model_cls The class object of the model class this endpoint returns. Fx. Profile.class
     */
    void init(Context context, Class model_cls);
    void waitForInit();
    void serveRequest(EndPointRequest request);
    void clearCache(Context c, Class model_cls);

    ICache<T> obtainCacheInstance(Context context, Class model_cls);
    IAuthentication obtainAuthenticationInstance(Context context);

    /**
     * must return the url, this endpoint will hit. Used for generating the cache id
     * together with the request params
     * @return The url
     */
    String getUrl();
    T call(EndPointRequest request) throws Exception;
    T performCall(EndPointRequest request);
    void setError(EndPointError error);
    EndPointError getError();
}
