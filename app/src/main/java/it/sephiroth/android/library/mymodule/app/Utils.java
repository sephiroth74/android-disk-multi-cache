package it.sephiroth.android.library.mymodule.app;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by alessandro on 21/05/14.
 */
public class Utils {
	private static final String LOG_TAG = "Utils";

	/**
	 * Executes a download syncronously from a remote server
	 *
	 * @param urlname the remote url file to fetch
	 * @param params  a list of optional params to be sent in a request POST method
	 * @return The downloaded stream as {@link java.io.ByteArrayInputStream}
	 * @throws java.io.IOException
	 */
	public static ByteArrayInputStream download(String urlname, List<NameValuePair> params) throws IOException {

		HttpClient client = new DefaultHttpClient();

		if (null != params) {
			urlname += URLEncodedUtils.format(params, "utf-8");
		}

		Log.i(LOG_TAG, "download: " + urlname);

		HttpGet httpget = new HttpGet(urlname);

		try {
			HttpResponse response = client.execute(httpget);
			HttpEntity entity = response.getEntity();
			StatusLine statusLine = response.getStatusLine();
			if (null != statusLine) {
				final int statusCode = statusLine.getStatusCode();
				Log.d(LOG_TAG, "status: " + statusCode);

				if (statusCode >= 300) {
					throw new IOException("HttpStatus exception. Got " + statusCode + " response from server");
				}

			}
			else {
				Log.w(LOG_TAG, "status not available");
			}

			if (null != entity) {
				byte[] result = EntityUtils.toByteArray(entity);
				entity.consumeContent();
				return new ByteArrayInputStream(result);
			}
			else {
				throw new IOException("null response");
			}
		} catch (IOException e) {
			e.printStackTrace();
			httpget.abort();
			throw e;
		}
	}
}
