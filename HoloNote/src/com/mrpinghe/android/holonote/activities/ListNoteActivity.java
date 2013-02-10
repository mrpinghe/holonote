package com.mrpinghe.android.holonote.activities;

import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
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
import com.mrpinghe.android.holonote.helpers.NoteCursorAdapter;
import com.mrpinghe.android.holonote.helpers.Util;
import com.mrpinghe.android.holonote.interfaces.HoloNoteDialogHost;

public class ListNoteActivity extends ListActivity implements HoloNoteDialogHost {

	private static final String LOG_TAG = "ListNoteActivity";

	private static final String CHOOSE_NOTE_TYPE = "Add New...";

	private static final String[] POSSIBLE_NOTE_TYPES = new String[]{"CHECKLIST", "NOTE"}; // the order is important

	private DatabaseAdapter mAdapter;
	private int mNoteType = Const.INVALID_INT;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Util.setPrefTheme(this);
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.list_notes);
		
        Log.i(LOG_TAG, "Creating DB adapter");		
		mAdapter = new DatabaseAdapter(this);
		// cannot do open readonly here since the first start up will need to create tables
		mAdapter.open();
        Log.i(LOG_TAG, "Populate list");
		this.populateNoteList();
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(LOG_TAG, "Inflating options menu");
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
				new InstantBackupTask().execute(Const.BACKUP);
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
					Log.i(LOG_TAG, "After dialog click event, note type is: " + mNoteType);
			        if (Util.isNoteTypeSupported(mNoteType)) {
						Intent newNoteInt = new Intent(ListNoteActivity.this, EditNoteActivity.class);
						newNoteInt.putExtra(DatabaseAdapter.TYPE_COL, mNoteType);
						ListNoteActivity.this.startActivity(newNoteInt);
			        }
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
			Log.i(LOG_TAG, "Close adapter");
			mAdapter.close();
		}
	}

	/**
	 * fill the page with a list of note titles
	 */
	private void populateNoteList() {
		Cursor c = mAdapter.getAllNoteCores(this);
		this.startManagingCursor(c);
		
		String[] fromFields = new String[]{DatabaseAdapter.TITLE_COL};
		int[] toViews = new int[]{R.id.list_note_title_item};

		NoteCursorAdapter sca = new NoteCursorAdapter(this, R.layout.list_notes_row, c, fromFields, toViews);
		this.setListAdapter(sca);
	}

	/* Back up and restore */
	
	private class InstantBackupTask extends AsyncTask<Integer, Void, Integer> {
		
		private ProgressDialog dialog;
		private int op = Const.INVALID_INT;
		private String opStr = "Unknown";

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog = ProgressDialog.show(ListNoteActivity.this, "Note Backup", "Processing request....", true);
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
			if (dialog != null && dialog.isShowing()) {
				dialog.dismiss();
			}
			switch (result) {
				case Const.SUCCESS:
					Util.alert(ListNoteActivity.this, opStr + " finished");
					break;
				default:
					Util.alert(ListNoteActivity.this, opStr + " failed");
					break;
			}
		}
	} // External backup task


	@Override
	public void onPositiveClick(Bundle args) {
		new InstantBackupTask().execute(Const.RECOVER);
		Log.i(LOG_TAG, "Positive click callback received for recovering notes");
	}

	@Override
	public void onNegativeClick(Bundle args) {
		int dialogType = args.getInt(Const.DIALOG_TYPE);
		Log.i(LOG_TAG, "Negative click callback received for dialog type " + dialogType);
	}

}
