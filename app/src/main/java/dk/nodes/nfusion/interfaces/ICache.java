package dk.nodes.nfusion.interfaces;

/**
 * Created by bison on 05/03/16.
 */
public interface ICache<T> {
    void setListener(ICacheListener listener);
    void load();
    void save();
    T getById(String id);
    void put(String id, T obj);
    boolean isStale(String id);
    void clear();
    void clearAsync();
}
