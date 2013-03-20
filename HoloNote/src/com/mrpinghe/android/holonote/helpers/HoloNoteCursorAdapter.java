package com.mrpinghe.android.holonote.helpers;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.mrpinghe.android.holonote.R;

public class HoloNoteCursorAdapter extends SimpleCursorAdapter {

	private static final String LOG_TAG = "NoteCursorAdapter";
	private final Cursor mCursor;
	
	
	public HoloNoteCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
		super(context, layout, c, from, to);
		mCursor = c;
	}

	public HoloNoteCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
		super(context, layout, c, from, to, flags);
		mCursor = c;
	}
	
	/**
	 * ListView should use match_parent as layout_height whenever possible to optimize how each view is loaded.
	 * wrap_content would load each view multiple times when 1) action bar presents and 2) one screen cannot fit all list items
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		// use the super class to do the inflating and binding
		View curView = super.getView(position, convertView, parent);
		
		if (!mCursor.moveToPosition(position)){
			return curView;
		}
		// check if is_checked column present in the cursor
		int isCheckedColIndex = mCursor.getColumnIndex(DatabaseAdapter.IS_CHECKED_COL);
		// if not, it means this is the front page (list notes), and the cursor consists of note_cores
		if (isCheckedColIndex == Const.INVALID_INDEX) {
			// get priority index
			int priorityIndex = mCursor.getColumnIndex(DatabaseAdapter.PRIORITY_COL);
			Log.i(LOG_TAG, "Is checked index is invalid and priority index is: " + priorityIndex);
			if (priorityIndex != Const.INVALID_INDEX && curView instanceof LinearLayout) {
				int priority = mCursor.getInt(priorityIndex);
				LinearLayout row = (LinearLayout) curView;
				ImageView img = (ImageView) row.findViewById(R.id.list_note_title_drawable);
				img.setBackgroundResource(Const.INDICATOR_RES_IDS[priority - 1]);
			}
//			if (priorityIndex != Const.INVALID_INDEX && curView instanceof TextView) {
//				// get priority
//				int priority = mCursor.getInt(priorityIndex);
//				TextView tv = (TextView) curView;
//				// set color accent
//				Log.i(LOG_TAG, "Setting text view accent " + priority + " at position " + position);
//				tv.setCompoundDrawablesWithIntrinsicBounds(Const.INDICATOR_RES_IDS[priority - 1], 0, 0, 0);
//			}
			return curView;
		}

		// is_checked is present, we use is_checked value to draw strike through
		boolean isChecked = mCursor.getInt(isCheckedColIndex) == Const.TRUE_INT;
		Log.i(LOG_TAG, "New View for cursor at " + mCursor.getPosition() + " is constructed and check status is " + isChecked);
		// view note/checklist page
		if (curView instanceof CheckedTextView) {
			Log.i(LOG_TAG, "It is a checkedtextview");
			CheckedTextView newCtv = (CheckedTextView) curView;
			newCtv.setChecked(isChecked);
			newCtv.setPaintFlags(newCtv.isChecked() ? newCtv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : newCtv.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
			Log.i(LOG_TAG, "Handled checked text view");
		}
		// edit checklist page
		else if (curView instanceof LinearLayout) {
			LinearLayout itemGrp = (LinearLayout) curView;
			Log.i(LOG_TAG, "It is a text");
			TextView newTv = (TextView) itemGrp.findViewById(R.id.edit_checklist_item);
			newTv.setPaintFlags(isChecked ? newTv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG : newTv.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
			Log.i(LOG_TAG, "Handled text view");
		}
		
		return curView;
	}
}
