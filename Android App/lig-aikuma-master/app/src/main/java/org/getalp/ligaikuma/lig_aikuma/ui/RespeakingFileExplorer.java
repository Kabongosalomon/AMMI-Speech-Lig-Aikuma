package org.getalp.ligaikuma.lig_aikuma.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.musicg.wave.Wave;
import com.semantive.waveformandroid.waveform.MediaPlayerFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.getalp.ligaikuma.lig_aikuma.Aikuma;
import org.getalp.ligaikuma.lig_aikuma.MainActivity;
import org.getalp.ligaikuma.lig_aikuma.ModeSelection;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;
import org.getalp.ligaikuma.lig_aikuma.model.Language;
import org.getalp.ligaikuma.lig_aikuma.model.Recording;
import org.getalp.ligaikuma.lig_aikuma.model.RecordingLig;
import org.getalp.ligaikuma.lig_aikuma.util.AikumaSettings;
import org.getalp.ligaikuma.lig_aikuma.util.FileIO;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.getalp.ligaikuma.lig_aikuma.model.FileModel.METADATA_SUFFIX;

public class RespeakingFileExplorer extends AikumaActivity {
	
	private static boolean translateMode=false;
	public static final String TAG = "RespeakingSelection";
	public static final String RESPEAK = "respeak";

	private static String _current_folder_position;
	private static boolean _is_init = false;
	private static RespeakingFileExplorer.FileAdapter _adapteur;
	private static ArrayList<String> _nav_list;

	private TextView _path_view;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.respeaking_selection);

		// Force portrait on phone
		if(getResources().getBoolean(R.bool.is_small_screen))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		if(!_is_init)
		{
			_nav_list = new ArrayList<>();
			_adapteur = new RespeakingFileExplorer.FileAdapter(this, _nav_list);
            _current_folder_position = FileIO.getAppRootPath()+"/"+RecordingLig.RECORDINGS;
			_is_init = true;
		}

		_path_view = (TextView) findViewById(R.id.path_view_respeak);
		translateMode = getIntent().getBooleanExtra(ModeSelection.TRANSLATE_MODE, false);
		if(translateMode) ((TextView) findViewById(R.id.respeaking_title)).setText(R.string.translatemode);
		((ListView) findViewById(R.id.search_file_list)).setAdapter(_adapteur);
		_path_view.setText(_current_folder_position);
		loadFileList(_current_folder_position);
	}

	/**
	 * Loads the list of files in the specified directory into mFileList
	 *
	 * @param	folder	The directory to go
	 */
	private void loadFileList(String folder) {
		File dir = new File(folder);
		if(!dir.exists())   return;
		String[] l = dir.list(new FilenameFilter() {
			@Override public boolean accept(File dir, String filename) {
				return filename.contains(".wav") || filename.contains(".WAV") || new File(dir, filename).isDirectory();}});
		if(l.length==0)
		{
			Toast.makeText(RespeakingFileExplorer.this, R.string.this_file_does_not_contain_any_audio_file, Toast.LENGTH_SHORT).show();
			return;
		}
		Log.d(TAG, "Folder: "+folder);
        _current_folder_position = folder;
		_path_view.setText(_current_folder_position);
		_nav_list.clear();
		ArrayUtils.reverse(l);
		Collections.addAll(_nav_list, l);
		_adapteur.notifyDataSetChanged();
	}

	/** Back on parent folder, not back if parent is Environment.getExternalStorageDirectory()
	 *
	 * @param v Item where the user clicked
	 */
	public void onBackFolderClick(View v)
	{
		if(_current_folder_position.equals(Environment.getExternalStorageDirectory().toString()+"/"))
			return;
		String s[] = _current_folder_position.split("/"), r="";
		for(int i=0;i<s.length-1;i++)   r+=s[i]+"/";
		loadFileList(r);
	}

	/** Go on next step with selected file
	 * @param file Open file
	 */
	public void openFile(String file)
	{
		MediaPlayerFactory._elicit_source = "";
		MediaPlayerFactory._elicit_rspk = "";
		if(file.contains("_elicit_")&&(file.contains("_trsl")||file.contains("_rspk")))
		{
			Toast.makeText(this, R.string.impossible_to_import_a_respeak_or_translated_file_from_an_elicitation_file, Toast.LENGTH_LONG).show();
			return;
		}
		File mPath = new File(file), aikumaFile = mPath.getParentFile().getParentFile().getParentFile();
		if(file.contains("_elicit_")&&!(file.contains("_trsl")||file.contains("_rspk")) &&
					aikumaFile != null && aikumaFile.getAbsolutePath().contains(FileIO.getAppRootPath().toString()))
			openEliciteFile(mPath);
		else if(aikumaFile != null && aikumaFile.getAbsolutePath().contains(FileIO.getAppRootPath().toString()))
		{
			// if recording, send to RespeakingMetadata
			String recordName;
			try { recordName = mPath.getName().substring(0,mPath.getName().length()-4); }
			catch (Exception e) {recordName = mPath.getName(); }
			Intent intent = new Intent(this, RespeakingMetadata.class);
			String dirName = mPath.getParentFile().getName();
			intent.putExtra(RecordActivity.intent_recordname, recordName);
			intent.putExtra("dirname", dirName);
			if(BuildConfig.DEBUG) {
				Log.i(TAG, "Respeaking on an existing aikuma file - original record name: " + recordName);
				Log.i(TAG, "Respeaking on an existing aikuma file - original record directory name: " + dirName);
			}
			intent.putExtra(ModeSelection.TRANSLATE_MODE, translateMode);
			intent.putExtra(RecordActivity.intent_rewindAmount,
					PreferenceManager.getDefaultSharedPreferences(RespeakingFileExplorer.this).getInt("respeaking_rewind", 500));
			startActivity(intent);
		} else {
			UUID uuid = UUID.randomUUID();
			try {
				Wave wave = new Wave(new FileInputStream(mPath));
				String format = wave.getWaveHeader().getFormat();
				int sampleRate = wave.getWaveHeader().getSampleRate(),
						durationMsec = (int) wave.length() * 1000,
						bitsPerSample = wave.getWaveHeader().getBitsPerSample(),
						numChannels = wave.getWaveHeader().getChannels();
				FileUtils.copyFile(mPath,new File(Recording.getNoSyncRecordingsPath(), uuid.toString() + ".wav"));
				FileUtils.copyFile(mPath,new File(Recording.getNoSyncRecordingsPath(), uuid.toString() + "-preview.wav"));
				if(BuildConfig.DEBUG)Log.i(TAG, "Copying original recording file to: " + Recording.getNoSyncRecordingsPath() + "/" + uuid.toString() + ".wav");
				Intent intent = new Intent(this, RecordingMetadata.class);
				intent.putExtra(RESPEAK, true);
				intent.putExtra("tempRecUUID", uuid.toString());
				intent.putExtra("sampleRate", sampleRate);
				intent.putExtra("duration", durationMsec);
				intent.putExtra("format", format);
				intent.putExtra("numChannels", numChannels);
				intent.putExtra("bitspersample", bitsPerSample);
				intent.putExtra(ModeSelection.TRANSLATE_MODE, translateMode);
				startActivity(intent);
			} catch (FileNotFoundException e) {
				Toast.makeText(this,R.string.failed_to_import_the_recording_, Toast.LENGTH_LONG).show();
				if(BuildConfig.DEBUG)Log.e(TAG, "Failed to import the file: " + e);
			} catch (IOException e) {
				Toast.makeText(this,R.string.failed_to_import_the_recording_, Toast.LENGTH_LONG).show();
				if(BuildConfig.DEBUG)Log.e(TAG, "Failed to copy the file to the temporary directory: " + e);
			}
		}
	}

	public void openEliciteFile(File mPath)
	{
		if(BuildConfig.DEBUG)Log.d(TAG, "openEliciteFile called");
		try {
			//Retrieving information from the wave file
			UUID uuid = UUID.randomUUID();
			Wave wave = new Wave(new FileInputStream(mPath));
			String format = wave.getWaveHeader().getFormat();
			int sampleRate = wave.getWaveHeader().getSampleRate(),
					durationMsec = (int) wave.length() * 1000,
					bitsPerSample = wave.getWaveHeader().getBitsPerSample(),
					numChannels = wave.getWaveHeader().getChannels();
			FileUtils.copyFile(mPath,new File(Recording.getNoSyncRecordingsPath(), uuid.toString() + ".wav"));
			FileUtils.copyFile(mPath,new File(Recording.getNoSyncRecordingsPath(), uuid.toString() + "-preview.wav"));
			if(BuildConfig.DEBUG)Log.i(TAG, "Copying original recording file to: " + Recording.getNoSyncRecordingsPath() + "/" + uuid.toString() + ".wav");

			//Retrieving information from the json file
			JSONObject jsonObj = FileIO.readJSONObject(new File(mPath.toString().replace(".wav", METADATA_SUFFIX)));
			String code = (String) jsonObj.get(RecordingMetadata.metaRecordLang);
			Language recordLang = code.isEmpty() ? new Language("","") : new Language(Aikuma.getLanguageCodeMap().get(code), code);
			List<Language> languages = Language.decodeJSONArray((JSONArray) jsonObj.get("languages"));
			code = (String) jsonObj.get(RecordingMetadata.metaMotherTong);
			Language motherTong = code.isEmpty() ? new Language("", "") : new Language(Aikuma.getLanguageCodeMap().get(code), code);
			String regionOrigin = (String) jsonObj.get(RecordingMetadata.metaOrigin),
					speakerName = (String) jsonObj.get(RecordingMetadata.metaSpkrName),
					speakerNote = (String) jsonObj.get(RecordingMetadata.metaSpkrNote);
			long age = (Long) jsonObj.get(RecordingMetadata.metaSpkrBirthYr);
			int speakerBirthYear= (int) age;
			String speakerGender = (String) jsonObj.get(RecordingMetadata.metaSpkrGender);

			//Calling RespekingMetadata with the right parameters
			String name = new SimpleDateFormat("yyMMdd-HHmmss").format(new Date()) + "_" + recordLang.getCode() + "_" +  Aikuma.getDeviceId();
			RecordingLig recording = new RecordingLig(uuid, name, new Date(),
					AikumaSettings.getLatestVersion(), AikumaSettings.getCurrentUserId(),
					recordLang, motherTong, languages, new ArrayList<String>(), Aikuma.getDeviceName(),
					Aikuma.getAndroidID(), null, null, (long)sampleRate, durationMsec,
					format, numChannels, bitsPerSample, MainActivity.locationDetector.getLatitude(),
					MainActivity.locationDetector.getLongitude(),
					regionOrigin, speakerName, speakerBirthYear, speakerGender, speakerNote);
			recording.write();
			MediaPlayerFactory._elicit_source = mPath.toString();
			MediaPlayerFactory._elicit_rspk = name;

			Intent intent = new Intent(RespeakingFileExplorer.this, RespeakingMetadata.class);
			intent.putExtra(RecordActivity.intent_recordname, name);
			intent.putExtra("dirname", name);
			intent.putExtra(RecordActivity.intent_rewindAmount, 500);
			startActivity(intent);
		} catch (IOException e) {
			Toast.makeText(this,R.string.failed_to_import_the_recording_, Toast.LENGTH_LONG).show();
			if(BuildConfig.DEBUG)Log.e(TAG, "Error !\n " + e);
		}
	}

	private class FileAdapter extends ArrayAdapter<String>
	{
		public static final String TAG = "FileAdapter";

		FileAdapter(Context context, List<String> objects) {super(context, 0, objects);}

		@NonNull
		@Override
		public View getView(final int position, View itemView, @NonNull ViewGroup parent)
		{
			RespeakingFileExplorer.SearchHolder sh;
			if(itemView == null) {
				itemView = LayoutInflater.from(getContext()).inflate(R.layout.content_dialog_select_file_frag, parent, false);
				sh = new RespeakingFileExplorer.SearchHolder();
				sh.name = (TextView) itemView.findViewById(R.id.textFileName);
				sh.icon = (ImageView) itemView.findViewById(R.id.image_file_type);
				itemView.setTag(sh);
			}
			else
				sh = (RespeakingFileExplorer.SearchHolder) itemView.getTag();

			String name_f = getItem(position);
			sh.name.setText(name_f);
			name_f = _current_folder_position+name_f;

			// Check if folder constraine rspk or trsl file
			File f = new File(name_f);
			itemView.findViewById(R.id.image_pastille_trsl).setVisibility(ImageView.INVISIBLE);
			itemView.findViewById(R.id.image_pastille_rspk).setVisibility(ImageView.INVISIBLE);
			if(f.isDirectory()) {
				boolean t=false, r=false;
				itemView.setBackgroundColor(Color.TRANSPARENT);
				for (String file : f.list(new FilenameFilter() {@Override public boolean accept(File dir, String filename) {
						return filename.contains(".wav") || filename.contains(".WAV") || new File(dir, filename).isDirectory();}}))
				{
					if(file.contains("_trsl")) {
						itemView.findViewById(R.id.image_pastille_trsl).setVisibility(ImageView.VISIBLE);
						t = true;
					} else if (file.contains("_rspk")) {
						((ImageView)itemView.findViewById(R.id.image_pastille_rspk)).setImageResource(R.drawable.pastille_rspk);
						itemView.findViewById(R.id.image_pastille_rspk).setVisibility(ImageView.VISIBLE);
						r = true;
					}
					if(t && r) break;
				}
			}
			else if(name_f.contains(".wav"))
			{
				if(name_f.contains("rspk"))
				{
					itemView.setBackgroundColor(Color.parseColor("#80673AB7"));
					((ImageView)itemView.findViewById(R.id.image_pastille_rspk)).setImageResource(R.drawable.pastille_rspk);
					itemView.findViewById(R.id.image_pastille_rspk).setVisibility(ImageView.VISIBLE);
					itemView.findViewById(R.id.image_pastille_trsl).setVisibility(ImageView.INVISIBLE);
				}
				else if(name_f.contains("trsl"))
				{
					itemView.setBackgroundColor(Color.parseColor("#80FF9800"));
					((ImageView)itemView.findViewById(R.id.image_pastille_rspk)).setImageResource(R.drawable.pastille_trsl);
					itemView.findViewById(R.id.image_pastille_rspk).setVisibility(ImageView.VISIBLE);
					itemView.findViewById(R.id.image_pastille_trsl).setVisibility(ImageView.INVISIBLE);
				}
				else
				{
					itemView.setBackgroundColor(Color.TRANSPARENT);
					itemView.findViewById(R.id.image_pastille_rspk).setVisibility(ImageView.INVISIBLE);
					itemView.findViewById(R.id.image_pastille_trsl).setVisibility(ImageView.INVISIBLE);
				}
			}
			else
				itemView.setBackgroundColor(Color.TRANSPARENT);

			// Icon, if it's file, changer folder icon by wav file icon
			((ImageView) itemView.findViewById(R.id.image_file_type)).setImageResource((new File(name_f).isDirectory())?
					R.drawable.folder_close_icon:R.drawable.wav_file_icon);

			// Go to folder or select item
			itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v)
				{
					String nf = _current_folder_position+_nav_list.get(position);
					if(new File(nf).isDirectory())	loadFileList(nf+"/");
					else							openFile(nf);
				}
			});
			return itemView;
		}
	}
	private static class SearchHolder
	{
		TextView name;
		ImageView icon;
	}

	public void onBackPressed(View v) {
		this.finish();
	}
	
}
