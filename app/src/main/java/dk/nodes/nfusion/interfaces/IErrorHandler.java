package dk.nodes.nfusion.interfaces;

import dk.nodes.nfusion.EndPointError;

/**
 * Created by bison on 01/11/16.
 */

public interface IErrorHandler {
    void onEndPointError(Class endPointClass, EndPointError error);
}
