package org.getalp.ligaikuma.lig_aikuma.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.semantive.waveformandroid.waveform.MediaPlayerFactory;

import org.getalp.ligaikuma.lig_aikuma.Aikuma;
import org.getalp.ligaikuma.lig_aikuma.MainActivity;
import org.getalp.ligaikuma.lig_aikuma.audio.SimplePlayer;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;
import org.getalp.ligaikuma.lig_aikuma.model.Language;
import org.getalp.ligaikuma.lig_aikuma.model.MetadataSession;
import org.getalp.ligaikuma.lig_aikuma.model.Recording;
import org.getalp.ligaikuma.lig_aikuma.model.RecordingLig;
import org.getalp.ligaikuma.lig_aikuma.util.AikumaSettings;
import org.getalp.ligaikuma.lig_aikuma.util.PDFBuilder;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.getalp.ligaikuma.lig_aikuma.util.FileIO.getNoSyncPath;

public class RecordingMetadata extends AikumaActivity implements OnClickListener {
	
	private static final String TAG = "RecordingMetadata";
	public static final int GENDER_MALE = 1;
	public static final int GENDER_FEMALE = 2;
	public static final int GENDER_UNSPECIFIED = 3;
	public static final int SELECT_LANG_ACT = 0;
	public static final int SELECT_SPEAKER_PROFILE = 1;
	public static final int PDF_CALL = 2;

	
	public static final String metaLanguages = "languages";
	public static final String metaRecordLang = "recording_lang";
	public static final String metaMotherTong = "mother_tongue";
	public static final String metaOrigin = "origin";
	public static final String metaSpkrName = "speaker_name";
	public static final String metaSpkrBirthYr = "speaker_birth_year";
	public static final String metaSpkrGender = "speaker_gender";
	public static final String metaSpkrNote = "speaker_note";
	public static final String metaDate = "date";
	public static final String metaBundle = "metadataBundle";
	
	/**
	 * the minimal speaker age
	 */
	public static final int AGE_MIN = 3;
	
	/**
	 * the maximal speaker age
	 */
	public static final int AGE_MAX = 130;
	
	private boolean respeak = false;
	private boolean elicitation = false;
	private UUID importWavUUID;
	private int importWavRate;
	private int importWavDur;
	private String importWavFormat;
	private int importWavChannels;
	private int importBitsSample;
	private ListenFragment listenFragment;

    private Language recordLang;
	private Language motherTongue;
	private ArrayList<Language> selectedLanguages;
	private String regionOrigin;
	private String speakerName;
	private String speakerNote;
	private int speakerAge=0;
	private int speakerGender=0;
	private static boolean _wait=false;

	public static final int FRAM_ID = 5555;

	/** Called when the activity is first created. */
	@Override
	@SuppressWarnings("ResourceType")
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	
	    setContentView(R.layout.recording_metadata_lig);
//	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    
	    respeak = getIntent().getBooleanExtra(RespeakingFileExplorer.RESPEAK, false);
	    elicitation = getIntent().getBooleanExtra(ElicitationMode.ELICITATION, false);
	    if (respeak) {
	    	Toast.makeText(this, R.string.please_fill_in_the_details_of_the_imported_recording, Toast.LENGTH_LONG).show();
	    	// TODO add a frame layout for the listener
			TableLayout l_parent = (TableLayout) findViewById(R.id.title_layout);
//			LinearLayout l_parent = ((LinearLayout) findViewById(R.id.layout_spokenLanguages).getParent());
			FrameLayout playerLayout = new FrameLayout(this);
			playerLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			playerLayout.setId(FRAM_ID);
			l_parent.addView(playerLayout);
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			listenFragment = new ListenFragment();
			ft.replace(FRAM_ID, listenFragment);
			ft.commit();
			importWavUUID = UUID.fromString(getIntent().getStringExtra("tempRecUUID"));
			importWavRate = getIntent().getIntExtra("sampleRate", 16000);
			importWavDur = getIntent().getIntExtra("duration", 0);
			importWavFormat = getIntent().getStringExtra("format");
			importWavChannels = getIntent().getIntExtra("numChannels", 1);
			importBitsSample = getIntent().getIntExtra("bitspersample", 0);
	    } else if (elicitation) {
	    	Toast.makeText(this, R.string.please_fill_in_details_about_the_speaker, Toast.LENGTH_LONG).show();
	    }
	    loadSession();
		MediaPlayerFactory._elicit_source = "";
		MediaPlayerFactory._elicit_rspk = "";
		_wait=false;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (respeak) {
			try {
				listenFragment.setPlayer(new SimplePlayer(
						new File(Recording.getNoSyncRecordingsPath(), importWavUUID + ".wav"), importWavRate, true));
			} catch (IOException e) {
				//The SimplePlayer cannot be constructed, so let's end the activity.
				Toast.makeText(this, R.string.there_has_been_an_error_in_the_creation_of_the_audio_file_which_prevents_it_from_being_read, Toast.LENGTH_LONG).show();
				RecordingMetadata.this.finish();
			}
		}
	}
	
	/**
	 * Load the session if it exists
	 * 
	 */
	private void loadSession() {
		MetadataSession session = MetadataSession.getMetadataSession();
		if (!session.isEmpty()) {
			Language recordLanguage = session.getRecordLanguage(), motherTongue = session.getMotherTongue();
			ArrayList<Language> extraLanguages = session.getExtraLanguages();
			String region = session.getRegionOrigin(), name = session.getSpeakerName();
			int age = session.getSpeakerAge(), gender = session.getSpeakerGender();
			if(BuildConfig.DEBUG) {
				Log.i(TAG, "load session: " + recordLanguage + "; " + motherTongue + "; " + extraLanguages);
				Log.i(TAG, "load session: " + region);
				Log.i(TAG, "load session: " + age);
				Log.i(TAG, "load session: " + gender);
				Log.i(TAG, "load session: " + name);}

			// Set languages
			((TextView) findViewById(R.id.tv_selectedRecordingLanguage)).setText(recordLanguage.getName());
			((TextView) findViewById(R.id.tv_selectedMotherTongue)).setText(motherTongue.getName());
			LinearLayout l_parent = (LinearLayout) findViewById(R.id.meta_list_other_languages);
			for(int i=0, j=extraLanguages.size(); i<j; i++)
			{
				if(i>=l_parent.getChildCount())	onClickMoreLanguages(null);
				((TextView)((LinearLayout)l_parent.getChildAt(i)).getChildAt(1)).setText(extraLanguages.get(i).getName());
			}

			// Set Personales informations
			((EditText) findViewById(R.id.edit_region_origin)).setText(region);
			((EditText) findViewById(R.id.edit_speaker_name)).setText(name);
			if(age==0)	((EditText) findViewById(R.id.edit_speaker_age)).setText("");
			else		((EditText) findViewById(R.id.edit_speaker_age)).setText("" + age);
			switch(gender)
			{
				case RecordingMetadata.GENDER_MALE:
					((RadioButton) findViewById(R.id.radio_gender_male)).setChecked(true);
				break;case RecordingMetadata.GENDER_FEMALE:
					((RadioButton) findViewById(R.id.radio_gender_female)).setChecked(true);
				break;case RecordingMetadata.GENDER_UNSPECIFIED:
					((RadioButton) findViewById(R.id.radio_gender_unspecified)).setChecked(true);
				break;
			}

			((EditText) findViewById(R.id.meta_edit_note)).setText(session.getSpeakerNote());
		}
	}
	
	/**
	 * Called when a user press the "Select from list" button
	 * 
	 * @param _view No used
	 */
	public void onAddISOLanguageButton(View _view) {
		Intent intent = new Intent(this, LanguageFilterList.class);
		intent.putExtra("textview_id", ((LinearLayout) _view.getParent().getParent()).getChildAt(1).getId());
		startActivityForResult(intent, SELECT_LANG_ACT);
	}

	/**
	 * Called when a user press the "More Languages" button
	 * Update the view with a new field for inserting a new language
	 * 
	 * @param _view No used
	 */
	@SuppressLint("NewApi")
	@SuppressWarnings("ResourceType")
	public void onClickMoreLanguages(View _view)
	{
		// Inflate view on layout
		ViewGroup l_parent = (ViewGroup) findViewById(R.id.meta_list_other_languages);
		((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.language_view, l_parent);

		// Add button action
		((LinearLayout)((LinearLayout)l_parent.getChildAt(l_parent.getChildCount()-1)).getChildAt(2))
		.getChildAt(0).setOnClickListener(new OnClickListener() {@Override public void onClick(final View v) {onAddISOLanguageButton(v);}});

		// Add custom id on textView
		((LinearLayout)l_parent.getChildAt(l_parent.getChildCount()-1)).getChildAt(1).setId(
				(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)?
						generateUniqueViewId():
						View.generateViewId());
	}
	
	/**
	 * Called when a user press the "Less Languages" button
	 * Update the view with a suppression of the last added language
	 * 
	 * @param _view No used
	 */
	public void onClickLessLanguages(View _view) {
		LinearLayout l_parent = (LinearLayout) findViewById(R.id.meta_list_other_languages); //Layout parent
		int index = l_parent.getChildCount()-1;
		if(index<1)
			Toast.makeText(this, R.string.you_can_t_delete_the_language_of_the_recording_the_mother_tongue_or_the_second_language_of_the_speaker,Toast.LENGTH_LONG).show();
		else
			l_parent.removeViewAt(index);
	}
	
	/**
	 * Called when a user press the Ok button
	 * 
	 * @param _view No used
	 */
	public void onOkButtonPressed(View _view) {
		if(_wait)	return;
		_wait=true;
		selectedLanguages = new ArrayList<>();
		if(BuildConfig.DEBUG)Log.i(TAG, "DEBUG - selected languages before validation: " + selectedLanguages.size());
		if(BuildConfig.DEBUG)Log.i(TAG, "DEBUG - selected languages before validation: " + selectedLanguages);
		// get spoken languages
		Map<String,String> language_code_map = Aikuma.getLanguageCodeMap();

		// Get languages
		String lang = (((TextView) findViewById(R.id.tv_selectedRecordingLanguage)).getText().toString()).replace("\n"," ");
		if(language_code_map.containsValue(lang))
			recordLang = getLanguageFromName(lang, language_code_map);
		
		lang = (((TextView) findViewById(R.id.tv_selectedMotherTongue)).getText().toString()).replace("\n"," ");
		if(language_code_map.containsValue(lang))
			motherTongue = getLanguageFromName(lang, language_code_map);

		LinearLayout l_languages = (LinearLayout) findViewById(R.id.meta_list_other_languages);
		for(int i=0, j=l_languages.getChildCount(); i<j; i++)
		{
			lang = (((TextView)((LinearLayout)l_languages.getChildAt(i)).getChildAt(1)).getText().toString()).replace("\n"," ");
			Log.d(TAG, lang);
			if(!lang.isEmpty())
				selectedLanguages.add((language_code_map.containsValue(lang))?
						getLanguageFromName(lang, language_code_map):
						new Language(lang, lang.substring(0, 3)));
			if(BuildConfig.DEBUG)Log.i(TAG, "DEBUG - selected languages: " + selectedLanguages.size());
		}
		Log.d(TAG, selectedLanguages.toString());

		// Get personal information
		regionOrigin = ((EditText) findViewById(R.id.edit_region_origin)).getText().toString();
		if(regionOrigin.equals(getString(R.string.region_origin_edit)))
			regionOrigin = "";
		speakerName = ((EditText) findViewById(R.id.edit_speaker_name)).getText().toString();
		if(speakerName.equals(getString(R.string.speaker_name_edit)))
			speakerName = "";
		String age = ((EditText) findViewById(R.id.edit_speaker_age)).getText().toString();
		try {
			if(!age.isEmpty()) {
				speakerAge = Integer.parseInt(age);
				int currentYear = Calendar.getInstance().get(Calendar.YEAR);
				if(speakerAge < currentYear - RecordingMetadata.AGE_MAX ||
						speakerAge > currentYear - RecordingMetadata.AGE_MIN) {
					new AlertDialog.Builder(this)
					.setMessage(getString(R.string.input_birth_year_is_implausible_interval) + Integer.toString(currentYear - RecordingMetadata.AGE_MAX) +
							" -> " + Integer.toString(currentYear- RecordingMetadata.AGE_MIN) + "]")
					.setPositiveButton(R.string.ok, null)
					.show();
					return;
				}
			}
		} catch (NumberFormatException e) {
			new AlertDialog.Builder(this)
				.setMessage(R.string.warning_age_is_incorrect)
				.setPositiveButton(R.string.ok, null)
				.show();
			return;
		}

		RadioGroup rg = ((RadioGroup) findViewById(R.id.edit_radio_gender));
		if(rg.getCheckedRadioButtonId() != -1)
		{
			String s = ((RadioButton)findViewById(rg.getCheckedRadioButtonId())).getText().toString();
			if(getString(R.string.male).equals(s))			speakerGender = RecordingMetadata.GENDER_MALE;
			else if(getString(R.string.female).equals(s))	speakerGender = RecordingMetadata.GENDER_FEMALE;
			else											speakerGender = RecordingMetadata.GENDER_UNSPECIFIED;
		}

		speakerNote = ((EditText) findViewById(R.id.meta_edit_note)).getText().toString();
		//End of data get

		if(recordLang == null || speakerName.isEmpty()) {
			new AlertDialog.Builder(this)
				.setMessage(((recordLang == null)?
						getString(R.string.the_recording_language_is_empty_please_fill_it):
						getString(R.string.the_name_of_the_speaker_is_empty_please_fill_it)))
				.setPositiveButton(R.string.ok, null)
				.show();
			return;
		}

		// Save user profil
		org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile tsp = new org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile(speakerName, speakerAge, speakerGender,
				recordLang, motherTongue, selectedLanguages, regionOrigin, speakerNote, false);
		if(org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile.profilExist(RecordingMetadata.this, tsp))
		{
			tsp = org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile.importFromSharedPreferences(RecordingMetadata.this, tsp.getKey());
			tsp.update(speakerName, speakerAge, speakerGender, recordLang, motherTongue, selectedLanguages, regionOrigin, speakerNote);
		}
		tsp.saveOnSharedPreferences(RecordingMetadata.this);

		// If need PDF generation
		if(!tsp.getSignature())
		{
			Toast.makeText(this, R.string.wait_generation_of_consent_pdf, Toast.LENGTH_LONG).show();
			tsp.setSignature(true).saveOnSharedPreferences(RecordingMetadata.this);
			new PDFBuilder(RecordingMetadata.this).BuildConsentForm(getNoSyncPath()+"/","tmp_form_consent.pdf", tsp.getName());

			new AlertDialog.Builder(this)
					.setMessage(R.string.a_new_consent_file_was_generated_do_you_want_to_open_it)
					.setPositiveButton(R.string.open_and_continue, new DialogInterface.OnClickListener() {
						@Override public void onClick(DialogInterface dialog, int which) {
							_wait=false;
							Intent intent = new Intent(Intent.ACTION_VIEW);
							intent.setDataAndType(Uri.fromFile(new File(getNoSyncPath()+"/tmp_form_consent.pdf")), "application/pdf");
							intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
							RecordingMetadata.this.startActivityForResult(intent, PDF_CALL);
						}})
					.setNeutralButton(R.string.ignore_and_continue, new DialogInterface.OnClickListener() {
						@Override public void onClick(DialogInterface dialog, int which) {
							_wait=false;
							valideGoToRecord();
						}})
					.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
						@Override public void onClick(DialogInterface dialog, int which) {
							// Remove speaker profil and PDF
							org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile.removeProfile(RecordingMetadata.this, org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile.getLastSpeaker().getKey());
							new File(getNoSyncPath()+"/tmp_form_consent.pdf").delete();
							_wait=false;
						}})
					.setCancelable(false)
					.show();
		}
		else {
			new AlertDialog.Builder(this)
					.setMessage(((motherTongue == null || selectedLanguages.isEmpty() || regionOrigin.isEmpty()
							|| speakerGender == 0 || speakerAge == 0) ? getString(R.string.some_fields_remain_empty_do_you_want_to_continue) : getString(R.string.you_are_about_to_start_recording_do_you_want_to_continue)))
					.setPositiveButton(R.string.continue_, new DialogInterface.OnClickListener() {
						@Override public void onClick(DialogInterface dialog, int which) {
							_wait=false;
							valideGoToRecord();
						}})
					.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
						@Override public void onClick(DialogInterface dialog, int which) {
							_wait=false;
						}})
					.setCancelable(false)
					.show();
		}
	}

	/** Go out of Metadata with normal way
	 */
	private void valideGoToRecord()
	{
		MetadataSession session = MetadataSession.getMetadataSession();
		session.setSession(recordLang, motherTongue, selectedLanguages, regionOrigin, speakerName, speakerAge, speakerGender, speakerNote);
		if(BuildConfig.DEBUG){
			Log.i(TAG, "Session saved - languages: " + selectedLanguages);
			Log.i(TAG, "Session saved - origin: " + regionOrigin);
			Log.i(TAG, "Session saved - speaker name: " + speakerName);
			Log.i(TAG, "Session saved - speaker age: " + speakerAge);
			Log.i(TAG, "Session saved - speaker gender: " + speakerGender);}

		Date date = new Date();

		Intent intent;
		if (!respeak && !elicitation) {	// MODE RECORD
			// pass metadata to RecordActivityLig and start it
			Bundle metadataBundle = new Bundle();
			metadataBundle.putParcelable(metaRecordLang, recordLang);
			metadataBundle.putParcelable(metaMotherTong, motherTongue);
			metadataBundle.putParcelableArrayList(metaLanguages, selectedLanguages);
			metadataBundle.putString(metaOrigin, regionOrigin);
			metadataBundle.putString(metaSpkrName, speakerName);
			metadataBundle.putString(metaSpkrNote, speakerNote);
			metadataBundle.putInt(metaSpkrBirthYr, speakerAge);
			metadataBundle.putInt(metaSpkrGender, speakerGender);
			metadataBundle.putString(RecordingMetadata.metaDate, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(date));
			intent = new Intent(RecordingMetadata.this, RecordActivity.class);
			intent.putExtra(metaBundle, metadataBundle);
			intent.putExtra(RespeakingFileExplorer.RESPEAK, respeak);
			startActivity(intent);
		} else if (!respeak) {		// MODE ELICITATION
			intent = new Intent(RecordingMetadata.this, ElicitationRecord.class);
			int choiceMode = getIntent().getIntExtra("selectedFileType", 0);

			Bundle metadataBundle = new Bundle();
			metadataBundle.putParcelable(metaRecordLang, recordLang);
			metadataBundle.putParcelable(metaMotherTong, motherTongue);
			metadataBundle.putParcelableArrayList(metaLanguages, selectedLanguages);
			metadataBundle.putString(metaOrigin, regionOrigin);
			metadataBundle.putString(metaSpkrName, speakerName);
			metadataBundle.putString(metaSpkrNote, speakerNote);
			metadataBundle.putInt(metaSpkrBirthYr, speakerAge);
			metadataBundle.putInt(metaSpkrGender, speakerGender);
			metadataBundle.putString(RecordingMetadata.metaDate, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(date));
			intent.putExtra(metaBundle, metadataBundle);
			intent.putExtra("selectedFileType", choiceMode);
			intent.putExtra(ElicitationMode.importFileName, getIntent().getStringExtra(ElicitationMode.importFileName));
			if(BuildConfig.DEBUG)Log.i(TAG, "Selected file: " + getIntent().getStringExtra(ElicitationMode.importFileName));
			startActivity(intent);
		} else if (!elicitation){	// MODE RESPEAKING
			String gender = "";
			switch(speakerGender) {
				case RecordingMetadata.GENDER_MALE:			gender = "Male";		break;
				case RecordingMetadata.GENDER_FEMALE:		gender = "Female";		break;
				case RecordingMetadata.GENDER_UNSPECIFIED:	gender = "Unspecified";	break;
			}
			String idDevice = Aikuma.getDeviceId(),
					name = new SimpleDateFormat("yyMMdd-HHmmss").format(date) + "_" + recordLang.getCode() + "_" +  idDevice;
			RecordingLig recording = new RecordingLig(importWavUUID, name, date,
					AikumaSettings.getLatestVersion(),
					AikumaSettings.getCurrentUserId(), recordLang, motherTongue,
					selectedLanguages, new ArrayList<String>(), Aikuma.getDeviceName(),
					Aikuma.getAndroidID(), null, null, (long)importWavRate, importWavDur,
					importWavFormat, importWavChannels, importBitsSample, MainActivity.locationDetector.getLatitude(),
					MainActivity.locationDetector.getLongitude(),
					regionOrigin, speakerName, speakerAge, gender, speakerNote);
			try {
				recording.write();
				intent = new Intent(RecordingMetadata.this, RespeakingMetadata.class);
				intent.putExtra(RecordActivity.intent_recordname, name);
				intent.putExtra("dirname", name);
				intent.putExtra(RecordActivity.intent_rewindAmount, 500);
			} catch (IOException e) {
				Toast.makeText(RecordingMetadata.this,
						getString(R.string.failed_to_import_the_recording) + e.getMessage(), Toast.LENGTH_LONG).show();
				if(BuildConfig.DEBUG)Log.e(TAG, "Failed to import the recording: " + e);
				intent = new Intent(RecordingMetadata.this, RespeakingFileExplorer.class);
			}
			startActivity(intent);
		}
		RecordingMetadata.this.finish();
	}
	
	private Language getLanguageFromName(String langName, Map<String,String> language_code_map) {
		for(String lang_code : language_code_map.keySet())
			if(language_code_map.get(lang_code).equals(langName))
				return new Language(langName, lang_code);
		return null;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(requestCode == PDF_CALL)	valideGoToRecord();
		if(resultCode != RESULT_OK)	return;
		if(requestCode == SELECT_LANG_ACT) {
			Language language = intent.getParcelableExtra("language");
			if(BuildConfig.DEBUG){	Log.i(TAG, "textview id: " + intent.getExtras().getInt("textview_id"));
									Log.i(TAG, "DEBUG - language: " + language.getCode() + ": " + language.getName());}
			TextView tv = (TextView) findViewById(intent.getIntExtra("textview_id", -1));
			if(tv != null)
				tv.setText(optimizeDisplay(language.getName()));
			else
				Toast.makeText(this, R.string.failed_to_retrieve_language_from_selection, Toast.LENGTH_SHORT).show();
		}
		else if(requestCode == SELECT_SPEAKER_PROFILE)
		{
            String profileKey = intent.getStringExtra("profileKey");
            if(profileKey.equals("")) return;
            org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile sp = org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile.importFromSharedPreferences(this, profileKey);

            ((TextView) findViewById(R.id.tv_selectedRecordingLanguage)).setText(sp.getRecordLang().getName());
            ((TextView) findViewById(R.id.tv_selectedMotherTongue)).setText(sp.getMotherTongue().getName());
            ArrayList<Language> otherLanguages = sp.getOtherLanguages();
            LinearLayout l_parent = (LinearLayout) findViewById(R.id.meta_list_other_languages);
            for(int i=0; i<otherLanguages.size(); i++)
            {
				if(i>=l_parent.getChildCount())	onClickMoreLanguages(null);
				((TextView)((LinearLayout)l_parent.getChildAt(i)).getChildAt(1)).setText(otherLanguages.get(i).getName());
            }
            ((EditText) findViewById(R.id.edit_region_origin)).setText(sp.getRegion());
			((TextView) findViewById(R.id.meta_edit_note)).setText(sp.getNote());
            ((EditText) findViewById(R.id.edit_speaker_name)).setText(sp.getName());

            int age = sp.getBirthYear();
            if(age!=0)	((EditText) findViewById(R.id.edit_speaker_age)).setText(String.valueOf(age));

            switch(sp.getGender())
            {
                case RecordingMetadata.GENDER_MALE:
                    ((RadioButton) findViewById(R.id.radio_gender_male)).setChecked(true);
                break;case RecordingMetadata.GENDER_FEMALE:
                    ((RadioButton) findViewById(R.id.radio_gender_female)).setChecked(true);
                break;case RecordingMetadata.GENDER_UNSPECIFIED:
                    ((RadioButton) findViewById(R.id.radio_gender_unspecified)).setChecked(true);
                break;
            }
		}
	}

	/** Optimize language to dispay it better is possible with "\n" insertion
	 *
	 * @param text Base language
	 * @return Text to display
	 */
	public String optimizeDisplay(String text)
	{
		if(text.length()<15)	return text;
		for(int i=0;i<text.length()/15;i++)
			for(int j=15*(i+1);j>=i;j--)
				if(text.charAt(j) == ' ')
				{
					text = new StringBuilder(text).replace(j,j+1,"\n").toString();
					break;
				}
		return text;
	}

	@Override
	public void onClick(View v) {onAddISOLanguageButton(v);}
	
	public void onBackPressed(View prev) {
		this.finish();
	}

	public void onImportPressed(View prev)
	{
		startActivityForResult(new Intent(RecordingMetadata.this, SpeakerProfile.class), SELECT_SPEAKER_PROFILE);
	}

	private static final AtomicInteger viewIdGenerator = new AtomicInteger(15000000);
	private static int generateUniqueViewId() {
		int result, newValue;
		while(true)
		{
			result = viewIdGenerator.get();
			newValue = result + 1;
			if(newValue > 0x00FFFFFF) newValue = 1;
			if(viewIdGenerator.compareAndSet(result, newValue))
				return result;
		}
	}
}