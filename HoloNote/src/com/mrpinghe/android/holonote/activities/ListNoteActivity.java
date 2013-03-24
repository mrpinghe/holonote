package com.mrpinghe.android.holonote.activities;

import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.mrpinghe.android.holonote.R;
import com.mrpinghe.android.holonote.fragments.HoloNoteDialog;
import com.mrpinghe.android.holonote.helpers.Const;
import com.mrpinghe.android.holonote.helpers.DatabaseAdapter;
import com.mrpinghe.android.holonote.helpers.HoloNoteCursorAdapter;
import com.mrpinghe.android.holonote.helpers.Util;
import com.mrpinghe.android.holonote.interfaces.HoloNoteDialogHost;
import com.mrpinghe.android.holonote.receivers.HoloNoteBroadcastReceiver;

public class ListNoteActivity extends ListActivity implements HoloNoteDialogHost {

	private static final String LOG_TAG = "ListNoteActivity";

	private static final String CHOOSE_NOTE_TYPE = "Add New...";

	private static final String[] POSSIBLE_NOTE_TYPES = new String[]{"Checklist", "Text"}; // the order is important

	private DatabaseAdapter mAdapter;
	private int mNoteType = Const.INVALID_INT;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// set themes
		Util.setPrefTheme(this);
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.list_notes);
		mAdapter = new DatabaseAdapter(this);
		// cannot do open readonly here since the first start up will need to create tables
		mAdapter.open();
		this.populateNoteList();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.list_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_new:
		        Log.i(LOG_TAG, "Sending intent to create a new note");
		        this.showDialog(R.id.menu_new);
				return true;
			case R.id.menu_settings:
				Log.i(LOG_TAG, "Sending to preference activity");
				Intent setInt = new Intent(this, SettingsActivity.class);
				this.startActivity(setInt);
				return true;
			case R.id.menu_backup:
				Log.i(LOG_TAG, "Backing up all to SD card");
				Intent bkInt = new Intent(this, HoloNoteBroadcastReceiver.class);
				bkInt.putExtra(Const.BC_METHOD, Const.BACKUP);
				this.sendBroadcast(bkInt);
				return true;
			case R.id.menu_restore:
				Log.i(LOG_TAG, "Restoring all from SD card");
				Map<String, Object> params = new HashMap<String, Object>();
				params.put(Const.DIALOG_TYPE, Const.RECOVER_ALL);
				HoloNoteDialog frag = HoloNoteDialog.newInstance(params);
				frag.show(this.getFragmentManager(), "preference_activity_dialog");
				return true;
			default:
		        Log.i(LOG_TAG, "Default option item select");		
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.i(LOG_TAG, "clicked item " + id + " at position: " + position);
		super.onListItemClick(l, v, position, id);
		Log.i(LOG_TAG, "Creating intent with extra");
		Intent viewInt = new Intent(this, ViewNoteActivity.class);
		viewInt.putExtra(DatabaseAdapter.ID_COL, id);
		Log.i(LOG_TAG, "Sending intent to view a note");
		this.startActivity(viewInt);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		switch (id) {
			case R.id.menu_new:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(CHOOSE_NOTE_TYPE);
				builder.setItems(POSSIBLE_NOTE_TYPES, new OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						// TYPE_CHECKLIST = 1, TYPE_NOTE = 2, PNT[0] = Checklist, PNT[1] = Note
						mNoteType = which + 1;
						Intent newNoteInt = new Intent(ListNoteActivity.this, EditNoteActivity.class);
						newNoteInt.putExtra(DatabaseAdapter.TYPE_COL, mNoteType);
						ListNoteActivity.this.startActivity(newNoteInt);
					}
				});
				dialog = builder.create();
				break;
		}
		return dialog;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mAdapter != null) {
			// close the adapter
			mAdapter.close();
		}
	}

	/**
	 * Populate the list of notes we have
	 */
	private void populateNoteList() {
		// we only want cores, no content needed
		Cursor c = mAdapter.getAllNoteCores(this);
		this.startManagingCursor(c);
		// used to map between columns and views
		String[] fromFields = new String[]{DatabaseAdapter.TITLE_COL};
		int[] toViews = new int[]{R.id.list_note_title_item};
		// custom cursor adapter to apply effect to each view if needed
		HoloNoteCursorAdapter sca = new HoloNoteCursorAdapter(this, R.layout.list_notes_row, c, fromFields, toViews);
		this.setListAdapter(sca);
	}


	@Override
	public void onPositiveClick(Bundle args) {
		// Dialog host callback
		int dialogType = args.getInt(Const.DIALOG_TYPE);
		if (dialogType == Const.RECOVER_ALL) {
			Intent recInt = new Intent(this, HoloNoteBroadcastReceiver.class);
			recInt.putExtra(Const.BC_METHOD, Const.RECOVER);
			this.sendBroadcast(recInt);
		}
	}

	@Override
	public void onNegativeClick(Bundle args) {
		// don't do anything with negative click
//		int dialogType = args.getInt(Const.DIALOG_TYPE);
	}

}
