/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma.audio.record;

import android.util.Log;

import org.getalp.ligaikuma.lig_aikuma.audio.Player;
import org.getalp.ligaikuma.lig_aikuma.audio.SimplePlayer;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.model.Recording;
import org.getalp.ligaikuma.lig_aikuma.model.RecordingLig;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.getalp.ligaikuma.lig_aikuma.audio.record.Microphone.MicException;

/**
 * Facilitates respeaking of an original recording by offering methods to start
 * and pause playing the original, and start and pause recording the
 * respeaking.
 * 
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public class ThumbRespeaker {

	/**
	 * Constructor
	 *
	 * @param	original	The original recording to make a respeaking of.
	 * @param	respeakingUUID	The UUID of the respeaking we will create.
	 * @param	rewindAmount	Rewind-amount in msec after each respeaking-segment
	 * @throws	MicException	If the microphone couldn't be used.
	 * @throws	IOException	If there is an I/O issue.
	 */
	public ThumbRespeaker(Recording original, UUID respeakingUUID, 
			int rewindAmount) throws Microphone.MicException, IOException {
		if(BuildConfig.DEBUG)Log.d("Rte-ThumbRespeaker", "smp : " + original.getSampleRate());
		recorder = new Recorder(1, new File(Recording.getNoSyncRecordingsPath(), respeakingUUID + ".wav"), original.getSampleRate());
		player = new SimplePlayer(original, true);
		mapper = new Mapper(respeakingUUID);
		setFinishedPlaying(false);
		this.rewindAmount = rewindAmount;
		ref = this;
	}

	/**
	 * Constructor
	 * LIG version
	 *
	 * @param	original	The original recording (of class RecordingLig) to make a respeaking of.
	 * @param	respeakingUUID	The UUID of the respeaking we will create.
	 * @param	rewindAmount	Rewind-amount in msec after each respeaking-segment
	 * @throws	MicException	If the microphone couldn't be used.
	 * @throws	IOException	If there is an I/O issue.
	 */
	public ThumbRespeaker(RecordingLig original, UUID respeakingUUID, int rewindAmount)
			throws MicException, IOException {
		recorder = new Recorder(1, new File(Recording.getNoSyncRecordingsPath(), respeakingUUID + ".wav"), original.getSampleRate());
		player = new SimplePlayer(original, true);
		mapper = new Mapper(respeakingUUID);
		setFinishedPlaying(false);
		this.rewindAmount = rewindAmount;
		ref = this;
	}
	
	public static ThumbRespeaker getThumbRespeaker() {
		return ref;
	}

	/* *
	 * Plays the original recording.
	 */
	/**
	 * Plays the original recording.
	 * @param playMode	(0:Continue, 1:Rewind and Store, 2:Only rewind)
	 */
	public void playOriginal(int playMode) {
		switch(playMode) {
		case 1:	//Rewind and record the start-sample in mapper
			player.seekToSample(mapper.getOriginalStartSample());
			if(BuildConfig.DEBUG)Log.d("original sample", "get original start sample : " + mapper.getOriginalStartSample());
			mapper.markOriginal(player);
			player.rewind(rewindAmount);
			break;
		case 2:	//Only rewind to the previous point
			player.seekToSample(previousEndSample);
			break;
		}
		if(BuildConfig.DEBUG)Log.d("previousEndSample", "previousEndSample -> " + previousEndSample/getSimplePlayer().getSampleRate());
		previousEndSample = player.getCurrentSample();
		if(BuildConfig.DEBUG)Log.d("previousEndSample", "previousEndSampleAfter -> " + previousEndSample/getSimplePlayer().getSampleRate());
		player.play();
	}

	/**
	 * Pauses playing of the original recording.
	 */
	public void pauseOriginal() {
		player.pause();		
	}

	/**
	 * Activates recording of the respeaking.
	 */
	public void recordRespeaking() {
		mapper.markRespeaking(player, recorder);
		recorder.listen();
	}

	/**
	 * called when we need to going on the previous sample.
	 */
	public void goToPreviousSample() {
			player.seekToSample((previousEndSample > 0l)?	previousEndSample:	0);
	}
	/**
	 * Pauses the respeaking process.
	 *
	 * @throws	MicException	If the micrphone recording couldn't be paused.
	 */
	public void pauseRespeaking() throws MicException {
		recorder.pause();
	}

	/**
	 * Saves the respeaking audio and mapping-information
	 */
	public void saveRespeaking() {
		recorder.save();
		
		// Because of rewind after each respeaking-segment,
		// Force user to record respeaking after listening next original-segment
		if(player.getCurrentSample() > mapper.getOriginalStartSample()) {
			if(BuildConfig.DEBUG)Log.d("save map file", "save map file at " + mapper.getMapFile().getAbsolutePath());
			mapper.store(player, recorder);			
		}
	}
	
	public long getPreviousEndSample() {
		return previousEndSample;
	}

	public void setPreviousEndSample(long previousEndSample) {
		this.previousEndSample = previousEndSample;
	}

	/**
	 * Stops/finishes the respeaking process
	 *
	 * @throws	MicException	If there is an issue stopping the microphone.
	 * @throws	IOException	If the mapping between original and respeaking
	 * couldn't be written to file.
	 */
	public void stop() throws MicException, IOException {
		recorder.stop();
		player.pause();
		mapper.stop();
	}

	public void setFinishedPlaying(boolean finishedPlaying) {
		this.finishedPlaying = finishedPlaying;
	}

	public int getCurrentMsec() {
		return recorder.getCurrentMsec();
	}
	
	/**
	 * finishedPlaying accessor
	 *
	 * @return	true if the original recording has finished playing; false
	 * otherwise.
	 */
	public boolean getFinishedPlaying() {
		return this.finishedPlaying;
	}
	
	public Mapper getMapper() {
		return this.mapper;
	}

	/**
	 * Sets the callback to be run when the original recording has finished
	 * playing.
	 *
	 * @param	ocl	The callback to be played on completion.
	 */
	public void setOnCompletionListener(Player.OnCompletionListener ocl) {
		player.setOnCompletionListener(ocl);
	}

	public SimplePlayer getSimplePlayer() {
		return this.player;
	}

	
	public void setMapper(Mapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * Releases the resources associated with this respeaker.
	 */
	public void release() {
		if(player != null) player.release();
	}

	public Recorder getRecorder() {
		return this.recorder;
	}

	public void setRemap(int[] remap) {_remapIndex = remap;}

	public int[] getRemap() {return _remapIndex;}

	public void setDestRecFullPath(String p){_dest_rec_full_path=p;}

	public String getDestRecFullPath(){return _dest_rec_full_path;}

	/** Player to play the original with. */
	private SimplePlayer player;

	/** The recorder used to get respeaking data. */
	private Recorder recorder;
	
	/** The mapper used to store mapping data. */
	private Mapper mapper;

	/** Indicates whether the recording has finished playing. */
	private boolean finishedPlaying;
	
	/** Previous sample-point */
	private long previousEndSample;

	/** Index used to remap segments */
	private int[] _remapIndex;
	
	/** The amount to rewind the original in msec 
	 * after each respeaking-segment. */
	private int rewindAmount;

	private String _dest_rec_full_path;
	
	private static ThumbRespeaker ref;
}
