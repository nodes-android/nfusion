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
public class SimpleDiskCache<T> extends Cache<T> {
    public static final String TAG = SimpleDiskCache.class.getSimpleName();
    public static final int DEFAULT_STALE_LIMIT = 120 * 1000;
    int staleLimit = DEFAULT_STALE_LIMIT;
    ConcurrentHashMap<String, T> map;
    ConcurrentHashMap<String, CacheInfo> infoMap;
    LoadTask loadTask;
    SaveTask saveTask;
    Context context;
    String fileName;
    String infoFileName;
    Class modelClass;
    ILog Log;

    public SimpleDiskCache(Context context, Class modelClass) {
        Log = nFusion.instance().log();
        infoMap = new ConcurrentHashMap<>();
        map = new ConcurrentHashMap<>();
        this.context = context;
        this.modelClass = modelClass;
        makeFilename();
    }

    public SimpleDiskCache(Context context, Class modelClass, int stalelimit) {
        Log = nFusion.instance().log();
        infoMap = new ConcurrentHashMap<>();
        map = new ConcurrentHashMap<>();
        this.context = context;
        this.modelClass = modelClass;
        this.staleLimit = stalelimit;
        makeFilename();
    }

    private void makeFilename()
    {
        fileName = modelClass.getName();
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.reset();
            messageDigest.update(fileName.getBytes());
            // Do the double hash here to get the final value
            final byte[] bytes = messageDigest.digest();
            fileName = getStringFromBytes(bytes) + ".dat";
            infoFileName = getStringFromBytes(bytes) + "_info.dat";
            Log.i(TAG, modelClass.getSimpleName() + " cache filename = " + fileName);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    protected String getStringFromBytes(final byte[] byteArray)
    {
        final StringBuffer buffer = new StringBuffer();
        for(final byte element: byteArray)
        {
            buffer.append(Integer.toString((element & 0xff) + 0x100, 16).substring(
                    1));
        }
        return buffer.toString();
    }

    @Override
    public void load() {
        loadDataAsync();
    }

    @Override
    public void save() {
        saveDataAsync();
    }

    @Override
    public T getById(String id) {
        return map.get(id);
    }

    @Override
    public void put(String id, T obj) {
        CacheInfo ci;
        if(infoMap.containsKey(id))
        {
            ci = infoMap.get(id);
        }
        else
        {
            ci = new CacheInfo();
            infoMap.put(id, ci);
        }
        ci.lastUpdate = System.currentTimeMillis();
        map.put(id, obj);
        save();
    }

    @Override
    public boolean isStale(String id) {
        if(infoMap.containsKey(id))
        {
            CacheInfo ci = infoMap.get(id);
            if(ci.timeSinceLastUpdate() > staleLimit) {
                return true;
            }
            else
                return false;
        }
        return true;
    }

    /**
     * This clears the caches and saves in the calling thread (blocking)
     */
    @Override
    public void clear() {
        Log.d(TAG, "Clearing cache (" + modelClass.getSimpleName() + ")");
        map.clear();
        infoMap.clear();
        saveData();
    }

    /**
     * This clears the caches and saves in the calling thread (nonblocking)
     */
    @Override
    public void clearAsync() {
        Log.d(TAG, "Clearing cache (" + modelClass.getSimpleName() + ")");
        map.clear();
        infoMap.clear();

        boolean res = context.deleteFile(fileName);
        if(res)
        {
            Log.d(TAG, "Deleted cache file " + fileName);
        }
        else
        {
            Log.e(TAG, "Unable to delete cache file " + fileName);
        }
    }

    public void loadDataAsync()
    {
        if(loadTask != null)
        {
            Log.d(TAG, "Load already in progress.");
            return;
        }
        loadTask = new LoadTask();
        loadTask.execute();
    }

    public void saveDataAsync()
    {

        if(saveTask != null)
        {
            Log.d(TAG, "Save already in progress.");
            return;
        }
        saveTask = new SaveTask();
        saveTask.execute();
    }

    private void saveData()
    {
        try {
            long then = System.currentTimeMillis();
            FileOutputStream e = context.openFileOutput(fileName, 0);
            ObjectOutputStream os = new ObjectOutputStream(e);
            os.writeObject(map);
            os.close();

            e = context.openFileOutput(infoFileName, 0);
            os = new ObjectOutputStream(e);
            os.writeObject(infoMap);
            os.close();

            long elapsed = System.currentTimeMillis() - then;
            Log.i(TAG, "Saving took " + elapsed + " ms, saved " + map.size() + " " + modelClass.getSimpleName() + " objects.");
            return;
        } catch (Exception e) {
            Log.e(TAG, "Saving failed (" + modelClass.getSimpleName() + ")");
            Log.e(e);
            return;
        }
    }

    public class SaveTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            saveData();
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            saveTask = null;
        }
    }


    public class LoadTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                long then = System.currentTimeMillis();
                FileInputStream e = context.openFileInput(fileName);
                ObjectInputStream is = new ObjectInputStream(e);
                map = (ConcurrentHashMap<String, T>) is.readObject();
                is.close();

                e = context.openFileInput(infoFileName);
                is = new ObjectInputStream(e);
                infoMap = (ConcurrentHashMap<String, CacheInfo>) is.readObject();
                is.close();

                long elapsed = System.currentTimeMillis() - then;
                Log.i(TAG, "Loading took " + elapsed + " ms. Loaded " + map.size() + " " + modelClass.getSimpleName() + " objects.");
                return null;
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Loading failed, file not found. (" + modelClass.getSimpleName() + ")");
                return null;
            } catch (Exception e) {
                Log.e(TAG, "Loading failed (" + modelClass.getSimpleName() +")");
                Log.e(e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void v) {
            loadTask = null;
            signalReady();
        }
    }
}
