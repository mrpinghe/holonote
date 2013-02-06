package com.mrpinghe.android.holonote.activities;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.mrpinghe.android.holonote.R;
import com.mrpinghe.android.holonote.fragments.HoloNoteDialog;
import com.mrpinghe.android.holonote.helpers.Const;
import com.mrpinghe.android.holonote.helpers.Util;
import com.mrpinghe.android.holonote.interfaces.HoloNoteDialogHost;
import com.mrpinghe.android.holonote.receivers.AlarmReceiver;

public class PreferenceActivity extends Activity implements HoloNoteDialogHost {

	private static final String LOG_TAG = "PreferenceActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(LOG_TAG, "Initialize setting view");
		Util.setPrefTheme(this);
		this.setContentView(R.layout.settings);
	}

	@Override
	protected void onPause() {
		super.onPause();
		SharedPreferences pref = this.getSharedPreferences(Const.GLOBAL_PREF_FILE, MODE_PRIVATE);
		boolean isAlreadyAuto = pref.getBoolean(Const.IS_BACKUP_AUTO, false);
		
		SharedPreferences.Editor prefEd = pref.edit();
		// write them pref
		CheckBox themeSet = (CheckBox) this.findViewById(R.id.theme_checker);
		prefEd.putBoolean(Const.IS_LIGHT_THEME, themeSet.isChecked());
		// write sorting pref
		CheckBox sortSet = (CheckBox) this.findViewById(R.id.sort_checker);
		prefEd.putBoolean(Const.IS_SORT_BY_PRIORITY, sortSet.isChecked());
		// write auto backup pref
		CheckBox autoBackupSet = (CheckBox) this.findViewById(R.id.auto_backup_checker);
		prefEd.putBoolean(Const.IS_BACKUP_AUTO, autoBackupSet.isChecked());
		prefEd.commit();

		this.manageAutoBackup(isAlreadyAuto, autoBackupSet.isChecked());
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences pref = this.getSharedPreferences(Const.GLOBAL_PREF_FILE, MODE_PRIVATE);
		
		boolean isLight = pref.getBoolean(Const.IS_LIGHT_THEME, false);
		CheckBox themeSet = (CheckBox) this.findViewById(R.id.theme_checker);
		themeSet.setChecked(isLight);
		
		boolean isSortByPriority = pref.getBoolean(Const.IS_SORT_BY_PRIORITY, false);
		CheckBox sortSet = (CheckBox) this.findViewById(R.id.sort_checker);
		sortSet.setChecked(isSortByPriority);
		
		boolean isBackupAuto = pref.getBoolean(Const.IS_BACKUP_AUTO, false);
		CheckBox autoBackupSet = (CheckBox) this.findViewById(R.id.auto_backup_checker);
		autoBackupSet.setChecked(isBackupAuto);
		
		this.setLastBackupTimeText();
	}

	private void setLastBackupTimeText() {
		File backupFile = new File(Const.EXT_DIR, Const.BACKUP_FILE_NAME);
		if (backupFile.exists()) {
			Calendar time = Calendar.getInstance();
			time.setTimeInMillis(backupFile.lastModified());
			TextView lastBackupTimeText = (TextView) this.findViewById(R.id.backup_time_text);
			Log.i(LOG_TAG, "Back up file exists. Set last modified time");
			lastBackupTimeText.setText("Last backup: " + time.getTime().toString());
		}
	}

	@Override
	public void onBackPressed() {
		// only home can start preference now, so go back = go home
		// we want to force home gets restarted, in case theme is changed
		Intent upInt = new Intent(this, ListNoteActivity.class);
		upInt.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		this.startActivity(upInt);
	}
	
	public void backupNotes(View v) {
		new InstantBackupTask().execute(Const.BACKUP);
	}
	
	public void recoverNotes(View v) {
		this.showDialogFragment(Const.RECOVER_ALL);
	}
	
	private void showDialogFragment(int recoverAll) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(Const.DIALOG_TYPE, Const.RECOVER_ALL);
		HoloNoteDialog frag = HoloNoteDialog.newInstance(params);
		frag.show(this.getFragmentManager(), "preference_activity_dialog");
	}

	public void onPositiveClick(Bundle args) {
		new InstantBackupTask().execute(Const.RECOVER);
		Log.i(LOG_TAG, "Positive click callback received for recovering notes");
	}

	public void onNegativeClick(Bundle args) {
		int dialogType = args.getInt(Const.DIALOG_TYPE);
		Log.i(LOG_TAG, "Negative click callback received for dialog type " + dialogType);
	}
	
	private void manageAutoBackup(boolean isAlreadyAuto, boolean isSet) {
		// direct to our AlarmReceiver
		Intent intent = new Intent(this, AlarmReceiver.class);
		// tell the receiver what we want to do
		intent.putExtra(Const.BC_METHOD, Const.BACKUP);
		// set up the pending intent, which will be later feed into AlarmManager
		PendingIntent autoBackupIntent = PendingIntent.getBroadcast(this, Const.AUTO_BACKUP_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager am = (AlarmManager) this.getSystemService(Activity.ALARM_SERVICE);
		// auto backup was not set up before this time, and it is now
		if (!isAlreadyAuto && isSet) {
			Log.i(LOG_TAG, "Set up auto backup");
			// set the reference time
			Calendar todayAt3Am = Calendar.getInstance();
			todayAt3Am.set(Calendar.HOUR_OF_DAY, 3);
			todayAt3Am.set(Calendar.MINUTE, 0);
			todayAt3Am.set(Calendar.SECOND, 0);
			long oneDay = 1000 * 60 * 60 * 24; // 1000 milliseconds (1 sec) * 60 secs (1 min) * 60 mins (1 hour) * 24 hours
			am.setRepeating(AlarmManager.RTC_WAKEUP, todayAt3Am.getTimeInMillis(), oneDay, autoBackupIntent);
		}
		else if (!isSet) {
			Log.i(LOG_TAG, "Cancel auto backup if applicable");
			am.cancel(autoBackupIntent);
		}
	}

	private class InstantBackupTask extends AsyncTask<Integer, Void, Integer> {
		
		private ProgressDialog dialog;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog = ProgressDialog.show(PreferenceActivity.this, "Note Backup", "Processing request....", true);
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
				case Const.RECOVER:
					Log.i(LOG_TAG, "Recovering database...");
					return Util.recoverDB();
				default:
					return Const.IO_ERROR;
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			if (dialog != null && dialog.isShowing()) {
				dialog.dismiss();
			}
			switch (result) {
				case Const.SUCCESS:
					Util.alert(PreferenceActivity.this, "Operation finished");
					PreferenceActivity.this.setLastBackupTimeText();
					break;
				default:
					Util.alert(PreferenceActivity.this, "Operation failed");
			}
		}
	} // External backup task

}
