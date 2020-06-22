/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma.audio.record;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import org.getalp.ligaikuma.lig_aikuma.audio.Sampler;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;


/**
 * A writer that can handle PCM/WAV files.
 *
 * Process:
 *  1. It opens a file.
 *  2. It writes a header with length etc. information missing.
 *  3. When it is closed, it writes the necessary information into the header.
 *  4. It closes the file.
 *
 * Note: The file cannot be reopened.
 *
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public class PCMWriter implements Sampler {

	/**
	 * The current sample, which represents where in the recording we are.
	 */
	private long currentSample;

	public long getCurrentSample(){
		return this.currentSample;
	}
	
	public long getSampleRate() {
		return this.sampleRate;
	}

	private String fullFilename;
	/**
	 * @param	sampleRate		Eg. 1000
	 * @param	channelConfig	Eg. AudioFormat.CHANNEL_IN_MONO
	 * @param	audioFormat		Eg. AudioFormat.ENCODING_PCM_16BIT
	 *
	 * @return an instance of a PCMWriter.
	 */
	public static PCMWriter getInstance(int sampleRate, int channelConfig, int audioFormat) {
		return new PCMWriter(sampleRate, channelConfig, audioFormat);
	}

	/**
	 * The interval in which the recorded samples are output to the file.
	 *
	 *  Note: Used only in uncompressed mode.
	 */
	private static final int TIMER_INTERVAL = 120;

	/**
	 * File writer (only in uncompressed mode).
	 */
	private RandomAccessFile randomAccessWriter;

	/**
	 * Number of channels, sample rate, sample size(size in bits), buffer size,
	 * audio source, sample size (see AudioFormat).
	 */
	private short numberOfChannels;
	private int sampleRate;
	private short sampleSize;

	/**
	 * Number of bytes written to file after header (only in uncompressed mode)
	 * after stop() is called, this size is written to the header/data chunk in
	 * the wave file.
	 */
	private int payloadSize = 0;
	
	public String getFullFileName() {
		return this.fullFilename;
	}

	public int getPayloadSize() {
		return payloadSize;
	}

	public void setPayloadSize(int payloadSize) {
		this.payloadSize = payloadSize;
	}

	public void setCurrentSample(long currentSample) {
		this.currentSample = currentSample;
	}

	
	//public RandomAccessFile getRandomAccessWriter() {return randomAccessWriter;}

	/**
	 * Write the given byte buffer to the file.
	 *
	 * Note: This method remembers the size of the buffer written so far.
	 *
	 * @param	buffer	The buffer containing the audio data to be written.
	 */
	public void write(byte[] buffer) {
		try {
			// Write buffer to file.
			//
			randomAccessWriter.write(buffer);

			// Remember larger payload.
			//
			payloadSize += buffer.length;
			if(BuildConfig.DEBUG)Log.d("payLoadSize", "payLoadSize = " + payloadSize);
		} catch (IOException e) {
			if(BuildConfig.DEBUG)Log.e(PCMWriter.class.getName(), "Error occured in updateListener, recording is aborted");
		}

		this.currentSample += (sampleSize == 16)? buffer.length / 2: buffer.length;
		if(BuildConfig.DEBUG)Log.d("currentSample", "currentSample = " + this.currentSample);
	}

	/**
	 * Write the given buffer to the file.
	 *
	 * @param	buffer	The buffer containing audio data to be written.
	 */
	public void write(short[] buffer) {
		write(buffer, buffer.length);
	}
	
	/**
	 * Write the given short buffer of the range: 0 - len-1
	 * 
	 * @param buffer	The buffer containing audio data to be written
	 * @param len		non-empty audio data length
	 */
	public void write(short[] buffer, int len) {
		byte[] byteBuffer = new byte[len * 2];
		short sample;
		for(int i = 0; i < len; i++) {
			sample = buffer[i];
			byteBuffer[i * 2] = (byte) sample;
			byteBuffer[i * 2 + 1] = (byte) (sample >>> 8);
		}
		write(byteBuffer);
	}

	/**
	 * Default constructor.
	 *
	 *  @param sampleRate    Eg. 1000
	 *  @param channelConfig Eg. AudioFormat.CHANNEL_IN_MONO
	 *  @param audioFormat   Eg. AudioFormat.ENCODING_PCM_16BIT
	 */
	private PCMWriter(int sampleRate, int channelConfig, int audioFormat) {
		currentSample = 0;
		// Convert the Android attributes to internal attributes.
		//

		// Sample size.
		//
		sampleSize = (short) ((audioFormat == AudioFormat.ENCODING_PCM_16BIT)?	16:	8);

		// Channels.
		//
		numberOfChannels = (short) ((channelConfig == AudioFormat.CHANNEL_IN_MONO)?	1:	2);

		// These are needed to save the file correctly.
		//
		this.sampleRate = sampleRate;

		/*
	  Number of frames written to file on each output (only in uncompressed
	  mode)
	 */
		int bufferSize = (sampleRate * TIMER_INTERVAL / 1000) * 2 * sampleSize * numberOfChannels / 8;

		// Check to make sure buffer size is not smaller than
		// the smallest allowed size.
		//
		if(bufferSize < AudioRecord.getMinBufferSize(sampleRate,channelConfig, audioFormat))
		{
			bufferSize = AudioRecord.getMinBufferSize(sampleRate,channelConfig, audioFormat);
			if(BuildConfig.DEBUG)Log.w(PCMWriter.class.getName(), "Increasing buffer size to " + Integer.toString(bufferSize));
		}
	}
	
	/**
	 * Tries to create a RandomAccessFile.
	 *
	 */
	private void createRandomAccessFile() {
		try {
			// Random access file.
			File file = new File(this.fullFilename);
			if(BuildConfig.DEBUG)Log.i("mkdirs", " " + file.getParentFile().mkdirs() + ", on " +
			file.getParentFile());
			randomAccessWriter = new RandomAccessFile(file, "rw");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/** Prepares the writer for recording by writing the WAV file header.
	 *
	 * @param fullFilename The full path of the file to write.
	 */
	public void prepare(String fullFilename) {
		this.fullFilename = fullFilename;
		if(BuildConfig.DEBUG)Log.d("targetFileName", "targetFileName = " + fullFilename);
		try {
			Boolean exists = new File(fullFilename).exists();
			/*if (exists && fullFilename.contains("no-sync")) {
				String[] p2 = fullFilename.split(".wav");
				fullFilename = p2[0]+"(1)"+p2[1];
				if(BuildConfig.DEBUG)Log.d("new full", "targetFileName => "+fullFilename);
			}*/
				
			createRandomAccessFile();
			// Write the full WAV PCM file header.
			//
						
			// Set file length to 0, to prevent unexpected
			// behaviour in case the file already existed.
			//
			if (!exists) {
				randomAccessWriter.setLength(0);
				if(BuildConfig.DEBUG)Log.d("passage", "targetFileName passage here for " + fullFilename);
			

				// "RIFF" announcement.
				//
				randomAccessWriter.writeBytes("RIFF");
	
				// File size, 0 = unknown.
				//
				randomAccessWriter.writeInt(0);
	
				// "WAVE fmt " = WAV format.
				//
				randomAccessWriter.writeBytes("WAVE");
				randomAccessWriter.writeBytes("fmt ");
	
				// Sub-chunk size, 16 = PCM.
				//
				randomAccessWriter.writeInt(Integer.reverseBytes(16));
	
				// AudioFormat, 1 = PCM.
				//
				randomAccessWriter.writeShort(Short.reverseBytes((short) 1));
	
				// Number of channels, 1 = mono, 2 = stereo.
				//
				randomAccessWriter.writeShort(
						Short.reverseBytes(numberOfChannels));
	
				// Sample rate.
				//
				randomAccessWriter.writeInt(Integer.reverseBytes(sampleRate));
	
				// Byte rate = SampleRate * NumberOfChannels * BitsPerSample / 8.
				//
				randomAccessWriter.writeInt(Integer.reverseBytes(sampleRate
						* sampleSize * numberOfChannels / 8));
	
				// Block align = NumberOfChannels * BitsPerSample / 8.
				//
				randomAccessWriter.writeShort(Short .reverseBytes(
						(short) (numberOfChannels * sampleSize / 8)));
	
				// Bits per sample.
				//
				randomAccessWriter.writeShort(Short.reverseBytes(sampleSize));
	
				// "data" announcement.
				//
				randomAccessWriter.writeBytes("data");
	
				// Data chunk size, 0 = unknown.
				//
				randomAccessWriter.writeInt(0);
			} else {
				if(BuildConfig.DEBUG){Log.d("payLoadSize", "payLoadSize = " + getPayloadSize());
				Log.d("length", "length = " + (this.randomAccessWriter.length()-44));}
				//randomAccessWriter.seek(getPayloadSize()+44);
				randomAccessWriter.seek(this.randomAccessWriter.length()+44);
			}
		
		
			// Clear the byte array.
			//
			// Note: Removed but here for inspiration.
			//
			// buffer = new byte[framePeriod * sampleSize / 8 *
			// numberOfChannels];
			//
		} catch (IOException e) {
			//If there is an issue here, throw a RuntimeException because
			//there's not much the app can do.
			throw new RuntimeException(e);
		}
	}

	/**
	 * Finalizes the wave file.
	 *
	 *  1. Opens the file (if closed).
	 *  2. Writes the PCM sizes to the header.
	 *  3. Closes the file
	 */
	public void close() {
		// This is only necessary as the randomAccessWriter
		// might have been closed.
		//
		createRandomAccessFile();
		
		try {
			// Write size to RIFF header.
			randomAccessWriter.seek(4);
			randomAccessWriter.writeInt(Integer.reverseBytes(36 + payloadSize));

			// Write size to Sub-chunk size header.
			randomAccessWriter.seek(40);
			randomAccessWriter.writeInt(Integer.reverseBytes(payloadSize));

			randomAccessWriter.close();
		} catch (IOException e) {
			if(BuildConfig.DEBUG)Log.e(PCMWriter.class.getName(),"I/O exception occured while closing output file");
		}
	}

}
