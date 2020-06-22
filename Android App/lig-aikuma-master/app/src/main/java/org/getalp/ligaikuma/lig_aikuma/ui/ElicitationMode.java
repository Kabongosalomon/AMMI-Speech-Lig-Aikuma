package org.getalp.ligaikuma.lig_aikuma.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;

public class ElicitationMode extends AikumaActivity{

    public static final String TAG = "ElicitationMode";
    public static final String ELICITATION = "elicitation";
    public static final String importFileName = "elicitationFileName";

    public static final int TEXT_MODE = 0;
    public static final int IMAGE_MODE = 1;
    public static final int VIDEO_MODE = 2;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.elicitation_mode);
    }

    public void onImportClick(View _view)
    {
        Intent intent = new Intent(this, ElicitationFileExplorer.class);
        switch(_view.getId())
        {
            case R.id.button_byText:
                if(BuildConfig.DEBUG)Log.i(TAG, "Import text; view id: " + _view.getId());
                intent.putExtra("mode", TEXT_MODE);
                break;
            case R.id.button_byImage:
                if(BuildConfig.DEBUG)Log.i(TAG, "Import image; view id: " + _view.getId());
                intent.putExtra("mode", IMAGE_MODE);
                break;
            case R.id.button_byVideo:
                if(BuildConfig.DEBUG)Log.i(TAG, "Import video; view id: " + _view.getId());
                intent.putExtra("mode", VIDEO_MODE);
                break;
        }
        startActivity(intent);
    }

    public void onBackPressed(View v) {
        this.finish();
    }
}
