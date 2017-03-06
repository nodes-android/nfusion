package dk.nodes.nfusion;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Unified error structure - nFusion catches parseexceptions, bad parameters from the endpoints
 * missing internet connection, http errors and produce one error object to the subscriber.
 *
 * @author bison
 */
public class EndPointError {
    // check against these codes and present translated strings to users
    public static final int UNKNOWN_ERROR = 0;
    public static final int FAILED_AUTH_ERROR = 1;
    public static final int UNKNOWN_HOST_ERROR = 2;
    public static final int HTTP_ERROR = 3;
    public static final int JSON_ERROR = 4;
    public static final int REQUEST_PARAM_ERROR = 5;
    public static final int TIMEOUT_ERROR = 6;
    // this stuff should never be shown to the user, its just here to identify error codes in log
    // output
    public static final String [] errorNames = new String[]
            {"UNKNOWN_ERROR",
            "FAILED_AUTH_ERROR",
            "UNKNOWN_HOST_ERROR",
            "HTTP_ERROR",
            "JSON_ERROR",
            "REQUEST_PARAM_ERROR",
            "TIMEOUT_ERROR"};
    public int code = -1;
    public int httpCode = -1;
    // you're not guaranteed to have any of this depending on where the error happened,
    // remember your null checks.
    public Response httpResponse;
    public Request httpRequest;
    public String message = "";
    public String stackTrace = "";

    /**
     * Constructs the object and injects data
     * @param code one of the above defined error codes
     * @param httpCode if the error occured as a result of a http error, this contains the error code
     * @param httpResponse this contains the http response in conjuction with the error if one is present
     * @param httpRequest this contains the http request in conjuction with the error if one is present
     */
    public EndPointError(int code, int httpCode, Response httpResponse, Request httpRequest) {
        this.code = code;
        this.httpCode = httpCode;
        this.httpResponse = httpResponse;
        this.httpRequest = httpRequest;
    }
}
