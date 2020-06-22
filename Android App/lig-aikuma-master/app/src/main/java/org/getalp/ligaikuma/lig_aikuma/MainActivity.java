/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma;

import android.app.ActionBar;
import android.app.ListActivity;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;

import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;
import org.getalp.ligaikuma.lig_aikuma.model.Recording;
import org.getalp.ligaikuma.lig_aikuma.ui.sensors.LocationDetector;

import java.io.IOException;

/**
 * The primary activity that lists existing recordings and allows you to select
 * them for listening and subsequent respeaking.
 *
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public class MainActivity extends ListActivity {

	private static final String TAG = "MainActivity";

	public static LocationDetector locationDetector;

	// Helps us store how far down the list we are when MainActivity gets
	// stopped.
	private Parcelable listViewState;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);

		// Start gathering location data
		MainActivity.locationDetector = new LocationDetector(this);

		// Create an index file when app starts
		try {
			Recording.indexAll();
		} catch (IOException e) {
			if(BuildConfig.DEBUG)Log.e(TAG, e.getMessage());
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		listViewState = getListView().onSaveInstanceState();
		MainActivity.locationDetector.stop();
	}

	@Override
	public void onResume() {
		super.onResume();
		if(BuildConfig.DEBUG)Log.i(TAG, "num: " +Recording.readAll().size());

		if(listViewState != null)
			getListView().onRestoreInstanceState(listViewState);

		MainActivity.locationDetector.start();
	}
	
	@Override
	protected void onStart() {
	    super.onStart();
	    showProgressStatus(View.GONE);  
	}

	@Override
	protected void onStop() {
	    super.onStop();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		MainActivity.locationDetector.stop();
	}

    /**
     * Show the status of cloud-background thread
     * @param visibility	Visibility of the progress bar View
     */
    public void showProgressStatus(int visibility) {
		findViewById(R.id.cloudProgress).setVisibility(visibility);
    }

}
