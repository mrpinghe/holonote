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
	private Object mHost = null;
	
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

		HoloNoteDialog frag = new HoloNoteDialog();
		Bundle args = new Bundle();
		if (params.get(Const.DIALOG_TYPE) != null) {
			args.putInt(Const.DIALOG_TYPE, (Integer) params.get(Const.DIALOG_TYPE));
		}
		if (params.get(DatabaseAdapter.ID_COL) != null) {
			args.putLong(DatabaseAdapter.ID_COL, (Long) params.get(DatabaseAdapter.ID_COL));
		}
		if (params.get(DatabaseAdapter.TEXT_COL) != null) {
			args.putString(DatabaseAdapter.TEXT_COL, (String) params.get(DatabaseAdapter.TEXT_COL));
		}
		if (params.get(Const.FRAG_RSRC_ID) != null) {
			args.putInt(Const.FRAG_RSRC_ID, (Integer) params.get(Const.FRAG_RSRC_ID));
		}
		frag.setArguments(args);
		return frag;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		mType = this.getArguments().getInt(Const.DIALOG_TYPE);
		int fragId = this.getArguments().getInt(Const.FRAG_RSRC_ID);
		if (fragId != 0) {
			mHost = this.getActivity().getFragmentManager().findFragmentById(fragId);
		}
		else {
			mHost = this.getActivity();
		}
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
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this.getActivity());
		dialogBuilder.setTitle(title);
		dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Bundle args = new Bundle();
				args.putInt(Const.DIALOG_TYPE, mType);
                ((HoloNoteDialogHost) HoloNoteDialog.this.getActivity()).onNegativeClick(args);
			}
		});
		dialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
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
		Dialog editDialog = new Dialog(this.getActivity());
		editDialog.setContentView(R.layout.dialog_edit_item);
		editDialog.setTitle(Const.EDIT_ITEM_DIALOG_TITLE);
		
		EditText holder = (EditText) editDialog.findViewById(R.id.edit_checklist_item);
		String itemText = this.getArguments().getString(DatabaseAdapter.TEXT_COL);
		holder.setText(itemText);
		holder.setSelection(itemText.length());
		
		// have to initialize listeners here, because configuration in the view file will looks for method in Dialog class
		Button editButton = (Button) editDialog.findViewById(R.id.button_done);
		editButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.i(LOG_TAG, "Done");
				LinearLayout dialogView = (LinearLayout) v.getParent().getParent();
				EditText editedText = (EditText) dialogView.findViewById(R.id.edit_checklist_item);
				
				Bundle args = new Bundle();
				args.putInt(Const.DIALOG_TYPE, mType);
				args.putLong(DatabaseAdapter.ID_COL, mItemId);
				args.putString(DatabaseAdapter.TEXT_COL, editedText.getText().toString());
				((HoloNoteDialogHost) mHost).onPositiveClick(args);
				HoloNoteDialog.this.dismiss();
			}
		});
		
		Button cancelButton = (Button) editDialog.findViewById(R.id.button_ccl);
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.i(LOG_TAG, "Cancel");
				Bundle args = new Bundle();
				args.putInt(Const.DIALOG_TYPE, mType);
				((HoloNoteDialogHost) mHost).onNegativeClick(args);
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
		Dialog dialog = new Dialog(this.getActivity());
		dialog.setContentView(R.layout.dialog_change_priority);
		dialog.setTitle(Const.CHANGE_PRIORITY_DIALOG_TITLE);
		dialog.findViewById(R.id.red_selector).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Bundle args = new Bundle();
				args.putInt(Const.DIALOG_TYPE, mType);
				args.putInt(Const.LEVEL_DRAWABLE, R.drawable.ic_menu_red);
				args.putInt(DatabaseAdapter.PRIORITY_COL, Const.LEVEL_RED);
				((HoloNoteDialogHost) mHost).onPositiveClick(args);
				HoloNoteDialog.this.dismiss();
			}
		});
		dialog.findViewById(R.id.orange_selector).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Bundle args = new Bundle();
				args.putInt(Const.DIALOG_TYPE, mType);
				args.putInt(Const.LEVEL_DRAWABLE, R.drawable.ic_menu_orange);
				args.putInt(DatabaseAdapter.PRIORITY_COL, Const.LEVEL_ORANGE);
				((HoloNoteDialogHost) mHost).onPositiveClick(args);
				HoloNoteDialog.this.dismiss();
			}
		});
		dialog.findViewById(R.id.yellow_selector).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Bundle args = new Bundle();
				args.putInt(Const.DIALOG_TYPE, mType);
				args.putInt(Const.LEVEL_DRAWABLE, R.drawable.ic_menu_yellow);
				args.putInt(DatabaseAdapter.PRIORITY_COL, Const.LEVEL_YELLOW);
				((HoloNoteDialogHost) mHost).onPositiveClick(args);
				HoloNoteDialog.this.dismiss();
			}
		});
		dialog.findViewById(R.id.green_selector).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Bundle args = new Bundle();
				args.putInt(Const.DIALOG_TYPE, mType);
				args.putInt(Const.LEVEL_DRAWABLE, R.drawable.ic_menu_green);
				args.putInt(DatabaseAdapter.PRIORITY_COL, Const.LEVEL_GREEN);
				((HoloNoteDialogHost) mHost).onPositiveClick(args);
				HoloNoteDialog.this.dismiss();
			}
		});
		dialog.findViewById(R.id.white_selector).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Bundle args = new Bundle();
				args.putInt(Const.DIALOG_TYPE, mType);
				args.putInt(Const.LEVEL_DRAWABLE, R.drawable.ic_menu_white);
				args.putInt(DatabaseAdapter.PRIORITY_COL, Const.LEVEL_WHITE);
				((HoloNoteDialogHost) mHost).onPositiveClick(args);
				HoloNoteDialog.this.dismiss();
			}
		});
		return dialog;
	}

}
