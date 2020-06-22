/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma.ui;

import android.app.Activity;
import android.os.Bundle;

/**
 * The superclass for all Aikuma activities (except those that are
 * ListActivities).
 *
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public abstract class AikumaActivity extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	/**
	 * Provides default back functionality, unless the activity requires safe
	 * transitions, in which case it first notifies the user that they'll lose
	 * data.
	 */
	public void onBackPressed() {
		this.finish();
	}
	
	/**
	 * Flag to indicate whether we need to warn the user about data loss if
	 * they transition from this activity
	 */
	protected boolean safeActivityTransition;
	/**
	 * Warning message to display if the safeActivityTransition flag is set.
	 */
	protected String safeActivityTransitionMessage;
}
