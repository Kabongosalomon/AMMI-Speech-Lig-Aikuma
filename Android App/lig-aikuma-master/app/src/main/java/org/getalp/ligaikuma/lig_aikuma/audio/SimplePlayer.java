/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma.audio;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import com.semantive.waveformandroid.waveform.MediaPlayerFactory;

import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.model.Recording;

import java.io.File;
import java.io.IOException;

/**
 * A wrapper class for android.media.MediaPlayer that makes simpler the task of
 * playing an Aikuma recording.
 *
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public class SimplePlayer extends Player implements Sampler
{
	/** The MediaPlayer used to play the recording. **/
	private MediaPlayer mediaPlayer;
	private long sampleRate;
	private boolean finishedPlaying;
	private Recording recording;

	/**
	 * Creates a player to play the supplied recording.
	 *
	 * @param	recording	The metadata of the recording to play.
	 * @param	playThroughSpeaker	True if the audio is to be played through the main
	 * speaker; false if through the ear piece (ie the private phone call style)
	 * @throws	IOException	If there is an issue reading the audio source.
	 */
	public SimplePlayer(Recording recording, boolean playThroughSpeaker) throws IOException {
		setRecording(recording);
		mediaPlayer = MediaPlayerFactory.getNewMediaPlayer();
		mediaPlayer.setAudioStreamType((playThroughSpeaker)?AudioManager.STREAM_MUSIC:AudioManager.STREAM_VOICE_CALL);
		if(BuildConfig.DEBUG)Log.i("SimplePlayer", "Media Player - data source: " + recording.getFile().getCanonicalPath());
		mediaPlayer.setDataSource(recording.getFile().getCanonicalPath());
		if(BuildConfig.DEBUG)Log.i("SimplePlayer", "Media Player - preparing...");
		mediaPlayer.prepare();
		if(BuildConfig.DEBUG)Log.i("SimplePlayer", "Media Player - sample rate: " + recording.getSampleRate());
		setSampleRate(recording.getSampleRate());
	}

	private void setRecording(Recording recording) {
		this.recording = recording;
	}

	/**
	 * Creates a player to play the supplied recording for when no Recording
	 * metadata file exists
	 *
	 * @param	recordingFile	The location of the recording as a File
	 * @param	sampleRate	The sample rate of the recording
	 * @param	playThroughSpeaker	True if the audio is to be played through the main
	 * speaker; false if through the ear piece (ie the private phone call style)
	 * @throws	IOException	If there is an issue reading the audio source.
	 */
	public SimplePlayer(File recordingFile, long sampleRate, boolean playThroughSpeaker) throws IOException {
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioStreamType((playThroughSpeaker)? AudioManager.STREAM_MUSIC:AudioManager.STREAM_VOICE_CALL);
		mediaPlayer.setDataSource(recordingFile.getCanonicalPath());
		mediaPlayer.prepare();
		setSampleRate(sampleRate);
	}
	
	public SimplePlayer(SimplePlayer player, boolean playThroughSpeaker) throws IOException{
		mediaPlayer = new MediaPlayer();
		setRecording(player.getRecording());
		mediaPlayer.setAudioStreamType((playThroughSpeaker)? AudioManager.STREAM_MUSIC:AudioManager.STREAM_VOICE_CALL);
		mediaPlayer.setDataSource(player.getRecording().getFile().getCanonicalPath());
		mediaPlayer.prepare();
		setSampleRate(player.getSampleRate());
	}

	/** Starts or resumes playback of the recording. */
	public void play() {
		this.finishedPlaying = false;
		mediaPlayer.start();
	}

	/** Pauses the playback. */
	public void pause() {
		try {
			// If it's not in a started state, then the OnCompletionListener
			// may be called when pause() is, so ensure it's already playing
			// before calling pause().
			if(mediaPlayer.isPlaying())
				mediaPlayer.pause();
		} catch (IllegalStateException e) {
			//If it's in an illegal state, then it wouldn't be playing anyway,
			//so no issue.
		}
	}
	
	/**
	 * Rewind back for rewindAmount
	 * @param rewindAmount	in msec
	 */
	public void rewind(int rewindAmount) {
		int rewindedPos = (getCurrentMsec()-rewindAmount);
		seekToMsec((rewindedPos < 0)? 0 : rewindedPos);
	}

	/**
	 * Indicates whether the recording is currently being played.
	 *
	 * @return	true if the player is currently playing; false otherwise.
	 */
	public boolean isPlaying() {
		return mediaPlayer.isPlaying();
	}

	/**
	 * Get current point in the recording in milliseconds.
	 *
	 * @return	The current point in the recording in milliseconds as an int.
	 */
	public int getCurrentMsec() {
		try {
			return mediaPlayer.getCurrentPosition();
		} catch (IllegalStateException e) {
			//If we get an IllegalStateException because the recording has
			//finished playing, just return the duration of the recording.
			return getDurationMsec();
		}
	}

	public long getCurrentSample() {
		return msecToSample(getCurrentMsec());
	}

	/**
	 * Get the duration of the recording in milliseconds.
	 *
	 * @return	The duration of the audio in milliseconds as an int.
	 */
	public int getDurationMsec() {

		try { return mediaPlayer.getDuration();
		} catch (IllegalStateException e) {
			return 0;
		}
	}

	/**
	 * Seek to a given point in the recording in milliseconds.
	 *
	 * @param	msec	The time to jump the playback to in milliseconds.
	 */
	public void seekToMsec(int msec) {
		mediaPlayer.seekTo(msec);
	}

	/**
	 * Moves the recording to the given sample.
	 *
	 * @param	sample	The sample to jump playback to.
	 */
	public void seekToSample(long sample) {
		seekToMsec(sampleToMsec(sample));
	}

	/** Releases the resources associated with the SimplePlayer */
	public void release() {
		mediaPlayer.release();
	}

	//Never use
	//protected MediaPlayer getMediaPlayer() {return mediaPlayer;}

	//Never use
	//protected void setFinishedPlaying(boolean finished) {this.finishedPlaying = finished;}
	
	public Recording getRecording() {
		return recording;
	}

	/**
	 * Set the callback to be run when the recording completes playing.
	 *
	 * @param	listener	The callback to be called when the recording
	 * complets playing.
	 */
	public void setOnCompletionListener(final OnCompletionListener listener) {
		mediaPlayer.setOnCompletionListener(
				new MediaPlayer.OnCompletionListener() {
					public void onCompletion(MediaPlayer _mp) {
						listener.onCompletion(SimplePlayer.this);
						SimplePlayer.this.finishedPlaying = true;
					}
				});
	}

	///**
	// * Increment the view count by adding a view file to the Recording's view
	// * directory.
	// */
	//Never use
	/*private void incrementViewCount() throws IOException {
		String versionName = recording.getVersionName();
		String ownerId = recording.getOwnerId();
		File viewDir = new File(FileIO.getOwnerPath(versionName, ownerId),
				"views/" +
				recording.getGroupId() + "/" + recording.getId());
		viewDir.mkdirs();

		File viewFile = new File(viewDir, UUID.randomUUID() + ".view");
		viewFile.createNewFile();
	}*/

	/**
	 * Returns the sample rate of this recording
	 *
	 * @return	The sample rate of this recording.
	 */
	public long getSampleRate() {
		//If the sample rate is less than zero, then this indicates that there
		//wasn't a sample rate found in the metadata file.
		if(sampleRate <= 0l)
			throw new RuntimeException("The sampleRate of the recording is not known.");
		return sampleRate;
	}

	private void setSampleRate(long sampleRate) {
		this.sampleRate = sampleRate;
	}

	/**
	 * Converts a value of samples into milliseconds assuming this recording's
	 * sample rate.
	 *
	 * @param	sample	sample value to be converted.
	 * @return	A millisecond value as an integer.
	 */
	public int sampleToMsec(long sample) {
		long msec = sample / (getSampleRate() / 1000);
		return (msec > Integer.MAX_VALUE)? Integer.MAX_VALUE: (int) msec;
	}

	/**
	 * Converts a millisecond value into samples assuming this recording's
	 * sample rate.
	 *
	 * @param	msec	A time value in milliseconds.
	 * @return	A sample value as a long.
	 */
	public long msecToSample(int msec) {
		return msec * (getSampleRate() / 1000);
	}

	public boolean isFinishedPlaying() {
		return this.finishedPlaying;
	}
}
