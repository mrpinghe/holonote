package com.mrpinghe.android.holonote.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.mrpinghe.android.holonote.fragments.SettingsFragment;
import com.mrpinghe.android.holonote.helpers.Util;


public class SettingsActivity extends PreferenceActivity {

	private static final String LOG_TAG = "SettingsActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "Starting settings activity");
		Util.setPrefTheme(this);
		super.onCreate(savedInstanceState);
		this.getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
	}
	
	@Override
	public void onBackPressed() {
		// only home can start preference now, so go back = go home
		// we want to force home gets restarted, in case theme is changed
		Intent upInt = new Intent(this, ListNoteActivity.class);
		upInt.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		this.startActivity(upInt);
	}

}
