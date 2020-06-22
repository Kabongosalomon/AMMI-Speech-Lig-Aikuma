package org.getalp.ligaikuma.lig_aikuma.ui;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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

public class CheckWordVariant extends AikumaActivity {
	
	public static final String TAG = "CheckWordVariant";
	//private UUID recordUUID;
	private BufferedReader reader;
	private BufferedWriter outputFile;
	private File checkedFile;
	private String variantTextFile;
	private int checkboxCount;
	private String variantchecked;
	private int nbReadLines;
	private String date;
	private SharedPreferences prefsUserSession;
	private InterleavedSeekBar progressBar;
	private int totalNumberOfExpressions;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.check_word_variant);
	    initProgressBars();
	    date = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.FRANCE).format(new Date());
	    prefsUserSession = getSharedPreferences("userSession", MODE_PRIVATE);
	    //case: session already exists
		boolean append;
		if (prefsUserSession.getBoolean("active", false)) {
	    	/*
	    	 * TODO rajouter une condition en && 
	    	 * soustraire date.now à date session : 
	    	 *  si <5min alors ed.clear
	    	 *  sinon continue;
	    	 */
	    	//retrieve selected file handled in the last session
	    	variantTextFile = prefsUserSession.getString("inputFile", null);
			if(BuildConfig.DEBUG)Log.i(TAG, "Selected import file: " + variantTextFile);
	    	//retrieve result file handled in the last session
	    	variantchecked = prefsUserSession.getString("checkExportFile",null);
			if(BuildConfig.DEBUG)Log.i(TAG, "Selected export file: " + variantchecked);
	    	//retrieve number of lines already handled in the last session
	    	nbReadLines = prefsUserSession.getInt("nbReadLine",0);
		    append = true;
		    
		    // clear the current stored session
		    prefsUserSession.edit().clear().apply();
		//case: no session
	    } else {
	    	variantTextFile = getIntent().getStringExtra(CheckMode.importFileName);
			if(BuildConfig.DEBUG)Log.i(TAG, "Selected file: " + variantTextFile);
		    append = false;
		    //output filename
		    variantchecked = variantTextFile.replace(".txt", "_"+date+"_CHECKED.txt");
		    nbReadLines=0;
	    }

	    //creation of output file
	  	checkedFile = new File(variantchecked);

	  	//new buffer to write in the output file
	    try {
			outputFile = new BufferedWriter(new FileWriter(checkedFile, append));
			if(BuildConfig.DEBUG)Log.i("onCreate", "outputFile initialisé");
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	  	
	  	//reading imported file and processing on
	    try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(variantTextFile)));
			// test on the lines read:
			// check that the file is not empty by ensuring that the line is not null (end of file reached)
			// check that the line is correctly formatted (no empty line and ', ' delimited found)
			String line;
			while((line = reader.readLine()) != null && (line.isEmpty() || line.split(", ").length <= 1));
			if(line == null)	throw new IOException("Empty text file");
			for (int countLine=0;countLine<nbReadLines;countLine++)
				line = reader.readLine();
			totalNumberOfExpressions = countNumberOfLines();

			//split on line. group1=word, group2=variant(s)
			String[] wordlist = line.split(", ");
			TextView lexeme = (TextView) findViewById(R.id.tv_verif_lexeme);
			lexeme.setText(wordlist[0]);
			
			TextView tv_total = (TextView) findViewById(R.id.check_total_number);
			tv_total.setText(" / " + totalNumberOfExpressions);

			TextView tv2 = (TextView) findViewById(R.id.check_line_number);
			tv2.setText("" + nbReadLines);
			
			progressBar.setProgress((int) (((float)nbReadLines)/(float) totalNumberOfExpressions* 100));
			
			LinearLayout llparent = (LinearLayout) findViewById(R.id.ll_variant);
			llparent.removeAllViews();
			
			String[] variantlist = wordlist[1].split(" ; ");
			CheckBox variant;
			checkboxCount=0;
		    int padding_in_px = (int) (10 * getResources().getDisplayMetrics().density + 0.5f);
			int tsizepx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 25, getResources().getDisplayMetrics());
			if((getResources().getBoolean(R.bool.is_small_screen)))	tsizepx*=0.5;
			if(BuildConfig.DEBUG)Log.i(TAG,"Text size in px: "+tsizepx);
			for (String element : variantlist){
				//variant + checkbox
				variant = new CheckBox(this);
				variant.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				variant.setPadding(padding_in_px, 0, 0, padding_in_px);
				variant.setTextSize(tsizepx);
				//variant.setTypeface(null, Typeface.ITALIC);
				variant.setTypeface(Typeface.SERIF);
				variant.setText(element);
				variant.setId(checkboxCount);
				llparent.addView(variant);
				checkboxCount+=1;
			}
			/*
			 * TODO 3 radiogroup avec des radio buttons plutôt qu'une... + associer un label (=variant)
			 */
			
		} catch (FileNotFoundException e1) {
			if(BuildConfig.DEBUG)Log.e(TAG,"text file could not be found: " + e1);
			Toast.makeText(this, R.string.an_error_occurred_the_text_file_could_not_be_found, Toast.LENGTH_LONG).show();
			this.finish();
		} catch (IOException e) {
			Toast.makeText(this, R.string.something_weird_happened_it_might_be_that_the_text_file_was_empty, Toast.LENGTH_LONG).show();
			if(BuildConfig.DEBUG)Log.e(TAG,"No more sentences to display or an error occurred: " + e);
			this.finish();
		}

	}
	
	
	
    /**
     * called to set progressionBar behavior
     * progressionBar is not modifiable by hand
     */
    private void initProgressBars() {
 	   progressBar = (InterleavedSeekBar) findViewById(R.id.seek_check_progression);
 	   progressBar.setOnSeekBarChangeListener(
                            new SeekBar.OnSeekBarChangeListener() {
                                    int originalProgress;
                                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                            if(fromUser)
                                                    seekBar.setProgress(originalProgress);
                                    }
                                    public void onStopTrackingTouch(SeekBar _seekBar) {}
                                    public void onStartTrackingTouch(SeekBar _seekBar) {
                                            originalProgress = progressBar.getProgress();
                                    }
                            });
    }

	public void onNextClick(View _view) {
		if (saveFile()) {
		    //recordUUID = UUID.randomUUID();
			try {
				String line;
				while((line = reader.readLine()) != null && (line.isEmpty() || line.split(", ").length <= 1));
				if (line == null) throw new IOException("Empty text file");
				nbReadLines++;
				//split on line. group1=word, group2=variant(s)
				String[] wordlist = line.split(", ");
				TextView lexeme = (TextView) findViewById(R.id.tv_verif_lexeme);
				lexeme.setText(wordlist[0]);
				
				TextView tv = (TextView) findViewById(R.id.check_line_number);
				tv.setText("" + nbReadLines);
				
				progressBar.setProgress((int) (((float)nbReadLines)/(float) totalNumberOfExpressions* 100));
				
				LinearLayout llparent = (LinearLayout) findViewById(R.id.ll_variant);
				llparent.removeAllViews();
				
				String[] variantlist = wordlist[1].split(" ; ");
				CheckBox variant;
				checkboxCount=0;
			    int padding_in_px = (int) (10 * getResources().getDisplayMetrics().density + 0.5f);
			    /** TODO
			     * tsizepx matches for tablets but is too large for phones (applyDimension to 10 is better)
			     * find a way to fix it for phones AND for tablets
			     */
			    int tsizepx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 25, getResources().getDisplayMetrics());
				if((getResources().getBoolean(R.bool.is_small_screen)))	tsizepx*=0.5;
				for (String element : variantlist){
					//variant + checkbox
					variant = new CheckBox(this);
					variant.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
					variant.setPadding(padding_in_px, 0, 0, padding_in_px);
					variant.setTextSize(tsizepx);
					//variant.setTypeface(null, Typeface.ITALIC);
					variant.setTypeface(Typeface.SERIF);
					variant.setText(element);
					variant.setId(checkboxCount);
					llparent.addView(variant);
					checkboxCount+=1;
				}
				/*
				 * TODO si rien n'est coché, désactiver le bouton next
				 */
				
			} catch (IOException e) {
				if(BuildConfig.DEBUG)Log.e(TAG,"No more words to display or an error occurred: " + e);
				try {
					outputFile.flush();
					outputFile.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Toast.makeText(this, getString(R.string.no_more_words_to_display_file_saved_in)+variantchecked+".", Toast.LENGTH_LONG).show();
				this.finish();
			}
		}
	}
	
   /**
    * called by constructor to set the number of lines in the file
    * @return the number of lines in good format in a txt file.
    * @throws IOException if the file cannot be read
    */
   public int countNumberOfLines() throws IOException {
	   int nbLine = 0;
	   BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(variantTextFile)));
	   for(String phrase;(phrase = bufferedReader.readLine()) != null;)
		   if(!phrase.isEmpty() && phrase.split(", ").length > 1)
			   nbLine++;
	   return nbLine;
   }
	
	public void onValidate(View _view) {

		if(!saveFile()) return;
			
		//si fin de fichier (buffer=vide) alors finish
		if (nbReadLines == 0) {
			Toast.makeText(this, R.string.nothing_to_save, Toast.LENGTH_LONG).show();
			checkedFile.delete();
			this.finish();
			return;
		}
		try {
			outputFile.flush();
			outputFile.close();
			Toast.makeText(this, R.string.file_saved_in+variantchecked+".", Toast.LENGTH_LONG).show();
			if(BuildConfig.DEBUG)Log.d(TAG, "Output file path: "+ variantchecked);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		boolean wordvariant = false;
		if(BuildConfig.DEBUG)Log.i(TAG, "boolean value : "+ wordvariant);

		//si fichier non terminé alors sauver session
		try {
			//if (reader.readLine() != null) {
			String line;
			while ((line = reader.readLine()) != null && (line.isEmpty() || line.split(", ").length <= 1));
			if (line == null) { throw new IOException("Empty text file"); }

			/*
			 * sauver nb lignes lues: Boolean nbReadLines
			 * + mode: Boolean checkMode
			 * + submode: Boolean checkVariant
			 * + fichier import: String variantTextFile
			 * + fichier export: String variantchecked
			 * + date: String date
			 */
			//Block détecté comme toujours à true
			if (nbReadLines < totalNumberOfExpressions || line != null) {
				 Toast.makeText(this, R.string.the_session_has_been_saved, Toast.LENGTH_SHORT).show();
				 date = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.FRANCE).format(new Date());
				 SharedPreferences.Editor ed = prefsUserSession.edit();
				 ed.putBoolean("active", true); //session activated
				 ed.putInt("nbReadLine", nbReadLines); //nb lines
				 ed.putString("progress", nbReadLines + "/" + totalNumberOfExpressions);
				 ed.putString("inputFile", variantTextFile); //handled file
				 ed.putString("checkExportFile", variantchecked); //resulting file
				 ed.putString("date", date); //set date
				 ed.putString("mode",TAG); //set mode
//					 ed.putBoolean(getString(R.string.checkVariant), true); //set submode
				 //save infos
				 ed.apply();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.finish();
	}
	
	private boolean saveFile() {
		/*
		 * Write word with its variant(s) checked in a new file
		 */
		/*
		 * TODO si les radiogroup ne sont pas tous coché : 
		 * -> afficher un Toast "toutes les cases n'ont pas été cochées"
		 * -> return false
		 */
	
		try {
			if(BuildConfig.DEBUG)Log.i("saveFile", "Writing on an existing file");
			//retrieve the word from the TextView
			outputFile.write(((TextView) findViewById(R.id.tv_verif_lexeme)).getText().toString());
			//retrieve variant checked
			for (int i=0; i<checkboxCount; i++) {
				//if checked
				CheckBox checkbox_x = (CheckBox) findViewById(i);
				if (checkbox_x.isChecked()) {
					/*
					 * TODO modifier pour prendre en compte le radiogroup
					 */
					if(BuildConfig.DEBUG)Log.i("CHECKED", checkbox_x.toString());
					outputFile.write(", "+checkbox_x.getText());
				}
			}
			outputFile.write("\n");
		  } catch (IOException e1) {
			e1.printStackTrace();
		  }
		  return true;
	}
	
	public void onBackPressed() {
		onValidate(null);
	}
}
