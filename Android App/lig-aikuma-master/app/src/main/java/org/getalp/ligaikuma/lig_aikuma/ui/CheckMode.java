package org.getalp.ligaikuma.lig_aikuma.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;
import org.getalp.ligaikuma.lig_aikuma.util.FileIO;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class CheckMode extends AikumaActivity{
	public static final String TAG = "CheckMode";
	public static final String importFileName = "checkFileName";
	private File mPath;
	private String[] mFileList;
	private String mChosenFile;
	private String fileType;
	private String title;
	private boolean wordvariant;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.check_mode);
	    wordvariant = false;
	}
	
	public void onImportClick(View _view) {
		switch (_view.getId()) {
		case R.id.button_checkvariant:
			wordvariant = true;
			if(BuildConfig.DEBUG)Log.i(TAG, "Import word with variant file; view id: " + _view.getId());
			mPath = Environment.getExternalStorageDirectory();
			fileType = ".txt";
			title = getString(R.string.import_word_with_variant_text_file);
			importContent();
			break;
		case R.id.button_checktranscript:
			wordvariant = false;
			if(BuildConfig.DEBUG)Log.i(TAG, "Import transcription file; view id: " + _view.getId());
			mPath = Environment.getExternalStorageDirectory();
			fileType = ".txt";
			title = getString(R.string.import_transcription_file);
			importContent();
			break;
		}
		
	}
	
	private void importContent() {
		loadFileList(mPath,fileType);
		showAudioFilebrowserDialog();
	}

	/**
	 * Loads the list of files in the specified directory into mFileList
	 * (copied from RespeakingSelection.java)
	 *
	 * @param	dir	The directory to scan.
	 * @param	fileType	The type of file (other than directories) to look
	 * for.
	 */
	private void loadFileList(File dir, final String fileType) {
		mFileList = (!dir.exists())?
				new String[0]:
				mPath.list(new FilenameFilter() {
					public boolean accept(File dir, String filename) {
						return filename.contains(fileType) || new File(dir, filename).isDirectory();
					}});
	}

	/**
	 * Presents the dialog for choosing audio files to the user.
	 * Copied from RespeakingSelection.java
	 */
	private void showAudioFilebrowserDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(CheckMode.this);
		builder.setTitle(title);
		if(mFileList == null) {
			if(BuildConfig.DEBUG)Log.e(TAG, "import file - Showing file picker before loading the file list");
			builder.create();
		}
		builder.setItems(mFileList, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				mChosenFile = mFileList[which];
				if(BuildConfig.DEBUG)Log.i(TAG, "mChosenFile: " + mChosenFile);
				mPath = new File(mPath, mChosenFile);
				if(mPath.isDirectory()) {
					loadFileList(mPath, fileType);
					showAudioFilebrowserDialog();
				} else {
					if(BuildConfig.DEBUG)Log.i(TAG, "boolean value : "+wordvariant);
					if (wordvariant) {
						if(BuildConfig.DEBUG)Log.i(TAG, "Go to CheckWordVariant.class, selected file: " + mPath.getAbsolutePath());
						if(!isValideFile(mPath,", ", R.string.the_file_is_not_valid_for_performing_a_word_check_variants))
							return;
						Intent intent = new Intent(CheckMode.this, CheckWordVariant.class);
						intent.putExtra(importFileName, mPath.getAbsolutePath());
						startActivity(intent);
					} else {
						if(BuildConfig.DEBUG)Log.i(TAG, "Go to CheckTranscription.class, selected file: " + mPath.getAbsolutePath());
						if(!isValideFile(mPath," : ", R.string.the_file_is_not_valid_for_performing_a_transcription_check))
							return;
						Intent intent = new Intent(CheckMode.this, CheckTranscription.class);
						intent.putExtra(importFileName, mPath.getAbsolutePath());
						startActivity(intent);
					}
				}
			}});
		builder.show();
	}

	public void onBackPressed(View v) {
		if(BuildConfig.DEBUG)Log.i(TAG, "boolean value : "+wordvariant);
		this.finish();
	}

	boolean isValideFile(File file, String separator, int id_bad_message)
	{
		try {
			for(String s : FileIO.read(file).split("\n"))
				if(!s.isEmpty()&&!s.contains(separator))
				{
					Toast.makeText(this, id_bad_message,Toast.LENGTH_LONG).show();
					return false;
				}
		} catch (IOException e)
		{
			Toast.makeText(this, id_bad_message,Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
}
