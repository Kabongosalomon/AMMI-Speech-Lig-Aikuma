/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma.ui;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.semantive.waveformandroid.waveform.MediaPlayerFactory;
import com.semantive.waveformandroid.waveform.WaveformFragment;

import org.getalp.ligaikuma.lig_aikuma.audio.Player;
import org.getalp.ligaikuma.lig_aikuma.audio.SimplePlayer;
import org.getalp.ligaikuma.lig_aikuma.audio.record.Microphone.MicException;
import org.getalp.ligaikuma.lig_aikuma.audio.record.PCMWriter;
import org.getalp.ligaikuma.lig_aikuma.audio.record.ThumbRespeaker;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;
import org.getalp.ligaikuma.lig_aikuma.util.FileIO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;

/**
 * This fragment contains all views which can be used by a respeaking/translating activity
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public class ThumbRespeakFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		v = inflater.inflate(R.layout.thumb_respeak_fragment, container, false);
		installButtonBehaviour(v);
		textTimeProgression = (TextView) v.findViewById(R.id.respeak_current_time);
		totalTime = (TextView) v.findViewById(R.id.respeak_total_time);
		undo = (ImageButton) v.findViewById(R.id.undoButton);
		undo.setEnabled(false);
		undo.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {onUndoAction();}});
		
		seekBar = (InterleavedSeekBar) v.findViewById(R.id.InterleavedSeekBar);
		seekBar.setOnSeekBarChangeListener(
				new SeekBar.OnSeekBarChangeListener() {
					int originalProgress;
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
					{
						if(fromUser) seekBar.setProgress(originalProgress);
					}
					public void onStopTrackingTouch(SeekBar _seekBar) {}
					public void onStartTrackingTouch(SeekBar _seekBar) {
						originalProgress = seekBar.getProgress();
					}
				});

		seekBar.invalidate();

		listenFragment = new ListenFragment();
		getFragmentManager().beginTransaction().replace(R.id.respeak_latest_segment_player, listenFragment).commit();

		String filename = MediaPlayerFactory._currentReadFile;
		Log.d(TAG,"File loaded = "+filename);
		_waveformFragment = new ImplementWaveformFragment();
		_waveformFragment.setFileName(filename);
		getFragmentManager().beginTransaction().add(R.id.waveform_view_container, _waveformFragment).commit();

		// Remapping
		_toRemap = false;
		_currentPosRemap = -1;
		_remap = new ArrayList<>();
		_is_speak = (CheckBox) v.findViewById(R.id.check_no_speak);

		return v;
	}
	
	/**
	 * Called when the fragment is destroyed; ensures resources are
	 * appropriately released.
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		//If you hit the stop button really quickly, the player may not/have been initialized fully.
		if(respeaker != null)
			respeaker.release();
		
		String filename = FileIO.getOwnerPath().getAbsolutePath() + "/recordings/temp_rspk.wav";
		if (listenFragment.getPlayer() != null)
			listenFragment.getPlayer().release();
		try {
			while (count>0) {
				FileIO.delete(new File(filename));
				if(BuildConfig.DEBUG)Log.i(TAG, "Deleting " + filename);
				count--;
				filename = FileIO.getOwnerPath().getAbsolutePath() + "/recordings/temp_rspk.wav";
			}
		} catch (IOException e) {
			if(BuildConfig.DEBUG)Log.e(TAG, "failed to delete " + filename + " - maybe the file didn't exist (1)");
		}
	}

	// Implements the behaviour for the play and respeak buttons.
	private void installButtonBehaviour(View v) {
		final ImageButton okButton = (ImageButton) v.findViewById(R.id.saveButton);
		okButton.setImageResource(R.drawable.ok_disabled_48);
		okButton.setEnabled(false);

		final ImageButton playButton = (ImageButton) v.findViewById(R.id.PlayButton);
		final ImageButton respeakButton = (ImageButton) v.findViewById(R.id.RespeakButton);
		//final int greyColor = 0xffd6d6d6;
		
		playButton.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_DOWN)
				{
					Log.d(TAG,"setIfMediaPlay(true)");
					_waveformFragment.setMediaPlay(true);
					if(count > 0 && undo.isEnabled()) {
						respeaker.saveRespeaking();
						if(BuildConfig.DEBUG)Log.d("save Respeak", "save respeak");
					}
					// Color change
					//playButton.setBackgroundColor(0xff00d500);
					long previousGestureTime = gestureTime;
					gestureTime = System.currentTimeMillis();
					gestureTimeUpToDown = System.currentTimeMillis() - gestureTimeUpToDown;

					if(isCommented) // After commentary is recorded(or At the start)
					{
						respeaker.playOriginal(1);											//Rewind and Store the start-point
						if(BuildConfig.DEBUG)Log.d("play", "playOriginal(1) with comm");
					}
					else	// After commentary is recorded(or At the start) and
					{
						if(previousGestureTime < VALID_GESTURE_TIME)						// green-arrow button is pressed more than once
						{
							respeaker.playOriginal(2); //Rewind
							if(BuildConfig.DEBUG)Log.d("play", "playOriginal(2)");
						}
						else if(gestureTimeUpToDown < VALID_GESTURE_TIME)
						{
							respeaker.playOriginal(0); //Continue
							if(BuildConfig.DEBUG)Log.d("play", "playOriginal(0)");
						}
						else
						{
							respeaker.playOriginal(1); //Rewind and Store the start-point
							if(BuildConfig.DEBUG)Log.d("play", "playOriginal(1)");
							isCommented = true;
						}
					}
					if(BuildConfig.DEBUG)Log.d("isCommented", "isCommented = " + Boolean.toString(isCommented));
					startProgressionGlobalBar();
					// Remapping
					if(_toRemap&&_is_speak.isChecked())
						_remap.add(_currentPosRemap);
					_currentPosRemap++;
					_toRemap=true;
					_is_speak.setChecked(false);
				}
				else if (event.getAction() == MotionEvent.ACTION_UP) {
					//playButton.setBackgroundColor(greyColor);
					respeaker.pauseOriginal();
					Log.d(TAG,"setIfMediaPlay(false)");
					_waveformFragment.setMediaPlay(false);
					stopThread(seekBarThread);
					gestureTime = System.currentTimeMillis() - gestureTime;
					gestureTimeUpToDown = System.currentTimeMillis();
					if(BuildConfig.DEBUG)Log.i("Thumb", ""+ gestureTime);
					if(gestureTime >= VALID_GESTURE_TIME)
						isCommented = false;
					if(BuildConfig.DEBUG)Log.d("isCommented", "isCommented when up = " + Boolean.toString(isCommented));
				}
				return false;
			}
		});

		respeakButton.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					//respeakButton.setBackgroundColor(0xffff2020);
					respeaker.pauseOriginal();
					respeaker.recordRespeaking();
					count++;
					
					String filename = FileIO.getOwnerPath().getAbsolutePath() + "/recordings/temp_rspk.wav";
					if (listenFragment.getPlayer() != null)
						listenFragment.getPlayer().release();
					try {
						FileIO.delete(new File(filename));
						if(BuildConfig.DEBUG)Log.i(TAG, "Deleting " + filename);
					} catch (IOException e) {
						if(BuildConfig.DEBUG)Log.e(TAG, "failed to delete " + filename + " - maybe the file didn't exist (2)");
					}
				}
				else if(event.getAction() == MotionEvent.ACTION_UP) {
					if(BuildConfig.DEBUG)Log.i("ThumbRespeak", "sleep: " + System.currentTimeMillis());
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						if(BuildConfig.DEBUG)Log.e("ThumbRespeak", "sleep");
					}
					if(BuildConfig.DEBUG)Log.i("ThumbRespeak", "sleep: " + System.currentTimeMillis());
					
					if(!okButton.isEnabled()) {
						okButton.setImageResource(R.drawable.ok_48);
						okButton.setEnabled(true);
					}
					//respeakButton.setBackgroundColor(greyColor);
					try {
						respeaker.pauseRespeaking();
						isCommented = true;
					} catch (MicException e) {
						ThumbRespeakFragment.this.getActivity().finish();
					}

                    undo.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.undo, null));
					undo.setEnabled(true);

					// save the latest segment in a temporary file
					int rate = respeaker.getRecorder().getMicrophone().getSampleRate();
					String filename = FileIO.getOwnerPath().getAbsolutePath() + "/recordings/temp_rspk.wav";
					PCMWriter file = PCMWriter.getInstance(rate,
							respeaker.getRecorder().getMicrophone().getChannelConfiguration(), 
							respeaker.getRecorder().getMicrophone().getAudioFormat());
					file.prepare(filename);
					file.write(respeaker.getRecorder().getAudioBuffer(), respeaker.getRecorder().getAudioBufferLength());
					file.close();
					if(BuildConfig.DEBUG)Log.i(TAG, "Temporary segment saved into file " + filename);

					try {
						if(BuildConfig.DEBUG)Log.i(TAG, "New SimplePlater of" + filename + " (rate: " + rate + ")");
						listenFragment.setPlayer(new SimplePlayer(new File(filename), rate, true));
					} catch (IOException | NoSuchElementException e) {
						if(BuildConfig.DEBUG)Log.e(TAG, e.toString());
					}
				}
				return false;
			}
		});
	}

	/**
	 *  Wrapper to more safely stop threads.
	 * @param thread which we want to stop
	 */
	private void stopThread(Thread thread) {
		if(thread != null)
			thread.interrupt();
	}
	
	/**
	 * Permits to the main SeekBar a global evolution every 1 sec
	 */
	private void startProgressionGlobalBar() {
		seekBarThread = new Thread(new Runnable() {
			public void run() {
				for(int currentPosition;true;)
				{
					currentPosition = respeaker.getSimplePlayer().getCurrentMsec();
					getActivity().runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							textTimeProgression.setText(respeaker.getSimplePlayer().getCurrentMsec()/1000 + "s");
						}
					});
					seekBar.setProgress(
							(int)(((float)currentPosition/(float) respeaker.getSimplePlayer().getDurationMsec())*100));
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {return;}
				}
			}});
		seekBarThread.start();
	}
	

	private void resetPlayer()
	{
		listenFragment = new ListenFragment();
		getFragmentManager().beginTransaction().replace(R.id.respeak_latest_segment_player, listenFragment).commit();
	}
	
	/**
	 * called when we need to delete last temp file and
	 * decrements the count attribute
	 */
	public void deleteLastTempFile() {
		String filename = FileIO.getOwnerPath().getAbsolutePath() + "/recordings/temp_rspk.wav";
		try {
			FileIO.delete(new File(filename));
			if(BuildConfig.DEBUG)Log.i(TAG, "Deleting " + filename);
		} catch (IOException e) {
			if(BuildConfig.DEBUG)Log.e(TAG, "failed to delete " + filename + " - maybe the file didn't exist (3)");
		}
	}

	/**
	 * called when we need to return one state before.
	 */
	private void onUndoAction() {
		if(count == 1){
			final ImageButton okButton = (ImageButton) v.findViewById(R.id.saveButton);
			okButton.setImageResource(R.drawable.ok_disabled_48);
			okButton.setEnabled(false);
            undo.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.undo2, null));
			undo.setEnabled(false);
		} else if(undo.isEnabled()) {
            undo.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.undo2, null));
            undo.setEnabled(false);
        }
		resetPlayer();
		deleteLastTempFile();
		count--;
		respeaker.goToPreviousSample();
		updateEvolution();
		// Remapping
		_toRemap=false;
		_currentPosRemap--;
	}
	
	private void updateEvolution() {
		seekBar.setProgress((int)(((float)respeaker.getSimplePlayer().getCurrentMsec()/(float)
				respeaker.getSimplePlayer().getDurationMsec())*100));
		getActivity().runOnUiThread(new Runnable() {
			@Override public void run() {
				textTimeProgression.setText(respeaker.getSimplePlayer().getCurrentMsec()/1000 + "s");
			}});
	}

	public void setThumbRespeaker(ThumbRespeaker respeaker) {
		this.respeaker = respeaker;
		respeaker.getSimplePlayer().setOnCompletionListener(onCompletionListener);
		updateDurationTextView();
	}

	/**
	 * ThumbRespeaker mutator.
	 *
	 * @param	respeaker	The ThumbRespeaker to use.
	 */
	public void setThumbRespeaker(ThumbRespeaker respeaker, Boolean newSession) {
		this.respeaker = respeaker;		
		respeaker.getSimplePlayer().setOnCompletionListener(onCompletionListener);
		updateDurationTextView();
		if(!newSession)
			updateEvolution();
	}
	
	public ThumbRespeaker getRespeaker() {
		return respeaker;
	}

	public String getSerializedRemap()
	{
		String b = "";
		for(Integer v : _remap)
			b+=(b.isEmpty())?v.toString():"§!§"+v.toString();
		return b;
	}
	public void setSerializedRemap(String sMap)
	{
		if(_remap == null)	_remap = new ArrayList<>();
		else				_remap.clear();
		for(String s: sMap.split("§!§"))
			_remap.add(Integer.parseInt(s));
	}

	/**
	 * Called when we need to setText for the duration of a selected wav file.
	 * Called by setThumbRespeaker
	 */
	private void updateDurationTextView() {
		if(BuildConfig.DEBUG)Log.d("respeaker", Boolean.toString(respeaker != null));
		getActivity().runOnUiThread(new Runnable() {
			@Override public void run() {
				totalTime.setText(" / " + respeaker.getSimplePlayer().getDurationMsec()/1000 + "s");
			}});
	}

	private Player.OnCompletionListener onCompletionListener =
			new Player.OnCompletionListener() {
				@Override public void onCompletion(Player _player) {
					stopThread(seekBarThread);
					seekBar.setProgress(seekBar.getMax());
				}
			};
	private ThumbRespeaker respeaker;
	
	/**
	 * a global progression bar updated every 0.001 sec
	 */
	private InterleavedSeekBar seekBar;
	
	/**
	 * thread used in startProgressionGlobalBar to update a 
	 * seekBar every 1 sec
	 */
	private Thread seekBarThread;
	
	/**
	 * listenFragment containing a seekBar, a play and pause button.
	 */
	private ListenFragment listenFragment;
	
	/**
	 * textview which displays the progression every seconds.
	 */
	private TextView textTimeProgression;
	
	/**
	 * textview which displays the total time of the selected file.
	 */
	private TextView totalTime;
	
	private final int VALID_GESTURE_TIME = 250; //0.25sec
	
	/**
	 * time we have kept in touch on the button
	 */
	private long gestureTime = VALID_GESTURE_TIME;
	
	/**
	 * time we have kept hand apart the button
	 */
	private long gestureTimeUpToDown = VALID_GESTURE_TIME;
	private boolean isCommented = true;
	private int count = 0;
	
	private View v;
	
	private ImageButton undo;

	public CheckBox _is_speak;

	public static final String TAG = "ThumbRespeakFragment";

	// Remapping
	public boolean _toRemap = false;
	public int _currentPosRemap = -1;
	public ArrayList<Integer> _remap = null;

	// WaveForm view
	private ImplementWaveformFragment _waveformFragment;

	@SuppressLint("ValidFragment")
	class ImplementWaveformFragment extends WaveformFragment {}
}
