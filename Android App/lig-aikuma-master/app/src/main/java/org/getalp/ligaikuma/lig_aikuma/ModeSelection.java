package org.getalp.ligaikuma.lig_aikuma;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.semantive.waveformandroid.waveform.MediaPlayerFactory;

import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;
import org.getalp.ligaikuma.lig_aikuma.model.Language;
import org.getalp.ligaikuma.lig_aikuma.model.RecordingLig;
import org.getalp.ligaikuma.lig_aikuma.ui.CheckMode;
import org.getalp.ligaikuma.lig_aikuma.ui.CheckTranscription;
import org.getalp.ligaikuma.lig_aikuma.ui.CheckWordVariant;
import org.getalp.ligaikuma.lig_aikuma.ui.ElicitationMode;
import org.getalp.ligaikuma.lig_aikuma.ui.ElicitationRecord;
import org.getalp.ligaikuma.lig_aikuma.ui.RecordingMetadata;
import org.getalp.ligaikuma.lig_aikuma.ui.RespeakingFileExplorer;
import org.getalp.ligaikuma.lig_aikuma.ui.ShareFile;
import org.getalp.ligaikuma.lig_aikuma.ui.ThumbRespeakActivity;
import org.getalp.ligaikuma.lig_aikuma.ui.sensors.LocationDetector;
import org.getalp.ligaikuma.lig_aikuma.util.AikumaSettings;
import org.getalp.ligaikuma.lig_aikuma.util.FileIO;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class ModeSelection extends Activity implements OnClickListener{
	
	private static final String TAG = "ModeSelection";
	public static final String TRANSLATE_MODE = "translate";
	private SharedPreferences prefsUserSession;
	private SharedPreferences.Editor ed;

	/** Called when the activity is first created. */
	@Override
	@SuppressLint("CommitPrefEdits")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mode_selection);

		// set owner id with gmail account first but replaced with a random id now
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		//AikumaSettings.setUserId(settings.getString(AikumaSettings.SETTING_OWNER_ID_KEY, null));
		if(AikumaSettings.getCurrentUserId() == null) {
			AikumaSettings.setUserId(Integer.toString(new Random().nextInt()));
			if(BuildConfig.DEBUG)Log.i(TAG, AikumaSettings.getCurrentUserId());
		}

		if(BuildConfig.DEBUG) {
			Map<String, ?> mapPrefs = settings.getAll();
			for(String key : mapPrefs.keySet())
					Log.i(TAG, "DEBUG - Shared Preferences - " + key + " -> " + mapPrefs.get(key));
		}

		Aikuma.loadLanguages(this.getApplicationContext());

		findViewById(R.id.button_mode_record).setOnClickListener(this);
		findViewById(R.id.button_mode_respeak).setOnClickListener(this);
		findViewById(R.id.mainTradBtn).setOnClickListener(this);
		findViewById(R.id.mainElicitBtn).setOnClickListener(this);
		findViewById(R.id.mainCheckBtn).setOnClickListener(this);

		// Start gathering location data
		MainActivity.locationDetector = new LocationDetector(this);

		// check the existing session and show popup to propose retrieving it
		prefsUserSession = getSharedPreferences("userSession", MODE_PRIVATE);
		ed = prefsUserSession.edit();

		File f = new File(FileIO.getAppRootPath()+"/examples");
		if(!f.exists())
		{
			f.mkdir();
			FileIO.copyFilesFromAssets(this);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			// TODO the last existing view of respeaking (summary) should allow to link back to the record/import selection view
			// TODO then adapt the respeaking interface to allow the edition of latest segments
			// TODO then adapt the summary view to allow the edition and playing of every segment
			case R.id.button_mode_respeak:
				startActivity(new Intent(ModeSelection.this, RespeakingFileExplorer.class));
				if(BuildConfig.DEBUG)Log.i(TAG, "Mode respeaking selected; view id: " + v.getId());
				break;
			case R.id.button_mode_record:
				startActivity(new Intent(ModeSelection.this, RecordingMetadata.class));
				break;
			case R.id.mainTradBtn:
				Intent intent = new Intent(ModeSelection.this, RespeakingFileExplorer.class);
				intent.putExtra(TRANSLATE_MODE, true);
				startActivity(intent);
				if(BuildConfig.DEBUG)Log.i(TAG, "Mode translation selected; view id: " + v.getId());
				break;
			case R.id.mainElicitBtn:
				startActivity(new Intent(ModeSelection.this, ElicitationMode.class));
				if(BuildConfig.DEBUG)Log.i(TAG, "Mode elicitation selected; view id: " + v.getId());
				break;
			case R.id.mainCheckBtn:
				startActivity(new Intent(ModeSelection.this, CheckMode.class));
				if(BuildConfig.DEBUG)Log.i(TAG, "Mode verification selected; view id: " + v.getId());
				break;
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		MainActivity.locationDetector.stop();
	}

	@Override
	public void onResume() {
		super.onResume();
		MainActivity.locationDetector.start();

		prefsUserSession = getSharedPreferences("userSession", MODE_PRIVATE);
		if(prefsUserSession.getBoolean("active", false))
			showSessionDialog();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		MainActivity.locationDetector.stop();
	}

	public void onShareButtonClick(View _v)
	{
		startActivity(new Intent(this, ShareFile.class));
	}

	// START DIALOG DECLARATION
	@SuppressLint("InflateParams")
	public void showSessionDialog() {
		View sessionView = getLayoutInflater().inflate(R.layout.session_dialog, null);

		setDialogDetails((LinearLayout) sessionView);

		new AlertDialog.Builder(this)
				.setView(sessionView)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override public void onClick(DialogInterface dialog, int which) {
						if(BuildConfig.DEBUG)Log.i(TAG,"yes, retrieve the session");
						retrieveSession();
					}})
				.setNegativeButton(R.string.no_but_keep_files, new DialogInterface.OnClickListener() {
					@Override public void onClick(DialogInterface dialog, int which) {
						if(BuildConfig.DEBUG)Log.i(TAG,"no don't retrieve the session but keep files");
						prefsUserSession = getSharedPreferences("userSession", MODE_PRIVATE);
						ed.clear().commit();
					}})
				.setNeutralButton(R.string.no_and_erase_files, new DialogInterface.OnClickListener() {
					@Override public void onClick(DialogInterface dialog, int which) {
						if(BuildConfig.DEBUG)Log.i(TAG,"no don't retrieve the session and erase files");
						eraseFiles();
						ed.clear().commit();
					}})
				.show();
	}

	public void retrieveSession()
	{
		String mode = prefsUserSession.getString("mode", null);
		if(BuildConfig.DEBUG)Log.d(TAG, "mode retrieval: "+mode);
		if(mode == null || mode.compareToIgnoreCase(CheckTranscription.TAG) == 0)
			startActivity(new Intent(ModeSelection.this, CheckTranscription.class));
		else if (mode.compareToIgnoreCase(CheckWordVariant.TAG) == 0)
			startActivity(new Intent(ModeSelection.this, CheckWordVariant.class));
		else if (mode.compareToIgnoreCase(ThumbRespeakActivity.TAG) == 0)
			startActivity(new Intent(ModeSelection.this, ThumbRespeakActivity.class));
		else if (mode.compareToIgnoreCase(ElicitationRecord.TAG) == 0)
			startActivity(new Intent(ModeSelection.this, ElicitationRecord.class));
		else
			Toast.makeText(this, "An error ocurred. The session could not be retrieved", Toast.LENGTH_LONG).show();
	}

	public void setDialogDetails(LinearLayout ll) {
		String mode = prefsUserSession.getString("mode", "undefined");
		if(mode.compareToIgnoreCase(CheckWordVariant.TAG) == 0 || mode.compareTo(CheckTranscription.TAG) == 0)
			((TextView) ll.findViewById(R.id.session_submode)).setText(
					(mode.compareToIgnoreCase(CheckWordVariant.TAG) == 0) ? CheckWordVariant.TAG : CheckTranscription.TAG);
		else if(mode.compareToIgnoreCase(ElicitationRecord.TAG) == 0)
		{
			((TextView) ll.findViewById(R.id.session_mode)).setText("Elicitation");
			((TextView) ll.findViewById(R.id.session_submode)).setText(prefsUserSession.getString("submode", "text").toLowerCase());
		}
		else if(mode.compareToIgnoreCase(ThumbRespeakActivity.TAG) == 0)
		{
			String submode = prefsUserSession.getString("submode", "undefined");
			if(submode.compareToIgnoreCase("Respeaking") == 0)
				((TextView) ll.findViewById(R.id.session_mode)).setText("Respeaking");
			else if(submode.compareToIgnoreCase("translation") == 0)
				((TextView) ll.findViewById(R.id.session_mode)).setText("Translation");
			((TextView) ll.findViewById(R.id.session_submode)).setText("None");
		}

		// display date in a more convenient way
		try {
			Date date = new SimpleDateFormat().parse(prefsUserSession.getString("date", "undefined"));
			// TODO parsing fails...
			((TextView) ll.findViewById(R.id.session_date)).setText(new SimpleDateFormat("dd/MM/yyyy at HH:mm",Locale.FRANCE).format(date));
		} catch (ParseException e) {
			if(BuildConfig.DEBUG)Log.e(TAG, ""+e);
			((TextView) ll.findViewById(R.id.session_date)).setText(prefsUserSession.getString("date", "undefined"));
		}

		((TextView) ll.findViewById(R.id.session_progress)).setText("" + prefsUserSession.getString("progress", "0"));

		// display only filename
		String file = new File(prefsUserSession.getString("inputFile", "undefined")).getName();
		((TextView) ll.findViewById(R.id.session_input_file)).setText(file);

		//Usefull only for Translating and Respeaking ode
		MediaPlayerFactory._currentReadFile = FileIO.getOwnerPath()+"/recordings/"+file+"/"+file+".wav";
		if(BuildConfig.DEBUG)Log.d(TAG,MediaPlayerFactory._currentReadFile);
	}

	public void eraseFiles() {
		String savedMode = prefsUserSession.getString("mode", null),
				savedSubmode = prefsUserSession.getString("submode", "undefined");
		if(BuildConfig.DEBUG){Log.d(TAG, "saved mode: "+savedMode);
			Log.d(TAG, "saved submode: "+savedSubmode);}

	    /*Check Mode*/
		if (savedMode.compareToIgnoreCase(CheckWordVariant.TAG) == 0 || savedMode.compareTo(CheckTranscription.TAG) == 0) {
			File savedFile = new File(prefsUserSession.getString("checkExportFile","undefined"));
			if(BuildConfig.DEBUG)Log.d(TAG, "saved file: "+savedFile);
			savedFile.delete();
			if(BuildConfig.DEBUG)Log.i(TAG, "saved file erased: "+savedFile);

	    /*Elicitation Mode*/
		} else if (savedMode.compareToIgnoreCase(ElicitationRecord.TAG) == 0) {
			if(BuildConfig.DEBUG)Log.d(TAG, "saved submode: "+savedSubmode);
			String idDevice = Aikuma.getDeviceId(), savedDate = prefsUserSession.getString("date", "");
			Language savedRecordLang = new Language(prefsUserSession.getString("Language name", ""),
					prefsUserSession.getString("LanguageCode", ""));
			String name = savedDate + "_" + savedRecordLang.getCode() + "_" + idDevice;
			//erase linker
			File savedTextFile = new File(FileIO.getOwnerPath()+"/recordings/"+name + "/" + name + "_linker.txt");
			if(BuildConfig.DEBUG)Log.d(TAG, "saved linker file: "+savedTextFile);
			savedTextFile.delete();
			if(BuildConfig.DEBUG)Log.i(TAG, "saved file erased: "+savedTextFile);
			//erase metadata and audio files
			int savedProgress = prefsUserSession.getInt("currentLine", 0);
			if(BuildConfig.DEBUG)Log.d(TAG, "saved progress: "+savedProgress);
			for(int i=savedProgress; i>=0; i--) {
				File savedMetadataFile = new File(FileIO.getOwnerPath()+"/recordings/"+name + "/" + name + "_" + i + RecordingLig.METADATA_SUFFIX),
						savedWavFile = new File(FileIO.getOwnerPath()+"/recordings/"+name + "/" + name + "_" + i +".wav");
				if(BuildConfig.DEBUG) {
					Log.d(TAG, "saved metadata file: " + savedMetadataFile);
					Log.d(TAG, "saved metadata file: " + savedWavFile);
				}
				savedMetadataFile.delete();
				savedWavFile.delete();
				if(BuildConfig.DEBUG) {
					Log.i(TAG, "saved metadata file erased: "+savedMetadataFile);
					Log.i(TAG, "saved audio erased: "+savedWavFile);
				}
			}

		/*Respeaking or Translating Mode*/
    		/*TODO
			 * Erase the respeaking or translating files if the
			 * user does not want to save its session
			 * nor its recordings
			 */
		}// else if (savedMode.compareToIgnoreCase(ThumbRespeakActivity.TAG) == 0) {
		//	if (savedSubmode.compareToIgnoreCase(getString(R.string.respeak)) == 0) {
				/*TODO
				 * erase the _rspk files
				 */

		//	} else if (savedSubmode.compareToIgnoreCase(getString(R.string.translation)) == 0) {

				/*TODO
				 * erase the _trsl files
				 */
		//	}
		//}

		Toast.makeText(this, R.string.session_and_files_erased, Toast.LENGTH_LONG).show();

	// END OF DIALOG DECLARATION

	

	}
	
}
