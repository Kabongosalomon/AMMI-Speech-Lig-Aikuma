/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma.model;

import android.util.Pair;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.getalp.ligaikuma.lig_aikuma.util.FileIO;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * A class to represent the alignment between segments in an original recording
 * and a respeaking.
 *
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public class Segments {

	/**
	 * Creates an object that represents a mapping of recording segments between
	 * the original and the respeaking.
	 *
	 * @param	respeaking	the respeaking in question.
	 */
	public Segments(Recording respeaking) {
		segmentMap = new LinkedHashMap<>();
		try {
			readSegments(new File(respeaking.getRecordingsPath(), 
					respeaking.getGroupId() + "/" +
					respeaking.getId() + "-mapping.txt"));
		} catch (IOException e) {
			//Issue with reading mapping.
			throw new RuntimeException(e);
		}
	}

	/**
	 * Constructor to create an empty Segments object.
	 */
	public Segments() {
		segmentMap = new LinkedHashMap<>();
	}

	/**
	 * Gets the respeaking segment associated with the supplied original
	 * segment.
	 *
	 * @param	originalSegment	A Segment object representing a segment of the original audio.
	 * @return	A respeaking Segment object corresponding to the
	 * originalSegment
	 */
	public Segment getRespeakingSegment(Segment originalSegment) {
		return segmentMap.get(originalSegment);
	}
	
	public LinkedHashMap<Segment, Segment> getSegmentMap() {
		return segmentMap;
	}

	/**
	 * Adds a pair of segments to the Segments; analogous to Map.add().
	 *
	 * @param	originalSegment	A segment of an original recording.
	 * @param	respeakingSegment	A respeaking segment corresponding to
	 * originalSegment.
	 */
	public void put(Segment originalSegment, Segment respeakingSegment) {
		segmentMap.put(originalSegment, respeakingSegment);
	}
	
	/**
	 * Removes a pair of segments; analogous to Map.remove()
	 * 
	 * @param originalSegment	A segment of an original recording.
	 */
	public void remove(Segment originalSegment) {
		segmentMap.remove(originalSegment);
	}

	/**
	 * Returns an iterator over the segments of the original recording.
	 *
	 * @return	An iterator over the segments of the original recording.
	 */
	public Iterator<Segment> getOriginalSegmentIterator() {
		return segmentMap.keySet().iterator();
	}

	/**
	 * Reads the segments from a file.
	 *
	 * @param	path	The path to the file.
	 */
	private void readSegments(File path) throws IOException {
		segmentMap = new LinkedHashMap<>();
		for (String line : FileIO.read(path).split("\n")) {
			String[] segmentMatch = line.split(":");
			if(segmentMatch.length != 2) {
				throw new RuntimeException(
				"There must be just one colon on in a segment mapping line");
			}
			String[] originalSegment = segmentMatch[0].split(","),
					respeakingSegment = segmentMatch[1].split(",");
			segmentMap.put(new Segment(Long.parseLong(originalSegment[0]),
								Long.parseLong(originalSegment[1])),
					new Segment(Long.parseLong(respeakingSegment[0])
						, Long.parseLong(respeakingSegment[1])));
		}
	}

	/**
	 * Writes the segment mapping to file.
	 *
	 * @param	path	The path to the file.
	 * @throws	IOException	If the segments cannot be written to file.
	 */
	public void write(File path) throws IOException {
		FileIO.write(path, toString());
	}

	/**
	 * Represents a segment.
	 */
	public static class Segment implements Serializable {
		private Pair<Long, Long> pair;

		/**
		 * Creates a segment given the sample at which the segment starts, and
		 * the sample at which it ends.
		 *
		 * @param	startSample	The sample at which the segment starts.
		 * @param	endSample	The sample at which the segment ends.
		 */
		public Segment(Long startSample, Long endSample) {
			if(startSample == null)
				throw new IllegalArgumentException("Null start of sample");
			if(endSample == null)
				throw new IllegalArgumentException("Null end of sample");
			this.pair = new Pair<>(startSample, endSample);
		}

		public Long getStartSample() {
			return this.pair.first;
		}

		public Long getEndSample() {
			return this.pair.second;
		}

		/**
		 * Returns the duration of the segment in samples
		 *
		 * @return	The duration of the segment in samples.
		 */
		public Long getDuration() {
			return this.pair.second - this.pair.first;
		}

		@Override
		public String toString() {
			return getStartSample() + "," + getEndSample();
		}

		@Override
		public boolean equals(Object obj) {
			if(obj == null || obj.getClass() != getClass())
				return false;
			if(obj == this)
				return true;
			Segment rhs = (Segment) obj;
			return new EqualsBuilder()
					.append(getStartSample(), rhs.getStartSample())
					.append(getEndSample(), rhs.getEndSample())
					.isEquals();
		}

		@Override
		public int hashCode() {
			return pair.hashCode();
		}
	}

	@Override
	public String toString() {
		String mapString = "";
		Segment respeakingSegment;
		for (Segment originalSegment : segmentMap.keySet()) {
			respeakingSegment = getRespeakingSegment(originalSegment);
			mapString +=
					originalSegment.getStartSample() + "," +
					originalSegment.getEndSample() + ":" 
					+ respeakingSegment.getStartSample() + "," +
					respeakingSegment.getEndSample() + "\n";
		}
		return mapString;
	}

	private LinkedHashMap<Segment, Segment> segmentMap;
}
