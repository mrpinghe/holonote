package com.mrpinghe.android.holonote.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.mrpinghe.android.holonote.helpers.Const;
import com.mrpinghe.android.holonote.helpers.Util;

public class AlarmReceiver extends BroadcastReceiver {

	private static final String LOG_TAG = "AlarmReceiver";
	private Context mCtx;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		mCtx = context;
		Bundle extras = intent.getExtras();
		int method = extras.getInt(Const.BC_METHOD);
		Log.i(LOG_TAG, "Received intent method: " + method);
		switch (method) {
			case Const.BACKUP:
				new ScheduledBackupTask().execute(Const.BACKUP);
				break;
			default:
				break;
		}
	}

	
	private class ScheduledBackupTask extends AsyncTask<Integer, Void, Integer> {
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Integer doInBackground(Integer... args) {
			int op = Const.INVALID_INT;
			
			if (!Util.isEmpty(args)) {
				op = args[0];
			}
			
			switch (op) {
				case Const.BACKUP:
					Log.i(LOG_TAG, "Backing up database...");
					return Util.backupDB();
				default:
					Log.e(LOG_TAG, "Unrecognized operation");
					return Const.IO_ERROR;
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			switch (result) {
				case Const.SUCCESS:
					Util.alert(mCtx, "Holo Note auto backup finished");
					break;
				default:
					Util.alert(mCtx, "Holo Note auto backup failed");
					break;
			}
		}
	} // External backup task

}
