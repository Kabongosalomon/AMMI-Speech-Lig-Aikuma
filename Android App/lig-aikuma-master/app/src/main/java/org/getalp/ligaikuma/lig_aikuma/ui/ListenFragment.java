/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;

import org.getalp.ligaikuma.lig_aikuma.audio.InterleavedPlayer;
import org.getalp.ligaikuma.lig_aikuma.audio.MarkedPlayer;
import org.getalp.ligaikuma.lig_aikuma.audio.Player;
import org.getalp.ligaikuma.lig_aikuma.audio.SimplePlayer;
import org.getalp.ligaikuma.lig_aikuma.audio.TranscriptPlayer;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;
import org.getalp.ligaikuma.lig_aikuma.model.Segments;
import org.getalp.ligaikuma.lig_aikuma.model.Segments.Segment;

import java.util.Iterator;

/**
 * A fragment used to perform audio playback; offers a seekbar, a play and
 * pause button
 *
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public class ListenFragment extends Fragment implements OnClickListener {

	/**
	 * Called to have the ListenFragment instantiate it's interface view.
	 *
	 * @param	inflater	The LayoutInflater to inflate views in the
	 * fragment.
	 * @param	container	The parent view to attach the fragment view to.
	 * @param	savedInstanceState	Non-null if the fragment is being
	 * reconstructed.
	 *
	 * @return	The created view.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		inflatedView = inflater.inflate(R.layout.listen_fragment, container, false);
		playPauseButton = (ImageButton) inflatedView.findViewById(R.id.PlayPauseButton);
		playPauseButton.setOnClickListener(this);
		playPauseButton.setEnabled(false);
		
		disabledSeekBar();

		return inflatedView;
	}
	
	/**
	 * called by constructor onCreate() because we need to 
	 * disabled seekBar touch event if we havn't set a player object
	 */
	private void disabledSeekBar() {
		seekBar = (InterleavedSeekBar) inflatedView.findViewById(R.id.InterleavedSeekBar);
		seekBar.setOnSeekBarChangeListener(
				new SeekBar.OnSeekBarChangeListener() {
					int originalProgress;
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
						if (fromUser)
							seekBar.setProgress(originalProgress);
					}
					public void onStopTrackingTouch(SeekBar _seekBar) {}

                    public void onStartTrackingTouch(SeekBar _seekBar) {
						originalProgress = seekBar.getProgress();
					}
                });

		seekBar.invalidate();
	}
	
	/**
	 * called after we have set a player object
	 */
	private void enabledSeekBar() {
		seekBar = (InterleavedSeekBar) inflatedView.findViewById(R.id.InterleavedSeekBar);
		seekBar.setOnSeekBarChangeListener(
				new SeekBar.OnSeekBarChangeListener() {
					int prog;
					boolean isPlayed = false;
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
						if(!fromUser)	return;
							if(player.isPlaying())
							{
								pause ();
								isPlayed = true;
							}
							prog = progress;		
						}
					public void onStopTrackingTouch(SeekBar _seekBar) {
						player.seekToMsec(Math.round((((float)prog)/100)* player.getDurationMsec()));
						if(!isPlayed)	return;
						isPlayed = false;
						play ();
					}
					public void onStartTrackingTouch(SeekBar _seekBar) {}
                });
		seekBar.invalidate();
	}

	/**
	 * Called when the Fragment is obstructed by another view, or the activity has
	 * changed.
	 */
	@Override
	public void onPause() {
		pause();
		super.onPause();
	}

	/**
	 * Called when the Fragment is destroyed; ensures proper cleanup.
	 */
	@Override
	public void onDestroy() {
		if(player != null)
			//If you hit the stop button really quickly, the player may not
			//have been initialized fully.
			player.release();
		super.onDestroy();
	}

	/**
	 * Used to evaluate what the playPauseButton should do when clicked, as
	 * ListenFragment implements OnClickListener.
	 *
	 * @param	v	The view clicked
	 */
	@Override
	public void onClick(View v) {
		if(v != playPauseButton)	return;
		if(player.isPlaying()) {
			pause();
			if(otherPlayer != null) otherPlayer.setEnabled(true);
			return;
		}
		if(otherPlayer != null) otherPlayer.setEnabled(false);
		play();
	}

	/**
	 * A wrapper to Thread.interrupt() to prevent null-pointer
	 * exceptions.
	 *
	 * @param	thread	The thread to interrupt.
	 */
	private void stopThread(Thread thread) {
		if(thread != null)
			thread.interrupt();
	}

	/**
	 * Pauses play of the audio and handles the GUI appropriately.
	 */
	private void pause() {
		if(player == null)	return;
		player.pause();
		stopThread(seekBarThread);
		playPauseButton.setImageResource(R.drawable.play_g);
	}

	/**
	 * Plays the audio and handles the GUI appropriately.
	 */
	private void play() {
		player.play();
		seekBarThread = new Thread(new Runnable() {
				public void run() {
					for(int currentPosition;true;)
					{
						currentPosition = player.getCurrentMsec();
						seekBar.setProgress((int)(((float)currentPosition/(float) player.getDurationMsec())*100));
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							currentPosition = player.getCurrentMsec();
							seekBar.setProgress((int)(((float)currentPosition/(float) player.getDurationMsec())*100));
							if(otherPlayer != null) otherPlayer.setProgress(currentPosition);
							return;
						}
					}
				}
		});
		seekBarThread.start();
		playPauseButton.setImageResource(R.drawable.pause_g);
	}
	
	/**
	 * Set the play-cursor to msec position
	 * 
	 * @param msec	Play-cursor position
	 */
	public void setProgress(int msec) {
		player.seekToMsec(msec);
		seekBar.setProgress((int)(((float)msec / (float)player.getDurationMsec())*100));
	}

	/**
	 * Sets the player that the ListenFragment is to use.
	 *
	 * @param	simplePlayer	The simple player to be used.
	 */
	public void setPlayer(SimplePlayer simplePlayer) {
		this.player = simplePlayer;
		player.setOnCompletionListener(onCompletionListener);
		playPauseButton.setEnabled(true);
		enabledSeekBar();
	}

	/**
	 * Sets the player that the ListenFragment is to use
	 *
	 * @param	markedPlayer	The marked player to be used.
	 */
	public void setPlayer(MarkedPlayer markedPlayer) {
		this.player = markedPlayer;
		player.setOnCompletionListener(onCompletionListener);
		playPauseButton.setEnabled(true);
		enabledSeekBar();
	}

	/**
	 * Sets the player that the ListenFragment is to use
	 *
	 * @param	transcriptPlayer	The transcript player to be used.
	 */
	public void setPlayer(TranscriptPlayer transcriptPlayer) {
		this.player = transcriptPlayer;
		player.setOnCompletionListener(onCompletionListener);
		playPauseButton.setEnabled(true);
		enabledSeekBar();
	}

	/**
	 * Sets the player that the ListenFragment is to use
	 *
	 * @param	interleavedPlayer	The interleaved player to be used.
	 */
	public void setPlayer(InterleavedPlayer interleavedPlayer) {
		this.player = interleavedPlayer;
		Iterator<Segment> originalSegmentIterator = new Segments(interleavedPlayer.getRecording()).getOriginalSegmentIterator();
		while(originalSegmentIterator.hasNext())
			seekBar.addLine((player.sampleToMsec(originalSegmentIterator.next().getEndSample())/(float) player.getDurationMsec())*100);
		player.setOnCompletionListener(onCompletionListener);
		playPauseButton.setEnabled(true);
		enabledSeekBar();
	}
	
	public void releasePlayer() {
		if (this.player == null)	return;
		this.player.release();
		playPauseButton.setImageResource(R.drawable.play_g);
		playPauseButton.setEnabled(false);
	}
	
	/**
	 * Connet this player with the other player
	 * 
	 * @param player	The other player
	 */
	public void setOtherPlayer(ListenFragment player) {
		this.otherPlayer = player;
	}
	
	/**
	 * Enable/Disable the play-pause button of the player
	 * 
	 * @param isEnabled		true: enable, false: disable
	 */
	private void setEnabled(boolean isEnabled) {
		playPauseButton.setEnabled(isEnabled);
		playPauseButton.setImageResource((isEnabled)?R.drawable.play_g:R.drawable.play_grey);
	}
	
	public Player getPlayer() {
		return player;
	}

	/** Defines behaviour for the fragment when a recording finishes playing.*/
	private Player.OnCompletionListener onCompletionListener =
			new Player.OnCompletionListener() {
				public void onCompletion(Player _player) {
					playPauseButton.setImageResource(R.drawable.play_g);
					stopThread(seekBarThread);
					seekBar.setProgress(seekBar.getMax());
				}
			};
			
	public void onMarkedReachedListener() {
		pause();
	}

	private Player player;
	private ImageButton playPauseButton;
	private InterleavedSeekBar seekBar;
	private Thread seekBarThread;
	private View inflatedView;
	private ListenFragment otherPlayer;
}
