package com.mrpinghe.android.holonote.activities;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import android.app.DialogFragment;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;

import com.mrpinghe.android.holonote.R;
import com.mrpinghe.android.holonote.fragments.HoloNoteDialog;
import com.mrpinghe.android.holonote.helpers.Const;
import com.mrpinghe.android.holonote.helpers.DatabaseAdapter;
import com.mrpinghe.android.holonote.helpers.NoteCursorAdapter;
import com.mrpinghe.android.holonote.helpers.Util;
import com.mrpinghe.android.holonote.interfaces.HoloNoteDialogHost;

public class ViewNoteActivity extends ListActivity implements HoloNoteDialogHost {

	private static final String LOG_TAG = "ViewNoteActivity";
	
	private DatabaseAdapter mAdapter;
	private long mNoteId;
	private int mNoteType;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Util.setPrefTheme(this);
		super.onCreate(savedInstanceState);
		Log.i(LOG_TAG, "initiating adapter");
		mAdapter = new DatabaseAdapter(this);
		mAdapter.open();
		
		Bundle b = this.getIntent().getExtras();
		if (b != null) {
			mNoteId = b.getLong(DatabaseAdapter.ID_COL, Const.INVALID_LONG);
			Log.i(LOG_TAG, "Got note ID from intent's bundle: " + mNoteId);
		}
		if (mNoteId == Const.INVALID_LONG) {
			Serializable savedState = savedInstanceState.getSerializable(DatabaseAdapter.ID_COL);
			mNoteId = savedState == null || !(savedState instanceof Long) ? Const.INVALID_LONG : (Long) savedState;
			Log.i(LOG_TAG, "Nothing in the intent, and mNoteId in saved instance state is: " + mNoteId);
		}
		this.setContentView(R.layout.view_note);
		// note display is in onResume
		this.displayNoteOrGoUp();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(LOG_TAG, "Inflate option menu for type " + mNoteType);
		switch (mNoteType) {
			case Const.TYPE_CHECKLIST:
				this.getMenuInflater().inflate(R.menu.view_checklist_menu, menu);
				break;
			case Const.TYPE_NOTE:
				this.getMenuInflater().inflate(R.menu.view_menu, menu);
				break;
			default:
				this.goUp();
				return false;
		}
		this.getActionBar().setDisplayHomeAsUpEnabled(true);
		this.getActionBar().setDisplayShowTitleEnabled(false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_edit:
				Intent i = new Intent(this, EditNoteActivity.class);
				i.putExtra(DatabaseAdapter.ID_COL, mNoteId);
				this.startActivity(i);
				break;
			case R.id.menu_delete:
				this.showDialogFragment(Const.DELETE_NOTE);
				return true;
			case R.id.menu_delete_checked:
				this.showDialogFragment(Const.DELETE_CHECKED);
				return true;
			case R.id.menu_uncheck_all:
				this.showDialogFragment(Const.UNCHECK_ALL);
				return true;
			case android.R.id.home:
				Log.i(LOG_TAG, "Home selected!");
				this.goUp();
				return true;
			default: 
				break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Log.i(LOG_TAG, id + " is clicked at position " + position);
		if (v instanceof CheckedTextView) {
			// doing asynchronous view changes, because the cursor still has old data while the database and the view are changed
			// because we force refresh on every resume, the synchronization after we leave the Activity is taken care of
			CheckedTextView ctv = (CheckedTextView) v;
			// this check view at the certain position, rather than the item, hence the asynchrony
			ctv.setChecked(!ctv.isChecked());
			mAdapter.flipCheckedStatus(id, mNoteId, ctv.isChecked());
			ctv.setPaintFlags(ctv.isChecked() ? ctv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : ctv.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
			((NoteCursorAdapter) this.getListAdapter()).getCursor().requery();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(DatabaseAdapter.ID_COL, mNoteId);
	}

	@Override
	protected void onResume() {
		Log.i(LOG_TAG, "On resume");
		super.onResume();
		this.displayNoteOrGoUp();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mAdapter != null) {
			Log.i(LOG_TAG, "Close adapter");
			mAdapter.close();
		}
	}

	public void onPositiveClick(Bundle args) {
		int dialogType = args.getInt(Const.DIALOG_TYPE);
		Log.i(LOG_TAG, "Received positive click callback from dialog. Dialog type was " + dialogType);
		switch (dialogType) {
			case Const.DELETE_NOTE:
				mAdapter.deleteNote(mNoteId);
				this.goUp();
				break;
			case Const.DELETE_CHECKED:
				mAdapter.deleteAllChecked(mNoteId);
				this.displayNoteOrGoUp();
				break;
			case Const.UNCHECK_ALL:
				mAdapter.uncheckAll(mNoteId);
				// Not sure which is faster, reload the list or loop through and asynchronously uncheck all
				this.displayNoteOrGoUp();
				break;
			default:
				Log.w(LOG_TAG, "Unknown dialog type returned positive click");
				break;
		}
	}

	public void onNegativeClick(Bundle args) {
		int dialogType = args.getInt(Const.DIALOG_TYPE);
		Log.i(LOG_TAG, "Negative click callback received for dialog type " + dialogType);
	}

	/**
	 * Put the note detail into view layout, and go up one level if no ID found
	 * 
	 * Note: it resets the ListAdapter each time it returns to the screen
	 */
	private void displayNoteOrGoUp() {
		// this will return empty cursor only if there is no note core entry
		Cursor noteCursor = mAdapter.getFullNoteOrCoreById(mNoteId);
		if (noteCursor.getCount() < 1) {
			Log.e(LOG_TAG, mNoteId + " doesn't point to a valid note");
			// if no results from getNoteOrCore(), it means no note exists under mNoteId
			this.goUp();
			return;
		}
		
		Log.i(LOG_TAG, "Found " + noteCursor.getCount() + " records for note " + mNoteId);
		mNoteType = noteCursor.getInt(noteCursor.getColumnIndexOrThrow(DatabaseAdapter.TYPE_COL));
		// set title in the proper view
		TextView titleView = (TextView) this.findViewById(R.id.show_note_title);
		titleView.setText(noteCursor.getString(noteCursor.getColumnIndexOrThrow(DatabaseAdapter.TITLE_COL)));
		
		switch (mNoteType) {
			case Const.TYPE_NOTE:
				this.startManagingCursor(noteCursor); // the cursor will not be changed
				TextView bodyView = (TextView) this.findViewById(R.id.show_note_body);
				// enable scrolling
				bodyView.setMovementMethod(new ScrollingMovementMethod());
				bodyView.setText(noteCursor.getString(noteCursor.getColumnIndex(DatabaseAdapter.TEXT_COL)));
				return;
			case Const.TYPE_CHECKLIST:
				Cursor listCursor = mAdapter.getFullNoteOrNoneById(mNoteId);
				this.startManagingCursor(listCursor); // all changes to the cursor is over
				// use empty cursor if there is only title. Create a new list adapter upon creating the activity and each subsequent resume
				this.setListAdapter(new NoteCursorAdapter(this, R.layout.view_checklist_row, listCursor, 
						new String[]{DatabaseAdapter.TEXT_COL}, new int[]{R.id.show_checklist_item}));
				return;
			default:
				Log.w(LOG_TAG, "Display note got an unkonwn type: " + mNoteType);
				break;
		}
	}
	
	private void showDialogFragment(int type) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(Const.DIALOG_TYPE, type);
		DialogFragment frag = HoloNoteDialog.newInstance(params);
		frag.show(this.getFragmentManager(), "view_note_activity_dialog");
	}
	
	private void goUp() {
		Intent upInt = new Intent(this, ListNoteActivity.class);
		upInt.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		this.startActivity(upInt);
	}
}
