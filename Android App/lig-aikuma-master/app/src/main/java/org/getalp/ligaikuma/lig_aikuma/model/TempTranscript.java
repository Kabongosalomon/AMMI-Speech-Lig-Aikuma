package org.getalp.ligaikuma.lig_aikuma.model;

import android.util.Log;

import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.model.Segments.Segment;
import org.getalp.ligaikuma.lig_aikuma.util.FileIO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Represents a transcript of a recording
 *
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 */
public class TempTranscript {
	private static final String TAG = "TempTranscript";
	private LinkedHashMap<Segment, TranscriptPair> transcriptMap;
	// The recording the transcript is of. Needed to obtain the sample rate.
	private Recording recording;

	/**
	 * Constructor
	 *
	 * @param	recording	The recording this is a transcript of
	 * @param	transcriptFile	The file the transcript data is contained in.
	 * @throws	IOException	If there is an issue reading the transcript file.
	 */
	public TempTranscript(Recording recording, File transcriptFile)
			throws IOException {
		this.recording = recording;
		transcriptMap = new LinkedHashMap<>();
		parseFile(transcriptFile);
	}

	/**
	 * Parses the segments from the given file into the mapping.
	 *
	 * @param	transcriptFile	The file containing the transcript
	 */
	private void parseFile(File transcriptFile) throws IOException {
		boolean firstSegment = true;
		String data = FileIO.read(transcriptFile), lines[] = data.split("\n"), splitLine[];
		for (String line : lines) {
			if (!line.startsWith(";;")) {
				splitLine = line.split("\t");
				// If it's the first segment, put a blank segment between 0l
				// samples and the start of the first segment.
				if (firstSegment) {
					firstSegment = false;
					transcriptMap.put(new Segment(0l,
							secondsToSample(Float.parseFloat(splitLine[0]))),
							new TranscriptPair("",""));
				}
				if(BuildConfig.DEBUG){Log.i(TAG, "start sample: " + secondsToSample(Float.parseFloat(splitLine[0])));
				Log.i(TAG, "end sample: " + secondsToSample(Float.parseFloat(splitLine[1])));
				Log.i(TAG, "text: " + splitLine[3]);}
				if (splitLine.length > 4)
					transcriptMap.put(new Segment(
							secondsToSample(Float.parseFloat(splitLine[0])),
							secondsToSample(Float.parseFloat(splitLine[1]))),
							new TranscriptPair(splitLine[3], splitLine[4]));
				else
					transcriptMap.put(new Segment(
							secondsToSample(Float.parseFloat(splitLine[0])),
							secondsToSample(Float.parseFloat(splitLine[1]))),
							new TranscriptPair(splitLine[3], ""));
			}
		}
	}

	/**
	 * Converts seconds to samples given the recordings sample rate.
	 */
	private long secondsToSample(float seconds) {
		return (long) (seconds * recording.getSampleRate());
	}

	public List<Segment> getSegmentList() {
		return new ArrayList<Segment>(transcriptMap.keySet());
	}

	/**
	 * Gets the TranscriptPair that corresponds to a given segment
	 *
	 * @param	segment	The segment to obtain a transcription of
	 * @return	A TranscriptPair that contains a transcript and a translation.
	 */
	public TranscriptPair getTranscriptPair(Segment segment) {
		return transcriptMap.get(segment);
	}

	/** 
	 * Gets the segment that this sample is in. If there is no segment in
	 * the sample, we should grab the first segment.
	 *
	 * @param	sample	The sample whose corresponding segment is required.
	 * @return	The segment corresponding to the input sample; null if not
	 * present
	 */
	public Segment getSegmentOfSample(long sample) {
		for (Segment segment : getSegmentList())
			if(sample >= segment.getStartSample() && sample <= segment.getEndSample())
				return segment;
		return getSegmentList().get(0);
	}

	/**
	 * Represents the text associated with a segment of the transcription,
	 * which includes a transcript and a translation.
	 */
	public class TranscriptPair {
		/** A segment of the transcript */
		public String transcript;
		/** A segment of the translation */
		public String translation;

		/**
		 * Constructor
		 *
		 * @param	transcript	The transcript segment
		 * @param	translation	The translation segment
		 */
		public TranscriptPair(String transcript, String translation) {
			this.transcript = transcript;
			this.translation = translation;
		}
	}
}
