package org.getalp.ligaikuma.lig_aikuma.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.common.primitives.Ints;
import com.semantive.waveformandroid.waveform.MediaPlayerFactory;

import org.getalp.ligaikuma.lig_aikuma.Aikuma;
import org.getalp.ligaikuma.lig_aikuma.MainActivity;
import org.getalp.ligaikuma.lig_aikuma.ModeSelection;
import org.getalp.ligaikuma.lig_aikuma.audio.record.Mapper;
import org.getalp.ligaikuma.lig_aikuma.audio.record.Microphone.MicException;
import org.getalp.ligaikuma.lig_aikuma.audio.record.Recorder;
import org.getalp.ligaikuma.lig_aikuma.audio.record.ThumbRespeaker;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;
import org.getalp.ligaikuma.lig_aikuma.model.Language;
import org.getalp.ligaikuma.lig_aikuma.model.MetadataSession;
import org.getalp.ligaikuma.lig_aikuma.model.Recording;
import org.getalp.ligaikuma.lig_aikuma.model.RecordingLig;
import org.getalp.ligaikuma.lig_aikuma.util.AikumaSettings;
import org.getalp.ligaikuma.lig_aikuma.util.FileIO;
import org.getalp.ligaikuma.lig_aikuma.util.PDFBuilder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import static org.getalp.ligaikuma.lig_aikuma.util.FileIO.getNoSyncPath;


/**
 * This Activity contains one ThrumbRespeakFragment fragment.
 * @author LIG
 *
 */
public class ThumbRespeakActivity extends AikumaActivity {

	public static final String TAG = "ThumbRespeakActivity";
	private final int code = 1000;

	private SharedPreferences prefsUserSession;

	private boolean translateMode = false;
	private ThumbRespeakFragment fragment;
	private String sourceName;
	private String mode;
	private int duration;
	private String strDate;
	private String dirName;
	private String recordName;
	private int rewindAmount;
	private Mapper mapper;
	private Language recordLang;

	/**
	 * containing informations about mapping and start/stop/pause
	 * the original or respeaking recording.
	 */
	private ThumbRespeaker respeaker;
	private String sourceId;
	private UUID respeakingUUID;
	private long sampleRate;
	private boolean edited = false;

	private Boolean isNewSession;
	private RecordingLig recording;


	/** Called when the activity is first created. */
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    prefsUserSession = getSharedPreferences("userSession", MODE_PRIVATE);

		isNewSession = true;
		long curSampl, totalAudioLength;
		int payLoadSize, tmpPosRemap = -1;
		String tmpRemap = null;

		if (prefsUserSession.getBoolean("active", false)) {
	    	//retrieve selected file handled in the last session
	    	rewindAmount = prefsUserSession.getInt("rewindAmount", 500);
	    	duration = prefsUserSession.getInt("duration", 0);
	    	//retrieve result file handled in the last session
	    	sourceName = prefsUserSession.getString("inputFile", "");
	    	recordName = prefsUserSession.getString("session_output_file", "");
	    	strDate = prefsUserSession.getString("date", "");
	    	mode = prefsUserSession.getString("selected_mode", "");
	    	respeakingUUID = UUID.fromString(prefsUserSession.getString("random_uuid", ""));
	    	dirName = prefsUserSession.getString("dirname", "");
	    	payLoadSize = prefsUserSession.getInt("payLoadaSize",0);
	    	curSampl = prefsUserSession.getLong("currentPCMSample",0);

	    	String recordLangCode = prefsUserSession.getString("LanguageCode", "");
	    	String recordLangName = prefsUserSession.getString("Language name", "");
	    	recordLang = new Language(recordLangName, recordLangCode);
			sampleRate = prefsUserSession.getLong("sampleRate", 0);
			if(BuildConfig.DEBUG)Log.d(TAG, "(loeaded) respeakingUUID = "+respeakingUUID);
			mapper = new Mapper(respeakingUUID);
			totalAudioLength = prefsUserSession.getLong("totalAutioLength", 0);
	    	// clear the current stored session
	    	prefsUserSession.edit().clear().apply();
			tmpRemap = prefsUserSession.getString("remapIndex", null);
			tmpPosRemap = prefsUserSession.getInt("positionRemapIndex", -1);

	    	isNewSession = false;
			if(BuildConfig.DEBUG)Log.d("recordingUUID", "recordingUUID -> " + respeakingUUID.toString());
		//case: no session
	    } else {
		//Lets method in superclass know to ask user if they are willing to
		//discard new data on an activity transition via the menu.
			Intent intent = getIntent();
			recordName = intent.getStringExtra(RecordActivity.intent_recordname);
			if(BuildConfig.DEBUG)Log.d("recordName", "record name : "+ recordName);
			//if(BuildConfig.DEBUG)Log.d("recordFileName : ", "record file name : "+recordingFileName);
			dirName = intent.getStringExtra("dirname");
//			sourceId = (String) intent.getExtras().get("sourceId");
			if(BuildConfig.DEBUG)Log.d("dirName", "dirName : " + dirName);
			rewindAmount = intent.getExtras().getInt(RecordActivity.intent_rewindAmount);
			sampleRate = intent.getLongExtra(RecordActivity.intent_sampleRate, 16000);
			respeakingUUID = UUID.randomUUID();
			if(BuildConfig.DEBUG)Log.d(TAG, "(New) respeakingUUID = "+respeakingUUID);
			mapper = new Mapper(respeakingUUID);
			safeActivityTransition = true;
			curSampl = 0;
			payLoadSize = 0;
			translateMode = intent.getBooleanExtra(ModeSelection.TRANSLATE_MODE, false);

			sourceName = getIntent().getStringExtra(RecordActivity.intent_recordname);
			dirName = getIntent().getStringExtra("dirname");
			duration = 0;
			totalAudioLength = 0L;

			recordLang = MetadataSession.getMetadataSession().getRecordLanguage();
	    }

		String fullPath = FileIO.getOwnerPath()+"/"+RecordingLig.RECORDINGS+dirName+"/"+sourceName+".wav";
		MediaPlayerFactory._currentReadFile = fullPath;
		if(new File(fullPath).length()>=125829120)	//120MB == 125829120B
			Toast.makeText(this, R.string.warning_source_file_exceeds_120mb_potential_source_of_error, Toast.LENGTH_LONG).show();
		setContentView(R.layout.thumb_respeak);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		fragment = (ThumbRespeakFragment) getFragmentManager().findFragmentById(R.id.ThumbRespeakFragment);

	    setUpThumbRespeaker();
	    fragment.setThumbRespeaker(respeaker, isNewSession);
	    fragment.getRespeaker().getRecorder().setTotalAudioLength(totalAudioLength);
	    fragment.getRespeaker().getRecorder().getFile().setCurrentSample(curSampl);
	    fragment.getRespeaker().getRecorder().getFile().setPayloadSize(payLoadSize);
	    fragment.getRespeaker().getRecorder().getFile().setPayloadSize(payLoadSize);
		if(tmpRemap!=null)
		{
			fragment.setSerializedRemap(tmpRemap);
			fragment._currentPosRemap = tmpPosRemap;
		}
	}

	/**
	 * set the ThumbRespeaker thanks to a metadata file which
	 * contains element about segments to recording a respeaking.
	 */
	private void setUpThumbRespeaker() {

		File metadataFile = new File(FileIO.getOwnerPath(),
				RecordingLig.RECORDINGS + dirName + "/" + recordName + RecordingLig.METADATA_SUFFIX);
		try {
			recording = RecordingLig.read(metadataFile);
			sourceId = recording.getId();
			if(BuildConfig.DEBUG)Log.i(TAG, "Initiating activity - metadatafile to read: " + metadataFile.getAbsolutePath());

			try {
				respeaker = new ThumbRespeaker(recording, respeakingUUID, rewindAmount);
				respeaker.setMapper(mapper);
				respeaker.setPreviousEndSample(duration);
				respeaker.getSimplePlayer().seekToMsec(duration);
			} catch (IOException e) {
				if(BuildConfig.DEBUG)Log.e(TAG, "initiating Thumbrespeaker - IOException e: " + e);
				ThumbRespeakActivity.this.finish();
			} catch (MicException e) {
				if(BuildConfig.DEBUG)Log.e(TAG, "initiating Thumbrespeaker - MicException e: " + e);
				ThumbRespeakActivity.this.finish();
			}
		} catch (IOException e) {
			if(BuildConfig.DEBUG)Log.e(TAG, "reading file impossible - IOException e: " + e);
			ThumbRespeakActivity.this.finish();
		}
	}

	/**
	 * this function is called when we need to save current session's
	 * parameters inside a sharedPreferences
	 */
	private void saveCurrentState() {
		if(BuildConfig.DEBUG)Log.d("saveCurrentSession", "true");
		Toast.makeText(this, R.string.the_session_has_been_saved, Toast.LENGTH_SHORT).show();
		duration = respeaker.getSimplePlayer().getCurrentMsec();
		strDate = new SimpleDateFormat("yyMMdd-HHmmss").format(new Date());
		mode = translateMode ? "_trsl" : "_rspk";

		SharedPreferences.Editor ed = prefsUserSession.edit();
		ed.putBoolean("active", true); //session activated
		ed.putInt("rewindAmount", rewindAmount);

		ed.putInt("duration", duration);
		ed.putString("progress", duration/1000 + "s/" + respeaker.getSimplePlayer().getDurationMsec()/1000);
		ed.putString("inputFile", sourceName);
		ed.putString("session_output_file", recordName);
		ed.putString("date", strDate);
		ed.putString("selected_mode", mode);
		ed.putString("mode",TAG);
		ed.putString("random_uuid",respeakingUUID.toString());
		ed.putString("dirname", dirName);
		ed.putLong("sampleRate", sampleRate);
		ed.putString("LanguageCode", recordLang.getCode());
		ed.putString("Language name", recordLang.getName());

		ed.putString("submode", (translateMode)?"translation":"Respeaking");
		ed.putLong("totalAutioLength", fragment.getRespeaker().getRecorder().getTotalAudioLength());
		ed.putInt("payLoadaSize", fragment.getRespeaker().getRecorder().getFile().getPayloadSize());
		ed.putLong("currentPCMSample", fragment.getRespeaker().getRecorder().getFile().getCurrentSample());
		ed.putString("remapIndex", fragment.getSerializedRemap());
		ed.putInt("positionRemapIndex", fragment._currentPosRemap);

		ed.apply();
	}


	/**
	 * function called when we need to save json file into no sync direcory
	 */
	private void saveRecordingJsonInNoSync() {
		try {
			FileIO.writeJSONObject(new File(FileIO.getNoSyncPath(),
					this.respeakingUUID + RecordingLig.METADATA_SUFFIX),recording.encode());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Function called to restore the recordingLig by a json file.
	 */
	private void restoreMetaDataFromJson() {
		File metadataFile = new File(FileIO.getNoSyncPath(), this.respeakingUUID + RecordingLig.METADATA_SUFFIX);
		try {
			recording = RecordingLig.read(metadataFile);
			recording.setDurationMsec(respeaker.getCurrentMsec());
			recording.setRecordingUUID(respeakingUUID);
			recording.setDeviceName(Aikuma.getDeviceName());
			metadataFile.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Function called when we need to initialize the recordingLig object
	 * used to write json and wav files.
	 */
	private void setRecordingLig() {
		if(isNewSession) {
			Date date = new Date();

			int duration = respeaker.getCurrentMsec();
			Double latitude = MainActivity.locationDetector.getLatitude(), longitude = MainActivity.locationDetector.getLongitude();
            String deviceName = Aikuma.getDeviceName(), androidID = Aikuma.getAndroidID(),
                    tmode = translateMode ? "_trsl" : "_rspk",
                    name = new SimpleDateFormat("yyMMdd-HHmmss").format(date) + "_" + recordLang.getCode() + "_" +  Aikuma.getDeviceId() + tmode,
                    groupId = Recording.getGroupIdFromId(sourceId),
                    sourceVerId = recording.getVersionName() + "-" + recording.getId(),
                    speakerGender;
            Recorder recorder = respeaker.getRecorder();
			switch (MetadataSession.getMetadataSession().getSpeakerGender())
			{
				case 1: speakerGender = "Male"; 		break;
				case 2: speakerGender = "Female"; 		break;
				case 3: speakerGender = "Unspecified";	break;
				default:speakerGender = "ERROR";
			}

			PDFBuilder.moveAndRenameFile(getNoSyncPath()+"/tmp_form_consent.pdf",
					FileIO.getOwnerPath()+"/recordings/"+dirName+"/"+name+"_form_consent.pdf");

			//TODO: Changer  le nom passé en paramètre.

			recording = new RecordingLig(respeakingUUID, name, date,
					AikumaSettings.getLatestVersion(),
					AikumaSettings.getCurrentUserId(), MetadataSession.getMetadataSession().getRecordLanguage(),
					MetadataSession.getMetadataSession().getMotherTongue(),
					MetadataSession.getMetadataSession().getExtraLanguages(), new ArrayList<String>(), deviceName, androidID,
					groupId, sourceVerId,dirName, sampleRate, duration,
					recorder.getFormat(), recorder.getNumChannels(),
					recorder.getBitsPerSample(), latitude, longitude,
					MetadataSession.getMetadataSession().getRegionOrigin(), MetadataSession.getMetadataSession().getSpeakerName(),
					MetadataSession.getMetadataSession().getSpeakerAge(),
					speakerGender,MetadataSession.getMetadataSession().getSpeakerNote());
		} else {
			restoreMetaDataFromJson();
		}
	}

	/**
	 * When the save respeaking button is called, stop the activity and send
	 * the relevant data to the RecordingMetadataActivity
	 * LIG version
	 *
	 * @param	view	The save respeaking button.
	 */
	public void onSaveRespeakingButton(View view)
	{
		if(!edited)
		{
			if(fragment._toRemap&&fragment._is_speak.isChecked())
				fragment._remap.add(fragment._currentPosRemap);
			respeaker.setRemap(Ints.toArray(fragment._remap));
			respeaker.saveRespeaking();
			try {
				respeaker.stop();
			} catch (MicException e) {
				Toast.makeText(this, R.string.there_has_been_an_error_stopping_the_microphone, Toast.LENGTH_LONG).show();
			} catch (IOException e) {
				Toast.makeText(this, R.string.there_has_been_an_error_writing_the_mapping_between_original_and_respeaking_to_file, Toast.LENGTH_LONG).show();
			}
			startActivityForResult(new Intent(this, ThumbRespeakSummary.class), code);
			return;
		}

		setRecordingLig();

		try {
			// Move the wave file from the nosync directory to
			// the synced directory and write the metadata
			recording.write();
			String feedback = translateMode ? getString(R.string.translation_saved_into_the_file) : getString(R.string.respeaking_saved_into_the_file);
			Toast.makeText(ThumbRespeakActivity.this,feedback + recording.getName(),Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Toast.makeText(ThumbRespeakActivity.this,
				getString(R.string.failed_to_write_the_respoken_recording_metadata) +
				e.getMessage(), Toast.LENGTH_LONG).show();
			if(BuildConfig.DEBUG)Log.e(TAG, "failed when saving files: " + e);
		}
		Intent intent = new Intent(this, RespeakingFileExplorer.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		ThumbRespeakActivity.this.finish();
	}

	@Override
	public void onBackPressed()
    {
		new AlertDialog.Builder(this)
				.setMessage((safeActivityTransitionMessage != null)?
                        safeActivityTransitionMessage:getString(R.string.a_session_is_in_progress_do_you_want_save_the_progression))
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								fragment.getRespeaker().saveRespeaking();
								setRecordingLig();
								saveRecordingJsonInNoSync();
								saveCurrentState();
								try {
									fragment.getRespeaker().stop();
								} catch (MicException | IOException e1) {
									e1.printStackTrace();
								}

								// Close all Activity
								ThumbRespeakActivity.this.finishAffinity();
							}
						})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ThumbRespeakActivity.this.fragment.deleteLastTempFile();
						new File(FileIO.getNoSyncPath(), ThumbRespeakActivity.this.respeakingUUID + RecordingLig.MAP_EXT).delete();
						new File(FileIO.getNoSyncPath(), ThumbRespeakActivity.this.respeakingUUID + RecordingLig.METADATA_SUFFIX).delete();
						new File(FileIO.getNoSyncPath() + "/items/" + ThumbRespeakActivity.this.respeakingUUID + ".wav").delete();
                        new File(FileIO.getNoSyncPath()+ ThumbRespeakActivity.this.respeakingUUID.toString()+ "_no_speech" + RecordingLig.MAP_EXT).delete();
						if(isNewSession)
							ThumbRespeakActivity.this.finish();
						else {
							Intent intent = new Intent(ThumbRespeakActivity.this, RespeakingFileExplorer.class);
							startActivity(intent);
							finish();
						}
					}
				})
				.setNeutralButton(R.string.cancel, null)
				.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(requestCode != this.code)    return;
        if(resultCode == RESULT_OK) {
			edited = true;
			onSaveRespeakingButton(null);
		} else if(resultCode == RESULT_CANCELED) {
			Toast.makeText(this, R.string.error_when_saving_the_respeaking_please_try_again, Toast.LENGTH_LONG).show();
			finish();
		}
	}
}
