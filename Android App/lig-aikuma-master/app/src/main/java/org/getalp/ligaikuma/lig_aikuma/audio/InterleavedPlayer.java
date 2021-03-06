/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma.audio;

import android.util.Log;

import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.model.Recording;
import org.getalp.ligaikuma.lig_aikuma.model.Segments;
import org.getalp.ligaikuma.lig_aikuma.model.Segments.Segment;

import java.io.IOException;
import java.util.Iterator;

/**
 * A player that plays both the original recording and its respeaking in an
 * interleaved fashion, whereby a segment of the original is played, followed
 * by the corresponding segment of the respeaking, followed by the next
 * original segment and so on.
 *
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public class InterleavedPlayer extends Player {

	/**
	 * Creates an InterleavedPlayer to play the supplied recording and it's
	 * original
	 *
	 * @param	recording	The metadata of the recording to play.
	 * @throws	IOException	If there is an issue reading the recordings.
	 */
	public InterleavedPlayer(Recording recording) throws IOException {
		setRecording(recording);
		if(recording.isOriginal())
			throw new IllegalArgumentException("The supplied Recording is not a respeaking. Use SimplePlayer instead.");
		setSampleRate(recording.getSampleRate());
		original = new MarkedPlayer(recording.getOriginal(), new OriginalMarkerReachedListener(), true);
		respeaking = new MarkedPlayer(recording, new RespeakingMarkerReachedListener(), true);
		segments = new Segments(recording);
		initializeCompletionListeners();
		completedOnce = false;
	}

	// Initializes the completion listeners for the original and respeaking,
	// such that when one of them completes the InterleavedPlayer's
	// onCompletionListener is run, only once.
	private void initializeCompletionListeners() {
		Player.OnCompletionListener bothCompletedListener = new 
				Player.OnCompletionListener() {
			//boolean completedOnce = false;
			@Override
			public void onCompletion(Player p) {
				if(completedOnce) {
					if(onCompletionListener != null)
						onCompletionListener.onCompletion(p);
					reset();
				}
				completedOnce = !completedOnce;
			}
		};
		original.setOnCompletionListener(bothCompletedListener);
		respeaking.setOnCompletionListener(bothCompletedListener);
	}

	/**
	 * Resumes playing the interleaved recording, from the original segment
	 * where it was last paused.
	 */
	public void play() {
		playOriginal();
	}

	/**
	 * Resets the player to the beginning.
	 */
	public void reset() {
		originalSegmentIterator = null;
		currentOriginalSegment = null;
		original.seekToSample(0l);
		respeaking.seekToSample(0l);
	}

	/**
	 * Indicates whether the recording is currently being played.
	 *
	 * @return	true if the recording is currently playing; false otherwise.
	 */
	public boolean isPlaying() {
		return original.isPlaying() || respeaking.isPlaying();
	}

	/** Pauses the playback. */
	public void pause() {
		if(original.isPlaying())	original.pause();
		if(respeaking.isPlaying())	respeaking.pause();
	}

	/**
	 * Get current point in the recording in milliseconds.
	 *
	 * @return	The current point in the recording in milliseconds as an int.
	 */
	public int getCurrentMsec() {
		return original.getCurrentMsec();
	}

	/**
	 * Get the duration of the recording in milliseconds.
	 *
	 * @return	The duration of the recording in milliseconds as an int.
	 */
	public int getDurationMsec() {
		return original.getDurationMsec();
	}

	/** Releases resources associated with the InterleavedPlayer. */
	public void release() {
		original.release();
		respeaking.release();
	}

	// Plays the current original segment.
	private void playOriginal() {
		playSegment(getCurrentOriginalSegment(), original);
	}

	private Segment getCurrentOriginalSegment() {
		if(currentOriginalSegment == null)
			advanceOriginalSegment();
		return currentOriginalSegment;
	}

	// Plays the current respeaking segment.
	private void playRespeaking() {
		playSegment(getCurrentRespeakingSegment(), respeaking);
	}

	private Segment getCurrentRespeakingSegment() {
		return segments.getRespeakingSegment(getCurrentOriginalSegment());
	}

	// Plays the specified segment in the specified player.
	private void playSegment(Segment segment, MarkedPlayer player) {
		if(segment == null) return;
		player.seekTo(segment);
		player.setNotificationMarkerPosition(segment);
		player.play();
	}

	// Moves forward to the next original segment in the recording.
	private void advanceOriginalSegment() {
		currentOriginalSegment = (getOriginalSegmentIterator().hasNext())?
			getOriginalSegmentIterator().next():
			new Segment((currentOriginalSegment != null)? currentOriginalSegment.getEndSample(): 0l, Long.MAX_VALUE);
	}

	// Gets an iterator over the segments of the original recording.
	private Iterator<Segment> getOriginalSegmentIterator() {
		if (originalSegmentIterator == null)
			originalSegmentIterator = segments.getOriginalSegmentIterator();
		return originalSegmentIterator;
	}

	private MarkedPlayer original;
	private MarkedPlayer respeaking;
	private Segments segments;
	private Segment currentOriginalSegment;
	private Iterator<Segment> originalSegmentIterator;
	private Player.OnCompletionListener onCompletionListener;
	private Recording recording;

	private void setRecording(Recording recording) {
		this.recording = recording;
	}

	public Recording getRecording() {
		return this.recording;
	}

	/**
	 * Implements a method for the original audio source to call when an
	 * original segment has finished playing.
	 */
	private class OriginalMarkerReachedListener extends
			MarkedPlayer.OnMarkerReachedListener {
		public void onMarkerReached(MarkedPlayer p) {
			if(BuildConfig.DEBUG)Log.i("release", "original onMarker reached, completedOnce = " + completedOnce);
			original.pause();
			playRespeaking();
		}
	}

	/**
	 * Implements a method for the respeaking audio source to call when a
	 * respeaking segment has finished playing.
	 */
	private class RespeakingMarkerReachedListener extends
			MarkedPlayer.OnMarkerReachedListener {
		public void onMarkerReached(MarkedPlayer p) {
			if(BuildConfig.DEBUG)Log.i("release", "respeaking onMarker reached, completedOnce = " + completedOnce);
			respeaking.pause();
			if(!completedOnce) {
				advanceOriginalSegment();
				playOriginal();
			}
		}
	}

	/**
	 * Gets the sample rate of this player.
	 *
	 * @return	The sample rate of the player as a long.
	 */
	public long getSampleRate() {
		//If the sample rate is less than zero, then this indicates that there
		//wasn't a sample rate found in the metadata file.
		if (sampleRate <= 0l)
			throw new RuntimeException("The sampleRate of the recording is not known.");
		return sampleRate;
	}

	private void setSampleRate(long sampleRate) {
		this.sampleRate = sampleRate;
	}

	/**
	 * Converts samples to milliseconds assuming this players sample rate
	 *
	 * @param	sample	A sample value to be converted
	 * @return	The value in milliseconds as an int.
	 */
	public int sampleToMsec(long sample) {
		long msec = sample / (getSampleRate() / 1000);
		return (msec > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) msec;
	}

	/**
	 * Seek to a given point in the recording in milliseconds.
	 *
	 * @param	msec	The point in the recording to seek to in milliseconds.
	 */
	public void seekToMsec(int msec) {
		reset();
		while(getOriginalSegmentIterator().hasNext()) {
			currentOriginalSegment = getOriginalSegmentIterator().next();
			if(msec >= sampleToMsec(currentOriginalSegment.getStartSample()) &&
				msec <= sampleToMsec(currentOriginalSegment.getEndSample()))
				break;
		}
	}

	/**
	 * Set the callback to be run when the recording completes playing.
	 *
	 * @param	listener	The callback to call when playing is complete.
	 */
	public void setOnCompletionListener(final Player.OnCompletionListener listener) {
		this.onCompletionListener = listener;
	}

	private static boolean completedOnce = false;
	private long sampleRate;
}
