package dk.nodes.nfusion.cache;

import android.content.Context;
import android.os.AsyncTask;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

import dk.nodes.nfusion.base.Cache;
import dk.nodes.nfusion.interfaces.ILog;
import dk.nodes.nfusion.nFusion;

/**
 * Created by bison on 05/03/16.
 */
public class EndpointDiskCache<T> extends SimpleDiskCache<T> {
    public static final String TAG = EndpointDiskCache.class.getSimpleName();

    String endpointName;

    public EndpointDiskCache(Context context, Class modelClass, String endpointName) {
        super(context, modelClass);
        this.endpointName = endpointName;
        makeFilename();
    }

    public EndpointDiskCache(Context context, Class modelClass, String endpointName, int stalelimit) {
        super(context, modelClass, stalelimit);
        this.endpointName = endpointName;
        makeFilename();
    }

    private void makeFilename() {
        fileName = endpointName + modelClass.getName();
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.reset();
            messageDigest.update(fileName.getBytes());
            // Do the double hash here to get the final value
            final byte[] bytes = messageDigest.digest();
            fileName = super.getStringFromBytes(bytes) + ".dat";
            infoFileName = super.getStringFromBytes(bytes) + "_info.dat";
            Log.i(TAG, modelClass.getSimpleName() + " cache filename = " + fileName);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
