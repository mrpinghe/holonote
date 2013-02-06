package com.mrpinghe.android.holonote.fragments;

import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.mrpinghe.android.holonote.R;
import com.mrpinghe.android.holonote.helpers.Const;
import com.mrpinghe.android.holonote.helpers.DatabaseAdapter;
import com.mrpinghe.android.holonote.interfaces.HoloNoteDialogHost;

public class HoloNoteDialog extends DialogFragment {

	private static final String LOG_TAG = "HoloNoteDialog";
	private int mType = Const.INVALID_INT;
	private long mItemId = Const.INVALID_LONG;
	
	/**
	 * Using factory design pattern to create a new custom DialogFragment.
	 * 
	 * params should at least contain the dialog type under the key Const.DIALOG_TYPE
	 * Other parameters are optional and based on specific dialog needs
	 * 
	 * @param params
	 * @return
	 */
	public static HoloNoteDialog newInstance(Map<String, Object> params) {

		Log.i(LOG_TAG, "Getting a new instance with " + (params == null ? 0 : params.size()) + " params");
		HoloNoteDialog frag = new HoloNoteDialog();
		Bundle args = new Bundle();
		if (params.get(Const.DIALOG_TYPE) != null) {
			int type = (Integer) params.get(Const.DIALOG_TYPE);
			Log.i(LOG_TAG, "Return new dialog of type " + type);
			args.putInt(Const.DIALOG_TYPE, type);
		}
		if (params.get(DatabaseAdapter.ID_COL) != null) {
			long id = (Long) params.get(DatabaseAdapter.ID_COL);
			Log.i(LOG_TAG, "ID argument for dialog " + id);
			args.putLong(DatabaseAdapter.ID_COL, id);
		}
		if (params.get(DatabaseAdapter.TEXT_COL) != null) {
			String text = (String) params.get(DatabaseAdapter.TEXT_COL);
			Log.i(LOG_TAG, "Text argument for dialog " + text);
			args.putString(DatabaseAdapter.TEXT_COL, text);
		}
		frag.setArguments(args);
		return frag;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		mType = this.getArguments().getInt(Const.DIALOG_TYPE);
		Log.i(LOG_TAG, "Creating a new dialog of type " + mType);
		String title = null;
		
		switch (mType) {
			case Const.DELETE_NOTE:
				title = "Are you sure you want to delete this note?";
				return createConfirmDialog(title);
			case Const.DELETE_CHECKED:
				title = "Are you sure you want to delete all checked?";
				return createConfirmDialog(title);
			case Const.UNCHECK_ALL:
				title = "Are you sure you want to uncheck all?";
				return createConfirmDialog(title);
			case Const.RECOVER_ALL:
				title = "Are you sure you want to recover last backup (cannot be reverted)?";
				return createConfirmDialog(title);
			case Const.EDIT_ITEM:
				mItemId = this.getArguments().getLong(DatabaseAdapter.ID_COL);
				return this.createEditDialog();
			case Const.PICK_PRIORITY:
				return this.createPriorityPickerDialog();
			default:
				Log.e(LOG_TAG, "Unsupported dialog type " + mType);
				return super.onCreateDialog(savedInstanceState);
		}
		
	}

	/**
	 * Build an alert dialog with two options, Yes and Cancel, and is used to confirm use action with a specific title
	 * 
	 * Note: AlertDialog will dismiss automatically after button click
	 * 
	 * @param title
	 * @return
	 */
	private Dialog createConfirmDialog(String title) {
		Log.i(LOG_TAG, "Build a alert dialog to ask for confirmation with title: " + title);
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this.getActivity());
		dialogBuilder.setTitle(title);
		dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Log.i(LOG_TAG, "Positive clicked for " + mType);
				Bundle args = new Bundle();
				args.putInt(Const.DIALOG_TYPE, mType);
                ((HoloNoteDialogHost) HoloNoteDialog.this.getActivity()).onNegativeClick(args);
			}
		});
		dialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Log.i(LOG_TAG, "Negative clicked for " + mType);
				Bundle args = new Bundle();
				args.putInt(Const.DIALOG_TYPE, mType);
                ((HoloNoteDialogHost) HoloNoteDialog.this.getActivity()).onPositiveClick(args);
			}
		});
		return dialogBuilder.create();
	}

	/**
	 * Custom dialog needs explicit dismiss
	 * 
	 * @return
	 */
	private Dialog createEditDialog() {
		Log.i(LOG_TAG, "Building a custom dialog for editing text");
		Dialog editDialog = new Dialog(this.getActivity());
		editDialog.setContentView(R.layout.dialog_edit_item);
		editDialog.setTitle(Const.EDIT_ITEM_DIALOG_TITLE);
		
		EditText holder = (EditText) editDialog.findViewById(R.id.edit_checklist_item);
		String itemText = this.getArguments().getString(DatabaseAdapter.TEXT_COL);
		holder.setText(itemText);
		holder.setSelection(itemText.length());
		
		// have to initialize listeners here, because configuration in the view file will looks for method in Dialog class
		Button editButton = (Button) editDialog.findViewById(R.id.button_edit);
		editButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.i(LOG_TAG, "Edit note clicked for: " + mItemId);
				LinearLayout dialogView = (LinearLayout) v.getParent().getParent();
				EditText editedText = (EditText) dialogView.findViewById(R.id.edit_checklist_item);
				
				Log.i(LOG_TAG, "Setting up args to pass to callback method");
				Bundle args = new Bundle();
				args.putInt(Const.DIALOG_TYPE, mType);
				args.putLong(DatabaseAdapter.ID_COL, mItemId);
				args.putString(DatabaseAdapter.TEXT_COL, editedText.getText().toString());
				((HoloNoteDialogHost) HoloNoteDialog.this.getActivity()).onPositiveClick(args);
				HoloNoteDialog.this.dismiss();
			}
		});
		
		Button cancelButton = (Button) editDialog.findViewById(R.id.button_cancel);
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.i(LOG_TAG, "Cancel operation: " + mType);
				Bundle args = new Bundle();
				args.putInt(Const.DIALOG_TYPE, mType);
				((HoloNoteDialogHost) HoloNoteDialog.this.getActivity()).onNegativeClick(args);
				HoloNoteDialog.this.dismiss();
			}
		});
		
		return editDialog;
	}

	/**
	 * A custom dialog for picking priority
	 * 
	 * @return
	 */
	private Dialog createPriorityPickerDialog() {
		Log.i(LOG_TAG, "Building priority picker dialog");
		Dialog dialog = new Dialog(this.getActivity());
		dialog.setContentView(R.layout.dialog_change_priority);
		dialog.setTitle(Const.CHANGE_PRIORITY_DIALOG_TITLE);
		dialog.findViewById(R.id.red_selector).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.i(LOG_TAG, "Picked red");
				Bundle args = new Bundle();
				args.putInt(Const.DIALOG_TYPE, mType);
				args.putInt(Const.LEVEL_DRAWABLE, R.drawable.ic_menu_red);
				args.putInt(DatabaseAdapter.PRIORITY_COL, Const.LEVEL_RED);
				((HoloNoteDialogHost) HoloNoteDialog.this.getActivity()).onPositiveClick(args);
				HoloNoteDialog.this.dismiss();
			}
		});
		dialog.findViewById(R.id.orange_selector).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.i(LOG_TAG, "Picked orange");
				Bundle args = new Bundle();
				args.putInt(Const.DIALOG_TYPE, mType);
				args.putInt(Const.LEVEL_DRAWABLE, R.drawable.ic_menu_orange);
				args.putInt(DatabaseAdapter.PRIORITY_COL, Const.LEVEL_ORANGE);
				((HoloNoteDialogHost) HoloNoteDialog.this.getActivity()).onPositiveClick(args);
				HoloNoteDialog.this.dismiss();
			}
		});
		dialog.findViewById(R.id.yellow_selector).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.i(LOG_TAG, "Picked yellow");
				Bundle args = new Bundle();
				args.putInt(Const.DIALOG_TYPE, mType);
				args.putInt(Const.LEVEL_DRAWABLE, R.drawable.ic_menu_yellow);
				args.putInt(DatabaseAdapter.PRIORITY_COL, Const.LEVEL_YELLOW);
				((HoloNoteDialogHost) HoloNoteDialog.this.getActivity()).onPositiveClick(args);
				HoloNoteDialog.this.dismiss();
			}
		});
		dialog.findViewById(R.id.green_selector).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.i(LOG_TAG, "Picked green");
				Bundle args = new Bundle();
				args.putInt(Const.DIALOG_TYPE, mType);
				args.putInt(Const.LEVEL_DRAWABLE, R.drawable.ic_menu_green);
				args.putInt(DatabaseAdapter.PRIORITY_COL, Const.LEVEL_GREEN);
				((HoloNoteDialogHost) HoloNoteDialog.this.getActivity()).onPositiveClick(args);
				HoloNoteDialog.this.dismiss();
			}
		});
		dialog.findViewById(R.id.white_selector).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.i(LOG_TAG, "Picked white");
				Bundle args = new Bundle();
				args.putInt(Const.DIALOG_TYPE, mType);
				args.putInt(Const.LEVEL_DRAWABLE, R.drawable.ic_menu_white);
				args.putInt(DatabaseAdapter.PRIORITY_COL, Const.LEVEL_WHITE);
				((HoloNoteDialogHost) HoloNoteDialog.this.getActivity()).onPositiveClick(args);
				HoloNoteDialog.this.dismiss();
			}
		});
		return dialog;
	}

}
