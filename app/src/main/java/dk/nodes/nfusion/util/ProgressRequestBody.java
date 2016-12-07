package dk.nodes.nfusion.util;

import java.io.IOException;

import dk.nodes.nfusion.interfaces.ISubscription;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * Wrapper for OKHTTP-3 requestbodies which reports the progress of the transfer to a listener <br />
 * copied from <link>http://stackoverflow.com/questions/35528751/okhttp-3-tracking-multipart-upload-progress?noredirect=1&lq=1</link>
 *
 * @author Christian
 * @since 12/7/2016.
 */
public class ProgressRequestBody extends RequestBody {
    public static final String TAG = ProgressRequestBody.class.getSimpleName();

    protected RequestBody mDelegate;
    ISubscription subscription;
    protected CountingSink mCountingSink;

    public ProgressRequestBody(RequestBody delegate, ISubscription subscription) {
        mDelegate = delegate;
        this.subscription = subscription;
    }

    @Override
    public MediaType contentType() {
        return mDelegate.contentType();
    }

    @Override
    public long contentLength() {
        try {
            return mDelegate.contentLength();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        mCountingSink = new CountingSink(sink);
        BufferedSink bufferedSink = Okio.buffer(mCountingSink);
        mDelegate.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    protected final class CountingSink extends ForwardingSink {
        private long bytesWritten = 0;

        public CountingSink(Sink delegate) {
            super(delegate);
        }
        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            bytesWritten += byteCount;
            subscription.onProgressChanged( bytesWritten / (float)contentLength());
        }
    }
}