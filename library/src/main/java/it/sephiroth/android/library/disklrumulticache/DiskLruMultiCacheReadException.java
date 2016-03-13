package it.sephiroth.android.library.disklrumulticache;

@SuppressWarnings ("unused")
public class DiskLruMultiCacheReadException extends Exception {

    private static final long serialVersionUID = -1;

    private Throwable ex;

    public DiskLruMultiCacheReadException() {
        super((Throwable) null);
    }

    public DiskLruMultiCacheReadException(Throwable cause) {
        super(cause == null ? null : cause.toString(), cause);
    }

    public DiskLruMultiCacheReadException(String detailMessage) {
        super(detailMessage, null);
    }

    public DiskLruMultiCacheReadException(String detailMessage, Throwable exception) {
        super(detailMessage);
        ex = exception;
    }

    public Throwable getException() {
        return ex;
    }

    @Override
    public Throwable getCause() {
        return ex;
    }
}
