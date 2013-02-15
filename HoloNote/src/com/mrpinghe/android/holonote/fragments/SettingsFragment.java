package com.mrpinghe.android.holonote.fragments;

import java.io.File;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.mrpinghe.android.holonote.R;
import com.mrpinghe.android.holonote.helpers.Const;
import com.mrpinghe.android.holonote.receivers.HNBroadcastReceiver;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

	private static final String LOG_TAG = "SettingsFragment";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "Starting settings fragment");
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.prefs);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		this.getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		File backupFile = new File(Const.EXT_DIR, Const.BACKUP_FILE_NAME);
		if (backupFile.exists()) {
			Calendar time = Calendar.getInstance();
			time.setTimeInMillis(backupFile.lastModified());
			Preference autoBackupPref = this.findPreference(Const.PREF_AUTO_BACKUP);
			autoBackupPref.setSummary("Last Backup: " + time.getTime().toString());
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		this.getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
		Log.d(LOG_TAG, "Preference changed");
		if (Const.PREF_AUTO_BACKUP.equals(key)) {
			Log.d(LOG_TAG, "Auto back up setting changed");
			boolean isAutoBackupOn = pref.getBoolean(Const.PREF_AUTO_BACKUP, false);
			// direct to our AlarmReceiver
			Intent intent = new Intent(this.getActivity(), HNBroadcastReceiver.class);
			// tell the receiver what we want to do
			intent.putExtra(Const.BC_METHOD, Const.BACKUP);
			// set up the pending intent, which will be later feed into AlarmManager
			PendingIntent autoBackupIntent = PendingIntent.getBroadcast(this.getActivity(), Const.AUTO_BACKUP_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager am = (AlarmManager) this.getActivity().getSystemService(Activity.ALARM_SERVICE);
			
			Log.d(LOG_TAG, "Auto back up setting changed to " + isAutoBackupOn);
			if (isAutoBackupOn) {
				Log.i(LOG_TAG, "Set up auto backup");
				// set the reference time
				Calendar todayAt3Am = Calendar.getInstance();
				todayAt3Am.set(Calendar.HOUR_OF_DAY, 3);
				todayAt3Am.set(Calendar.MINUTE, 0);
				todayAt3Am.set(Calendar.SECOND, 0);
				long oneDay = 1000 * 60 * 60 * 24; // 1000 milliseconds (1 sec) * 60 secs (1 min) * 60 mins (1 hour) * 24 hours
				am.setRepeating(AlarmManager.RTC_WAKEUP, todayAt3Am.getTimeInMillis(), oneDay, autoBackupIntent);
				// the backup occurs right after it's turned on as well as everyday at 3am, so we display the last backup time
				Preference autoBackupPref = this.findPreference(key);
				autoBackupPref.setSummary("Last Backup: " + Calendar.getInstance().getTime().toString());
			}
			else {
				Log.i(LOG_TAG, "Cancel auto backup if applicable");
				am.cancel(autoBackupIntent);
			}
		}
	}

}