package com.mrpinghe.android.holonote.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Collection;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * TODO: add comments and logs
 * 
 * @author heping
 *
 */
public class Util {

	private static final String LOG_TAG = "Util";
	
	public static boolean isEmpty(Object obj) {
		
		if (obj instanceof Collection) {
			Collection<?> col = (Collection<?>) obj;
			return col.size() <= 0;
		}
		else if (obj instanceof Object[]) {
			Object[] arry = (Object[]) obj;
			return arry.length <= 0;
		}
		else {
			return obj == null || obj.toString().trim().length() == 0;
		}
	}

	public static boolean isNoteTypeSupported(int noteType) {
		return noteType == Const.TYPE_TEXT || noteType == Const.TYPE_CHECKLIST;
	}
	
	public static void alert(Context ctx, String msg) {
    	Toast toast = Toast.makeText(ctx, msg, Toast.LENGTH_LONG);
    	toast.show();
    }
	
	public static void setPrefTheme(Context ctx) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		if (pref.getBoolean(Const.PREF_LIGHT_THEME, false)) {
			ctx.setTheme(android.R.style.Theme_Holo_Light);
		}
	}
	
	public static boolean isSortByPriority(Context ctx) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
		return pref.getBoolean(Const.PREF_SORT_PRIORITY, false);
	}

	public static int recoverDB() {
		
		File dbFile = new File(Const.DATABASE_DIR, DatabaseAdapter.DB_NAME);
		Log.i(LOG_TAG, "Looking for DB file: " + dbFile.getAbsolutePath());
		File dbTempFile = null;
		
		if (dbFile.exists()) {
			Log.i(LOG_TAG, "DB file exists. Create a temp backup");
			dbTempFile = new File(Const.DATABASE_DIR, DatabaseAdapter.DB_NAME + "_temp");
			try {
				if (!dbTempFile.createNewFile()) {
					Log.e(LOG_TAG, "Creat temp file failed. Abort");
					dbTempFile.delete();
					return Const.IO_ERROR;
				}
				int copyStatus = copyFile(dbFile, dbTempFile);
				if (copyStatus != Const.SUCCESS) {
					Log.e(LOG_TAG, "Copy to temp failed. Abort");
					dbTempFile.delete();
					return copyStatus;
				}
			} catch (IOException e) {
				Log.e(LOG_TAG, "Copy to temp failed. Abort. Exception: " + e.getMessage());
				dbTempFile.delete();
				return Const.IO_ERROR;
			}
		}
		else {
			try {
				Log.i(LOG_TAG, "No DB file found. Create an empty one");
				dbFile.createNewFile();
			} catch (IOException e) {
				Log.e(LOG_TAG, "Create empty DB file failed");
				return Const.IO_ERROR;
			}
		}
		
		File backupFile = new File(Const.EXT_DIR, Const.BACKUP_FILE_NAME);
		Log.i(LOG_TAG, "Looking for back up file");
		if (!backupFile.exists()) {
			Log.e(LOG_TAG, "No backup file. Abort");
			if (dbTempFile != null) {
				dbTempFile.delete();
			}
			return Const.FILE_NOT_FOUND;
		}
		else {
			Log.i(LOG_TAG, "Back up found. Recovering");
			int copyStatus = copyFile(backupFile, dbFile);
			if (copyStatus != Const.SUCCESS && dbTempFile != null) {
				Log.e(LOG_TAG, "Recovery failed. Recover with temp file. Abort");
				copyFile(dbTempFile, dbFile);
			}
			if (dbTempFile != null) {
				dbTempFile.delete();
			}
			return copyStatus;
		}
	}
	
	public static int backupDB() {
		File dbFile = new File(Const.DATABASE_DIR, DatabaseAdapter.DB_NAME);
		Log.i(LOG_TAG, "Looking for DB file: " + dbFile.getAbsolutePath());
		if (!dbFile.exists()) {
			Log.e(LOG_TAG, "No DB file found. Abort.");
			return Const.FILE_NOT_FOUND;
		}
		
		File backupDir = new File(Const.EXT_DIR);
		Log.i(LOG_TAG, "Looking for back up directory");
		if (!backupDir.exists()) {
			Log.i(LOG_TAG, "No directory yet, create one");
			if (!backupDir.mkdirs()) {
				Log.e(LOG_TAG, "Create directory failed. Abort.");
				return Const.WRITE_ERROR;
			}
		}
		
		File backupFile = new File(backupDir, Const.BACKUP_FILE_NAME);
		try {
			Log.i(LOG_TAG, "Start copy db file to back up dir");
			backupFile.createNewFile();
			return copyFile(dbFile, backupFile);
		} catch (IOException e) {
			Log.e(LOG_TAG, "IO Exception: " + e.getMessage());
			return Const.IO_ERROR;
		}
	}

	private static int copyFile(File src, File dest) {
		
		FileInputStream srcInput = null;
		FileOutputStream destOutput = null;
		FileChannel srcChan = null;
		FileChannel destChan = null;
		
		try {
			srcInput = new FileInputStream(src);
			destOutput = new FileOutputStream(dest);
			srcChan = srcInput.getChannel();
			destChan = destOutput.getChannel();
			long byteTransferred = srcChan.transferTo(0, srcChan.size(), destChan);
			Log.i(LOG_TAG, "Byte transferred: " + byteTransferred);
			if (byteTransferred == srcChan.size()) {
				Log.i(LOG_TAG, "Size matches. Success!");
				return Const.SUCCESS;
			}
			else {
				Log.e(LOG_TAG, "Didn't copy every byte, because the db file size is: " + srcChan.size());
				dest.delete();
				return Const.COPY_ERROR;
			}
		} catch (FileNotFoundException e) {
			Log.i(LOG_TAG, "File not found during copying: " + e.getMessage());
			return Const.FILE_NOT_FOUND;
		} catch (IOException e) {
			Log.i(LOG_TAG, "IO exception during copying: " + e.getMessage());
			return Const.IO_ERROR;
		} finally {
			if (srcInput != null) {
				try {
					srcInput.close();
				} catch (IOException e) {
					Log.i(LOG_TAG, "IO exception during closing srcInput: " + e.getMessage());
				}
			}
			if (destOutput != null) {
				try {
					destOutput.close();
				} catch (IOException e) {
					Log.i(LOG_TAG, "IO exception during closing destOutput: " + e.getMessage());
				}
			}
			if (srcChan != null) {
				try {
					srcChan.close();
				} catch (IOException e) {
					Log.i(LOG_TAG, "IO exception during closing srcChan: " + e.getMessage());
				}
			}
			if (destChan != null) {
				try {
					destChan.close();
				} catch (IOException e) {
					Log.i(LOG_TAG, "IO exception during closing destChan: " + e.getMessage());
				}
			}
		}
	} // copy file method
	

}
