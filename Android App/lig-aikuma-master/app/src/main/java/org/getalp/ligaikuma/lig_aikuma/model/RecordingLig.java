package org.getalp.ligaikuma.lig_aikuma.model;

import android.util.Log;

import com.semantive.waveformandroid.waveform.MediaPlayerFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.getalp.ligaikuma.lig_aikuma.Aikuma;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.ui.RecordingMetadata;
import org.getalp.ligaikuma.lig_aikuma.util.FileIO;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class RecordingLig extends Recording {
	
	public static final String TAG = "RecordingLig";
	public static final String RECORDINGS = "recordings/";
	public static final String WAV_EXT = ".wav";
	public static final String MAP_EXT = ".map";
	
	private Language recordLang;
	private Language motherTong;
	private String regionOrigin;
	private String speakerName;
	private int speakerBirthYear = 0;
	private String speakerGender;
	private String speakerNote;

	/**
	 * The constructor used when we create a translation or respeaking
	 * @param recordingUUID The temporary UUID of the recording in question.
	 * 							(recording can be wav or movie(mp4))
	 * @param name 			The recording's name
	 * @param date 			The date of creation
	 * @param versionName	The recording's version (v0x)
	 * @param ownerId		The recording owner's ID(Google account)
	 * @param recordLang	The recording record language	
	 * @param motherTong	The recording mother language
	 * @param languages		The languages associated with the recording
	 * @param speakersIds	The Ids of the speakers associated with the recording
	 * @param deviceName	The model name of the device		
	 * @param androidID		The android ID of the device that created the recording
	 *   						(androidId is unique for one device)
	 * @param groupId		The ID of the group of recordings this recording
	 * @param sourceVerId	The version-ID of the source recording of this recording
	 * @param respeakingId	The original name of the file (without extension)
	 * @param sampleRate	The sample rate of the recording 
	 * @param durationMsec	The duration of the recording in milliseconds
	 * @param format		The mime type
	 * @param numChannels	The number of channels of the audio
	 * @param bitsPerSample	The bits per sample of the audio
	 * @param latitude		The location data
	 * @param longitude		The location data
	 * @param region		The region where the recording has been done
	 * @param spkrName		The name of the speaker who did the recording		
	 * @param spkrAge		The age of the speaker who did the recording
	 * @param spkrGndr		The gender of the speaker who did the recording
	 * @param spkrNote		Optionnal additionnal note
	 */
	public RecordingLig(UUID recordingUUID, String name, Date date,
			String versionName, String ownerId, Language recordLang, Language motherTong,
			List<Language> languages, List<String> speakersIds, String deviceName,
			String androidID, String groupId, String sourceVerId, String respeakingId,
			long sampleRate, int durationMsec, String format, int numChannels, 
			int bitsPerSample, Double latitude, Double longitude,
			String region, String spkrName, int spkrAge, String spkrGndr, String spkrNote) {
		super(recordingUUID, name, date, versionName, ownerId,
				languages, speakersIds, deviceName, androidID, groupId, 
				sourceVerId, respeakingId, sampleRate, durationMsec, format, numChannels, 
				bitsPerSample, latitude, longitude);
		this.regionOrigin = region;
		this.speakerName = spkrName;
		this.speakerBirthYear = spkrAge;
		this.speakerGender = spkrGndr;
		this.speakerNote = spkrNote;
		this.recordLang = (recordLang != null) ? new Language(recordLang.getName(),recordLang.getCode())
											: new Language("", "");
		this.motherTong = (motherTong != null) ? new Language(motherTong.getName(),motherTong.getCode())
											: new Language("", "");
	}
	
	public RecordingLig(UUID recordingUUID, String name, Date date,
			String versionName, String ownerId, Language recordLang, Language motherTong,
			List<Language> languages, List<String> speakersIds,
			String deviceName, String androidID, String groupId, String sourceVerId,
			long sampleRate, int durationMsec, String format, int numChannels, 
			int bitsPerSample, Double latitude, Double longitude,
			String region, String spkrName, int spkrAge, String spkrGndr, String spkrNote) {
		super(recordingUUID, name, date, versionName, ownerId,
				languages, speakersIds, deviceName, androidID, groupId, 
				sourceVerId, sampleRate, durationMsec, format, numChannels, 
				bitsPerSample, latitude, longitude);
		Log.d(TAG, "sourceVerId(2) (respeakingId) = "+sourceVerId);
		this.regionOrigin = region;
		this.speakerName = spkrName;
		this.speakerBirthYear = spkrAge;
		this.speakerGender = spkrGndr;
		this.speakerNote = spkrNote;
		this.recordLang = (recordLang != null) ? new Language(recordLang.getName(),recordLang.getCode())
											: new Language("", "");
		this.motherTong = (motherTong != null) ? new Language(motherTong.getName(),motherTong.getCode())
											: new Language("", "");
	}

	public RecordingLig(UUID recordingUUID, String name, Date date,
			String versionName, String ownerId,
			List<Language> languages, List<String> speakersIds,
			String deviceName, String androidID, String groupId, String sourceVerId,
			long sampleRate, int durationMsec, String format, int numChannels, 
			int bitsPerSample, Double latitude, Double longitude,
			String region, String spkrName, int spkrAge, String spkrGndr, String spkrNote) {
		
		super(recordingUUID, name, date, versionName, ownerId,
			languages, speakersIds, deviceName, androidID, groupId, 
			sourceVerId, sampleRate, durationMsec, format, numChannels, 
			bitsPerSample, latitude, longitude);
		this.regionOrigin = region;
		this.speakerName = spkrName;
		this.speakerBirthYear = spkrAge;
		this.speakerGender = spkrGndr;
		this.speakerNote = spkrNote;
	}
	
	
	/**
	 * Public constructor from super class Recording
	 * for existing recordings
	 * @param r the original recording
	 */
	public RecordingLig(Recording r) {
		super(r.name, r.date, r.versionName, r.ownerId, r.sourceVerId, 
				r.languages, r.speakersIds, r.androidID, r.groupId, 
				r.respeakingId, r.sampleRate, r.durationMsec, r.format, r.fileType);
	}

	
	// Moves a WAV file with a temporary UUID from a no-sync directory to
	// its rightful place in the connected world of Aikuma, with a proper name
	// and where it will find it's best friend - a JSON metadata file.
	private void importWav(UUID wavUUID, String ext) throws IOException {
		importWav(wavUUID + ".wav", ext);
	}
	
	private void importWav(String wavUUIDExt, String ext)
			throws IOException {
		File wavFile = new File(getNoSyncRecordingsPath(), wavUUIDExt);
		if(BuildConfig.DEBUG){Log.i(TAG, "importwav: " + wavFile.length());
		Log.i(TAG, "Elicitation mode? " + isElicit());}
		File destFile = new File(getIndividualRecordingPath(), this.name + ext);
		FileUtils.moveFile(wavFile, destFile);
		if(BuildConfig.DEBUG)Log.i(TAG, "src file " + wavFile.getAbsolutePath() + " moves to " + destFile.getAbsolutePath());
	}

	// Similar to importWav, except for the mapping file.
	private void importMapping(UUID wavUUID, String id)
			throws IOException {
		if(BuildConfig.DEBUG)Log.i(TAG, "importmapping - started");
		File mapFile = new File(getNoSyncRecordingsPath(), wavUUID + MAP_EXT);
		if(BuildConfig.DEBUG)Log.i(TAG, "importmapping - map file " + mapFile.getAbsolutePath() + " and exists: " + mapFile.exists());
		File destFile = new File(getIndividualRecordingPath(), this.name + MAP_EXT);
		if(BuildConfig.DEBUG)Log.i(TAG, "importmapping - dest file: " + destFile.getAbsolutePath());
		FileUtils.moveFile(mapFile, destFile);
		if(BuildConfig.DEBUG)Log.i(TAG, "mapping file " + mapFile.getAbsolutePath() + " moves to " + destFile.getAbsolutePath());
		File f = new File(FileIO.getNoSyncPath(), "tmp_no_speech"+MAP_EXT);
		if(f.exists())	FileUtils.moveFile(f, new File(getIndividualRecordingPath(), this.name + "_no_speech" + MAP_EXT));
		File f2 = new File(FileIO.getNoSyncPath(), "tmp_elan.csv");
		if(f2.exists())	FileUtils.moveFile(f2, new File(getIndividualRecordingPath(), this.name + ".csv"));
	}
	
	
	/**
	 * called when we want to save wav and json file into recording folder.
	 */
	public void write() throws IOException {
		// Ensure the directory exists
		File dir = getIndividualRecordingPath();
		dir.mkdir();
		if(BuildConfig.DEBUG)Log.i(TAG, "start write - iniate directory " + dir.getAbsolutePath());
		// Import the wave file into the new recording directory.

		if(this.isMovie())
			super.importMov(recordingUUID, getId());
		else
			this.importWav(recordingUUID, WAV_EXT);

		// if the recording is original
		if(isOriginal() || isElicit()) {
			// Import the sample wave file into the new recording directory
			File wavFile = new File(getNoSyncRecordingsPath(), recordingUUID + SAMPLE_SUFFIX);
			FileIO.delete(wavFile);
			if(BuildConfig.DEBUG)Log.d("file deleted", wavFile.getName());
		} else {
			// Try and import the mapping file
			importMapping(recordingUUID, getId());
			
			// Write the index file
			index(sourceVerId, getVersionName() + "-" + getId());
		}

		JSONObject encodedRecording = this.encode();

		// Write the json metadata.
		File metadataFile = new File(dir.toString(), this.name + METADATA_SUFFIX);

		FileIO.writeJSONObject(metadataFile,encodedRecording);
		Log.i(TAG, "Saved metadata file to " + metadataFile.getAbsolutePath());

		// To respeaking elicitation file, move it at right location
		moveRespeakedElicitationFile();
	}

	/**
	 *  Move all files at right location when respek an elicitation file.
	 *  Edit matadate file with right data
	 */
	public void moveRespeakedElicitationFile() {
		if(MediaPlayerFactory._elicit_source.isEmpty()||MediaPlayerFactory._elicit_rspk.isEmpty())	return;
		String source = FileIO.getOwnerPath()+"/recordings/"+ MediaPlayerFactory._elicit_rspk+"/",
				dest = "/"+FilenameUtils.getPath(MediaPlayerFactory._elicit_source),
				num = new File(MediaPlayerFactory._elicit_source).getName().split("_")[4].replace(".wav","");
		File f = new File(source);
		try {
			for(String s : f.list()) {
				if(s.contains(METADATA_SUFFIX)&&(s.contains("_rspk")||s.contains("_trsl")))
				{
					File tf = new File(source+s);
					String t = FileIO.read(tf);
					FileIO.delete(tf);
					FileIO.write(tf,t.replaceFirst(MediaPlayerFactory._elicit_rspk,FilenameUtils.getBaseName(MediaPlayerFactory._elicit_source)));
				}
				if(s.contains("_rspk"))
					FileUtils.moveFile(new File(source+s), new File(dest+s.replace("_rspk", "_elicit_" + num + "_rspk")));
				else if(s.contains("_trsl"))
					FileUtils.moveFile(new File(source + s), new File(dest + s.replace("_trsl", "_elicit_" + num + "_trsl")));
			}

			// Delete "temporary" files and directory
			FileUtils.deleteDirectory(f);

			// Clear memory of path
			MediaPlayerFactory._elicit_source = "";
			MediaPlayerFactory._elicit_rspk = "";
		} catch (IOException ignored) {}
	}

	/**
	 * Read a recording from the file containing JSON describing the Recording
	 * LIG version
	 *
	 * @param	metadataFile	The file containing the metadata of the recording.
	 * @return	A Recording object corresponding to the json file.
	 * @throws	IOException	If the recording metadata cannot be read.
	 */
	public static RecordingLig read(File metadataFile) throws IOException {
		JSONObject jsonObj = FileIO.readJSONObject(metadataFile);
		RecordingLig recording = new RecordingLig(read(jsonObj));
		String code = (String) jsonObj.get(RecordingMetadata.metaRecordLang);
		recording.recordLang = code.isEmpty() ? new Language("","") : new Language(Aikuma.getLanguageCodeMap().get(code), code);
		recording.languages = Language.decodeJSONArray((JSONArray) jsonObj.get("languages"));
		code = (String) jsonObj.get(RecordingMetadata.metaMotherTong);
		recording.motherTong = code.isEmpty() ? new Language("", "") : new Language(Aikuma.getLanguageCodeMap().get(code), code);
		recording.regionOrigin = (String) jsonObj.get(RecordingMetadata.metaOrigin);
		recording.speakerName = (String) jsonObj.get(RecordingMetadata.metaSpkrName);
		recording.speakerNote = (String) jsonObj.get(RecordingMetadata.metaSpkrNote);
		long age = (Long) jsonObj.get(RecordingMetadata.metaSpkrBirthYr);
		recording.speakerBirthYear= (int) age;
		recording.speakerGender = (String) jsonObj.get(RecordingMetadata.metaSpkrGender);
		Long i = (Long)jsonObj.get("BitsPerSample");
		recording.bitsPerSample = i.intValue();
		i = (Long)jsonObj.get("NumChannels");
		recording.numChannels = i.intValue();
		return recording;
	}
	
	/**
	 * Fill a json object to permit to create a metadata file containing it.
	 * 
	 * @return the encoded json object
	 */
	public JSONObject encode() {
		JSONObject encodedRecording = super.encode();
		encodedRecording.put(RecordingMetadata.metaOrigin, regionOrigin);
		encodedRecording.put(RecordingMetadata.metaSpkrName, speakerName);
		encodedRecording.put(RecordingMetadata.metaSpkrNote, speakerNote);
		encodedRecording.put(RecordingMetadata.metaSpkrBirthYr, speakerBirthYear);
		encodedRecording.put(RecordingMetadata.metaSpkrGender, speakerGender);
		encodedRecording.put(RecordingMetadata.metaRecordLang, this.recordLang.getCode());
		encodedRecording.put(RecordingMetadata.metaMotherTong, this.motherTong.getCode());
		if(BuildConfig.DEBUG)Log.i(TAG, "encoding metadata into json format");
		return encodedRecording;
	}

	/**
	 * Returns a File that refers to the actual recording file.
	 * LIG version
	 *
	 * @return	The file the recording is stored in.
	 */
	public File getFile() {
		return new File(getIndividualRecordingPath(), this.name + ((this.isMovie())? ".mp4" : ".wav"));
	}
	
	
	/**
	 * Returns true if the Recording is an elicitation;
	 *
	 * @return	True if the recording is an elicitation.
	 */
	public boolean isElicit() {
		return this.name.contains("_elicit_") && !(this.name.contains("_rspk") || this.name.contains("_trsl"));
	}
	
	
	public File getRecordingsPath() {
		File path = new File(FileIO.getOwnerPath(), RECORDINGS);
		path.mkdirs();
		if(BuildConfig.DEBUG)Log.d("recordingPath", path.toString());
		return path;
	}
	
	/**
	 * This function gives us the RecordingPath
	 * @return a recording path
	 */
	public File getIndividualRecordingPath() {
		File path = null;
		Log.d(TAG, "getIndividualRecordingPath() base path: "+this.getRespeakingId());
		String[] dir = this.getRespeakingId().split("_");
		if (this.name.contains("rspk") || this.name.contains("trsl")) {
			if(dir.length < 4) {
				if(BuildConfig.DEBUG)Log.d(TAG, "path -> "+this.getRespeakingId());
				path = new File(getRecordingsPath(), dir[0] + "_" + dir[1] + "_" + dir[2] + "/");
			} else if(dir[3].equals("elicit")) {
				if(BuildConfig.DEBUG)Log.d(TAG, "elicitation path -> "+this.getRespeakingId());
				path = new File(getRecordingsPath(), dir[0] + "_" + dir[1] + "_" + dir[2] + "_" + dir[3] + "/");
			}	
		} else if (isElicit()) {
			if(BuildConfig.DEBUG)Log.d(TAG, "elicitation path -> "+this.getRespeakingId());
			path = new File(getRecordingsPath(), dir[0] + "_" + dir[1] + "_" + dir[2] + "_" + dir[3] + "/");
		} else {
			if(BuildConfig.DEBUG)Log.d(TAG, "other -> "+this.name);
			path = new File(getRecordingsPath(), this.name + "/");
		}
		path.mkdirs();
		if(BuildConfig.DEBUG)Log.d(TAG, "destPath: "+path.toString());
		return path;
	}
}
