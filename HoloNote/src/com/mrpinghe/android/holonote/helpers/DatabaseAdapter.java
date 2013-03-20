package com.mrpinghe.android.holonote.helpers;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

/**
 * @author heping
 *
 */
public class DatabaseAdapter {

	/* public constants */
	public static final String DB_NAME = "ping_note_data";

	public static final String CORES_TABLE = "note_cores";
	public static final String ID_COL = "_id"; // underscore is needed for making use of cursors
	public static final String TITLE_COL = "title";
	public static final String TYPE_COL = "note_type";
	public static final String PRIORITY_COL = "priority";
	
	public static final String CONTENTS_TABLE = "note_contents";
	public static final String NOTE_ID_FK_COL = "nid";
	public static final String TEXT_COL = "text";
	public static final String IS_CHECKED_COL = "is_checked";
	
	/* private constants */
	private static final int DB_VER = 3;
	private static final String LOG_TAG = "DatabaseAdapter";
	
	/* member fields. Convention: start with m */
	private final Context mContext;
	private SQLiteDatabase mDatabase;
	private DatabaseHelper mHelper;
	
	/* SQL statements */
	private static final String CREATE_NOTES_TABLE = 
			"create table " + CORES_TABLE + "(" + ID_COL + " integer primary key autoincrement, " +
					TITLE_COL + " text not null, " +
					TYPE_COL + " integer not null, " +
					PRIORITY_COL + " integer default " + Const.LEVEL_DEFAULT +");";
	
	// one notes to many contents, mainly for checklists
	private static final String CREATE_CONTENTS_TABLE =
			"create table " + CONTENTS_TABLE + "(" + ID_COL + " integer primary key autoincrement, " +
					NOTE_ID_FK_COL + " integer, " +
					TEXT_COL + " text not null, " +
					IS_CHECKED_COL + " bool default " + Const.FALSE_INT + ", " +
					"foreign key(" + NOTE_ID_FK_COL + ") references " + CORES_TABLE + "(" + ID_COL + "));";
	 

	public DatabaseAdapter(Context mContext) {
		super();
		Log.i(LOG_TAG, "Construct DB Adapter");
		this.mContext = mContext;
	}
	
	public DatabaseAdapter open() {
		Log.i(LOG_TAG, "Open database");
		// we don't start writing until caller explicitly calls open()
		mHelper = new DatabaseHelper(mContext);
		mDatabase = mHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		Log.i(LOG_TAG, "Close database");
		// this will close the database as well
		mHelper.close();
	}
	
	public Cursor getAllNoteCores(Context ctx) {
		Log.i(LOG_TAG, "Getting all note cores");
		String orderBy = Util.isSortByPriority(ctx) ? PRIORITY_COL : null;
		Cursor results = mDatabase.query(CORES_TABLE, new String[]{ID_COL, TITLE_COL, TYPE_COL, PRIORITY_COL}, null, null, null, null, orderBy);
		Log.i(LOG_TAG, "Total results returned: " + Integer.toString(results.getCount()));
		results.moveToFirst();
		return results;
	}

	/**
	 * Get only the core of the note
	 * @param noteId
	 * @return
	 */
	public Cursor getFullNoteOrCoreById(long noteId) {
		return this.getNoteById(noteId, true);
	}
	
	/**
	 * Gives the full note (core and content) of a note. If no content, return empty cursor
	 * @param noteId
	 * @return
	 */
	public Cursor getFullNoteOrNoneById(long noteId) {
		return this.getNoteById(noteId, false);
	}

	private Cursor getNoteById(long noteId, boolean leftJoin) {
		Log.i(LOG_TAG, "Getting note #" + noteId);
		SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
		// only checklist can have no corresponding content
		if (leftJoin) {
			qBuilder.setTables(CORES_TABLE + " n LEFT JOIN "+ CONTENTS_TABLE + " c ON n." + ID_COL + " = " + "c." + NOTE_ID_FK_COL);
		}
		else {
			qBuilder.setTables(CORES_TABLE + " n JOIN "+ CONTENTS_TABLE + " c ON n." + ID_COL + " = " + "c." + NOTE_ID_FK_COL);
		}
		Cursor results = qBuilder.query(mDatabase, null, "n." + ID_COL + " = " + noteId, null, null, null, null);
		Log.i(LOG_TAG, "Total results returned: " + Integer.toString(results.getCount()));
		// could be multiple rows in the case of checklist
		results.moveToFirst();
		return results;
	}
	
	/**
	 * Create a new note or checklist. Will not create if title is null, message is null, or type is invalid
	 * 
	 * @param title
	 * @param message
	 * @param priority 
	 * @return - the new ID, or {@link Const.INVALID_LONG} if failed
	 */
	public long createNote(String title, String text, int type, int priority) {
		Log.i(LOG_TAG, "Creating a new note");

		if (Util.isEmpty(title) && Util.isEmpty(text)) {
			Log.i(LOG_TAG, "Both title and message are emtpy, no create");
			return Const.INVALID_LONG;
		}
		else if (Util.isEmpty(title)) {
			title = (new Date()).toLocaleString();
			Log.i(LOG_TAG, "title is empty. Default to date string");
		}
		
		Log.i(LOG_TAG, "Creating a note of type: " + type);
		
		ContentValues noteVal = new ContentValues();
		noteVal.put(TITLE_COL, title);
		noteVal.put(TYPE_COL, type);
		noteVal.put(PRIORITY_COL, priority);
		long nid = mDatabase.insert(CORES_TABLE, null, noteVal);
		Log.i(LOG_TAG, "Core is added. Adding contents now");
		nid = nid == Const.FAILED_INSERT ? Const.INVALID_LONG : nid;
		
		if (nid != Const.INVALID_LONG && !Util.isEmpty(text)) {
			ContentValues contentVal = new ContentValues();
			contentVal.put(NOTE_ID_FK_COL, nid);
			contentVal.put(TEXT_COL, text);
			mDatabase.insert(CONTENTS_TABLE, null, contentVal);
		}
		return nid;
	}
	
	/**
	 * Update the note with ID noteId. Will delete the note if the note has no title and no content
	 * 
	 * @param mNoteId
	 * @param title
	 * @param text
	 * @param priority 
	 * @return - the number of rows updated
	 */
	public int updateOrDeleteNote(long noteId, String title, String text, int priority) {
		
		Log.i(LOG_TAG, "Starting to update note: " + noteId);
		int type = Const.INVALID_INT;

		Cursor note = this.getFullNoteOrCoreById(noteId);
		if (note.getCount() > 0) {
			Log.i(LOG_TAG, "Found a note to update with");
			type = note.getInt(note.getColumnIndexOrThrow(TYPE_COL));
		}
		else {
			Log.e(LOG_TAG, "Cannot find a note to update with");
			return Const.INVALID_INT;
		}

		int numOfRowsAffected = 0;
		// this switch only update contents. Title is updated in the end
		switch (type) {
			case Const.TYPE_TEXT:
				// delete note or fault tolerance
				if (Util.isEmpty(title) && Util.isEmpty(text)) {
					Log.i(LOG_TAG, "Both title and text are emtpy for NOTE, delete " + noteId);
					return this.deleteNote(noteId);
				}
				else if (Util.isEmpty(text)) {
					Log.i(LOG_TAG, "There is a title but no text. Default empty string");
					text = "";
				}
				else if (Util.isEmpty(title)) {
					title = (new Date()).toLocaleString();
					Log.i(LOG_TAG, "There is a text but no title. Default local date string: " + title);
				}
				// simply update the assumed only entry in content table for note
				Log.i(LOG_TAG, "Updating note's body");
				ContentValues coontentVals = new ContentValues();
				coontentVals.put(TEXT_COL, text);
				numOfRowsAffected += mDatabase.update(CONTENTS_TABLE, coontentVals, NOTE_ID_FK_COL + " = " + noteId, null);
				break;
			case Const.TYPE_CHECKLIST:
				// delete note or fault tolerance
				if (Util.isEmpty(title)) {
					if (note.getCount() == 1) {
						String str = note.getString(note.getColumnIndexOrThrow(TEXT_COL));
						if (Util.isEmpty(str)) {
							Log.i(LOG_TAG, "Nothing exists in this checklist. Delete " + noteId);
							return this.deleteNote(noteId);
						}
					}
					// title is empty, but there is at least one valid existing checklist item (note)
					title = (new Date()).toLocaleString();
					Log.i(LOG_TAG, "There is something in the checklist but no title. Default local date string: " + title);
				}
				Log.i(LOG_TAG, "Check if anything to add to checklist");
				break;
			default:
				Log.e(LOG_TAG, "Somehow note " + noteId + " has an invalid type and passed all the previous checks...");
				break;
		}
		
		Log.i(LOG_TAG, "Update core table");
		ContentValues coreVals = new ContentValues();
		coreVals.put(TITLE_COL, title);
		coreVals.put(PRIORITY_COL, priority);
		numOfRowsAffected += mDatabase.update(CORES_TABLE, coreVals, ID_COL + " = " + noteId, null);
		return numOfRowsAffected;
	}

	public void flipCheckedStatus(long id, long mNoteId, boolean checked) {
		Log.i(LOG_TAG, "Change the status of item " + id + " from note " + mNoteId + " to " + checked);
		ContentValues values = new ContentValues();
		values.put(IS_CHECKED_COL, checked);
		mDatabase.update(CONTENTS_TABLE, values, ID_COL + " = " + id + " AND " + NOTE_ID_FK_COL + " = " + mNoteId, null);
	}
	
	public int deleteNote(long noteId) {
		int rowsDeleted = 0;
		rowsDeleted += mDatabase.delete(CONTENTS_TABLE, NOTE_ID_FK_COL + " = " + noteId, null);
		Log.i(LOG_TAG, rowsDeleted + " rows content deleted for " + noteId);
		rowsDeleted += mDatabase.delete(CORES_TABLE, ID_COL + " = " + noteId, null);
		Log.i(LOG_TAG, rowsDeleted + " rows total deleted for " + noteId);
		return rowsDeleted;
	}

	public int deleteChecklistItem(long itemId) {
		Log.i(LOG_TAG, "Delete content ID: " + itemId);
		return mDatabase.delete(CONTENTS_TABLE, ID_COL + " = " + itemId, null);
	}

	public int updateItemContent(long mEditedItemId, String string) {
		ContentValues values = new ContentValues();
		values.put(TEXT_COL, string);
		return mDatabase.update(CONTENTS_TABLE, values, ID_COL + " = " + mEditedItemId, null);
	}

	public int deleteAllChecked(long mNoteId) {
		return mDatabase.delete(CONTENTS_TABLE, NOTE_ID_FK_COL + " = " + mNoteId + " AND " + IS_CHECKED_COL + " = " + Const.TRUE_INT, null);
	}

	public int uncheckAll(long mNoteId) {
		ContentValues values = new ContentValues();
		values.put(IS_CHECKED_COL, false);
		return mDatabase.update(CONTENTS_TABLE, values, NOTE_ID_FK_COL + " = " + mNoteId + " AND " + IS_CHECKED_COL + " = " + Const.TRUE_INT, null);
	}


	public int updateNotePriority(long noteId, int level) {
		if (level != Const.LEVEL_INVALID) {
			Log.i(LOG_TAG, "Update note " + noteId + " to priority " + level);
			ContentValues values = new ContentValues();
			values.put(PRIORITY_COL, level);
			return mDatabase.update(CORES_TABLE, values, ID_COL + " = " + noteId, null);
		}
		return 0;
	}

	public long addChecklistItem(String string, long mNoteId) {
		if (mNoteId != Const.INVALID_LONG) {
			ContentValues values = new ContentValues();
			values.put(TEXT_COL, string);
			values.put(NOTE_ID_FK_COL, mNoteId);
			return mDatabase.insert(CONTENTS_TABLE, null, values);
		}
		return Const.INVALID_LONG;
	}


	/**
	 * Make this class private, because this adapter should be the only class to use this helper.
	 * 
	 * This helper helps to manage database versions, create/retrieve database, so adapter can open and query the database
	 * 
	 * @author heping
	 *
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DB_NAME, null, DB_VER);
			Log.i(LOG_TAG, "DatabaseHelper created");
		}

		/**
		 * Create database for the first time
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.i(LOG_TAG, "Create tables");
			db.execSQL(CREATE_NOTES_TABLE);
			db.execSQL(CREATE_CONTENTS_TABLE);
		}

		/**
		 * destroy all data upon version update
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(LOG_TAG, "Version updated from " + oldVersion + " to " + newVersion);
			Log.i(LOG_TAG, "priority is added");
		}
		
	}
}
