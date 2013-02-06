package com.mrpinghe.android.holonote.interfaces;

import android.os.Bundle;

/**
 * @author heping
 *
 * All activities / fragments, which want to utilize the dialog fragment, should implement this interface
 * so two callback functions will be provided/visible in the dialog fragment
 */
public interface HoloNoteDialogHost {
	/**
	 * Callback method that passes args to the host, usually an Activity.
	 * 
	 * @param args
	 */
	public abstract void onPositiveClick(Bundle args);
	/**
	 * Callback method that passes args to the host, usually an Activity.
	 * @param args
	 */
	public abstract void onNegativeClick(Bundle args);

}
