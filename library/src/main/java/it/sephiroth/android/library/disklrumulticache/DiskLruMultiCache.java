package it.sephiroth.android.library.disklrumulticache;

import android.content.Context;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.jakewharton.disklrucache.DiskLruCache;

import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings("unused")
public class DiskLruMultiCache {
	private static final String LOG_TAG = "DiskLruMultiCache";

	private static final int APP_VERSION = 2;
	private static final int APP_VALUES = 2;

	private static final int ENTRY_INDEX = 0;
	private static final int METADATA_INDEX = 1;

	final DiskLruCache mDiskCache;

	public DiskLruMultiCache(Context context, final String name, int maxSize) throws IOException {
		File dir = getCacheDir(context, name);
		mDiskCache = DiskLruCache.open(dir, APP_VERSION, APP_VALUES, maxSize);
	}

	public DiskLruMultiCache(Context context, final String name, int maxSize, int version) throws IOException {
		File dir = getCacheDir(context, name);
		mDiskCache = DiskLruCache.open(dir, version, APP_VALUES, maxSize);
	}

	public static File getCacheDir(Context context, final String name) {
		Log.i(LOG_TAG, "getCacheDir: " + name);

		final String storageState = Environment.getExternalStorageState();
		final File cacheDir;
		if (Environment.MEDIA_CHECKING.equals(storageState) || Environment.MEDIA_MOUNTED.equals(storageState) ||
		    ! DiskUtils.isExternalStorageRemovable()) {
			cacheDir = DiskUtils.getExternalCacheDir(context);
		}
		else {
			cacheDir = context.getCacheDir();
		}

		Log.i(LOG_TAG, "cacheDir:" + cacheDir.getAbsolutePath());
		return new File(cacheDir, name);
	}

	public <K extends Metadata, T extends EntryObject> Entry<K, T> get(
		final String key, Class<K> metadataClass, Class<T> entryObjectClass) throws DiskLruMultiCacheReadException {
		DiskLruCache.Snapshot snapshot = null;

		try {
			snapshot = mDiskCache.get(makeKey(key));
			if (null != snapshot) {
				K metadata = readMetadata(snapshot, metadataClass);
				T entry = readEntry(snapshot, entryObjectClass);
				if (null != entry) {
					return new Entry<K, T>(metadata, entry);
				}
			}
		} catch (Exception e) {
			throw new DiskLruMultiCacheReadException(e);
		} finally {
			if (null != snapshot) {
				snapshot.close();
			}
		}
		return null;
	}

	public <K extends Metadata> K getMetadata(
		final String key, Class<K> metadataClass) throws DiskLruMultiCacheReadException {
		DiskLruCache.Snapshot snapshot = null;

		try {
			snapshot = mDiskCache.get(makeKey(key));
			if (null != snapshot) {
				return readMetadata(snapshot, metadataClass);
			}
		} catch (Exception e) {
			throw new DiskLruMultiCacheReadException(e);
		} finally {
			if (null != snapshot) {
				snapshot.close();
			}
		}
		return null;
	}

	public boolean put(final String key, Entry entry) throws IOException {
		DiskLruCache.Editor editor = null;
		try {
			editor = mDiskCache.edit(makeKey(key));
			if (null == editor) {
				Log.w(LOG_TAG, "editor is null");
				return false;
			}

			writeMetadata(editor, entry.getMetadata());

			try {
				write(editor, entry);
				mDiskCache.flush();
				editor.commit();
			} catch (IOException e) {
				Log.w(LOG_TAG, "failed to write entry");
				editor.abort();
			}

		} catch (IOException e) {
			e.printStackTrace();

			try {
				if (null != editor) {
					editor.abort();
				}
			} catch (IOException e1) {
				Log.w(LOG_TAG, "abort failed", e1);
			}
		}
		return false;
	}

	private void write(final DiskLruCache.Editor editor, Entry entry) throws IOException {
		OutputStream out = null;

		try {
			out = new BufferedOutputStream(editor.newOutputStream(ENTRY_INDEX), DiskUtils.IO_BUFFER_SIZE);
			entry.getValue().write(out);
		} finally {
			if (null != out) {
				IOUtils.closeQuietly(out);
			}
		}
	}

	private <T extends EntryObject> T readEntry(DiskLruCache.Snapshot snapshot, Class<T> entryClass)
		throws IOException, IllegalAccessException, InstantiationException {
		InputStream stream = snapshot.getInputStream(ENTRY_INDEX);
		T entry = entryClass.newInstance();
		entry.read(stream);
		return entry;
	}

	private <K extends Metadata> K readMetadata(DiskLruCache.Snapshot snapshot, Class<K> metadataClass)
		throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
		InputStream input = null;
		Parcel parcel = null;
		try {
			input = snapshot.getInputStream(METADATA_INDEX);
			byte[] bytes = IOUtils.toByteArray(input);

			parcel = Parcel.obtain();
			parcel.unmarshall(bytes, 0, bytes.length);
			parcel.setDataPosition(0);

			if (null != metadataClass) {
				K metadata = metadataClass.newInstance();
				metadata.readFromParcel(parcel);
				return metadata;
			}

		} finally {
			if (null != input) {
				IOUtils.closeQuietly(input);
			}

			if (null != parcel) {
				parcel.recycle();
			}
		}
		return null;
	}

	private void writeMetadata(
		DiskLruCache.Editor editor, Metadata metadata) throws IOException {
		OutputStream output = null;
		try {
			final Parcel parcel = Parcel.obtain();
			if (null != metadata) {
				metadata.writeToParcel(parcel, 0);
			}
			byte[] bytes = parcel.marshall();
			parcel.recycle();

			output = editor.newOutputStream(METADATA_INDEX);
			output.write(bytes);

		} finally {
			if (null != output) {
				IOUtils.closeQuietly(output);
			}
		}
	}

	public long size() {
		return mDiskCache.size();
	}

	public void remove(final String key) throws IOException {
		// Log.i( LOG_TAG, "remove: " + key );
		mDiskCache.remove(makeKey(key));
	}

	public boolean containsKey(String key) {
		// Log.i( LOG_TAG, "containsKey: " + key );

		DiskLruCache.Snapshot snapshot = null;
		try {
			snapshot = mDiskCache.get(makeKey(key));
			return snapshot != null;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (snapshot != null) {
				snapshot.close();
			}
		}

		return false;
	}

	public long getMaxSize() {
		return mDiskCache.getMaxSize();
	}

	public boolean isClosed() {
		return mDiskCache.isClosed();
	}

	public synchronized void close() throws IOException {
		Log.i(LOG_TAG, "close");
		mDiskCache.close();
	}

	public synchronized void delete() throws IOException {
		Log.i(LOG_TAG, "delete");
		mDiskCache.delete();
	}

	public File getDirectory() {
		return mDiskCache.getDirectory();
	}

	private String makeKey(final String key) {
		return DigestUtils.md5Hex(key).toLowerCase();
	}

	public static abstract class EntryObject {
		public EntryObject() {}

		public abstract void read(InputStream stream) throws IOException;

		public abstract void write(OutputStream out) throws IOException;
	}

	public static class Metadata implements Parcelable {

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(final Parcel dest, final int flags) {
		}

		public void readFromParcel(final Parcel in) {

		}
	}

	public static final class Entry<K extends Metadata, T extends EntryObject> {
		private final T object;
		private final K metadata;

		public Entry(K metadata, T object) {
			this.object = object;
			this.metadata = metadata;
		}

		public T getValue() {
			return object;
		}

		public K getMetadata() {
			return metadata;
		}
	}
}
