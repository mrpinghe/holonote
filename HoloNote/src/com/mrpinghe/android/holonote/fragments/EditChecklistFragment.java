package com.mrpinghe.android.holonote.fragments;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.app.ListFragment;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.mrpinghe.android.holonote.R;
import com.mrpinghe.android.holonote.activities.ListNoteActivity;
import com.mrpinghe.android.holonote.activities.ViewNoteActivity;
import com.mrpinghe.android.holonote.helpers.Const;
import com.mrpinghe.android.holonote.helpers.DatabaseAdapter;
import com.mrpinghe.android.holonote.helpers.HoloNoteCursorAdapter;
import com.mrpinghe.android.holonote.helpers.Util;
import com.mrpinghe.android.holonote.interfaces.HoloNoteDialogHost;

public class EditChecklistFragment extends ListFragment implements HoloNoteDialogHost {

	private static final String LOG_TAG = "EditChecklistFragment";
	private DatabaseAdapter mAdapter;
	private long mNoteId = Const.INVALID_LONG;
	private int mPriority = Const.LEVEL_DEFAULT;
	private MenuItem mPriorityPicker = null;

	public static EditChecklistFragment newInstance(Map<String, Object> params) {
		
		EditChecklistFragment frag = new EditChecklistFragment();
		Bundle args = new Bundle();
		if (params.get(DatabaseAdapter.ID_COL) != null) {
			args.putLong(DatabaseAdapter.ID_COL, (Long) params.get(DatabaseAdapter.ID_COL));
		}
		frag.setArguments(args);
		
		return frag;
	}
	/* ****************** */
	/*  lifecycle methods */
	/* ****************** */
	
	/* (non-Javadoc)
	 * @see android.app.Fragment#onCreate(android.os.Bundle)
	 * This should initialize all member variables
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new DatabaseAdapter(this.getActivity());
		mAdapter.open();
		
		Bundle args = this.getArguments();
		mNoteId = args.getLong(DatabaseAdapter.ID_COL, Const.INVALID_LONG);

		if (mNoteId == Const.INVALID_LONG && savedInstanceState != null) {
			Serializable savedId = savedInstanceState.getSerializable(DatabaseAdapter.ID_COL);
			mNoteId = (savedId != null && savedId instanceof Long) ? (Long) savedId : Const.INVALID_LONG;
		}
		// no ID == new checklist
		this.setHasOptionsMenu(true);
	}
	
	/* (non-Javadoc)
	 * @see android.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 * This should inflate the view and do whatever is needed for before/after the view is inflated
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO still don't quite understand what the "attachToRoot" boolean does to the container (root) and the fragment
		View rootView = inflater.inflate(R.layout.edit_checklist, container, false);
		// if we cannot find a checklist based on the ID, we assume it's for creating a new one
		Cursor note = mAdapter.getFullNoteOrCoreById(mNoteId);
		// Check if this is an edit request. If it is, populate the title
		if (note.getCount() > 0) {
			String title = note.getString(note.getColumnIndexOrThrow(DatabaseAdapter.TITLE_COL));
			EditText titleView = (EditText) rootView.findViewById(R.id.edit_note_title);
			titleView.setText(title);
			
			mPriority = note.getInt(note.getColumnIndexOrThrow(DatabaseAdapter.PRIORITY_COL));
			// we only need this cursor to display the title. 
			// We want to use FullNoteOrNone, so that when there is no checklist item, the view will display nothing rather than an empty item
			note.close();
			// we use this cursor to populate the list view.
			note = mAdapter.getFullNoteOrNoneById(mNoteId);
		}
		// if this is a new checklist, we also want to initialize the adapter, 
		// so we only need to switch cursor once mNoteId is no longer INVALID_LONG
		this.setListAdapter(new HoloNoteCursorAdapter(this.getActivity(), R.layout.edit_checklist_row, 
				note, new String[]{DatabaseAdapter.TEXT_COL}, new int[]{R.id.edit_checklist_item}));
		
		// register listener
		rootView.findViewById(R.id.add_new_item_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EditChecklistFragment.this.addChecklistItem(v);
			}
		});
		
		return rootView;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		TextView textItem = (TextView) v.findViewById(R.id.edit_checklist_item);
		String text = textItem.getText().toString();
		// pop up a dialog
		this.showDialogFragment(Const.EDIT_ITEM, id, text);
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
			this.createUpdateOrDelete(false);
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
	public void onResume() {
		super.onResume();
		((HoloNoteCursorAdapter) this.getListAdapter()).getCursor().requery();
	}

	/* (non-Javadoc)
	 * @see android.app.Fragment#onPause()
	 * This should save things into persistent storage, i.e. database
	 */
	@Override
	public void onPause() {
		super.onPause();
		this.createUpdateOrDelete(false);
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
		((HoloNoteCursorAdapter) this.getListAdapter()).getCursor().close();
	}

	@Override
	public void onPositiveClick(Bundle args) {
		int dialogType = args.getInt(Const.DIALOG_TYPE);
		switch (dialogType) {
			case Const.EDIT_ITEM:
				long itemId = args.getLong(DatabaseAdapter.ID_COL);
				String string = args.getString(DatabaseAdapter.TEXT_COL);
				if (Util.isEmpty(string)) {
					mAdapter.deleteChecklistItem(itemId);
				}
				else {
					mAdapter.updateItemContent(itemId, string);
				}
				// onResume is not called after dialog is dismissed so we requery manually
				((HoloNoteCursorAdapter) this.getListAdapter()).getCursor().requery();
				break;
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
	 * called in the view as an onClick event
	 * @param v
	 */
	public void addChecklistItem(View v) {
    	EditText newItemView = (EditText) this.getView().findViewById(R.id.new_checklist_item_text);
    	// ((RelativeLayout) v.getParent()).findViewById(R.id.new_checklist_item_text); // this may be more efficient??
    	String newItem = newItemView.getText().toString();
    	if (Util.isEmpty(newItem)) {
    		return;
    	}
    	
		if (mNoteId == Const.INVALID_LONG) {
			// create a note with the title first
			this.createUpdateOrDelete(true);
		}
		
		mAdapter.addChecklistItem(newItem, mNoteId);
		((HoloNoteCursorAdapter) this.getListAdapter()).getCursor().requery();
		this.getListView().setSelection(this.getListAdapter().getCount() - 1);
		newItemView.setText(null);
	}

	/**
	 * delete the checklist item next to the clicked delete button
	 * 
	 * TODO not used at this moment, consider using long press and change to Roman's awesome slide delete
	 * @param v
	 */
	public void delChecklistItem(View v) {
		// although v is actually the button, it has the same position value as its parent, since its parent is the unit of the list view
		int position = this.getListView().getPositionForView(v);
		long itemId = this.getListView().getItemIdAtPosition(position);
		mAdapter.deleteChecklistItem(itemId);
		((HoloNoteCursorAdapter) this.getListAdapter()).getCursor().requery();
	}
	
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
			case Const.EDIT_ITEM:
				params.put(DatabaseAdapter.ID_COL, itemId);
				params.put(DatabaseAdapter.TEXT_COL, text);
				dialogFrag = HoloNoteDialog.newInstance(params);
				dialogFrag.show(this.getFragmentManager(), "edit_text_dialog");
				break;
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
	private void createUpdateOrDelete(boolean forceCreate) {
		EditText title = (EditText) this.getView().findViewById(R.id.edit_note_title);
		String listTitle = title.getText().toString();
		if (mNoteId == Const.INVALID_LONG) {
			if (forceCreate && Util.isEmpty(listTitle)) {
				listTitle = (new Date()).toLocaleString();
			}
			mNoteId = mAdapter.createNote(listTitle, null, Const.TYPE_CHECKLIST, mPriority);
			((HoloNoteCursorAdapter) this.getListAdapter()).changeCursor(mAdapter.getFullNoteOrNoneById(mNoteId));
		}
		else {
			// updateOrDelete will check how many items this checklist has
			mAdapter.updateOrDeleteNote(mNoteId, listTitle, null, mPriority);
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
