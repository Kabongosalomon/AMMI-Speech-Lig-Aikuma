package org.getalp.ligaikuma.lig_aikuma.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.semantive.waveformandroid.waveform.MediaPlayerFactory;

import org.getalp.ligaikuma.lig_aikuma.Aikuma;
import org.getalp.ligaikuma.lig_aikuma.ModeSelection;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;
import org.getalp.ligaikuma.lig_aikuma.model.Language;
import org.getalp.ligaikuma.lig_aikuma.model.MetadataSession;
import org.getalp.ligaikuma.lig_aikuma.model.RecordingLig;
import org.getalp.ligaikuma.lig_aikuma.util.FileIO;
import org.getalp.ligaikuma.lig_aikuma.util.PDFBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static org.getalp.ligaikuma.lig_aikuma.ui.RecordingMetadata.PDF_CALL;
import static org.getalp.ligaikuma.lig_aikuma.util.FileIO.getNoSyncPath;

public class RespeakingMetadata extends AikumaActivity implements OnClickListener
{
	public static final String TAG = "RespeakingMetadata";
	public static final String metabundle = "RespeakingMetadataBundle";

	
	private Language recordLang;
	private Language motherTong;
	private ArrayList<Language> languages;
	private String regionOrigin;
	private String speakerName;
	private String speakerNote;
	private int speakerAge = 0;
	private String speakerGender;

	private long recordSampleRate;
	private int rewindAmount;
	private String OrigDirName;
	private String origRecName;

	public boolean _wait = false;

	/**
	 * if translate mode has been selected in ModeSelection.java or not.
	 * In other words, if it's a respeaking or a translation
	 */
	public static boolean translateMode = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.respeaking_metadata);
	    origRecName = getIntent().getStringExtra(RecordActivity.intent_recordname);
	    OrigDirName = getIntent().getStringExtra("dirname");
	    translateMode = getIntent().getBooleanExtra(ModeSelection.TRANSLATE_MODE, false);
		try {
			JSONObject metaJSON = FileIO.readJSONObject(new File(FileIO.getOwnerPath(), RecordingLig.RECORDINGS+OrigDirName+"/"+origRecName+RecordingLig.METADATA_SUFFIX));
			recordSampleRate = 16000L;

			// fill in the recording details about languages
			String code = (String) metaJSON.get(RecordingMetadata.metaRecordLang);
            ((TextView) findViewById(R.id.record_edit_recording_lang)).setText(new Language(Aikuma.getLanguageCodeMap().get(code), code).getName());
			code = (String) metaJSON.get(RecordingMetadata.metaMotherTong);
            ((TextView) findViewById(R.id.record_edit_mother_tongue)).setText(new Language(Aikuma.getLanguageCodeMap().get(code), code).getName());
			List<Language> languages = Language.decodeJSONArray((JSONArray) metaJSON.get("languages"));;
			if(BuildConfig.DEBUG) Log.d(TAG, "extra spoken languages: " + languages.size());
			if(languages.size() > 0)
			{
				StringBuilder s = new StringBuilder();
				if(languages.size() == 1)
					for(Language l : languages)
						s.append(l.getName());
				else
					for(Language l : languages)
						s.append(l.getName() + ";");
                ((TextView) findViewById(R.id.record_edit_extra_lang)).setText(s.toString());
			} else
                ((TextView) findViewById(R.id.record_edit_extra_lang)).setText(R.string.none);
			// fill in the recording details about speaker
            ((TextView) findViewById(R.id.record_edit_spkr_name)).setText((String) metaJSON.get(RecordingMetadata.metaSpkrName));
            ((TextView) findViewById(R.id.record_edit_spkr_age)).setText(Long.toString((Long) metaJSON.get(RecordingMetadata.metaSpkrBirthYr)));
            ((TextView) findViewById(R.id.record_edit_spkr_gender)).setText((String) metaJSON.get(RecordingMetadata.metaSpkrGender));
            ((TextView) findViewById(R.id.record_edit_spkr_region_orig)).setText((String) metaJSON.get(RecordingMetadata.metaOrigin));

            loadSession();

		} catch (IOException e) {
			if(BuildConfig.DEBUG) Log.e(TAG, "Exception caught when reading json metadata file - " + e);
			Toast.makeText(RespeakingMetadata.this, R.string.error_failed_to_load_the_recording_please_try_again, Toast.LENGTH_LONG).show();
			startActivity(new Intent(RespeakingMetadata.this, RecordingMetadata.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			RespeakingMetadata.this.finish();
		}

		// Change title for translate mode
		if(translateMode)
		{
			((TextView) findViewById(R.id.respeak_metadata_title)).setText(getResources().getString(R.string.translate_metadata));
			((TextView) findViewById(R.id.respeak_tv_langRespeaking)).setText(getResources().getString(R.string.translating_lang));
			((TextView) findViewById(R.id.textView11)).setText(getResources().getString(R.string.translate_metadata));
		}
		_wait = false;
		retrieveRecordIntent();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode == PDF_CALL)	valideGoToRecord();
		if(resultCode != RESULT_OK)	return;
		if(requestCode == RecordingMetadata.SELECT_LANG_ACT)
		{
			Language language = intent.getParcelableExtra("language");
			if(BuildConfig.DEBUG)Log.i(TAG, "textview id: " + intent.getExtras().getInt("textview_id"));
			TextView tv = (TextView) findViewById(intent.getIntExtra("textview_id", -1));
			if(tv != null)
				tv.setText(optimizeDisplay(language.getName()));
			else
				Toast.makeText(this, R.string.failed_to_retrieve_language_from_selection, Toast.LENGTH_SHORT).show();
		}
		else if(requestCode == RecordingMetadata.SELECT_SPEAKER_PROFILE)
		{
			String profileKey = intent.getStringExtra("profileKey");
			if(profileKey.equals("")) return;
			org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile sp = org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile.importFromSharedPreferences(this, profileKey);

			((TextView) findViewById(R.id.respeak_tv_selectedlangRespeaking)).setText(retrieveOriginalRecordLang().getName());
			((TextView) findViewById(R.id.respeak_tv_selectedmotherTongue)).setText(sp.getMotherTongue().getName());
			ArrayList<Language> otherLanguages = sp.getOtherLanguages();
			LinearLayout l_parent = (LinearLayout) findViewById(R.id.respeak_layout_languages);
			for(int i=0; i<otherLanguages.size(); i++)
			{
				if(i>=l_parent.getChildCount()-2)	onAddMoreLanguagesField(null);
				((TextView) ((LinearLayout) (l_parent.getChildAt(i+2))).getChildAt(1)).setText(otherLanguages.get(i).getName());
			}

			int age = sp.getBirthYear();
			((EditText) findViewById(R.id.respeak_edit_spkr_age)).setText((age==0)?"": String.valueOf(age));
			((EditText) findViewById(R.id.respeak_edit_spkr_region)).setText(sp.getRegion());
			((EditText) findViewById(R.id.respeak_edit_spkr_name)).setText(sp.getName());
			((EditText) findViewById(R.id.meta_edit_note_respeak)).setText(sp.getNote());

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

	@SuppressWarnings("ResourceType")
	public void onAddMoreLanguagesField(View _view) {
		LinearLayout l_parent = (LinearLayout) findViewById(R.id.respeak_layout_languages);
		LinearLayout l_language = (LinearLayout) l_parent.getChildAt(l_parent.getChildCount()-1);
		LinearLayout ll_more = new LinearLayout(this);
		ll_more.setId(l_language.getId() + 1000);
		ll_more.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		
		//retrieval of the style attributes
		int[] attrs = {android.R.attr.textSize, android.R.attr.textColor};
		
		TextView tv = (TextView) findViewById(R.id.respeak_tv_secondlanguage);
		TextView tv_otherlang = new TextView(this);
		tv_otherlang.setText(R.string.other_language);
		tv_otherlang.setWidth(tv.getWidth());
		tv_otherlang.setId(ll_more.getId() + 1001);
		ll_more.addView(tv_otherlang);
		tv_otherlang.setLayoutParams(new LinearLayout.LayoutParams(tv.getLayoutParams()));
		TypedArray ta = obtainStyledAttributes(R.style.respeak_TextviewMetadata2, attrs);
		String tsize = ta.getString(0);
		if(BuildConfig.DEBUG)Log.i("Retrieved text:", tsize);
		float textSize = Float.parseFloat(tsize.split("sp")[0]);
		tv_otherlang.setTextSize(textSize); 
		int tcolor = ta.getColor(1, Color.BLACK);
		String getcolorcode = Integer.toHexString(tcolor).substring(2);
		String color = "#"+getcolorcode;
		tv_otherlang.setTextColor(Color.parseColor(color));
		if(BuildConfig.DEBUG)Log.i("Set TextColor Hexacode", color);
		ta.recycle();
		tv_otherlang.setTypeface(null, Typeface.BOLD);
		//if(BuildConfig.DEBUG)Log.d(this.toString(), "" + l_language.getId());
				
		TextView tv_selected_language = new TextView(l_language.getContext());
//		tv_selected_language.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		tv_selected_language.setLayoutParams(((TextView)findViewById(R.id.respeak_tv_selectedsecondlanguage)).getLayoutParams());
		//tv_selected_language.setEms(10);
		//tv_selected_language.setTextSize(14);
		TypedArray ta_tvSelectedLang = obtainStyledAttributes(R.style.respeak_TextViewSelectedLang, attrs);
		String textSizetvSelectedLang = ta_tvSelectedLang.getString(0);
		float txtSzetv = Float.parseFloat(textSizetvSelectedLang.split("sp")[0]);
		tv_selected_language.setTextSize(txtSzetv); //set textSize of the TextView
		ta_tvSelectedLang.recycle();
		tv_selected_language.setId(ll_more.getId() + 1010);
		ll_more.addView(tv_selected_language);
		
		Button btn = (Button)findViewById(R.id.respeak_btn_secondlanguage);
		Button btn_otherlang = new Button(this);
		btn_otherlang.setLayoutParams(btn.getLayoutParams());
		btn_otherlang.setText(getResources().getString(R.string.select_from_list));
		//btn_otherlang.setTextSize(14);
		TypedArray ta_btnChooseLang = obtainStyledAttributes(R.style.ButtonChooseLanguage, attrs);
		String btn_textsize = ta_btnChooseLang.getString(0);
		if(BuildConfig.DEBUG)Log.i("SelectLanguage TextSize", btn_textsize);
		float buttontextSize = Float.parseFloat(btn_textsize.split("sp")[0]);
		btn_otherlang.setTextSize(buttontextSize); //set textSize of the button
		ta_btnChooseLang.recycle();
		btn_otherlang.setId(l_language.getId() + 1100);
		btn_otherlang.setOnClickListener(this);
		ll_more.addView(btn_otherlang);
		
		l_parent.addView(ll_more);
	}
	
	/**
	 * Called when a user press the "Less Languages" button
	 * Update the view with a suppression of the last added language
	 * 
	 * @param _view
	 */
	public void onDelLanguagesField(View _view) {
		LinearLayout l_parent = (LinearLayout) findViewById(R.id.respeak_layout_languages); //Layout parent
		int index = l_parent.getChildCount()-1; 
		if(index<3)
			Toast.makeText(this, (translateMode)?
					R.string.you_can_t_delete_the_of_the_translating_the_mother_tongue_or_the_second_language_of_the_speaker:
					R.string.you_can_t_delete_the_of_the_respeaking_the_mother_tongue_or_the_second_language_of_the_speaker,Toast.LENGTH_LONG).show();
		else
			l_parent.removeViewAt(index);
	}
	
	public void onPickLanguage(View _view) {
		Intent intent = new Intent(this, LanguageFilterList.class);
		int tv_id = ((LinearLayout) _view.getParent()).getChildAt(1).getId();
		intent.putExtra("textview_id", tv_id);
		if(BuildConfig.DEBUG)Log.i(TAG, "textview_id: " + tv_id);
		startActivityForResult(intent, RecordingMetadata.SELECT_LANG_ACT);	// org.lp20.aikuma.ui.AddSpeakerActivity2.SELECT_LANGUAGE = 0
	}
	
	
	public void onOkButtonClick(View _view) {
		if(_wait)	return;
		_wait=true;
		// get spoken languages
		recordLang = retrieveSelectedRecordLang();
		motherTong = retrieveMotherTong();
		languages = retrieveLanguages();
		// get personal information
		regionOrigin = ((EditText) findViewById(R.id.respeak_edit_spkr_region)).getText().toString();
		speakerName = ((EditText) findViewById(R.id.respeak_edit_spkr_name)).getText().toString();

		String age = ((EditText) findViewById(R.id.respeak_edit_spkr_age)).getText().toString();
		String msgError;
		try {
			if (!age.isEmpty()) {
				//speakerAge = new Integer(age);
				speakerAge = Integer.parseInt(age);
				int currentYear = Calendar.getInstance().get(Calendar.YEAR);
				if (speakerAge < RecordingMetadata.AGE_MAX || speakerAge > currentYear - RecordingMetadata.AGE_MIN) {
					msgError = this.getString(R.string.input_birth_year_is_implausible_interval) + Integer.toString(currentYear - RecordingMetadata.AGE_MAX) +
							" -> " + Integer.toString(currentYear- RecordingMetadata.AGE_MIN) + "]";
					new AlertDialog.Builder(this)
					.setMessage(msgError)
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

		RadioGroup rg = ((RadioGroup) findViewById(R.id.respeak_edit_gender));
		if (rg.getCheckedRadioButtonId() != -1) {
			String rbtn = ((RadioButton)findViewById(rg.getCheckedRadioButtonId())).getText().toString();
			if(rbtn.equals(getString(R.string.male)))
				speakerGender = "Male";
			else if(rbtn.equals(getString(R.string.female)))
				speakerGender = "Female";
			else
				speakerGender = "Unspecified";
		}

		speakerNote = ((EditText) findViewById(R.id.meta_edit_note_respeak)).getText().toString();

		String message, respeak_lang = ((TextView) findViewById(R.id.respeak_tv_selectedlangRespeaking)).getText().toString();
		
		if(respeak_lang.isEmpty() || speakerName.isEmpty()) {
			new AlertDialog.Builder(this)
			.setMessage((respeak_lang.isEmpty())?
					getString((translateMode)?R.string.please_fill_the_translating_language_field:R.string.please_fill_the_respeaking_language_field):
					getString(R.string.please_fill_the_name_of_the_speaker))
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override public void onClick(DialogInterface dialog, int which) {
					_wait=false;
				}})
			.show();
			return;
		} else if (languages.isEmpty() || regionOrigin.isEmpty() || speakerAge == 0)
			message = getString(R.string.some_fields_remain_empty_do_you_want_to_continue);
		else if (translateMode && recordLang.getName().equals(((TextView) findViewById(R.id.record_edit_recording_lang)).getText().toString()))
			message = getString(R.string.in_translation_mode_the_respeaking_language_should_be_different_from_the_original_language);
		else
			message = getString(R.string.you_are_about_to_start_recording_do_you_want_to_continue);

        int gender;
        switch (speakerGender)
        {
            case "Male":    gender = RecordingMetadata.GENDER_MALE;          break;
            case "Female":  gender = RecordingMetadata.GENDER_FEMALE;        break;
            default:        gender = RecordingMetadata.GENDER_UNSPECIFIED;   break;
        }

        org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile tsp = new org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile(speakerName, speakerAge, gender, recordLang,
                motherTong, languages, regionOrigin, speakerNote, false);
        if(org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile.profilExist(RespeakingMetadata.this, tsp))
        {
            tsp = org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile.importFromSharedPreferences(RespeakingMetadata.this, tsp.getKey());
            tsp.update(speakerName, speakerAge, gender, recordLang, motherTong, languages, regionOrigin, speakerNote);
        }
        tsp.saveOnSharedPreferences(RespeakingMetadata.this);

        // PDF Génération
        if(!tsp.getSignature())
        {
			Toast.makeText(this, R.string.wait_generation_of_consent_pdf, Toast.LENGTH_LONG).show();
            tsp.setSignature(true).saveOnSharedPreferences(RespeakingMetadata.this);
            new PDFBuilder(RespeakingMetadata.this).BuildConsentForm(getNoSyncPath()+ "/",
                    "tmp_form_consent.pdf", tsp.getName());

            new AlertDialog.Builder(this)
                    .setMessage(R.string.a_new_consent_file_was_generated_do_you_want_to_open_it)
                    .setPositiveButton(R.string.open_and_continue, new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {
                            _wait=false;
							Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(new File(getNoSyncPath()+"/tmp_form_consent.pdf")), "application/pdf");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            RespeakingMetadata.this.startActivityForResult(intent, PDF_CALL);
                        }})
                    .setNeutralButton(R.string.ignore_and_continue, new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {
							_wait=false;
							valideGoToRecord();
                        }})
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) {
                            // Remove speaker profil and PDF
							_wait=false;
							org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile.removeProfile(RespeakingMetadata.this, org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile.getLastSpeaker().getKey());
                            new File(getNoSyncPath()+"/tmp_form_consent.pdf").delete();
                        }})
					.setCancelable(false)
                    .show();
        }
        else {
			new AlertDialog.Builder(this)
					.setMessage(message)
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

	public void valideGoToRecord()
    {
        MetadataSession session = MetadataSession.getMetadataSession();
        int gender;
        switch (speakerGender)
        {
            case "Male":    gender = RecordingMetadata.GENDER_MALE;          break;
            case "Female":  gender = RecordingMetadata.GENDER_FEMALE;        break;
            default:        gender = RecordingMetadata.GENDER_UNSPECIFIED;   break;
        }

        session.setSession(recordLang, motherTong, languages, regionOrigin,
                speakerName, speakerAge, gender,speakerNote);

        if(BuildConfig.DEBUG)
        {
            Log.i(TAG, "languages: " + recordLang + "; " + motherTong + "; " + languages.toString());
            Log.i(TAG, "origin: " + regionOrigin );
            Log.i(TAG, "speaker name: " + speakerName);
            Log.i(TAG, "speaker age " + speakerAge);
            Log.i(TAG, "speaker gender " + speakerGender);
        }

        Bundle metadataBundle = new Bundle();
        metadataBundle.putParcelable(RecordingMetadata.metaRecordLang, recordLang);
        metadataBundle.putParcelable(RecordingMetadata.metaMotherTong, motherTong);
        metadataBundle.putParcelableArrayList(RecordingMetadata.metaLanguages, languages);
        metadataBundle.putString(RecordingMetadata.metaOrigin, regionOrigin);
        metadataBundle.putString(RecordingMetadata.metaSpkrName, speakerName);
        metadataBundle.putString(RecordingMetadata.metaSpkrNote, speakerNote);
        metadataBundle.putInt(RecordingMetadata.metaSpkrBirthYr, speakerAge);
        metadataBundle.putString(RecordingMetadata.metaSpkrGender, speakerGender);
        Intent intent = new Intent(RespeakingMetadata.this, ThumbRespeakActivity.class);
        intent.putExtra(metabundle, metadataBundle);
        intent.putExtra(RecordActivity.intent_recordname, origRecName);
        intent.putExtra("dirname", OrigDirName);
        intent.putExtra(RecordActivity.intent_rewindAmount, rewindAmount);
        intent.putExtra(RecordActivity.intent_sampleRate, recordSampleRate);
        intent.putExtra(ModeSelection.TRANSLATE_MODE, translateMode);
        startActivity(intent);
        finish();
    }
	
	/**
	 * Find the good language object with a given language name
	 * @param lang the language name
	 * @return the language object associated
	 */
	private Language getLanguageFromName(String lang) {
		Map<String,String> language_code_map = Aikuma.getLanguageCodeMap();
		for(String lang_code : language_code_map.keySet())
			if(language_code_map.get(lang_code).compareTo(lang) == 0)
				return new Language(lang, lang_code);
		return null;
	}
	
	/**
	 * retrieve the selected language object thanks to reading textview
	 * @return the good language object
	 */
	private Language retrieveSelectedRecordLang() {
		return getLanguageFromName((((TextView)findViewById(R.id.respeak_tv_selectedlangRespeaking)).getText().toString()).replace("\n"," "));
	}
	
	/**
	 * retrieve the original language thanks to reading original textview
	 * @return the good language object
	 */
	private Language retrieveOriginalRecordLang() {
		return getLanguageFromName((((TextView) findViewById(R.id.record_edit_recording_lang)).getText().toString()).replace("\n"," "));
	}
	
	private Language retrieveMotherTong() {
		return getLanguageFromName((((TextView)findViewById(R.id.respeak_tv_selectedmotherTongue)).getText().toString()).replace("\n"," "));
	}
	
	private ArrayList<Language> retrieveLanguages() {
		ArrayList<Language> selectedLanguages = new ArrayList<>();
		LinearLayout l_lang, l_languages = (LinearLayout) findViewById(R.id.respeak_layout_languages);
		Map<String,String> language_code_map = Aikuma.getLanguageCodeMap();
		for(int i=2; i<l_languages.getChildCount(); i++) {
			l_lang = (LinearLayout) l_languages.getChildAt(i);
			if(BuildConfig.DEBUG)Log.i(TAG, "layout id: " + l_lang.getId());
			String lang_name = (((TextView) l_lang.getChildAt(1)).getText().toString()).replace("\n"," ");
			if (!lang_name.isEmpty()) {
				if(BuildConfig.DEBUG)Log.i(TAG, "Language: " + lang_name);
				if (language_code_map.containsValue(lang_name))
					selectedLanguages.add(getLanguageFromName(lang_name));
				else {
					selectedLanguages.add(new Language(lang_name, lang_name.substring(0, 3)));
					if(BuildConfig.DEBUG)Log.i(TAG, "DEBUG - 'else if map does not contain value'; selected languages size: " + selectedLanguages.size());
				}
			}
			if(BuildConfig.DEBUG)Log.i(TAG, "DEBUG - selected languages: " + selectedLanguages.size());
		}
		return selectedLanguages;
	}

	@Override
	public void onClick(View v) {
		onPickLanguage(v);
	}
	
	public void onBackPressed(View v) {
		MediaPlayerFactory._elicit_source="";
		this.finish();
	}
	
	/**
	 * function called to load a session into activity. It takes element previously entered by user and
	 * set graphical views to his values.
	 */
	private void loadSession() {
		MetadataSession session = MetadataSession.getMetadataSession();
		Language recordLanguage = retrieveOriginalRecordLang();
        TextView tv = (TextView) findViewById(R.id.respeak_tv_selectedlangRespeaking);
        if(!translateMode)
			tv.setText(recordLanguage.getName());
		if(!session.isEmpty()) {
			if (translateMode) {
				recordLanguage = session.getRecordLanguage();
				tv.setText(recordLanguage.getName());
			}
			Language motherTongue = session.getMotherTongue();
			ArrayList<Language> extraLanguages = session.getExtraLanguages();
			String region = session.getRegionOrigin();
			int age = session.getSpeakerAge(), gender = session.getSpeakerGender();
			String name = session.getSpeakerName();
			if(BuildConfig.DEBUG)
			{
				Log.i(TAG,"load session: " + recordLanguage + "; " + motherTongue + "; " + extraLanguages);
				Log.i(TAG,"load session: " + region);
				Log.i(TAG,"load session: " + age);
				Log.i(TAG,"load session: " + gender);
				Log.i(TAG,"load session: " + name);
			}

			LinearLayout l_parent = (LinearLayout) findViewById(R.id.respeak_layout_languages);
			if(BuildConfig.DEBUG)Log.i(TAG, "# languages: " + (extraLanguages.size()+2) + "; # layouts: " + l_parent.getChildCount());
			if(extraLanguages.size() > l_parent.getChildCount()-2)
				for(int i=l_parent.getChildCount()-2; i<extraLanguages.size(); i++)
					onAddMoreLanguagesField(null);

			((TextView) findViewById(R.id.respeak_tv_selectedmotherTongue)).setText(motherTongue.getName());
            for(int i=0; i<extraLanguages.size(); i++)
				((TextView) ((LinearLayout) (l_parent.getChildAt(i+2))).getChildAt(1)).setText(extraLanguages.get(i).getName());

			((EditText) findViewById(R.id.respeak_edit_spkr_region)).setText(region);
			((EditText) findViewById(R.id.respeak_edit_spkr_name)).setText(name);
			((EditText) findViewById(R.id.respeak_edit_spkr_age)).setText((age==0)?"": String.valueOf(age));

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
			((EditText) findViewById(R.id.meta_edit_note_respeak)).setText(session.getSpeakerNote());
		}
	}


	private void retrieveRecordIntent() {
		rewindAmount = getIntent().getIntExtra(RecordActivity.intent_rewindAmount, 500);
	}

	public void onImportPressed(View prev)
	{
		startActivityForResult(new Intent(RespeakingMetadata.this, SpeakerProfile.class), RecordingMetadata.SELECT_SPEAKER_PROFILE);
	}
}
