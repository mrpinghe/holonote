package com.mrpinghe.android.holonote.activities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.mrpinghe.android.holonote.R;
import com.mrpinghe.android.holonote.fragments.HoloNoteDialog;
import com.mrpinghe.android.holonote.helpers.Const;
import com.mrpinghe.android.holonote.helpers.DatabaseAdapter;
import com.mrpinghe.android.holonote.helpers.NoteCursorAdapter;
import com.mrpinghe.android.holonote.helpers.Util;
import com.mrpinghe.android.holonote.interfaces.HoloNoteDialogHost;

public class EditNoteActivity extends ListActivity implements HoloNoteDialogHost {
	
	private static final String LOG_TAG = "EditNoteActivity";
	
	private DatabaseAdapter mAdapter;
	// promote to class level, so we can determine the correct behavior when activity paused/destroyed
	// these two values are default to zero because that is the default value for bundle.getInt/getLong
	private long mNoteId = Const.INVALID_LONG;
	private int mNoteType = Const.INVALID_INT;
	private int mPriority = Const.LEVEL_INVALID;
	// used to determine whether we attempt to create/update when activity paused
	private boolean isViewInitialized = false;
	// used to asynchronously change the item color upon priority change
	private MenuItem mPriorityItem = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		Util.setPrefTheme(this);
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG, "Initializing DB adapter");
        mAdapter = new DatabaseAdapter(this);
        mAdapter.open();
        // if no ID or type is initialized, this will go up one level
		this.initViewOrGoUp(savedInstanceState);
        Log.i(LOG_TAG, "Edit note view created");
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.getMenuInflater().inflate(R.menu.edit_menu, menu);
		this.getActionBar().setDisplayHomeAsUpEnabled(true);
		this.getActionBar().setDisplayShowTitleEnabled(false);
		// hard coded priority picker's index at action bar
		mPriorityItem = menu.getItem(1);
		mPriorityItem.setIcon(Const.LEVEL_RES_IDS[mPriority - 1]);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.menu_save:
				this.onBackPressed(); // use onPause to handle save/update. goUp() would not reflect the correct activity stack
				return true;
			case android.R.id.home:
				this.goUp();
				return true;
			case R.id.menu_priority_selector:
				mPriorityItem = item;
				this.showDialogFragment(Const.PICK_PRIORITY, Const.INVALID_LONG, null);
				return true;
			default:
				break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {
        Log.i(LOG_TAG, "Pressed back button");
		super.onPause();
		if (!isViewInitialized) {
			return;
		}
		
		if (mNoteId == Const.INVALID_LONG) {
			// this means the note was not saved when the activity is paused
	        Log.i(LOG_TAG, "Creating note");
			mNoteId = this.createNote();
			if (mNoteId != Const.INVALID_LONG) {
				Util.alert(this, "Saved");
			}
		}
		else {
			// if there is no text in the new item box in checklist edit mode, this will not do anything
			Log.i(LOG_TAG, "Updating note");
			int rowsUpdated = this.updateNote();
			if (rowsUpdated != Const.INVALID_INT) {
				Util.alert(this, "Updated");
			}
		}
		super.onBackPressed();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mAdapter != null) {
			Log.i(LOG_TAG, "Close adapter");
			mAdapter.close();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(DatabaseAdapter.TYPE_COL, mNoteType);
		outState.putSerializable(DatabaseAdapter.ID_COL, mNoteId);
	}

	/**
	 * called in the view as an onClick event
	 * @param v
	 */
	public void addChecklistItem(View v) {
		if (mNoteId == Const.INVALID_LONG) {
			// this will automatically read text from the add item EditText
			mNoteId = this.createNote();
			// success
			if (mNoteId != Const.INVALID_LONG) {
				Log.i(LOG_TAG, "A new checklist created: " + mNoteId);
				// this will always return full note because we just created one with an actual item
				Cursor noteCursor = mAdapter.getFullNoteOrNoneById(mNoteId);
				this.startManagingCursor(noteCursor);
				// although we use Activity's startManagingCursor during init, we can still close whenever we want without side effects
				// because we are not coming back to this activity from another activity before this Activity is killed already
				((NoteCursorAdapter) this.getListAdapter()).changeCursor(noteCursor);
				EditText titleView = (EditText) this.findViewById(R.id.edit_note_title);
				titleView.setText(noteCursor.getString(noteCursor.getColumnIndexOrThrow(DatabaseAdapter.TITLE_COL)));
				refreshEditChecklistView();
			}
		}
		else {
			// this will automatically read text from the add item EditText
			this.updateNote();
			refreshEditChecklistView();
		}
		this.getListView().setSelection(this.getListAdapter().getCount() - 1);
	}

	/**
	 * OnListItemClick doesn't work because each item already has a clickable element (ImageButton)
	 * Therefore, use an onclick event handler
	 * @param v
	 */
	public void editChecklistItem(View v) {
		View candidate = v.findViewById(R.id.edit_checklist_item);
		if (candidate != null) {
			TextView textItem = (TextView) candidate;
			String text = textItem.getText().toString();
			long itemId = this.getListView().getItemIdAtPosition(this.getListView().getPositionForView(v));
			Log.i(LOG_TAG, "Edit checklist item " + itemId);
			// pop up a dialog
			this.showDialogFragment(Const.EDIT_ITEM, itemId, text);
		}
	}

	/**
	 * delete the checklist item next to the clicked delete button
	 * @param v
	 */
	public void delChecklistItem(View v) {
		// although v is actually the button, it has the same position value as its parent, since its parent is the unit of the list view
		int position = this.getListView().getPositionForView(v);
		long itemId = this.getListView().getItemIdAtPosition(position);
		Log.i(LOG_TAG, "Delete item ID: " + itemId + " of note: " + mNoteId);
		mAdapter.deleteChecklistItem(itemId);
		refreshEditChecklistView();
	}
	
	/* (non-Javadoc)
	 * @see com.mrpinghe.android.holonote.interfaces.HoloNoteDialogHost#onPositiveClick(android.os.Bundle)
	 */
	public void onPositiveClick(Bundle args) {
		int dialogType = args.getInt(Const.DIALOG_TYPE);
		Log.i(LOG_TAG, "Positive click callback received. Dialog type was " + dialogType);
		switch (dialogType) {
			case Const.EDIT_ITEM:
				long itemId = args.getLong(DatabaseAdapter.ID_COL);
				Log.i(LOG_TAG, "Editing item " + itemId);
				String string = args.getString(DatabaseAdapter.TEXT_COL);
				if (Util.isEmpty(string)) {
					mAdapter.deleteChecklistItem(itemId);
				}
				else {
					mAdapter.updateItemContent(itemId, string);
				}
				this.refreshEditChecklistView();
				break;
			case Const.PICK_PRIORITY:
				int resId = args.getInt(Const.LEVEL_DRAWABLE);
				mPriorityItem.setIcon(resId);
				mPriority = args.getInt(DatabaseAdapter.PRIORITY_COL);
				Log.i(LOG_TAG, "Picking priority " + mPriority);
				break;
			default:
				Log.w(LOG_TAG, "Unknown dialog type " + dialogType);
				break;
		}
	}

	/* (non-Javadoc)
	 * @see com.mrpinghe.android.holonote.interfaces.HoloNoteDialogHost#onNegativeClick(android.os.Bundle)
	 */
	public void onNegativeClick(Bundle args) {
		int dialogType = args.getInt(Const.DIALOG_TYPE);
		Log.i(LOG_TAG, "Received negative click callback for type " + dialogType);
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
		HoloNoteDialog dialogFrag = null;
		switch (type) {
			case Const.EDIT_ITEM:
				Log.i(LOG_TAG, "Show edit text dialog for " + itemId);
				params.put(DatabaseAdapter.ID_COL, itemId);
				params.put(DatabaseAdapter.TEXT_COL, text);
				dialogFrag = HoloNoteDialog.newInstance(params);
				dialogFrag.show(this.getFragmentManager(), "edit_text_dialog");
				break;
			case Const.PICK_PRIORITY:
				Log.i(LOG_TAG, "Show priority picker");
				dialogFrag = HoloNoteDialog.newInstance(params);
				dialogFrag.show(this.getFragmentManager(), "pick_priority_dialog");
				break;
			default:
				Log.w(LOG_TAG, "No action for unknown dialog type " + type);
				break;
		}
	}

	/**
	 * no cursor reusable from the caller, just initialize our own cursor
	 */
	private void refreshEditChecklistView() {
		// this line will throw an exception if this Activity instance was not instantiated for a checklist view
		EditText newItemView = (EditText) this.findViewById(R.id.new_checklist_item_text);
		newItemView.setText("");
		((NoteCursorAdapter) this.getListAdapter()).getCursor().requery();
	}
	
	/**
	 * Retrieve information from the view and call DB adapter to create the note
	 * 
	 * @return - note ID (row ID)
	 */
	private long createNote() {
		EditText titleView = (EditText) this.findViewById(R.id.edit_note_title);
		String title = titleView.getText().toString();
		
		List<String> text = this.getNoteBody();
        Log.i(LOG_TAG, "Creating a " + mNoteType + " note with title [" + title + "] and body [" + text + "]");		

		mNoteId = mAdapter.createNote(title, text, mNoteType, mPriority);
		return mNoteId;
	}

	/**
	 * Retrieve information from the view and call DB adapter to update the note
	 * 
	 * @return
	 */
    private int updateNote() {
    	EditText titleView = (EditText) this.findViewById(R.id.edit_note_title);
    	String title = titleView.getText().toString();
    	
    	List<String> text = this.getNoteBody();
        Log.i(LOG_TAG, "Updating " + mNoteId + " note with title [" + title + "] and body [" + text + "]");		
    	
		return mAdapter.updateOrDeleteNote(mNoteId, title, text, mPriority);
	}
    
    /**
     * Get the text from the note body
     * 
     * @return
     */
	private List<String> getNoteBody() {
		List<String> text = new ArrayList<String>();
		switch (mNoteType) {
	        case Const.TYPE_NOTE:
	    		EditText textView = (EditText) this.findViewById(R.id.edit_note_body);
	        	if (!Util.isEmpty(textView.getText().toString())) {
	        		text.add(textView.getText().toString());
	        	}
	    		break;
	        case Const.TYPE_CHECKLIST:
	        	// because edit existing checklist item is done in dialog, they are already in database
	        	// only the text at new item text box should be added upon update, if any
	        	EditText newItem = (EditText) this.findViewById(R.id.new_checklist_item_text);
	        	// have to check the actual text as well here because otherwise, an empty line will be added to the checklist if the title is not empty
	        	if (!Util.isEmpty(newItem.getText().toString())) {
	        		text.add(newItem.getText().toString());
	        	}
	        	// don't auto save what's in new item box, in case the screen blacks out during adding
	        	break;
        	default:
        		Log.e(LOG_TAG, "Type not supported: " + mNoteType);
            	Util.alert(this, "Type not supported");
            	break;
        }
		return text;
	}
	
	/**
	 * Go up one level
	 */
	private void goUp() {
		Intent upInt;
		if (mNoteId == Const.INVALID_LONG) {
			upInt = new Intent(this, ListNoteActivity.class);
		}
		else {
			upInt = new Intent(this, ViewNoteActivity.class);
			upInt.putExtra(DatabaseAdapter.ID_COL, mNoteId);
		}
		upInt.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		this.startActivity(upInt);
	}
	
	/**
	 * Populate the view for either create new note or update existing note based on the cursor content and mNoteType.
	 * Go up one level if no view to populate
	 * * mNoteId can be INVALID_LONG -> new note
	 * * mNoteType cannot be INVALID_TYPE -> if none of 1) initialized in initMember, 2) stored in saved instance state, 
	 * or 3) mNoteId is valid, and read type from the database, occurred correctly, this will go up a level
	 * * mPriority is LEVEL_DEFAULT if mNoteId is INVALID_LONG, and read from the database if mNoteId exists
	 * 
	 * @param noteCursor
	 */
	private void initViewOrGoUp(Bundle savedInstanceState) {

		// get the bundle extra
		Bundle bundle = this.getIntent().getExtras();
		if (bundle != null) {
			// will get invalid values if not present
		    this.mNoteId = bundle.getLong(DatabaseAdapter.ID_COL, Const.INVALID_LONG);
			this.mNoteType = bundle.getInt(DatabaseAdapter.TYPE_COL, Const.INVALID_INT);
		}
		
		if (this.mNoteId == Const.INVALID_LONG && this.mNoteType == Const.INVALID_INT) {
			Serializable savedId = savedInstanceState.getSerializable(DatabaseAdapter.ID_COL);
			this.mNoteId = savedId == null || !(savedId instanceof Long) ? Const.INVALID_LONG : (Long) savedId;
			Serializable savedType = savedInstanceState.getSerializable(DatabaseAdapter.TYPE_COL);
			this.mNoteType = savedType == null || !(savedType instanceof Integer) ? Const.INVALID_INT : (Integer) savedType;
		}
		Log.i(LOG_TAG, "Reading from bundles done. ID is " + this.mNoteId + " and type is " + this.mNoteType);
		
		Cursor noteCursor = this.mAdapter.getFullNoteOrCoreById(this.mNoteId);
		boolean isEdit = false;
		this.mPriority = Const.LEVEL_DEFAULT;
		if (noteCursor.getCount() > 0) {
			// ID has a higher priority. If a note exists, we use its type, even if this one is invalid while there is a valid one in the Bundle
			this.mNoteType = noteCursor.getInt(noteCursor.getColumnIndexOrThrow(DatabaseAdapter.TYPE_COL));
			this.mPriority = noteCursor.getInt(noteCursor.getColumnIndexOrThrow(DatabaseAdapter.PRIORITY_COL));
			Log.i(LOG_TAG, "The type of the note is " + this.mNoteType);
			isEdit = true;
		}
  	
        switch (mNoteType) {
	    	case Const.TYPE_NOTE:
	            Log.i(LOG_TAG, "Initializing note editing layout");
	            this.setContentView(R.layout.edit_note);
	    		// edit note, rather than create note
	    		if (isEdit) {
	    			Log.i(LOG_TAG, "Putting values into edit note views");
	    			// set note body
	    			EditText bodyView = (EditText) this.findViewById(R.id.edit_note_body);
	    			bodyView.setText(noteCursor.getString(noteCursor.getColumnIndexOrThrow(DatabaseAdapter.TEXT_COL)));
	    		}
            	isViewInitialized = true;
	    		break;
	    	case Const.TYPE_CHECKLIST:
	    		Log.i(LOG_TAG, "Initializing checklist editing layout");
	    		this.setContentView(R.layout.edit_checklist);
	    		// this line will throw an exception if this Activity instance was not instantiated for a checklist view
	    		EditText newItemView = (EditText) this.findViewById(R.id.new_checklist_item_text);
	    		newItemView.setText("");
	    		// change cursor for list, because requery will return empty cursor if no item exists, rather than one row with empty content
	    		Cursor listCursor = mAdapter.getFullNoteOrNoneById(mNoteId);
	    		this.startManagingCursor(listCursor);
	    		this.setListAdapter(new NoteCursorAdapter(this, R.layout.edit_checklist_row, listCursor, 
	    				new String[]{DatabaseAdapter.TEXT_COL}, new int[]{R.id.edit_checklist_item}));
	    		isViewInitialized = true;
            	break;
    		default: 
        		Log.e(LOG_TAG, "Note type is not supported");
            	Util.alert(this, "Note type is not supported");
            	this.goUp();
            	break;
    	}
        
        if (isViewInitialized && isEdit) {
			// populate title
			EditText titleView = (EditText) this.findViewById(R.id.edit_note_title);
			titleView.setText(noteCursor.getString(noteCursor.getColumnIndexOrThrow(DatabaseAdapter.TITLE_COL)));
			Log.i(LOG_TAG, "Title is populated for " + mNoteId);
        }
	}
}