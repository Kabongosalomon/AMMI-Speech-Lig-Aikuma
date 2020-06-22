/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma.audio.record;

import android.media.AudioFormat;
import android.util.Log;

import org.getalp.ligaikuma.lig_aikuma.audio.Sampler;
import org.getalp.ligaikuma.lig_aikuma.audio.record.analyzers.Analyzer;
import org.getalp.ligaikuma.lig_aikuma.audio.record.analyzers.SimpleAnalyzer;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.model.Recording;

import java.io.File;

import static org.getalp.ligaikuma.lig_aikuma.audio.record.Microphone.MicException;

/**
 *  A Recorder used to get input from a microphone and output into a file.
 *
 *  Usage:
 *    Recorder recorder = new Recorder(File, sampleRate [, Analyzer]);
 *    recorder.listen();
 *    recorder.pause();
 *    recorder.listen();
 *    recorder.stop();
 *
 *  Note that stopping the recorder closes and finalizes the WAV file.
 *
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public class Recorder implements AudioHandler, MicrophoneListener, Sampler {

	/**
	 * Creates a Recorder that uses an analyzer which tells the recorder to
	 * always record regardless of input.
	 * only used in ThumbRespeaker so Recorder-class only uses SimpleAnalyzer.
	 *
	 * @param	type	The recording type (0: original, 1: respeaking)
	 * @param	path	The path to the file where the recording will be stored
	 * @param	sampleRate	The sample rate that the recording should be taken
	 * at.
	 * @throws	MicException	If there is an issue setting up the microphone.
	 */
	public Recorder(int type, File path, long sampleRate) throws MicException {
		this(type, path, sampleRate, new SimpleAnalyzer());
		if(BuildConfig.DEBUG)Log.d("Recorder", "filePath : " + path.getAbsolutePath());
	}

	/**
	 * Creates a Recorder that uses an analyzer which tells the recorder to
	 * always record regardless of input. Contains a beeper.
	 */
	 /*
	public Recorder(File path, long sampleRate, RecordActivity recordActivity) throws MicException {
		this(path, sampleRate, new SimpleAnalyzer());
		beeper = new Beeper(recordActivity);
	}
	*/

	/**
	 * Constructor
	 *
	 * @param	type	The recording type (0: original, 1: respeaking)
	 * @param	path	The path to where the recording should be stored.
	 * @param	sampleRate	The sample rate the recording should be taken at.
	 * @param	analyzer	The analyzer that determines whether the recorder
	 * should record or ignore the input
	 * @throws	MicException	If there is an issue setting up the microphone.
	 */
	public Recorder(int type, File path, long sampleRate, Analyzer analyzer) throws MicException {
		this.type = type;
		this.analyzer = analyzer;
		setUpMicrophone(sampleRate);
		setUpFile();
		this.prepare(path.getPath());
		
		audioBuffer = new short[10 * Math.round(1000f*sampleRate/44100)];
		audioBufLength = 0;
		totalAudioLength = 0;
	}

	/** Start listening. */
	public void listen() {
		/*
		if (beeper != null) {
			beeper.beepBeep(new MediaPlayer.OnCompletionListener() {
				public void onCompletion(MediaPlayer _) {
					microphone.listen(Recorder.this);
					beeper.getRecordActivity().substituteRecordButton();
				}
			});
		} else {
		*/
		audioBufLength = 0;
		microphone.listen(this);
	}

	/**
	 * Returns the point in the recording at which the Recorder is up to, in
	 * samples.
	 *
	 * @return	The point in the recording that the Recorder is up to, in
	 * samples
	 */
	public long getCurrentSample() {
		return file.getCurrentSample();
	}

	public int getCurrentMsec() {
		return sampleToMsec(getCurrentSample());
	}
	
	public short[] getAudioBuffer() {
		return audioBuffer;
	}
	
	public int getAudioBufferLength() {
		return audioBufLength;
	}

	// Converts a sample value to milliseconds.
	private int sampleToMsec(long sample) {
		long msec = sample / (microphone.getSampleRate() / 1000);
		return (msec > Integer.MAX_VALUE)?
				Integer.MAX_VALUE:
				(int) msec;
	}

	/**
	 * Prepares the recorder for recording.
	 */
	private void prepare(String targetFilename) {
		file.prepare(targetFilename);
		
		if(this.type == 0) {
			String sampleFileName = 
					targetFilename.substring(0, targetFilename.lastIndexOf('.')) + 
					Recording.SAMPLE_SUFFIX;
			sampleFile.prepare(sampleFileName);
		}
	}

	/** Sets up the micrphone for recording */
	private void setUpMicrophone(long sampleRate) throws MicException {
		if(BuildConfig.DEBUG)Log.d("Rte-Recorder", "samrate: " + sampleRate);
		this.sampleRate = sampleRate;
		this.microphone = new Microphone();
	}

	/** Sets the file up for writing. */
	private void setUpFile() {
		file = PCMWriter.getInstance(
				microphone.getSampleRate(),
				microphone.getChannelConfiguration(),
				microphone.getAudioFormat()
		);
		
		if(this.type == 0) {
			sampleFile = PCMWriter.getInstance(
					microphone.getSampleRate(),
					microphone.getChannelConfiguration(),
					microphone.getAudioFormat()
			);
		}
	}
	
	public PCMWriter getWriter() {
		if (this.type == 0) { return sampleFile; }
		else if (this.type == 1) { return file; }
		return null;
	}

	/**
	 * Returns the number of channels of the WAV.
	 *
	 * @return	The number of channels of the WAV.
	 */
	public int getNumChannels() {
		return (microphone.getChannelConfiguration() == AudioFormat.CHANNEL_IN_MONO)?	1:	2;
	}

	/**
	 * Returns the bits per sample of the WAV
	 *
	 * @return	The bits per sample of the WAV.
	 */
	public int getBitsPerSample() {
		return (microphone.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT)?	16:	8;
	}
	
	public Microphone getMicrophone() {
		return microphone;
	}

	/**
	 * Returns the audio mime type (but only the section after the forward
	 * slash)
	 *
	 * @return	The audio format
	 */
	public String getFormat() {
		return "vnd.wave";
	}

	/**
	 * Stop listening to the microphone and close the file.
	 *
	 * Note: Once stopped you cannot restart the recorder.
	 *
	 * @throws	MicException	If there was an issue stopping the microphone.
	 */
	public void stop() throws MicException {
		microphone.stop();
		file.close();
		if(this.type == 0)
			sampleFile.close();
	}

	/**
	 * Release resources associated with this recorder.
	 */
	public void release() {
		if(microphone != null)
			microphone.release();
	}

	/**
	 * Pause listening to the microphone.
	 *
	 * @throws	MicException	If there was an issue stopping the microphone.
	 */
	public void pause() throws MicException {
		microphone.stop();
		/*
		beeper.beep();
		*/
	}
	
	/** 
	 * By default simply writes the audioBuffer to the file.
	 */
	public void save() {
		file.write(audioBuffer, audioBufLength);

		totalAudioLength += audioBufLength;
		if(BuildConfig.DEBUG)Log.d("DDtotalAudioLength", "totalAudioLength = "+totalAudioLength);
		if (this.type == 0 && 
				Math.round((double) totalAudioLength / sampleRate) < Recording.SAMPLE_SEC) // 15sec sample
			sampleFile.write(audioBuffer, audioBufLength);
		audioBufLength = 0;
	}

	/**
	 * Callback for the microphone.
	 *
	 * @param	buffer	The buffer containing audio data.
	 */
	public void onBufferFull(short[] buffer) {
		// This will call back the methods:
		//  * silenceTriggered
		//  * audioTriggered
		//
		
		// SimplieAnalyzer.analyze calls audioTriggered, which is just a writing function
		if(type == 0)	analyzer.analyze(this, buffer);
		else 			storeBuffer(buffer);
		
	}
	
	// Append all audio-values in srcBuffer to audioBuffer
	private void storeBuffer(short[] srcBuffer) {
		if(audioBuffer.length < audioBufLength + srcBuffer.length) {
			short[] newBuffer = new short[(2 * audioBuffer.length)];
			System.arraycopy(audioBuffer, 0, newBuffer, 0, audioBufLength);
			audioBuffer = newBuffer;
		}
		
		System.arraycopy(srcBuffer, 0, audioBuffer, audioBufLength, srcBuffer.length);
		audioBufLength += srcBuffer.length;
	}
	

	//The following two methods handle silences/speech (called by Analyzer class)
	// discovered in the input data.
	//
	// If you need a different behaviour, override.
	//

	/** 
	 * By default simply writes the buffer to the file.
	 * @param	buffer	The buffer containing the audio data.
	 * @param	justChanged	Indicates whether audio has just been triggered
	 * after a bout of silence.
	 */
	public void audioTriggered(short[] buffer, boolean justChanged) {
		file.write(buffer);
		
		totalAudioLength += buffer.length;
		if (this.type == 0 &&
				Math.round((double) totalAudioLength / sampleRate) < Recording.SAMPLE_SEC) // 15sec sample
			sampleFile.write(buffer);
	}

	
	public long getTotalAudioLength() {
		return totalAudioLength;
	}

	public void setTotalAudioLength(long totalAudioLength) {
		this.totalAudioLength = totalAudioLength;
	}
	
	

	public PCMWriter getFile() {
		return file;
	}

	public void setFile(PCMWriter file) {
		this.file = file;
	}

	/**
	 * Does nothing by default if silence is triggered.
	 *
	 * @param	buffer	The buffer containing the audio data.
	 * @param	justChanged	Indicates whether silence has just been triggered
	 * after a bout of audio.
	 */
	public void silenceTriggered(short[] buffer, boolean justChanged) {
		// Intentionally empty.
	}
	
	/** type of recorder (0: original, 1: respeaking) */
	private int type;
	
	/** File to write to. */
	private PCMWriter file;
	
	private PCMWriter sampleFile;

	/** Buffer to keep audio data temporarily **/
	private short[] audioBuffer;
	
	private int audioBufLength;
	
	private long totalAudioLength;
	private long sampleRate;
	
	/** Microphone input */
	private Microphone microphone;

	/** Analyzer that analyzes the incoming data. */
	private Analyzer analyzer;

	/* * Facilitates beeping when recording starts and stops.*/
	//private Beeper beeper;
}
