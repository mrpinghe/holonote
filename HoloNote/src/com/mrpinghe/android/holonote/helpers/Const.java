package com.mrpinghe.android.holonote.helpers;

import java.io.File;

import android.os.Environment;

import com.mrpinghe.android.holonote.R;

public class Const {

	// note types
	public static final int TYPE_CHECKLIST = 1;
	public static final int TYPE_NOTE = 2;

	// invalid values
	public static final long INVALID_LONG = -1L;
	public static final int INVALID_INT = -1;
	
	// database invalid values
	public static final long FAILED_INSERT = -1L;
	public static final int INVALID_INDEX = -1;
	
	public static final int TRUE_INT = 1;
	public static final int FALSE_INT = 0;
	
	// Priority level related
	public static final int LEVEL_INVALID = -1;
	public static final int LEVEL_RED = 1;
	public static final int LEVEL_ORANGE = 2;
	public static final int LEVEL_YELLOW = 3;
	public static final int LEVEL_GREEN = 4;
	public static final int LEVEL_WHITE = 5;
	public static final int LEVEL_DEFAULT = LEVEL_YELLOW;
	public static final int[] LEVEL_RES_IDS = new int[] {R.drawable.ic_menu_red, R.drawable.ic_menu_orange, 
		R.drawable.ic_menu_yellow, R.drawable.ic_menu_green, R.drawable.ic_menu_white};
	public static final int[] INDICATOR_RES_IDS = new int[] {R.drawable.ic_list_red, R.drawable.ic_list_orange, 
		R.drawable.ic_list_yellow, R.drawable.ic_list_green, R.drawable.ic_list_white};
	
	// AsyncTask method type indicator
	public static final int BACKUP = 1;
	public static final int RECOVER = 2;
	
	public static final CharSequence EDIT_ITEM_DIALOG_TITLE = "Edit Text";
	public static final CharSequence CHANGE_PRIORITY_DIALOG_TITLE = "Change Priority";

	// preference related
	public static final String GLOBAL_PREF_FILE = "Global";
	public static final String IS_LIGHT_THEME = "is_light_theme";
	public static final String IS_SORT_BY_PRIORITY = "is_sort_by_priority";
	public static final String IS_BACKUP_AUTO = "is_backup_auto";
	
	// file system related
	public static final String EXT_DIR = Environment.getExternalStorageDirectory() + File.separator + "Holo Note";
	public static final String DATABASE_DIR = Environment.getDataDirectory() + File.separator + "data" + File.separator + 
			"com.mrpinghe.android.holonote" + File.separator + "databases";
	public static final String BACKUP_FILE_NAME = DatabaseAdapter.DB_NAME + "_backup";

	// back up / recover methods
	public static final int SUCCESS = 0;
	public static final int FILE_NOT_FOUND = -1;
	public static final int WRITE_ERROR = -2;
	public static final int COPY_ERROR = -3;
	public static final int IO_ERROR = -4;
	
	// broadcast
	public static final String BC_METHOD = "broadcast_method";
	// an arbitrary number
	public static final int AUTO_BACKUP_REQ_CODE = 629416;
	
	// Dialog Fragment
	public static final String DIALOG_TYPE = "dialog_type";
	public static final String LEVEL_DRAWABLE = "level_drawable";
	public static final int DELETE_NOTE = 1;
	public static final int DELETE_CHECKED = 2;
	public static final int UNCHECK_ALL = 3;
	public static final int RECOVER_ALL = 4;
	public static final int EDIT_ITEM = 5;
	public static final int PICK_PRIORITY = 6;
}
