package com.mrpinghe.android.holonote.receivers;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mrpinghe.android.holonote.helpers.Const;
import com.mrpinghe.android.holonote.helpers.Util;

public class HoloNoteBroadcastReceiver extends BroadcastReceiver {

	private static final String LOG_TAG = "HNBroadcastReceiver";
	private Context mCtx;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		mCtx = context;
		String action = intent.getAction();
		int method = Const.INVALID_INT;
		
		if (!Util.isEmpty(action) && "android.intent.action.BOOT_COMPLETED".equals(action)) {
			method = Const.RESET_AUTO_BACKUP;
		}
		else {
			Bundle extras = intent.getExtras();
			method = extras.getInt(Const.BC_METHOD);
		}
		
		Log.i(LOG_TAG, "Received intent method: " + method);
		switch (method) {
			case Const.BACKUP:
				new InstantBackupTask().execute(Const.BACKUP);
				break;
			case Const.RECOVER:
				new InstantBackupTask().execute(Const.RECOVER);
				break;
			case Const.RESET_AUTO_BACKUP:
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mCtx);
				boolean isAutoBackupSet = prefs.getBoolean(Const.PREF_AUTO_BACKUP, false);
				if (isAutoBackupSet) {
					Log.i(LOG_TAG, "Set up auto backup after boot up");
					// direct back to this receiver
					Intent autoBackup = new Intent(mCtx, HoloNoteBroadcastReceiver.class);
					// tell the receiver what we want to do
					autoBackup.putExtra(Const.BC_METHOD, Const.BACKUP);
					// set up the pending intent, which will be later feed into AlarmManager
					PendingIntent autoBackupIntent = PendingIntent.getBroadcast(mCtx, Const.AUTO_BACKUP_REQ_CODE, autoBackup, PendingIntent.FLAG_UPDATE_CURRENT);
					AlarmManager am = (AlarmManager) mCtx.getSystemService(Activity.ALARM_SERVICE);
					
					// set the reference time
					Calendar todayAt3Am = Calendar.getInstance();
					todayAt3Am.set(Calendar.HOUR_OF_DAY, 3);
					todayAt3Am.set(Calendar.MINUTE, 0);
					todayAt3Am.set(Calendar.SECOND, 0);
					long oneDay = 1000 * 60 * 60 * 24; // 1000 milliseconds (1 sec) * 60 secs (1 min) * 60 mins (1 hour) * 24 hours
					am.setRepeating(AlarmManager.RTC_WAKEUP, todayAt3Am.getTimeInMillis(), oneDay, autoBackupIntent);
				}
				break;
			default:
				break;
		}
	}

	
	
	private class InstantBackupTask extends AsyncTask<Integer, Void, Integer> {
		
//		private ProgressDialog dialog;
		private int op = Const.INVALID_INT;
		private String opStr = "Unknown";

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
//			dialog = ProgressDialog.show(mCtx, "Note Backup", "Processing request....", true);
		}

		@Override
		protected Integer doInBackground(Integer... args) {
			
			
			if (!Util.isEmpty(args)) {
				op = args[0];
			}
			
			switch (op) {
				case Const.BACKUP:
					Log.i(LOG_TAG, "Backing up database...");
					opStr = "Back up";
					return Util.backupDB();
				case Const.RECOVER:
					Log.i(LOG_TAG, "Recovering database...");
					opStr = "Restore";
					return Util.recoverDB();
				default:
					return Const.IO_ERROR;
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
//			if (dialog != null && dialog.isShowing()) {
//				dialog.dismiss();
//			}
			switch (result) {
				case Const.SUCCESS:
					Util.alert(mCtx, opStr + " finished");
					break;
				default:
					Util.alert(mCtx, opStr + " failed");
					break;
			}
		}
	} // External backup task
}
