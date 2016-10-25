package dk.nodes.nfusion.interfaces;

/**
 * Copy the log family of functions to this interface and make the rest of the library use it to make
 * it easily pluggable with custom logging solutions such as NLog.
 * @author bison
 */
public interface ILog {
    void d(String msg);
    void d(String tag, String msg);
    void e(Exception e);
    void e(String msg);
    void e(String tag, String msg);
    void i(String msg);
    void i(String tag, String msg);
    void v(String msg);
    void v(String tag, String msg);
    void setDebug(boolean debug);
}
