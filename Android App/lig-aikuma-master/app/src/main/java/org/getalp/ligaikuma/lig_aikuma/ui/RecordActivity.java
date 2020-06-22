package org.getalp.ligaikuma.lig_aikuma.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.getalp.ligaikuma.lig_aikuma.Aikuma;
import org.getalp.ligaikuma.lig_aikuma.MainActivity;
import org.getalp.ligaikuma.lig_aikuma.ModeSelection;
import org.getalp.ligaikuma.lig_aikuma.audio.Beeper;
import org.getalp.ligaikuma.lig_aikuma.audio.record.Microphone.MicException;
import org.getalp.ligaikuma.lig_aikuma.audio.record.Recorder;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;
import org.getalp.ligaikuma.lig_aikuma.model.Language;
import org.getalp.ligaikuma.lig_aikuma.model.Recording;
import org.getalp.ligaikuma.lig_aikuma.model.RecordingLig;
import org.getalp.ligaikuma.lig_aikuma.ui.sensors.ProximityDetector;
import org.getalp.ligaikuma.lig_aikuma.util.AikumaSettings;
import org.getalp.ligaikuma.lig_aikuma.util.FileIO;
import org.getalp.ligaikuma.lig_aikuma.util.PDFBuilder;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import static org.getalp.ligaikuma.lig_aikuma.util.FileIO.getNoSyncPath;

public class RecordActivity extends AikumaActivity
{

	public static final String intent_sourceId = "sourceId";
	public static final String intent_ownerId = "ownerId";
	public static final String intent_versionName = "versionName";
	public static final String intent_sampleRate = "sampleRate";
	public static final String intent_rewindAmount = "rewindAmount";
	public static final String intent_recordname = "recordname";
	static final String TAG = "RecordActivityLig";

	private boolean respeak = false;

	private Language recordLang;
	private Language motherTong;
	private ArrayList<Language> selectedLanguages = new ArrayList<>();
	private String regionOrigin;
	private String speakerName;
	private String speakerNote;
	private int speakerAge=0;
	private int speakerGender=0;
	private Date date;


	protected boolean recording;
	protected Recorder recorder;
	protected UUID soundUUID;
	protected long sampleRate = 16000L;
	protected TextView timeDisplay;
	protected ProximityDetector proximityDetector;
	protected Beeper beeper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		respeak = getIntent().getBooleanExtra(RespeakingFileExplorer.RESPEAK, false);
		// retrieve metadata from the intent
		Bundle bundle = getIntent().getBundleExtra(RecordingMetadata.metaBundle);
		recordLang = bundle.getParcelable(RecordingMetadata.metaRecordLang);
		motherTong = bundle.getParcelable(RecordingMetadata.metaMotherTong);
		selectedLanguages = bundle.getParcelableArrayList(RecordingMetadata.metaLanguages);
		regionOrigin = bundle.getString(RecordingMetadata.metaOrigin);
		speakerName = bundle.getString(RecordingMetadata.metaSpkrName);
		speakerNote = bundle.getString(RecordingMetadata.metaSpkrNote);
		speakerAge = bundle.getInt(RecordingMetadata.metaSpkrBirthYr, 0);
		speakerGender = bundle.getInt(RecordingMetadata.metaSpkrGender, 0);
		try {
			date = new SimpleDateFormat().parse(bundle.getString(RecordingMetadata.metaDate));
		} catch (ParseException e1) {
			date = new Date();
		} catch (Exception e) {
			if(BuildConfig.DEBUG)Log.e(TAG, "Exceptopn caught: " + e);
		}

		// DEBUG
		if(BuildConfig.DEBUG){
			Log.i(TAG, "start intent - selected languages: " + selectedLanguages.toString());
			Log.i(TAG, "start intent - region of origin: " + regionOrigin);
			Log.i(TAG, "start intent - speaker name: " + speakerName);
			Log.i(TAG, "start intent - speaker age: " + speakerAge);
			Log.i(TAG, "start intent - speaker gender: " + speakerGender);
			Log.i(TAG, "start intent - date: " + date);}

		// code from inherited class RecordActivity
		super.onCreate(savedInstanceState);
		setContentView(R.layout.record_activity_lig);
		//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		soundUUID = UUID.randomUUID();
		// Disable the stopButton(saveButton) before the recording starts
		ImageButton stopButton = (ImageButton) findViewById(R.id.stopButton);
		stopButton.setImageResource(R.drawable.ok_disabled_48);
		stopButton.setEnabled(false);

		// Set up the Beeper that will make beeps when recording starts and
		// pauses
		beeper = new Beeper(this, new MediaPlayer.OnCompletionListener() {
			public void onCompletion(MediaPlayer _v) {
				if(BuildConfig.DEBUG)Log.i("RecordActivity", "in onCompletion, recording:" + recording);
				if (recording) {
					recorder.listen();
					findViewById(R.id.recordButton).setVisibility(View.GONE);
					findViewById(R.id.pauseButton).setVisibility(View.VISIBLE);
				}
			}
		});

		try {
			File f = new File(Recording.getNoSyncRecordingsPath(), soundUUID.toString() + ".wav");
			recorder = new Recorder(0, f, sampleRate);
		} catch (MicException e) {
			this.finish();
			Toast.makeText(getApplicationContext(), R.string.error_setting_up_microphone, Toast.LENGTH_LONG).show();
		}
		timeDisplay = (TextView) findViewById(R.id.timeDisplay);
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					timeDisplay.post(new Runnable() {
						public void run() {
							int time = recorder.getCurrentMsec();
							//Lets method in superclass know to ask user if
							//they are willing to discard audio if time>250msec
							if(time > 250) {
								safeActivityTransition = true;
								safeActivityTransitionMessage = getString(R.string.discard_udio);
							}
							timeDisplay.setText(Float.toString(time/1000f) + "s");
						}
					});
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		}).start();

		// Set orientation: portrait on phone and landscap on tab
		setRequestedOrientation((getResources().getBoolean(R.bool.is_small_screen))?
				ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
				ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	}

	public void onStopButton(View view) {
		try {
			recorder.stop();
		} catch (MicException e) {
			// Maybe make a recording metadata file that refers to the error so
			// that the audio can be salvaged.
		}

		int duration = recorder.getCurrentMsec();
		ArrayList<String> speakerIds = new ArrayList<>();
		String name = new SimpleDateFormat("yyMMdd-HHmmss").format(date) + "_" + recordLang.getCode() + "_" +  Aikuma.getDeviceId();

		if(BuildConfig.DEBUG)Log.d("new_name", name);

		String gender="ERROR";
		switch (speakerGender)
		{
			case RecordingMetadata.GENDER_MALE:			gender = "Male";		break;
			case RecordingMetadata.GENDER_FEMALE:		gender = "Female";		break;
			case RecordingMetadata.GENDER_UNSPECIFIED:	gender = "Unspecified";	break;
		}

		RecordingLig recording = new RecordingLig(soundUUID, name, date,
				AikumaSettings.getLatestVersion(),
				AikumaSettings.getCurrentUserId(), recordLang, motherTong,
				selectedLanguages, speakerIds, Aikuma.getDeviceName(), Aikuma.getAndroidID(),
				null, null, sampleRate, duration,
				recorder.getFormat(), recorder.getNumChannels(),
				recorder.getBitsPerSample(), MainActivity.locationDetector.getLatitude(), MainActivity.locationDetector.getLongitude(),
				regionOrigin, speakerName, speakerAge, gender, speakerNote);

		try {
			// Move the wave file from the nosync directory to
			// the synced directory and write the metadata
			recording.write();
		} catch (IOException e) {
			Toast.makeText(RecordActivity.this, getString(R.string.failed_to_write_the_recording_metadata) + e.getMessage(), Toast.LENGTH_LONG).show();
			Intent intent = new Intent(RecordActivity.this, (!respeak)? ModeSelection.class: RespeakingFileExplorer.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return;
		}

		Intent intent;
		if (!respeak) {

			PDFBuilder.moveAndRenameFile(getNoSyncPath()+"/tmp_form_consent.pdf",
					FileIO.getOwnerPath()+"/recordings/"+name+"/form_consent.pdf");


			intent = new Intent(this, ModeSelection.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			Toast.makeText(RecordActivity.this,
					getString(R.string.file_saved)+ recording.getNameAndLang() +" | "+new SimpleDateFormat("yyyy-MM-dd").format(recording.getDate())+" ("+(duration / 1000)+")"
					,Toast.LENGTH_LONG).show();
		} else {
			// TODO allow for the second mode of respeaking, phone near to ear?
			intent = new Intent(this, RespeakingMetadata.class);
			SharedPreferences preferences =	PreferenceManager.getDefaultSharedPreferences(this);
			int rewind = preferences.getInt("respeaking_rewind", 500);
			if(BuildConfig.DEBUG)Log.i(TAG, "respeakingMode: respeaking_mode, rewindAmount: " + rewind);
			intent.putExtra(intent_sourceId, recording.getId());
			intent.putExtra(intent_ownerId, AikumaSettings.getCurrentUserId());
			intent.putExtra(intent_versionName, AikumaSettings.getLatestVersion());
			intent.putExtra(intent_sampleRate, sampleRate);
			intent.putExtra(intent_rewindAmount, rewind);
			intent.putExtra(intent_recordname, name);
			Toast.makeText(RecordActivity.this,
					getString(R.string.please_fill_in_the_details_related_of_the_respeaking_speaker)
							+ recording.getNameAndLang(), Toast.LENGTH_LONG).show();
		}
		startActivity(intent);
		this.finish();
	}


	/**
	 * Called when the record button is pressed - starts recording.
	 *
	 * @param	view	The record button
	 */
	public void onRecordButton(View view) {
		record();
	}

	/**
	 * Called when the pause button is pressed - pauses recording.
	 *
	 * @param	view	The pause button
	 */
	public void onPauseButton(View view) {
		pause();
	}

	// Activates recording
	private void record() {
		if(!recording) {
			recording = true;
			ImageButton recordButton = (ImageButton) findViewById(R.id.recordButton);
			recordButton.setEnabled(false);
			beeper.beepBeep();
		}
	}

	// Pauses the recording.
	private void pause() {
		if (recording) {
			recording = false;
			ImageButton recordButton = (ImageButton) findViewById(R.id.recordButton),
					pauseButton = (ImageButton) findViewById(R.id.pauseButton),
					stopButton = (ImageButton) findViewById(R.id.stopButton);
			recordButton.setEnabled(true);
			stopButton.setImageResource(R.drawable.ok_48);
			stopButton.setEnabled(true);

			recordButton.setVisibility(View.VISIBLE);
			pauseButton.setVisibility(View.GONE);
			try {
				recorder.pause();
				Beeper.beep(this, null);
			} catch (MicException e) {
				// Maybe make a recording metadata file that refers to the error so
				// that the audio can be salvaged.
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(recorder == null)	return;
		recorder.release();
	}

	@Override
	public void onPause() {
		super.onPause();
		pause();

		if(AikumaSettings.isProximityOn)
			this.proximityDetector.stop();
	}

	@Override
	public void onResume() {
		super.onResume();

		if (AikumaSettings.isProximityOn) {
			this.proximityDetector = new ProximityDetector(this) {
				public void near(float distance) {
					WindowManager.LayoutParams params = getWindow().getAttributes();
					params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
					params.screenBrightness = 0;
					getWindow().setAttributes(params);
					//record();
				}

				public void far(float distance) {
					WindowManager.LayoutParams params = getWindow().getAttributes();
					params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
					params.screenBrightness = 1;
					getWindow().setAttributes(params);
					//pause();
				}
			};
			this.proximityDetector.start();
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		return !(AikumaSettings.isProximityOn && proximityDetector.isNear()) && super.dispatchTouchEvent(event);
	}

	public void onBackPressed(View v) {
		this.finish();
	}
}
