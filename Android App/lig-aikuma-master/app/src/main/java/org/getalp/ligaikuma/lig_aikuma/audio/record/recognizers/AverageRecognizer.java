/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma.audio.record.recognizers;

import android.util.Log;

import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;

/** 
 *
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 */
public class AverageRecognizer extends Recognizer {

	/** The threshold below which silence is detected.*/
	protected int silenceThreshold;
	/** The threshold above which sound is detected.*/
	protected int speechThreshold;

	/**
	* Default Constructor.
	*
	*  Default silence is less than 1/32 of the maximum.
	*  Default speech is more than 1/32 of the maximum.
	*/
	public AverageRecognizer() {
		this(32768/32, 32768/32); // MediaRecorder.getAudioSourceMax();
	}

	/**
	 * Constructor.
	 * @param silenceThreshold Silence is less than 1/silenceDivisor of the maximum.
	 * @param speechThreshold  Speech is more than 1/speechDivisor of the maximum.
	 */
	public AverageRecognizer(int silenceThreshold, int speechThreshold) {
		// Silence is less than
		// 1/n of max amplitude.
		//
		this.silenceThreshold = silenceThreshold;

		// Speech is more than 1/m of max amplitude.
		//
		this.speechThreshold = speechThreshold;
	}

	@Override
	public boolean isSilence(short[] buffer) {
		int reading = getAverage(buffer);
		if(BuildConfig.DEBUG)Log.i("sound", "reading: " + reading + ", silenceThreshold: " +	silenceThreshold);
		return reading < silenceThreshold;
	}

	@Override
	public boolean isSpeech(short[] buffer) {
		int reading = getAverage(buffer);

		if(BuildConfig.DEBUG)Log.i("sound", "silent reading: " + reading + ", silenceThreshold: " +	silenceThreshold);
		return reading > speechThreshold;
	}

	private int getAverage(short[] buffer) {
		int sum = 0, amount = 0;

		for (short aBuffer : buffer)
			if (aBuffer < 0) {
				sum += aBuffer;
				amount++;
			}
		return amount == 0 ? 0 : sum / amount;
	}
}
