package org.getalp.ligaikuma.lig_aikuma.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.getalp.ligaikuma.lig_aikuma.ModeSelection;
import org.getalp.ligaikuma.lig_aikuma.audio.SimplePlayer;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CheckTranscription extends AikumaActivity {
	
	public static final String TAG = "CheckTranscription";
	private BufferedReader reader;
	private BufferedWriter outputFile;
	private File checkedFile;
	private String transcripTextFile;
	private String transcriptFilename;
	private String transcriptChecked;
	private TextView transcriptID;
	private CheckBox transcriptOK;
	protected long sampleRate = 16000l;
	private int nbReadLines;
	private int nb_total_lines;
	private String date;
	private SharedPreferences prefsUserSession;
	private String externalStoragePath = Environment.getExternalStorageDirectory().getPath();
	private String dataPath = externalStoragePath;
    private InterleavedSeekBar progressBar;
	private Boolean isNewSession = true;

	private ListenFragment fragment;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.check_transcription);
	    
	    initProgressBars();
		if(BuildConfig.DEBUG)Log.d("dataPath", dataPath);
	    prefsUserSession = getSharedPreferences("userSession", MODE_PRIVATE);
	    date = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.FRANCE).format(new Date());
	    //case: session already exists
		boolean append;
		if (prefsUserSession.getBoolean("active", false)) {
	    	//retrieve selected file handled in the last session
	    	transcripTextFile = prefsUserSession.getString("inputFile",null);
			if(BuildConfig.DEBUG)Log.i(TAG, "Selected import file: " + transcripTextFile);
	    	//retrieve result file handled in the last session
	    	transcriptChecked = prefsUserSession.getString("checkExportFile",null);
			if(BuildConfig.DEBUG)Log.i(TAG, "Selected export file: " + transcriptChecked);
	    	//retrieve number of lines already handled in the last session
	    	//nbReadLines = Integer.parseInt(prefsUserSession.getString(getString(R.string.sessionProgress),"0"));
	    	nbReadLines = prefsUserSession.getInt("currentLine", 0);
			if(BuildConfig.DEBUG)Log.i(TAG, "onCreate - #sentences treated: " + nbReadLines);
	    	append = true;
	    	// clear the current stored session
	    	prefsUserSession.edit().clear().apply();
	    	isNewSession = false;
			if(BuildConfig.DEBUG)Log.i(TAG, "Is it a new session ? "+ false);
		//case: no session
	    } else {
		    transcripTextFile = getIntent().getStringExtra(CheckMode.importFileName);
			if(BuildConfig.DEBUG){Log.i(TAG, "Is it a new session ? "+isNewSession);
			Log.i(TAG, "Selected file: " + transcripTextFile);}
		    append = false;
		    //output filename
		    transcriptChecked = transcripTextFile.replace(".txt", "_"+date+"_CHECKED.txt");
		    nbReadLines=0;
	    }
	    
	    //creation
	  	checkedFile = new File(transcriptChecked);
	  	try {
	  		//new buffer to write in
			outputFile = new BufferedWriter(new FileWriter(checkedFile, append));
			if(BuildConfig.DEBUG)Log.i("onCreate", "outputFile initialisé");
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	  	
	  	
		//reading imported file and processing on
	    try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(transcripTextFile)));
			// test on the lines read:
			// check that the file is not empty by ensuring that the phrase is not null (end of file reached)
			// check that the sentence is correctly formatted (no empty line and ' : ' delimited found)
			nb_total_lines = countNumberOfLines();
			String line;
			while((line = reader.readLine()) != null && (line.isEmpty() || line.split(" : ").length <= 1));
			if(line == null) throw new IOException("Empty text file");
			for(int countLine=0;countLine<nbReadLines;countLine++)
				line = reader.readLine();
			//split on line. group1=id, group2=transcription
			String[] splittedline = line.split(" : ");
			//group1
			transcriptID = (TextView) findViewById(R.id.transcription_id);
			transcriptID.setText(splittedline[0]);
			//group2
			String transcription = splittedline[1];
			
			TextView tv = (TextView) findViewById(R.id.check_total_phrase_number);
			tv.setText(" / " + nb_total_lines);
			
			TextView tv2 = (TextView) findViewById(R.id.check_phrase_number);
			tv2.setText("" + nbReadLines);
			
			progressBar.setProgress((int) (((float)nbReadLines)/(float) nb_total_lines* 100));

			//retrieve layout to put element in
			LinearLayout llparent = (LinearLayout) findViewById(R.id.ll_variant);
			llparent.removeAllViews();
			
			final float scale = getResources().getDisplayMetrics().density;
			if(BuildConfig.DEBUG)Log.i(TAG,"Scale: "+scale);
		    int padding_in_px = (int) (10 * scale + 0.5f);
			if(BuildConfig.DEBUG)Log.i(TAG,"Padding in px: "+padding_in_px);
		    /** TODO
		     * tsizepx matches for tablets but is too large for phones (applyDimension to 10 is better)
		     * find a way to fix it for phones AND for tablets
		     */
			int tsizepx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 25, getResources().getDisplayMetrics());
			if((getResources().getBoolean(R.bool.is_small_screen)))	tsizepx*=0.5;
			if(BuildConfig.DEBUG)Log.i(TAG,"Text size in px: "+tsizepx);
			//transcription + checkbox
			transcriptOK = new CheckBox(this);
			transcriptOK.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			transcriptOK.setPadding(padding_in_px, 0, 0, padding_in_px);
			transcriptOK.setTextSize(tsizepx);
			transcriptOK.setTypeface(Typeface.SERIF);
			transcriptOK.setText(transcription);
			llparent.addView(transcriptOK);
			if(BuildConfig.DEBUG)Log.i(TAG, "current line: " + nbReadLines);
		} catch (FileNotFoundException e1) {
			if(BuildConfig.DEBUG)Log.e(TAG,"transcript file could not be found: " + e1);
			Toast.makeText(this, R.string.an_error_occurred_the_transcript_file_could_not_be_found, Toast.LENGTH_LONG).show();
			this.finish();
		} catch (IOException e) {
			Toast.makeText(this, R.string.something_weird_happened, Toast.LENGTH_LONG).show();
			if(BuildConfig.DEBUG)Log.e(TAG,"No more sentences to display or an error occurred: " + e);
			this.finish();
		}
	    

	    fragment = (ListenFragment) getFragmentManager().findFragmentById(R.id.phrase_player);
	    
	    //player initialization
	    SimplePlayer player;
		try {
			transcriptFilename = transcripTextFile.substring(transcripTextFile.lastIndexOf("/") + 1);
			String audioFileFullPath = transcripTextFile.replace(transcriptFilename, transcriptID.getText().toString()+".wav");
			player = new SimplePlayer(new File(audioFileFullPath),sampleRate, true);
			fragment.setPlayer(player);
			if(BuildConfig.DEBUG)Log.d(TAG, "Get corresponding audio:" + dataPath+transcriptID.getText().toString()+".wav");
		} catch (IOException e) {
			if(BuildConfig.DEBUG)Log.d(TAG, "Try to reach: "+transcripTextFile.replace(transcriptFilename, transcriptID.getText().toString()+".wav"));
			Toast.makeText(this, R.string.the_audio_file_is_unobtainable_please_proceed_to_the_next_transcript, Toast.LENGTH_LONG).show();
			transcriptOK.setEnabled(false);
		}
	}
	
    /**
     * called to set progressionBar behavior
     * progressionBar is not modifiable by hand
     */
    private void initProgressBars() {
 	   progressBar = (InterleavedSeekBar) findViewById(R.id.check_seekbar_transcription);
 	   progressBar.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {
                int originalProgress;
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
				{
					if(fromUser) seekBar.setProgress(originalProgress);
                }
                public void onStopTrackingTouch(SeekBar _seekBar) {}
                public void onStartTrackingTouch(SeekBar _seekBar) {
                        originalProgress = progressBar.getProgress();
                }
            });
    }
	
    
    /**
     * called by constructor to set the number of lines in the file
     * @return the number of lines in good format in a txt file.
     * @throws IOException if the file cannot be read
     */
    public int countNumberOfLines() throws IOException {
 	   int nbLine = 0;
 	   BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(transcripTextFile)));
 	   for(String phrase;(phrase = bufferedReader.readLine()) != null;)
 		   if(!phrase.isEmpty() && phrase.split(" : ").length > 1)
 			   nbLine++;
 	   bufferedReader.close();
 	   return nbLine;
    }
    
    
	public void onNextClick(View _view) {
		if (saveFile()) {
		    try {
				String line;
				while((line = reader.readLine()) != null && (line.isEmpty() || line.split(" : ").length <= 1));
				if (line == null) { throw new IOException("Empty text file"); }
				nbReadLines++;
				//split on line. group1=id, group2=transcription
				String[] splittedline = line.split(" : ");
				//group1
				transcriptID = (TextView) findViewById(R.id.transcription_id);
				transcriptID.setText(splittedline[0]);
				//group2
				String transcription = splittedline[1];
				
				TextView tv = (TextView) findViewById(R.id.check_phrase_number);
				tv.setText("" + nbReadLines);
				
				progressBar.setProgress((int) (((float)nbReadLines)/(float) nb_total_lines* 100));
				LinearLayout llparent = (LinearLayout) findViewById(R.id.ll_variant);
				llparent.removeAllViews();
				
				final float scale = getResources().getDisplayMetrics().density;
			    int padding_in_px = (int) (10 * scale + 0.5f);
			    /** TODO
			     * tsizepx matches for tablets but is too large for phones (applyDimension to 10 is better)
			     * find a way to fix it for phones AND for tablets
			     */
			    int tsizepx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 25, getResources().getDisplayMetrics());
				if((getResources().getBoolean(R.bool.is_small_screen)))	tsizepx*=0.5;
				//transcription + checkbox
				transcriptOK = new CheckBox(this);
				transcriptOK.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				transcriptOK.setPadding(padding_in_px, 0, 0, padding_in_px);
				transcriptOK.setTextSize(tsizepx);
				//transcriptionOK.setTypeface(null, Typeface.ITALIC);
				transcriptOK.setTypeface(Typeface.SERIF);
				transcriptOK.setText(transcription);
				llparent.addView(transcriptOK);

				if(BuildConfig.DEBUG)Log.i(TAG, "current line: " + nbReadLines);
			} catch (IOException e) {
				e.printStackTrace();
				if(BuildConfig.DEBUG)Log.e(TAG,"No more transcript to display or an error occurred: " + e);
				try {
					outputFile.flush();
					outputFile.close();
					fragment.releasePlayer();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Toast.makeText(this, getString(R.string.no_more_transcript_to_display_file_saved_in)+transcriptChecked+".", Toast.LENGTH_LONG).show();
				this.finish();
			}
		    try {
		    	//player release
				fragment.releasePlayer();
				transcriptFilename = transcripTextFile.substring(transcripTextFile.lastIndexOf("/") + 1);
				String audioFileFullPath = transcripTextFile.replace(transcriptFilename, transcriptID.getText().toString()+".wav");
				SimplePlayer player = new SimplePlayer(new File(audioFileFullPath),sampleRate, true);
				//allocation of the player to the fragment
				fragment.setPlayer(player);
		    } catch (IOException e) {
		    	Toast.makeText(this, R.string.the_audio_file_is_unobtainable_please_proceed_to_the_next_transcript, Toast.LENGTH_LONG).show();
		    	transcriptOK.setEnabled(false);
		    }
		} else {
			Toast.makeText(this, R.string.going_to_next_transcript, Toast.LENGTH_LONG).show();
		}
	}
	
	public void saveCurrentState() {
		/*
		 * sauver nb lignes lues: Boolean nbReadLines
		 * + mode: Boolean checkMode
		 * + submode: Boolean checkTranscript
		 * + fichier import: String transcripTextFile
		 * + fichier export: String transcriptChecked
		 * + date: String date
		 */
		 date = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.FRANCE).format(new Date());
		 SharedPreferences.Editor ed = prefsUserSession.edit();
		 ed.putBoolean("active", true); //session activated
		 ed.putInt("currentLine", nbReadLines); //nb lines
		 ed.putString("progress", nbReadLines + " / " + nb_total_lines);
		 ed.putString("inputFile", transcripTextFile); //handled file
		 ed.putString("checkExportFile", transcriptChecked); //resulting file
		 ed.putString("date", date); //set date
		 ed.putString("mode",TAG); //set mode
//		 ed.putBoolean(getString(R.string.checkTranscript), true); //set submode
		 //save infos
		 ed.commit();
		 
		 Toast.makeText(this, getString(R.string.file_saved_in)+transcriptChecked+".", Toast.LENGTH_LONG).show();

		if(BuildConfig.DEBUG)Log.i(TAG, "progress: " + nbReadLines);
		if(BuildConfig.DEBUG)Log.i(TAG, "input file: " + transcripTextFile);
		if(BuildConfig.DEBUG)Log.i(TAG, "export file: " + transcriptChecked);
		if(BuildConfig.DEBUG)Log.i(TAG, "date: " + date);
		if(BuildConfig.DEBUG)Log.i(TAG, "mode: " + TAG);
	}
	
	public void onValidate(View _view) {
		if (saveFile()) {
			//if not completed session
			try {
				if (reader.readLine() != null) {
					//then offer to save or not the session
					interruptionCallback();
				} else {
					//else quit
					this.finish();
					Toast.makeText(this, getString(R.string.no_more_transcript_to_display_file_saved_in)+transcriptChecked+".", Toast.LENGTH_LONG).show();
				}
				outputFile.flush();
				outputFile.close();
				fragment.releasePlayer();
			} catch (IOException e) {e.printStackTrace();}
		}
	}
	
	
	private boolean saveFile() {
		/**
		 * Write transcription checked and its id in a new file
		 **/
		try {
			if(BuildConfig.DEBUG)Log.i("saveFile", "Writing on file");
			//if checked
			if(transcriptOK.isChecked())
				outputFile.write(transcriptID.getText()+" : "+transcriptOK.getText()+"\n");
		} catch (IOException e1) {e1.printStackTrace();}
		return true;
	}

	private void interruptionCallback() {
		
		if (nbReadLines == 0) {
			Toast.makeText(this, R.string.nothing_to_save, Toast.LENGTH_LONG).show();
			checkedFile.delete(); //nothing has been verified, so the output file can be deleted
			this.finish();
		}

		/*
		 * popup with 3 choices:
		 * - yes i want quit and save my progression
		 * - no i do not want to save
		 * - cancel (i did not want to push validate/back button)
		 */
		new AlertDialog.Builder(this)
				.setMessage((safeActivityTransitionMessage != null)?
						safeActivityTransitionMessage:
						getString(R.string.a_session_is_in_progress_do_you_want_to_save_the_progression))
				.setPositiveButton(R.string.yes,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (nbReadLines < nb_total_lines)
								saveCurrentState();
							if (isNewSession)
								CheckTranscription.this.finish();
							else{
								startActivity(new Intent(CheckTranscription.this, CheckMode.class));
								finish();
							}
						}
					})
				.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						/*
						 * TODO : pour le moment, si on choisit "No" ça n'enregistre pas la session mais ça écrit quand même fichier.
						 */
						if (isNewSession)
							CheckTranscription.this.finish();
						else {
							startActivity(new Intent(CheckTranscription.this, ModeSelection.class));
							finish();
						}
					}
				})
				.setNeutralButton(R.string.cancel, null)
				.show();
	}
	
	public void onBackPressed() {
		interruptionCallback();
	}
}
