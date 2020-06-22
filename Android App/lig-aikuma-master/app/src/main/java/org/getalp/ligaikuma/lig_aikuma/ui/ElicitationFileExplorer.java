package org.getalp.ligaikuma.lig_aikuma.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.ArrayUtils;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;
import org.getalp.ligaikuma.lig_aikuma.util.FileIO;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.getalp.ligaikuma.lig_aikuma.ui.ElicitationMode.ELICITATION;
import static org.getalp.ligaikuma.lig_aikuma.ui.ElicitationMode.importFileName;


public class ElicitationFileExplorer extends AikumaActivity
{
    public static final String TAG = "ElicitationFileExplorer";

    private static String _current_folder_position;
    private static boolean _is_init = false;
    private static ElicitationFileExplorer.FileAdapter _adapteur;
    private static ArrayList<String> _nav_list;
    private static int _mode;
    private static String _title;
    private static int _msg_empty;  // R ref for empty file.

    public static String[] FILE_VIDEO = new String[]{".mp4",".avi",".3gp",".webm",".mkv",".MP4",".AVI",".3GP",".WEBM",".MKV"};
    public static String[] FILE_IMAGE = new String[]{".jpg",".jpeg",".bmp",".gif",".png",".webp",".JPG",".JPEG",".BMP",".GIF",".PNG",".WEBP"};

    private TextView _path_view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.elicitation_file_search);

        _mode = getIntent().getIntExtra("mode",ElicitationMode.TEXT_MODE);

        // Force portrait on phone
        if(getResources().getBoolean(R.bool.is_small_screen))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if(!_is_init)
        {
            _nav_list = new ArrayList<>();
            _adapteur = new ElicitationFileExplorer.FileAdapter(this, _nav_list);
            _is_init = true;
            _current_folder_position = Environment.getExternalStorageDirectory().toString()+"/";
            _mode = getIntent().getIntExtra("mode",ElicitationMode.TEXT_MODE);

            switch(_mode)
            {
                case ElicitationMode.TEXT_MODE:
                    _title = getString(R.string.elicitText);
                    _msg_empty = R.string.this_file_does_not_contain_any_text_file;
                    break;
                case ElicitationMode.IMAGE_MODE:
                    _title = getString(R.string.elicitImage);
                    _msg_empty = R.string.this_file_does_not_contain_any_image_file;
                    break;
                case ElicitationMode.VIDEO_MODE:
                    _title = getString(R.string.elicitVideo);
                    _msg_empty = R.string.this_file_does_not_contain_any_video_file;
                    break;
            }
        }

        ((TextView) findViewById(R.id.maintextViewElicit1)).setText(_title);
        _path_view = (TextView) findViewById(R.id.path_view);
        ((ListView) findViewById(R.id.elicit_file_list)).setAdapter(_adapteur);
        loadFileList(_current_folder_position);
    }

    /**
     * Loads the list of files in the specified directory into mFileList
     *
     * @param	folder	The directory to go
     */
    private void loadFileList(String folder)
    {
        File dir = new File(folder);
        if(!dir.exists())   return;
        String[] l = dir.list((_mode == ElicitationMode.TEXT_MODE)?
                new FilenameFilter() {
            @Override public boolean accept(File dir, String filename) {
                return filename.contains(".txt") || filename.contains(".TXT") || new File(dir, filename).isDirectory();}}:
                new FilenameFilter() {
            @Override public boolean accept(File dir, String filename) {
                return new File(dir, filename).isDirectory();}});
        if(l.length==0)
        {
            Toast.makeText(ElicitationFileExplorer.this, _msg_empty, Toast.LENGTH_LONG).show();
            return;
        }
        _current_folder_position = folder;
        _nav_list.clear();
        ArrayUtils.reverse(l);
        Collections.addAll(_nav_list, l);
        _adapteur.notifyDataSetChanged();
        _path_view.setText(folder);
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

    /** Verify if it's valid file and go on next step
     *
     * @param filePath Open file, it's directory or .txt file
     */
    public void selectFile(File filePath)
    {
        if(_mode == ElicitationMode.TEXT_MODE && !isValideElicitFile(filePath))
        {
            Toast.makeText(this, R.string.this_file_is_not_valid_for_an_elicitation, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, RecordingMetadata.class);
        intent.putExtra(importFileName, filePath.getAbsolutePath());
        intent.putExtra(ELICITATION, true);
        intent.putExtra("selectedFileType", _mode);
        startActivity(intent);
    }

    /**  Verify if text file is valid for text elicitation
     * @param textFile Verified text file
     * @return True if it's valid text file, false otherwise
     */
    public boolean isValideElicitFile(File textFile)
    {
        try {
            for(String s : FileIO.read(textFile).split("\n"))
                if(!s.isEmpty()&&!s.contains("##"))
                    return false;
        } catch (IOException e) {return false;}
        return true;
    }

    private class FileAdapter extends ArrayAdapter<String>
    {
        public static final String TAG = "FileAdapter";

        FileAdapter(Context context, List<String> objects) {super(context, 0, objects);}

        @NonNull
        @Override
        public View getView(final int position, View itemView, @NonNull ViewGroup parent)
        {
            ElicitationFileExplorer.SearchHolder sh;
            if(itemView == null) {
                itemView = LayoutInflater.from(getContext()).inflate(R.layout.content_dialog_selected_folder_frag, parent, false);
                sh = new ElicitationFileExplorer.SearchHolder();
                sh.name = (TextView) itemView.findViewById(R.id.textFileName);
                sh.icon = (ImageView) itemView.findViewById(R.id.image_file_type);
                itemView.setTag(sh);
            }
            else
                sh = (ElicitationFileExplorer.SearchHolder) itemView.getTag();

            String name_f = getItem(position);
            sh.name.setText(name_f);
            name_f = _current_folder_position+name_f;

            File f = new File(name_f);
            if(f.isDirectory())
            {
                ((ImageView) itemView.findViewById(R.id.image_file_type)).setImageResource(R.drawable.folder_close_icon);
                switch(_mode)
                {
                    case ElicitationMode.IMAGE_MODE:
                        if(containsFile(f, FILE_IMAGE))
                        {
                            ImageView iv = (ImageView) itemView.findViewById(R.id.image_folder_type);
                            iv.setImageResource(R.drawable.type_folder_image);
                            iv.setVisibility(ImageView.VISIBLE);
                        }
                        else
                            itemView.findViewById(R.id.image_folder_type).setVisibility(ImageView.INVISIBLE);
                        break;
                    case ElicitationMode.VIDEO_MODE:
                        if(containsFile(f, FILE_VIDEO))
                        {
                            ImageView iv = (ImageView) itemView.findViewById(R.id.image_folder_type);
                            iv.setImageResource(R.drawable.type_folder_video);
                            iv.setVisibility(ImageView.VISIBLE);
                        }
                        else
                            itemView.findViewById(R.id.image_folder_type).setVisibility(ImageView.INVISIBLE);
                        break;
                }
            }
            else if(name_f.contains(".txt"))
                ((ImageView) itemView.findViewById(R.id.image_file_type)).setImageResource(R.drawable.text_file_icon);

            // Go to folder or select item
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v)
                {
                    String nf = _current_folder_position+_nav_list.get(position);
                    File f = new File(nf);
                    if(f.isDirectory())
                    {
                        if(_mode==ElicitationMode.IMAGE_MODE && containsFile(f,FILE_IMAGE) ||
                                _mode==ElicitationMode.VIDEO_MODE && containsFile(f,FILE_VIDEO))
                            selectFile(f);
                        else
                            loadFileList(nf+"/");
                    }
                    else if(nf.contains(".txt"))
                        selectFile(f);
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

    /** Verify if folder constain one or more file with fileTypes extension
     * @param f Folder with file
     * @param fileTypes Array with extension to verify
     * @return True if contain file with fileTypes extension, false otherwise.
     */
    public static boolean containsFile(File f, String[] fileTypes)
    {
        String[] l = f.list();
        if(!f.isDirectory()||l.length == 0)
            return false;
        for(String file : l)
            for(String t : fileTypes)
                if(file.contains(t))
                    return true;
        return false;
    }

    public void onBackPressed(View v) {
        _is_init=false;
        this.finish();
    }

    // Phisical button back
    public void onBackPressed()
    {
        _is_init=false;
        this.finish();}
}