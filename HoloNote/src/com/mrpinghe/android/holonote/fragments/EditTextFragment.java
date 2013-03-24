package com.mrpinghe.android.holonote.fragments;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.mrpinghe.android.holonote.R;
import com.mrpinghe.android.holonote.activities.ListNoteActivity;
import com.mrpinghe.android.holonote.activities.ViewNoteActivity;
import com.mrpinghe.android.holonote.helpers.Const;
import com.mrpinghe.android.holonote.helpers.DatabaseAdapter;
import com.mrpinghe.android.holonote.interfaces.HoloNoteDialogHost;

public class EditTextFragment extends Fragment implements HoloNoteDialogHost {
	
	private static final String LOG_TAG = "EditTextFragment";
	private long mNoteId = Const.INVALID_LONG;
	private DatabaseAdapter mAdapter;
	private MenuItem mPriorityPicker;
	private int mPriority = Const.LEVEL_DEFAULT;

	public static EditTextFragment newInstance(Map<String, Object> params) {
		EditTextFragment frag = new EditTextFragment();
		Bundle args = new Bundle();
		if (params.get(DatabaseAdapter.ID_COL) != null) {
			args.putLong(DatabaseAdapter.ID_COL, (Long) params.get(DatabaseAdapter.ID_COL));
		}
		frag.setArguments(args);
		return frag;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new DatabaseAdapter(this.getActivity());
		mAdapter.open();
		Bundle args = this.getArguments();
		mNoteId = args.getLong(DatabaseAdapter.ID_COL, Const.INVALID_LONG);
		if (savedInstanceState != null && mNoteId == Const.INVALID_LONG) {
			Serializable savedId = savedInstanceState.getSerializable(DatabaseAdapter.ID_COL);
			mNoteId = savedId != null && savedId instanceof Long ? (Long) savedId : Const.INVALID_LONG;
		}
		this.setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.edit_note, container, false);
		
		Cursor note = mAdapter.getFullNoteOrCoreById(mNoteId);
		if (note.getCount() > 0) {
			String title = note.getString(note.getColumnIndexOrThrow(DatabaseAdapter.TITLE_COL));
			EditText titleView = (EditText) rootView.findViewById(R.id.edit_note_title);
			titleView.setText(title);
			
			String content = note.getString(note.getColumnIndexOrThrow(DatabaseAdapter.TEXT_COL));
			EditText contentView = (EditText) rootView.findViewById(R.id.edit_note_body);
			contentView.setText(content);
			
			mPriority = note.getInt(note.getColumnIndexOrThrow(DatabaseAdapter.PRIORITY_COL));
		}
		
		return rootView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.edit_menu, menu);
		mPriorityPicker = menu.getItem(1);
		mPriorityPicker.setIcon(Const.LEVEL_RES_IDS[mPriority - 1]);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.menu_save:
			this.createUpdateOrDelete();
			return true;
		case android.R.id.home:
			this.goUp();
			return true;
		case R.id.menu_priority_selector:
			this.showDialogFragment(Const.PICK_PRIORITY, Const.INVALID_LONG, null);
			return true;
		default:
			Log.e(LOG_TAG, "Invalid option menu item: " + item.getItemId());
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPause() {
		super.onPause();
		this.createUpdateOrDelete();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(DatabaseAdapter.ID_COL, mNoteId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mAdapter != null) {
			mAdapter.close();
		}
	}

	@Override
	public void onPositiveClick(Bundle args) {
		int dialogType = args.getInt(Const.DIALOG_TYPE);
		switch (dialogType) {
			case Const.PICK_PRIORITY:
				int resId = args.getInt(Const.LEVEL_DRAWABLE);
				mPriorityPicker.setIcon(resId);
				mPriority = args.getInt(DatabaseAdapter.PRIORITY_COL);
				break;
			default:
				break;
		}
		
	}

	@Override
	public void onNegativeClick(Bundle args) {
//		int dialogType = args.getInt(Const.DIALOG_TYPE);
	}
	/* *************** */
	/*  helper methods */
	/* *************** */
	
	/**
	 * create a HoloNoteDialog dialogFragment and wait for callbacks
	 * 
	 * @param type
	 * @param itemId
	 * @param text
	 */
	private void showDialogFragment(int type, long itemId, String text) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(Const.DIALOG_TYPE, type);
		params.put(Const.FRAG_RSRC_ID, android.R.id.content);
		HoloNoteDialog dialogFrag = null;
		switch (type) {
			case Const.PICK_PRIORITY:
				dialogFrag = HoloNoteDialog.newInstance(params);
				dialogFrag.show(this.getFragmentManager(), "pick_priority_dialog");
				break;
			default:
				Log.e(LOG_TAG, "Dialog type not supported: " + type);
				break;
		}
	}

	/**
	 * If there is no valid note ID, we treat this as a new note, and create it in the database
	 * If there is a valid note ID, we check if there is a title or a valid item. If yes, we update, and if no, we delete
	 * For checklist, we just 
	 * @param forceCreate - if true, even if there is nothing in the note yet, we create a note with date string as title
	 */
	private void createUpdateOrDelete() {
		EditText title = (EditText) this.getView().findViewById(R.id.edit_note_title);
		EditText text = (EditText) this.getView().findViewById(R.id.edit_note_body);
		String noteTitle = title.getText().toString();
		String noteBody = text.getText().toString();
		if (mNoteId == Const.INVALID_LONG) {
			mNoteId = mAdapter.createNote(noteTitle, noteBody, Const.TYPE_TEXT, mPriority);
		}
		else {
			// updateOrDelete will check how many items this checklist has
			mAdapter.updateOrDeleteNote(mNoteId, noteTitle, noteBody, mPriority);
		}
	}
	
	/**
	 * Go up one level
	 */
	private void goUp() {
		Intent upInt;
		if (mNoteId == Const.INVALID_LONG) {
			upInt = new Intent(this.getActivity(), ListNoteActivity.class);
		}
		else {
			upInt = new Intent(this.getActivity(), ViewNoteActivity.class);
			upInt.putExtra(DatabaseAdapter.ID_COL, mNoteId);
		}
		upInt.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		this.startActivity(upInt);
	}
}
