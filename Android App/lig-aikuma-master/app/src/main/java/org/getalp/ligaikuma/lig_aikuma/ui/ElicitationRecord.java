package org.getalp.ligaikuma.lig_aikuma.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.apache.commons.io.FileUtils;
import org.getalp.ligaikuma.lig_aikuma.Aikuma;
import org.getalp.ligaikuma.lig_aikuma.MainActivity;
import org.getalp.ligaikuma.lig_aikuma.audio.SimplePlayer;
import org.getalp.ligaikuma.lig_aikuma.audio.record.Microphone.MicException;
import org.getalp.ligaikuma.lig_aikuma.audio.record.Recorder;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;
import org.getalp.ligaikuma.lig_aikuma.model.Language;
import org.getalp.ligaikuma.lig_aikuma.model.MetadataSession;
import org.getalp.ligaikuma.lig_aikuma.model.Recording;
import org.getalp.ligaikuma.lig_aikuma.model.RecordingLig;
import org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile;
import org.getalp.ligaikuma.lig_aikuma.util.AikumaSettings;
import org.getalp.ligaikuma.lig_aikuma.util.FileIO;
import org.getalp.ligaikuma.lig_aikuma.util.IdUtils;
import org.getalp.ligaikuma.lig_aikuma.util.PDFBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import static org.getalp.ligaikuma.lig_aikuma.util.FileIO.getNoSyncPath;

public class ElicitationRecord extends AikumaActivity {
	public static final String TAG = "ElicitationRecord";
	private UUID recordUUID;
	private Recorder recorder;
	protected long sampleRate = 16000L;
	private boolean recording = false;
	private BufferedReader reader;
	private int entityId = 0;
	private String ptrSelectedReference;
	private ListenFragment fragment;
	private File[] listFile;
	private Boolean isNewSession = true;
	private Date date;
	private SharedPreferences prefsUserSession;
	private String strFolderDate;
	private RecordingLig recordingLig;
	private String dirName;
	private String recordingName;
	private String mode = "_elicit";
	
	/**
	 * the "linker" file
	 */
	private File textFile;
	private BufferedWriter bufferedWriter;
	private int choiceMode;
	/**
	 * the progress bar used to display the user's progression.
	 */
	private InterleavedSeekBar progressBar;
	private int numberOfEntities;
	private Language recordLang;
	/**
	 * UNDO
	 */
	private ImageButton _button_undo;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    recordUUID = UUID.randomUUID();
	    // retrieving metadata
		prefsUserSession = getSharedPreferences("userSession", MODE_PRIVATE);
		initSession();
		switch(choiceMode) {
		case ElicitationMode.TEXT_MODE:
			setContentView(R.layout.elicitation_text);
			break;
		case ElicitationMode.IMAGE_MODE:
			setContentView(R.layout.elicitation_image);
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			break;
		case ElicitationMode.VIDEO_MODE:
			setContentView(R.layout.elicitation_video);
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			break;
		default:
			setContentView(R.layout.elicitation_text);
			break;
		}
		
        setProgressBarsAtNotTouchableState();
	    
	    try {
	    	initProgressText();
	    	switch (choiceMode) {
				case ElicitationMode.TEXT_MODE:		loadTextViews();	break;
				case ElicitationMode.IMAGE_MODE:	loadImage();		break;
				case ElicitationMode.VIDEO_MODE:	loadVideo();		break;
				default:	initProgressText();	loadTextViews();		break;
			}
	    	updateProgressBar();
			if(BuildConfig.DEBUG)Log.d(TAG, "Progress bar (onCreate state) = "+entityId+"/"+numberOfEntities);

		} catch (FileNotFoundException e1) {
			if(BuildConfig.DEBUG)Log.e(TAG,"text file could not be found: " + e1);
			Toast.makeText(this, R.string.an_error_occurred_the_resource_could_not_be_found, Toast.LENGTH_LONG).show();
			try {
				bufferedWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.finish();
		} catch (IOException e) {
			Toast.makeText(this, R.string.something_weird_happened_it_might_be_that_the_resource_file_was_empty, Toast.LENGTH_LONG).show();
			if(BuildConfig.DEBUG)Log.e(TAG,"No more sentences to display or an error occurred: " + e);
			try {
				bufferedWriter.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			this.finish();
		}

	    fragment = (ListenFragment) getFragmentManager().findFragmentById(R.id.phrase_player);
		_button_undo = (ImageButton) findViewById(R.id.button_undo);
		_button_undo.setEnabled(false);
	}
	
	private void loadVideo() {
		Log.d(TAG,"entityId = "+entityId+"; numberOfEntities = "+numberOfEntities);
		if(entityId >= numberOfEntities) {
			Toast.makeText(this, getResources().getString(R.string.file_saved) + strFolderDate + "_" + MetadataSession.getMetadataSession().getRecordLanguage().getCode()
					+ "_" + Aikuma.getDeviceId() + mode + "_" + entityId, Toast.LENGTH_SHORT).show();
			if(bufferedWriter != null) {
				try {
					bufferedWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			this.finish();
		} else {
			Bitmap thumb = ThumbnailUtils.createVideoThumbnail(listFile[entityId].getAbsolutePath(),
	                MediaStore.Images.Thumbnails.MINI_KIND);
			VideoView videoView = (VideoView) findViewById(R.id.vid_orig);
			videoView.setZOrderOnTop(true);
			videoView.setMediaController(new MediaController(this));
			videoView.setVideoPath(listFile[entityId].getAbsolutePath());
			videoView.setDrawingCacheEnabled(true);
			videoView.setBackgroundDrawable(new BitmapDrawable(this.getResources(), thumb));
			videoView.setVisibility(View.GONE);
			videoView.setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.image_x)).setText(getString(R.string.video) + entityId);
		}
	}

	private void updateProgressBar() {
		progressBar.setProgress((int) (((float)entityId)/(float)numberOfEntities * 100));
	}
	
	private void initProgressText() throws IOException {
		numberOfEntities = countNumberOfEntities();
		((TextView) findViewById(R.id.elicit_phrase_number)).setText(String.valueOf(entityId));
		((TextView) findViewById(R.id.elicit_total_number)).setText(" / " + numberOfEntities);
	}
	
	private void loadTextViews() throws IOException
	{
		String phrase;
		while((phrase = reader.readLine()) != null && (phrase.isEmpty() || phrase.split("##").length <= 1));
		if(phrase == null) { throw new IOException("Empty text file"); }

		if(phrase.matches("##$"))	phrase+=" ";
		String[] phrasePair = phrase.split("##");
		((TextView) findViewById(R.id.tv_elicit_phrase)).setText(phrasePair[0]);
		((TextView) findViewById(R.id.tv_orig_phrase)).setText(phrasePair[1]);
		((TextView) findViewById(R.id.phrase_x)).setText(getString(R.string.phrase) + (entityId+1));
	}
	
	private void loadImage()
	{
		if(BuildConfig.DEBUG)Log.d(TAG,"entityId = "+entityId+"; numberOfEntities = "+numberOfEntities);
		if(entityId >= numberOfEntities){
			Toast.makeText(this, getResources().getString(R.string.file_saved) + strFolderDate + "_" + MetadataSession.getMetadataSession().getRecordLanguage().getCode()
					+ "_" + Aikuma.getDeviceId() + mode + "_" + entityId, Toast.LENGTH_SHORT).show();
			if(bufferedWriter != null) {
				try {
					bufferedWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			this.finish();
		} else {
			new BitmapFactory.Options().inPreferredConfig = Bitmap.Config.ARGB_8888;
			new BitmapWorkerTask((ImageView) findViewById(R.id.img_orig)).execute(listFile[entityId]);
			((TextView) findViewById(R.id.image_x)).setText(getString(R.string.image) + entityId);
		}
	}
	
	public static Bitmap decodeSampledBitmapFromResource(File file, int reqWidth, int reqHeight)
	{

	    // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeFile(file.getAbsolutePath(), options);

	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
	}
	
	private void initBufferedWriter() throws FileNotFoundException {
		bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(textFile, true)));
	}
	
	@SuppressWarnings("unchecked")
	private void initSession() {
	    if(prefsUserSession.getBoolean("active", false))
	    {
	    	strFolderDate=prefsUserSession.getString("date", "");
	    	recordLang = new Language(prefsUserSession.getString("Language name", ""), prefsUserSession.getString("LanguageCode", ""));
	    	try {
	    		date = new SimpleDateFormat().parse(strFolderDate);
			} catch (ParseException e) {
				date = new Date();
				e.printStackTrace();
			}
	 
	    	ptrSelectedReference = prefsUserSession.getString("inputFile", "");
			entityId = prefsUserSession.getInt("currentLine", 0);
			choiceMode = prefsUserSession.getInt("selectedFileType", ElicitationMode.TEXT_MODE);
			dirName = prefsUserSession.getString("dirname", "");
			recordingName = prefsUserSession.getString("session_output_file", "");
			if(BuildConfig.DEBUG)Log.d(TAG, "last saved recorded filename: " + recordingName);
			prefsUserSession.edit().clear().apply();
		    try {
				numberOfEntities = countNumberOfEntities();
			} catch (IOException e) {
				e.printStackTrace();
			}
		    
			isNewSession = false;
	    } else {
	    	ptrSelectedReference = getIntent().getStringExtra(ElicitationMode.importFileName);
			if(BuildConfig.DEBUG)Log.i(TAG, "Selected file: " + ptrSelectedReference);
		    choiceMode = getIntent().getIntExtra("selectedFileType", 0);


	    	recordLang = MetadataSession.getMetadataSession().getRecordLanguage();

           try {
               date = new SimpleDateFormat().parse(getIntent()
					   .getBundleExtra(RecordingMetadata.metaBundle)
					   .getString(RecordingMetadata.metaDate));
           } catch (ParseException e1) { 
			   date = new Date();
           }

	    	strFolderDate = new SimpleDateFormat("yyMMdd-HHmmss").format(date);
	    }
	    
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(ptrSelectedReference)));
			if(!isNewSession)
				goToGoodPlaceInTextFile();
		} catch (FileNotFoundException e1) {
				e1.printStackTrace();
		}


    	dirName = strFolderDate + "_" + recordLang.getCode() + "_" +  Aikuma.getDeviceId() + mode;
    	textFile = new File(FileIO.getOwnerPath()+"/recordings/"+dirName+"/"+dirName+"_linker.txt");
    	listFile = getFiles();

		// PDF Génération
		SpeakerProfile tsp = SpeakerProfile.getLastSpeaker();
		if(tsp!=null && !tsp.getSignature())
		{
			tsp.setSignature(true).saveOnSharedPreferences(this);
			new PDFBuilder(ElicitationRecord.this).BuildConsentForm(FileIO.getOwnerPath()+
					"/recordings/"+dirName+"/", "form_consent.pdf", tsp.getName());
		}

		PDFBuilder.moveAndRenameFile(getNoSyncPath()+"/tmp_form_consent.pdf",
				FileIO.getOwnerPath()+"/recordings/"+dirName+"/form_consent.pdf");

		if (!isNewSession) {
			try {	
				initBufferedWriter();
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(this, R.string.problem_with_text_output_file_linker, Toast.LENGTH_SHORT).show();
				this.finish();
			}
		}
	}

	/** Get files used for elicitation
	 *
	 * @return File[] with the right extension
	 */
	public File[] getFiles()
	{
		switch(choiceMode) {
			case ElicitationMode.IMAGE_MODE:
				return new File(ptrSelectedReference).listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String filename) {
						for(String t : ElicitationFileExplorer.FILE_IMAGE) if(filename.contains(t))	return true;
						return false;}});
			case ElicitationMode.VIDEO_MODE:
				return new File(ptrSelectedReference).listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String filename) {
						for(String t : ElicitationFileExplorer.FILE_VIDEO) if(filename.contains(t))	return true;
						return false;}});
		}
		return null;
	}

	
	private void initTxtEncodedFile() {
		try {
			if(!textFile.exists())
				initBufferedWriter();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.problem_with_text_output_file_linker, Toast.LENGTH_SHORT).show();
			this.finish();
		}
	}
	/**
	 * Called to going to the previous state (phraseID and others graphical attributes)
	 */
	private void goToGoodPlaceInTextFile() {
		if(choiceMode != ElicitationMode.TEXT_MODE)	return;
		String phrase;
		try {
			for(int i = 0;i<entityId;)
				if(!((phrase = reader.readLine()) != null && (phrase.isEmpty() || phrase.split("##").length <= 1))&&phrase != null)
					i++;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * called when we want to save one record. Used when we touch next button
	 */
	private void saveOneRecord() {
		recordUUID = UUID.randomUUID();
		try {
			if (recorder != null) recorder.release();
			recorder = new Recorder(0, new File(Recording.getNoSyncRecordingsPath(), recordUUID.toString() + ".wav"), sampleRate);
			if(BuildConfig.DEBUG)Log.i(TAG, "recorder filename: " + recorder.getWriter().getFullFileName());
			fragment.releasePlayer();
			// test on the lines read:
			// check that the file is not empty by ensuring that the phrase is not null (end of file reached)
			// check that the sentence is correctly formatted (no empty line and '##' delimited found)
			
			entityId++;
			
			switch (choiceMode) {
			case ElicitationMode.TEXT_MODE:
				String phrase;
				while((phrase = reader.readLine()) != null && (phrase.isEmpty() || phrase.split("##").length <= 1));
				if (phrase == null) { throw new IOException("End of text file reached."); }
				String[] phrasePair = phrase.split("##");
				((TextView) findViewById(R.id.tv_elicit_phrase)).setText(phrasePair[0]);
				((TextView) findViewById(R.id.tv_orig_phrase)).setText(phrasePair[1]);
				((TextView) findViewById(R.id.phrase_x)).setText("Phrase" + (entityId+1));
				break;
			case ElicitationMode.IMAGE_MODE:
				loadImage();
				break;
			case ElicitationMode.VIDEO_MODE:
				loadVideo();
				break;
			default:
				break;
			}

			((TextView) findViewById(R.id.elicit_phrase_number)).setText(entityId + "");
			
			updateProgressBar();
			if(BuildConfig.DEBUG)Log.d(TAG, "Progress bar (saveOneRecord state) = "+ entityId +"/"+numberOfEntities);
		} catch (MicException e) {
			this.finish();
			Toast.makeText(getApplicationContext(), "Error setting up microphone.", Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Toast.makeText(this, getResources().getString(R.string.file_saved) + strFolderDate + "_" + MetadataSession.getMetadataSession().getRecordLanguage().getCode()
					+ "_" + Aikuma.getDeviceId() + mode + "_" + entityId, Toast.LENGTH_SHORT).show();
			if(BuildConfig.DEBUG)Log.e(TAG,"No more to display or an error occurred: " + e);
			this.finish();
		}
	}
	
   /**
    * called to set progressionBar behavior
    * progressionBar is not modifiable by hand
    */
   private void setProgressBarsAtNotTouchableState() {
	   progressBar = (InterleavedSeekBar) findViewById(R.id.seek_elicit_progression);
	   progressBar.setOnSeekBarChangeListener(
           new SeekBar.OnSeekBarChangeListener() {
                   int originalProgress;
                   public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                           if(fromUser) seekBar.setProgress(originalProgress);
                   }
                   public void onStopTrackingTouch(SeekBar _seekBar) {}
                   public void onStartTrackingTouch(SeekBar _seekBar) {
                           originalProgress = progressBar.getProgress();
                   }
           });
   }

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(recorder != null)
			recorder.release();
	}
	
	public void onRecordClick(View _view) {
		if(recording)	pause();
		else			record();
	}
	
	/**
	 * called when we click on next button
	 * @param _view the next button
	 */
	public void onNextClick(View _view) {onNextClick();}

	/**
	 * Work to go on next stat (next text line, image or video)
	 */
	public void onNextClick()
	{
		if(!saveRecording() && (entityId < numberOfEntities-1))
			Toast.makeText(this, R.string.going_next, Toast.LENGTH_SHORT).show();
		saveOneRecord();
		if(entityId >= numberOfEntities-1)
			deleteAllTempFiles();

		_button_undo.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.undo2));
		_button_undo.setEnabled(false);
		ImageButton recordButton = (ImageButton) findViewById(R.id.btn_record_elicit);
		recordButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.record));
		recordButton.setEnabled(true);
	}


	/**
	 * Delete all temporary files in dedicated folder
	 */
	private void deleteAllTempFiles()
	{
		for(File f : Recording.getNoSyncRecordingsPath().listFiles())
			if(!f.delete() && BuildConfig.DEBUG)
				Log.i(TAG, "Failed to delete file : "+f.toString());
	}

	public void onValidate(View _view) {
		saveRecording();
		if(entityId == 0)	this.finish();
		else				interruptionCallback();
	}
	
	/**
	 * Function called to restore the recordingLig by a json file.
	 */
	private void restoreMetaDataFromJson() {

		if(BuildConfig.DEBUG)Log.d(TAG, "last saved metadata file: " + recordingName+RecordingLig.METADATA_SUFFIX);
		
		if(entityId > 0) {
			int i = entityId-1;
			
			File metadataFile;
			// Do is used to get the last json file. Useful in case we have skip sentences
			do{
//				metadataFile = new File(FileIO.getOwnerPath().getAbsolutePath() + "/"+ "recordings" + "/" +this.strFolderDate + "_"+recordLang.getCode()+"_"+Aikuma.getDeviceId(), this.strFolderDate  + "_" + recordLang.getCode() + "_" + Aikuma.getDeviceId() + "_" + i + RecordingLig.METADATA_SUFFIX);
				metadataFile = new File(FileIO.getOwnerPath().getAbsolutePath() + "/"+ RecordingLig.RECORDINGS + dirName +"/",
						dirName + "_" + i + RecordingLig.METADATA_SUFFIX);
				i--;
			} while (!metadataFile.exists());
			if(BuildConfig.DEBUG)Log.d(TAG, "metadataFile : " + metadataFile.getAbsolutePath());

			String splitRecordingName[] = recordingName.split("_");
			try {
				recordingLig = RecordingLig.read(metadataFile);
				recordingLig.setDurationMsec(recorder.getCurrentMsec());
				recordingLig.setRecordingUUID(recordUUID);
				recordingLig.setDeviceName(Aikuma.getDeviceName());
				recordingLig.setName(splitRecordingName[0]+"_" +splitRecordingName[1]+"_" +splitRecordingName[2]+"_"+splitRecordingName[3]+"_" + entityId);
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}
	
	/**
	 * Function called when we need to initialize the recordingLig object 
	 * used to write json and wav files.
	 */
	private void setRecordingLig() {
		if (isNewSession) {
			int duration = recorder.getCurrentMsec();
			String deviceName = Aikuma.getDeviceName(), androidID = Aikuma.getAndroidID();
			ArrayList<String> speakerIds = new ArrayList<>();

			// Not used
			//String suffix = (new File(ptrSelectedReference).getName().replace(".txt", "").length() >= 10)? new File(ptrSelectedReference).getName().substring(0, 10): new File(ptrSelectedReference).getName().replace(".txt", "");

			String idDevice = Aikuma.getDeviceId();
//			String recordingName = strFolderDate + "_" + MetadataSession.getMetadataSession().getRecordLanguage().getCode() + "_" +  idDevice; + "_elicit_" + new File(eliciTextFile).getName().substring(0,10);+ "_" + eliciTextFile.substring(0, 10) + "_" + phraseId
			recordingName = strFolderDate + "_" +
					MetadataSession.getMetadataSession().getRecordLanguage().getCode()
									+ "_" +  idDevice + mode + "_" + entityId;
			if(BuildConfig.DEBUG){Log.i(TAG, "recording name: " + recordingName);
			Log.i(TAG, "record directory name: " + dirName);}
			String gender = "";
			switch(MetadataSession.getMetadataSession().getSpeakerGender())
			{
				case RecordingMetadata.GENDER_MALE:			gender = "Male";break;
				case RecordingMetadata.GENDER_FEMALE:		gender = "Female";break;
				case RecordingMetadata.GENDER_UNSPECIFIED:	gender = "Unspecified";break;
			}
			String groupId = IdUtils.sampleFromAlphabet(12, "abcdefghijklmnopqrstuvwxyz"),
					sourceVerId = AikumaSettings.getLatestVersion() + "-" + groupId;
/*			recordingLig = new RecordingLig(recordUUID, recordingName, date, AikumaSettings.getLatestVersion(), AikumaSettings.getCurrentUserId(), recordLang, MetadataSession.getMetadataSession().getMotherTongue(), MetadataSession.getMetadataSession().getExtraLanguages(), speakerIds, deviceName, androidID, null, null, sampleRate, duration, recorder.getFormat(), recorder.getNumChannels(), recorder.getBitsPerSample(), latitude, longitude, MetadataSession.getMetadataSession().getRegionOrigin(), MetadataSession.getMetadataSession().getSpeakerName(), MetadataSession.getMetadataSession().getSpeakerAge(), gender);*/
			recordingLig = new RecordingLig(recordUUID, recordingName, date, 
					AikumaSettings.getLatestVersion(), 
					AikumaSettings.getCurrentUserId(), recordLang, MetadataSession.getMetadataSession().getMotherTongue(),
					MetadataSession.getMetadataSession().getExtraLanguages(), speakerIds, deviceName, androidID, 
					groupId, sourceVerId, dirName, sampleRate, duration, 
					recorder.getFormat(), recorder.getNumChannels(), 
					recorder.getBitsPerSample(), MainActivity.locationDetector.getLatitude(), MainActivity.locationDetector.getLongitude(),
					MetadataSession.getMetadataSession().getRegionOrigin(),
					MetadataSession.getMetadataSession().getSpeakerName(),
					MetadataSession.getMetadataSession().getSpeakerAge(), gender,
					MetadataSession.getMetadataSession().getSpeakerNote());
		} else
			restoreMetaDataFromJson();
	}
	
	/**
	 * called when we want save a file.
	 * @return If record is saved
	 */
	private boolean saveRecording() {
		if ((recorder != null && recorder.getFile().getPayloadSize() == 0) || recorder == null)
			return false;
		try {
			recorder.stop();
		} catch (MicException e) {
			if(BuildConfig.DEBUG)Log.e(TAG, "error when saving the recording: " + e);
			Toast.makeText(this, R.string.an_error_occurred_with_the_microphone_please_try_again, Toast.LENGTH_LONG).show();
			return false;
		}

		setRecordingLig();



		try {
			recordingLig.write();
			String filePath = recordingLig.getFile().getAbsolutePath(), //absolute path of the file
			filenamePath = ""; //name of the file with its absolute path;
			//filenamePath = filePath.substring(0, filePath.length()-(5+String.valueOf(entityId).length()))+".wav";	//TODO: voir si ca fonctionne
			if(entityId >= 0 && entityId < 10)
				filenamePath = filePath.substring(0, filePath.length()-6)+".wav";
			else if(entityId >= 10 && entityId < 100)
				filenamePath = filePath.substring(0, filePath.length()-7)+".wav";
			else if(entityId >= 100 && entityId < 1000)
				filenamePath = filePath.substring(0, filePath.length()-8)+".wav";
			else if(entityId >= 1000 && entityId < 10000)
				filenamePath = filePath.substring(0, filePath.length()-9)+".wav";
			else if(entityId >= 10000 && entityId < 100000)
				filenamePath = filePath.substring(0, filePath.length()-10)+".wav";
			Log.i(TAG, "filename path = "+filenamePath);

			/*audio file*/
			FileUtils.copyFile(recordingLig.getFile(), new File(filenamePath));
			FileIO.delete(recordingLig.getFile());
			FileUtils.copyFile(new File(filenamePath),
					new File(filenamePath.replace(".wav", "_" + entityId + ".wav")));
			/*metadata file*/
//			FileUtils.copyFile(new File(filenamePath.replace(".wav", "-metadata.json")), new File(filenamePath.replace(".wav", "_" + entityId + "-metadata.json")));
//			FileIO.delete(new File(filenamePath.replace(".wav", "-metadata.json")));

			FileIO.delete(new File(filenamePath));

			recorder.release();
			recorder = null;
			//write linker file
			initTxtEncodedFile();
			if (choiceMode == ElicitationMode.IMAGE_MODE || choiceMode == ElicitationMode.VIDEO_MODE) {
//				bufferedWriter.write(listFile[entityId].getPath() + " ; " +new File(filePath).getPath().replace(".wav", "_"+entityId+".wav"));
				bufferedWriter.write(listFile[entityId].getPath() + " ; " +new File(filePath).getPath());
				Log.d(TAG, "linker content :  " + listFile[entityId].getPath() + " ; " + new File(filePath));
				bufferedWriter.newLine();
				bufferedWriter.flush();
			} else {
//				bufferedWriter.write(ptrSelectedReference + " (line "+ (entityId+1) + ") ; " + new File(filePath).getPath().replace(".wav", "_"+entityId+".wav"));
				bufferedWriter.write(ptrSelectedReference + " (line "+ (entityId+1) + ") ; " + new File(filePath).getPath());
				Log.d(TAG, "linker content :  " + ptrSelectedReference + " (line "+ (entityId+1) + ") ; " + new File(filePath).getPath());
				bufferedWriter.newLine();
				bufferedWriter.flush();
			}
		} catch (IOException e) {
			if(BuildConfig.DEBUG)Log.e(TAG, "error when saving the recording: " + e);
			Toast.makeText(this, R.string.an_error_occurred_when_saving_the_recording_please_try_again, Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
	
	// Pauses the recording.
	private void pause() {
		if (recording) {
			recording = false;
			_button_undo.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.undo));
			_button_undo.setEnabled(true);
			ImageButton recordButton = (ImageButton) findViewById(R.id.btn_record_elicit);
			recordButton.setImageResource(R.drawable.record_disabled);
			recordButton.setEnabled(false);
			findViewById(R.id.btn_next).setEnabled(true);
			findViewById(R.id.btn_validate).setEnabled(true);
			try {
				recorder.pause();
				recorder.stop();
				// the player can only be set when the file is closed (hence the recorder stopped)
				// indeed, they both require a file descriptor (fd) on the same file;
				// when i tried to set up the player while the recorder was still holding the fd,
				// there was an error about the failure to set the fd; which i interpreted this way
				fragment.releasePlayer();
				fragment.setPlayer(new SimplePlayer(new File(recorder.getWriter().getFullFileName()), sampleRate, true));
			} catch (MicException e) {
				// Maybe make a recording metadata file that refers to the error so
				// that the audio can be salvaged.
			}
			catch (IOException e) {
				if(BuildConfig.DEBUG)Log.e(TAG, "Could not start the fragment of the file "
						+new File(Recording.getNoSyncRecordingsPath(),recordUUID.toString()+".wav")+" : " + e);
			}
		}
	}
		
	// Activates recording
	private void record()
	{
		if (!recording) {
			recording = true;
			((ImageButton) findViewById(R.id.btn_record_elicit)).setImageResource(R.drawable.pause);
			findViewById(R.id.btn_next).setEnabled(false);
			findViewById(R.id.btn_validate).setEnabled(false);
			if(recorder != null) recorder.release();
			try {
				recorder = new Recorder(0, new File(Recording.getNoSyncRecordingsPath(),"/"+recordUUID.toString()+".wav"), sampleRate);
			} catch (MicException e) {
				this.finish();
				Toast.makeText(getApplicationContext(),
						R.string.error_setting_up_microphone,
						Toast.LENGTH_LONG).show();
			}
			recorder.listen();
		}
	}
	
	
   /**
    * called by constructor to set the number of lines in the file
    * @return the number of lines in good format in a txt file.
    * @throws IOException if the file cannot be read
    */
   public int countNumberOfEntities() throws IOException {
	   if (choiceMode == ElicitationMode.TEXT_MODE) {
		   int nbLine = 0;
		   BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(ptrSelectedReference)));
		   for(String phrase;(phrase = bufferedReader.readLine()) != null;)
			   if(!phrase.isEmpty() && phrase.split("##").length > 1)
			   	nbLine++;
		   bufferedReader.close();
		   return nbLine;
	   }
	   // If not ElicitationMode.IMAGE_MODE it's ElicitationMode.VIDEO_MODE
	   return new File(ptrSelectedReference).listFiles((choiceMode == ElicitationMode.IMAGE_MODE)?
	   		new FileFilter() {@Override public boolean accept(File pathname)
				{return contain(pathname, ElicitationFileExplorer.FILE_IMAGE);}}:
		   	new FileFilter() {@Override public boolean accept(File pathname)
				{return contain(pathname, ElicitationFileExplorer.FILE_VIDEO);}}).length;
   }


   /**
    * Called when we need to save the current state of the activity
    */
   private void saveCurrentState() {
		Toast.makeText(this, R.string.progression_saved, Toast.LENGTH_SHORT).show();
	   	SharedPreferences.Editor ed = prefsUserSession.edit();
	   	ed.putBoolean("active", true);
		ed.putString("mode",TAG);
		ed.putString("dirname", dirName);
		ed.putString("session_output_file", recordingName);
		ed.putString("Language name", recordLang.getName());
		ed.putString("LanguageCode", recordLang.getCode());
		ed.putString("date", strFolderDate);
		ed.putInt("currentLine", entityId);
		ed.putString("inputFile", ptrSelectedReference);
		ed.putString("progress", entityId + " / " + numberOfEntities);
		ed.putInt("selectedFileType", choiceMode);
		switch (choiceMode)
		{
			case ElicitationMode.TEXT_MODE:		ed.putString("submode", "text");	break;
			case ElicitationMode.IMAGE_MODE:	ed.putString("submode", "image");	break;
			case ElicitationMode.VIDEO_MODE:	ed.putString("submode", "video");	break;
		}		
		ed.apply();
   }
   
   public void onButtonBackPressed(View v) {if(interruptionCallback()) finish();}
   
	@Override
	public void onBackPressed() {
		if(interruptionCallback()) finish();
	}
	
	private boolean interruptionCallback() {
		if(entityId==0)	return true;
		String message = (safeActivityTransitionMessage != null)?
			   safeActivityTransitionMessage:
			   getString(R.string.a_session_is_progress_do_you_want_to_save_the_progression);
		new AlertDialog.Builder(this)
			.setMessage(message)
			.setPositiveButton(R.string.yes,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						if(entityId!=0 && !findViewById(R.id.btn_record_elicit).isEnabled())
							onNextClick();

						if(entityId<numberOfEntities)
							saveCurrentState();

						if (bufferedWriter != null) {
							try {
								bufferedWriter.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}

						// Close all Activity
						ElicitationRecord.this.finishAffinity();
					}
				})
			.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				@Override public void onClick(DialogInterface dialog, int which) {
					File metadataFile = new File(FileIO.getNoSyncPath(), ElicitationRecord.this.recordUUID + RecordingLig.METADATA_SUFFIX),
							mapFile = new File(FileIO.getNoSyncPath(), ElicitationRecord.this.recordUUID + RecordingLig.MAP_EXT),
							wavFile = new File(FileIO.getNoSyncPath() + "/items/" + ElicitationRecord.this.recordUUID
							+ ".wav");
					mapFile.delete();
					metadataFile.delete();
					wavFile.delete();
					if(BuildConfig.DEBUG){Log.i(TAG, "temp map file deleted: "+mapFile);
					Log.i(TAG, "temp metadata file deleted: "+metadataFile);
					Log.i(TAG, "temp wave file deleted: "+wavFile);}
					if (isNewSession)
						ElicitationRecord.this.finish();
					else {
						startActivity(new Intent(ElicitationRecord.this, ElicitationMode.class));
						finish();
					}
				}
			})
			.setNeutralButton(R.string.cancel, null)
			.show();
		return false;
	}
	
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight, width = options.outWidth;
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        for(int halfHeight = height / 2, halfWidth = width / 2;(halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth;)
            inSampleSize *= 2;

    }

    return inSampleSize;
	}


	public class BitmapWorkerTask extends AsyncTask<File, Void, Bitmap> {
		private final WeakReference<ImageView> imageViewReference;

		public BitmapWorkerTask(ImageView imageView) {
			// Use a WeakReference to ensure the ImageView can be garbage collected
			imageViewReference = new WeakReference<>(imageView);
		}

		// Decode image in background.
		@Override
		protected Bitmap doInBackground(File... params) {
			return ElicitationRecord.decodeSampledBitmapFromResource(params[0], 800, 450);
		}

		// Once complete, see if ImageView is still around and set bitmap.
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null) {
				final ImageView imageView = imageViewReference.get();
				if(imageView != null)
					imageView.setImageBitmap(bitmap);
			}
		}
	}

	/**
	 * Call by Undo button. Reset player and recorder and delete last temps file.
	 *
	 * @param _view Unuse param
	 */
	public void onUndoClick(View _view) {
		// Button management
		_button_undo.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.undo2));
		_button_undo.setEnabled(false);
		ImageButton recordButton = (ImageButton) findViewById(R.id.btn_record_elicit);
		recordButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.record));
		recordButton.setEnabled(true);

		// Reset player
		getFragmentManager().beginTransaction().replace(R.id.phrase_player, new ListenFragment()).commit();
		fragment.setProgress(0);
		fragment.releasePlayer();

		// Delete last temp file
		File file = new File(Recording.getNoSyncRecordingsPath(), recordUUID.toString()+".wav");
		try {
			FileIO.delete(file);
			if(BuildConfig.DEBUG)Log.i(TAG, "Deleting " + file.getAbsolutePath());
		} catch (IOException e) {if(BuildConfig.DEBUG)Log.e(TAG, "Failed to delete " + file.getAbsolutePath() + " - maybe the file didn't exist");}

		// Reset recorder
		recordUUID = UUID.randomUUID();
		if (recorder != null) recorder.release();
		try {
			recorder = new Recorder(0, new File(Recording.getNoSyncRecordingsPath(), recordUUID.toString()+".wav"), sampleRate);
		} catch (MicException ignored) {if(BuildConfig.DEBUG)Log.i(TAG, "Failed to create new recorder");}

		try {
			initBufferedWriter();
		} catch (FileNotFoundException ignored) {}
	}

	public static boolean contain(File f, String[] fileTypes)
	{
		if(f.isDirectory())	return false;
		String s = f.toString();
		for(String t : fileTypes)
			if(s.contains(t))
				return true;
		return false;
	}
}

