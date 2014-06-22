package it.sephiroth.android.library.mymodule.app;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.sephiroth.android.library.disklrumulticache.DiskLruMultiCache;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

	private static final String LOG_TAG = "MainActivity";
	Button button;

	DiskLruMultiCache mCache;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		button = (Button) findViewById(R.id.button);
		button.setOnClickListener(this);

		try {
			mCache = new DiskLruMultiCache(this, "test", 1024 * 1024 * 10, 3);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(final View v) {

		final String url = "http://dev003.aviary.local:2347/streams/editorial/android";
		new CustomTask().execute(url);
	}

	class CustomTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(final String... params) {
			final String url = params[0];

			if (mCache.containsKey(url)) {
				Log.d(LOG_TAG, "diskCache contains key!");


				try {
					JsonMetadata metadata = mCache.getMetadata(url, JsonMetadata.class);
					Log.d(LOG_TAG, "metadata: " + metadata.fileTime);

					long diff = (System.currentTimeMillis() - metadata.fileTime) / 1000;
					Log.d(LOG_TAG, "diff: " + diff);

					if (diff < 60) {
						DiskLruMultiCache.Entry<JsonMetadata, JsonEntry> entry =
							mCache.get(url, JsonMetadata.class, JsonEntry.class);

						if (null != entry && ! TextUtils.isEmpty(entry.getValue().string)) {
							return entry.getValue().string;
						}
					} else {
						Log.w(LOG_TAG, "too much time passed... skip cache");
						mCache.remove(url);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			InputStream stream;
			try {
				stream = Utils.download(url, null);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}

			Log.d(LOG_TAG, "stream: " + stream);

			String string = null;
			try {
				string = IOUtils.toString(stream);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(stream);
			}

			if (null != string) {
				try {
					JsonEntry jsonEntry = new JsonEntry(string);
					JsonMetadata metadata = new JsonMetadata(System.currentTimeMillis());
					final DiskLruMultiCache.Entry<JsonMetadata, JsonEntry> entry = new DiskLruMultiCache.Entry(metadata,
					                                                                                           jsonEntry);
					mCache.put(url, entry);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			return string;
		}

		@Override
		protected void onPostExecute(final String s) {
			super.onPostExecute(s);

			Log.d(LOG_TAG, "result: " + s);
		}
	}

	public static class JsonEntry extends DiskLruMultiCache.EntryObject {
		String string;

		public JsonEntry() {

		}

		public JsonEntry(String string) throws JSONException {
			this.string = string;
		}

		@Override
		public void read(final InputStream stream) throws IOException {
			Log.i(LOG_TAG, "JsonEntry::read");
			string = IOUtils.toString(stream);
		}

		@Override
		public void write(final OutputStream out) throws IOException {
			Log.i(LOG_TAG, "JsonEntry::write");
			if (null != string) {
				IOUtils.write(string, out);
			}
			else {
				Log.e(LOG_TAG, "string is null!!");
			}
		}
	}

	public static class JsonMetadata extends DiskLruMultiCache.Metadata {
		long fileTime;

		public JsonMetadata() {
			super();
		}

		public JsonMetadata(long time) {
			fileTime = time;
		}

		@Override
		public void writeToParcel(final Parcel dest, final int flags) {
			Log.d(LOG_TAG, "JsonMetadata::write: " + fileTime);
			dest.writeLong(fileTime);
		}

		@Override
		public void readFromParcel(final Parcel in) {
			fileTime = in.readLong();
			Log.d(LOG_TAG, "JsonMetadata::fileTime: " + fileTime);
		}
	}
}
