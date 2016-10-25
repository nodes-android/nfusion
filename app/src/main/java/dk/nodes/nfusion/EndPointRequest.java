package dk.nodes.nfusion;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Iterator;
import java.util.concurrent.Callable;

import dk.nodes.nfusion.interfaces.ISubscription;

/**
 * Created by bison on 05/03/16.
 */
public class EndPointRequest extends JSONObject {
    public static final String TAG = EndPointRequest.class.getSimpleName();
    public static final int STATE_BEGIN = 0;

    protected ISubscription subscription;
    public int state;
    boolean forceRefresh = false;
    boolean enableCache = true;
    // 'tis named cryptic on purpose
    public boolean ICR = false;
    boolean sequential = false;

    public EndPointRequest() {
        state = STATE_BEGIN;
    }

    @Override
    public JSONObject put(String name, boolean value) {
        try {
            return super.put(name, value);
        } catch (JSONException e) {
            nFusion.instance().log().e(e);
        }
        return this;
    }

    @Override
    public JSONObject put(String name, double value) {
        try {
            return super.put(name, value);
        } catch (JSONException e) {
            nFusion.instance().log().e(e);
        }
        return this;
    }

    @Override
    public JSONObject put(String name, int value) {
        try {
            return super.put(name, value);
        } catch (JSONException e) {
            nFusion.instance().log().e(e);
        }
        return this;
    }

    @Override
    public JSONObject put(String name, long value) {
        try {
            return super.put(name, value);
        } catch (JSONException e) {
            nFusion.instance().log().e(e);
        }
        return this;
    }

    @Override
    public JSONObject put(String name, Object value) {
        try {
            return super.put(name, value);
        } catch (JSONException e) {
            nFusion.instance().log().e(e);
        }
        return this;
    }

    public ISubscription getSubscription() {
        return subscription;
    }

    public void setSubscription(ISubscription subscription) {
        this.subscription = subscription;
    }

    public String generateCacheId()
    {
        StringBuilder sb = new StringBuilder();
        try {
            Iterator<String> keys = this.keys();
            while(keys.hasNext())
            {
                String key = keys.next();
                sb.append(key);
                sb.append(String.valueOf(this.get(key)));
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        if(sb.length() == 0)
            return "NO_ID";
        nFusion.instance().log().i(TAG, "Computed cache id = " + sb.toString());
        return sb.toString();
    }

    public boolean getForceRefresh() {
        return forceRefresh;
    }

    public void setForceRefresh(boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }

    public boolean getEnableCache() {
        return enableCache;
    }

    public void setEnableCache(boolean enableCache) {
        this.enableCache = enableCache;
    }

    public boolean isSequential() {
        return sequential;
    }

    public void setSequential(boolean sequential) {
        this.sequential = sequential;
    }
}
