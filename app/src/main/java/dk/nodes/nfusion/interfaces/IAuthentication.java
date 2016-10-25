package dk.nodes.nfusion.interfaces;


import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by bison on 05/03/16.
 */
public interface IAuthentication {
    boolean detectFailedAuth(Response r);
    Request prepareRequest(Request r);
    // this can block for a long time
    boolean obtain();
    void setAbortedByUser(boolean aborted);
    boolean getAbortedByUser();
}
