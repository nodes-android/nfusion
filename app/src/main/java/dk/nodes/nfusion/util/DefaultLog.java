package dk.nodes.nfusion.util;

import android.util.Log;

import dk.nodes.nfusion.interfaces.ILog;

/**
 * Created by bison on 12-03-2016.
 */
public class DefaultLog implements ILog {
    private final static String TAG = "Log";
    private boolean debug = false;

    @Override
    public void d(String msg) {
        Log.d(TAG, msg);
    }

    @Override
    public void d(String tag, String msg) {
        Log.d(tag, msg);
    }

    @Override
    public void e(Exception e) {
        Log.e(TAG, e.toString());
    }

    @Override
    public void e(String msg) {
        Log.e(TAG, msg);
    }

    @Override
    public void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    @Override
    public void i(String msg) {
        Log.i(TAG, msg);
    }

    @Override
    public void i(String tag, String msg) {
        Log.i(tag, msg);
    }

    @Override
    public void v(String msg) {
        Log.v(TAG, msg);
    }

    @Override
    public void v(String tag, String msg) {
        Log.v(tag, msg);
    }

    @Override
    public void setDebug(boolean debug) {
        debug = true;
    }

}
