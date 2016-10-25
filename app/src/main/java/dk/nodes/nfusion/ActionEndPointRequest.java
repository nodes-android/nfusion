package dk.nodes.nfusion;

/**
 * Created by bison on 05/03/16.
 * This deactivates caching by default.
 */
public class ActionEndPointRequest extends EndPointRequest {
    public static final String TAG = ActionEndPointRequest.class.getSimpleName();

    public ActionEndPointRequest() {
        super();
        this.setEnableCache(false);
        this.setForceRefresh(true);
        this.setSequential(false);
    }
}
