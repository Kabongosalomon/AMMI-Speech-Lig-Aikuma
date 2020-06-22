/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma.audio.record;

import android.util.Log;

import org.getalp.ligaikuma.lig_aikuma.audio.Sampler;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.model.Recording;
import org.getalp.ligaikuma.lig_aikuma.model.Segments;
import org.getalp.ligaikuma.lig_aikuma.util.FileIO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.UUID;

import static org.getalp.ligaikuma.lig_aikuma.model.RecordingLig.MAP_EXT;
import static org.getalp.ligaikuma.lig_aikuma.ui.RespeakingMetadata.translateMode;

/**
 * Facilitates creation of segment mappings (Segments). To be used when
 * recording a respeaking.
 *
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public class Mapper {

	public static String TAG = "Mapper";

	/** The segment mapping between the original and the respeaking. */
	private Segments segments;

	private BufferedReader reader;

	/**
	 * Temporarily store the boundaries of segments before being put in
	 * segments */
	private Long originalStartOfSegment = 0L;
	private Long originalEndOfSegment;
	private Long respeakingStartOfSegment = 0L;

	/** The mapping file */
	private File mappingFile;

	/**
	 * Constructor
	 *
	 * @param	uuid	The UUID of the respeaking.
	 */
	public Mapper(UUID uuid)
	{
		this.segments = new Segments();
		try {
			this.mappingFile = new File(Recording.getNoSyncRecordingsPath(), uuid + MAP_EXT);
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.mappingFile)));
			restoreFromMappingFile();
			if(BuildConfig.DEBUG)Log.d("mapper generation", "yes");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public File getMapFile() {
		return mappingFile;
	}
	
	public Segments getSegments() {
		return segments;
	}


	/**
	 * Stops and writes the segments to file.
	 *
	 * @throws	IOException	If the segments couldn't be written.
	 */
	public void stop() throws IOException {
		segments.write(mappingFile);
	}

	/**
	 * Gets the first sample of the original segment.
	 *
	 * @return	The start of the original segment; or 0L if there is none.
	 */
	Long getOriginalStartSample() {
		return (originalStartOfSegment != null)?	originalStartOfSegment:	0L;
	}
	
	private void restoreFromMappingFile() throws IOException {
		String line, pair[], strFirstSeg[], strSecondSeg[];
		while((line = reader.readLine()) != null && (line.isEmpty() || line.split(":").length <= 1));
		if(BuildConfig.DEBUG && line!=null)Log.d("line", line);
		do {
			pair = (line != null) ? line.split(":"):new String[0];
			strFirstSeg = pair[0].split(",");
			strSecondSeg = pair[1].split(",");
			Segments.Segment original = new Segments.Segment(Long.parseLong(strFirstSeg[0]), Long.parseLong(strFirstSeg[1]));
			if(BuildConfig.DEBUG)Log.d("original seg ", original.getStartSample() + " -> " + original.getEndSample());
			Segments.Segment rspkSeg = new Segments.Segment(Long.parseLong(strSecondSeg[0]), Long.parseLong(strSecondSeg[1]));
			if(BuildConfig.DEBUG)Log.d("rspkSeg seg ", rspkSeg.getStartSample() + " -> " + rspkSeg.getEndSample());
			segments.put(original, rspkSeg);
			if(BuildConfig.DEBUG)Log.d("segments size",  ""+segments.getSegmentMap().size());
			originalStartOfSegment = original.getEndSample();
		} while((line = reader.readLine()) != null && !line.isEmpty() && line.split(":").length > 1);
	}

	/**
	 * Marks the start of an original segment.
	 *
	 * @param	original	The source of the original segments.
	 */
	void markOriginal(Sampler original) {
		// If we have already specified an end of the segment then we're
		// starting a new one. Otherwise just continue with the old
		// originalStartOfSegment
		if(originalEndOfSegment == null) return;
		originalStartOfSegment = original.getCurrentSample();
		if(BuildConfig.DEBUG)Log.d("mark", "mark original segment : " + original.getCurrentSample());
	}

	/**
	 * Marks the end of an original segment and the start of a respeaking segment.
	 *
	 * @param	original	The source of the original segments.
	 * @param	respoken	The source of the respoken segments.
	 */
	void markRespeaking(Sampler original, Sampler respoken) {
		originalEndOfSegment = original.getCurrentSample();
		respeakingStartOfSegment = respoken.getCurrentSample();
	}
	
	/**
	 * Stores a segment determined by the samplers current locations.
	 *
	 * A segment may not be stored if there hasn't been an end to the current
	 * original segment.
	 *
	 * @param	original	The source of the original segments.
	 * @param	respoken	The source of the respoken segments.
	 * @return	Returns true if a segment gets stored; false otherwise.
	 */
	boolean store(Sampler original, Sampler respoken) {
		//If we're not respeaking and still playing an original segment, do nothing
		if (originalEndOfSegment == null) return false;
		//Otherwise lets end this respeaking segment
		Long respeakingEndOfSegment = respoken.getCurrentSample();
		//And store these two segments
		Segments.Segment originalSegment;
		try {
			originalSegment = new Segments.Segment(originalStartOfSegment, originalEndOfSegment);
		} catch (IllegalArgumentException e) {
			// This could only have happened if no original had been recorded at all.
			originalSegment = new Segments.Segment(0L, 0L);
		}
		segments.put(originalSegment, new Segments.Segment(respeakingStartOfSegment, respeakingEndOfSegment));
		//Now we say we're marking the start of the new original and respekaing
		//segments
		originalStartOfSegment = original.getCurrentSample();
		respeakingStartOfSegment = respeakingEndOfSegment;
		//We currently have no end for these segments.
		originalEndOfSegment = null;
		return true;
	}
	/** Remap an existing map file with index file, used the create a temp no speak map file
	 *
	 * @param index index used for remap
	 */
	public void remap(int[] index)
	{
		if(index.length==0)	return;
		String bufferWrite="";
		Arrays.sort(index);
		try {
			// Read original map file and remap it on buffer
			String[] v = FileIO.read(mappingFile).split("\n");
			for(int i=0,j=0;i<v.length&&j<index.length;i++)
				if(i == index[j]) {
					bufferWrite += v[i] + "\n";
					j++;
				}
			FileIO.write(new File(FileIO.getNoSyncPath(),"tmp_no_speech"+MAP_EXT), bufferWrite);
		} catch (IOException ignored) {}
	}


	/** Convert map files to csv file can be processed by softwar like ELAN
	 *
	 * @param index index used for remap
	 */
	public void toCSV(int [] index){toCSV(index, mappingFile);}

	/** Convert map files to csv file to be processed by software like ELAN
	 *
	 * @param index index used for remap
	 * @param mapFile Original map file use to create csv
	 */
	public void toCSV(int [] index, File mapFile)
	{
		String bufferWrite="", tSplit1[], tSplit2[], tNospeech, t=(translateMode)?"trsl,":"rspk,";
		float a,b,c,d;
		Arrays.sort(index);
		try {
			// Read original map file and remap it on buffer
			String[] v = FileIO.read(mapFile).split("\n");
			for(int i=0,j=0;i<v.length&&j<index.length;i++) {
				tSplit1 = v[i].split(":");
				tSplit2 = tSplit1[1].split(",");
				tSplit1 = tSplit1[0].split(",");
				a = Float.parseFloat(tSplit1[0])/16000;
				b = Float.parseFloat(tSplit1[1])/16000;
				c = Float.parseFloat(tSplit2[0])/16000;
				d = Float.parseFloat(tSplit2[1])/16000;
				if(i == index[j]){tNospeech = "non-speech";j++;}
				else tNospeech = "";
				if(i!=0)	bufferWrite+="\n";
				bufferWrite += 	"original,"+ Float.toString(a) + "," + Float.toString(b) + "," + Float.toString(b-a) + "," + tNospeech + "\n" +
								t +	Float.toString(c) + "," + Float.toString(d) + "," + Float.toString(d-c) + "," + tNospeech;
			}
			FileIO.write(new File(FileIO.getNoSyncPath(),"tmp_elan.csv"), bufferWrite);
		} catch (IOException ignored) {}
	}

	/** Not tested but use it if you want integrate it on an other soft
	 *
	 * @param mapFile Base map file
	 * @param noSpeechFile Map file used to select non-speech segments
	 * @param destinationFile Full name and path of destination file example: ./record/csv/example.csv
	 */
	public void toCSV(File mapFile, File noSpeechFile, File destinationFile)
	{
		// Get index of non-speech segment
		LinkedList<Integer> l = new LinkedList<>();
		try {
			String[] v1 = FileIO.read(mapFile).split("\n"),
					v2 = FileIO.read(noSpeechFile).split("\n");
			for(int i=0,j=0;i<v1.length&&j<v2.length;i++)
				if(v1[i].equals(v2[j]))
				{
					l.add(i);
					j++;
				}
		} catch (IOException ignored) {}

		// Cast Integer List to int Array (potentially not work)
		Object tmpidex[] = l.toArray();
		int index[] = new int[tmpidex.length], k=0;
		for(Object o : tmpidex) {index[k]=(int) o;k++;}

		// Build csv file
		String bufferWrite="", tSplit1[], tSplit2[], tNospeech, t=(translateMode)?"trsl,":"rspk,";
		float a,b,c,d;
		try {
			// Read original map file and remap it on buffer
			String[] v = FileIO.read(mapFile).split("\n");
			for(int i=0,j=0;i<v.length&&j<index.length;i++) {
				tSplit1 = v[i].split(":");
				tSplit2 = tSplit1[1].split(",");
				tSplit1 = tSplit1[0].split(",");
				a = Float.parseFloat(tSplit1[0])/16000;
				b = Float.parseFloat(tSplit1[1])/16000;
				c = Float.parseFloat(tSplit2[0])/16000;
				d = Float.parseFloat(tSplit2[1])/16000;
				if(i == index[j]){tNospeech = "non-speech";j++;}
				else tNospeech = "";
				if(i!=0)	bufferWrite+="\n";
				bufferWrite += 	"original,"+	Float.toString(a)+","+Float.toString(b)+","+Float.toString(b-a)+","+tNospeech+"\n"+
						t +		Float.toString(c)+","+Float.toString(d)+","+Float.toString(d-c)+","+tNospeech;
			}
			FileIO.write(destinationFile, bufferWrite);
		} catch (IOException ignored) {}
	}
}
