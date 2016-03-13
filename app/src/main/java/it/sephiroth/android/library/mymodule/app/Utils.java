package it.sephiroth.android.library.mymodule.app;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by alessandro on 21/05/14.
 */
public class Utils {
    private static final String LOG_TAG = "Utils";

    /**
     * Executes a download syncronously from a remote server
     *
     * @param urlname the remote url file to fetch
     * @return The downloaded stream as {@link java.io.ByteArrayInputStream}
     * @throws java.io.IOException
     */
    public static ByteArrayInputStream download(String urlname) throws IOException {
        URL url = new URL(urlname);
        HttpURLConnection conn = null;
        InputStream stream = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            stream = conn.getInputStream();
            final byte[] bytes = IOUtils.toByteArray(stream);
            return new ByteArrayInputStream(bytes);
        } finally {
            if (null != conn) {
                conn.disconnect();
            }
            if (null != stream) {
                IOUtils.closeQuietly(stream);
            }
        }
    }
}
