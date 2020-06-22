package org.getalp.ligaikuma.lig_aikuma.ui;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.ArrayUtils;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;
import org.getalp.ligaikuma.lig_aikuma.util.FileIO;
import org.getalp.ligaikuma.lig_aikuma.util.ShareTool;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShareFile extends AikumaActivity
{
    public static final String TAG = "ShareFile";

    private static ArrayList<String> _selectedFiles;
    private static ArrayList<String> _nav_list;
    private static ArrayList<CheckBox> _checkbox_list;
    private CheckBox _global_check;
    private static FileAdapter _adapteur;
    private static String _current_folder_position;
    private static boolean _is_init = false;

    private TextView _path_view;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_file);

        // Force portrait on phone
        if(getResources().getBoolean(R.bool.is_small_screen))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if(!_is_init) {
            _selectedFiles = new ArrayList<>();
            _nav_list = new ArrayList<>();
            _current_folder_position = FileIO.getAppRootPath() + "/recordings/";
            _adapteur = new FileAdapter(this, _nav_list);
            _checkbox_list = new ArrayList<>();
            _is_init=true;
        }
        _global_check = (CheckBox) findViewById(R.id.select_all_files);
        _global_check.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if(((CheckBox) v).isChecked()){
                    for(String s : _nav_list)
                        if(!new File(_current_folder_position+s).isDirectory()&&!_selectedFiles.contains(_current_folder_position+s))
                            _selectedFiles.add(_current_folder_position + s);
                } else
                    for(String s2 : _nav_list)
                        if(_selectedFiles.contains(_current_folder_position+s2))
                            _selectedFiles.remove(_current_folder_position+s2);
                _adapteur.notifyDataSetChanged();
            }});
        _checkbox_list.clear();
        ((ListView) findViewById(R.id.list_share)).setAdapter(_adapteur);
        _path_view = (TextView) findViewById(R.id.path_view_share);
        _path_view.setText(_current_folder_position);
        loadFileList(_current_folder_position);
    }

    /** Load all wav file and folder
     *
     * @param folder Path of directory to load all files
     */
    public void loadFileList(String folder)
    {
        File dir = new File(folder);
        if(!dir.exists())   return;
        String[] l = dir.list(new FilenameFilter() {
                @Override public boolean accept(File dir, String filename) {
                return filename.contains(".wav") || filename.contains(".WAV") || new File(dir, filename).isDirectory();}});
        if(l.length==0)
        {
            Toast.makeText(ShareFile.this, R.string.this_file_does_not_contain_any_audio_file, Toast.LENGTH_SHORT).show();
            return;
        }
        _current_folder_position = folder;
        _checkbox_list.clear();
        _nav_list.clear();
        ArrayUtils.reverse(l);
        Collections.addAll(_nav_list, l);
        _adapteur.notifyDataSetChanged();
        _path_view.setText(folder);
    }

    /** If item is on selected list it's remove, add it otherwise.
     *
     * @param file File to add or remove
     */
    public void add_remove_selected_item(String file)
    {
        if(!file.contains(".wav") || !new File(file).exists()) return;
        if(_selectedFiles.contains(file))   _selectedFiles.remove(file);
        else                                _selectedFiles.add(file);
    }

    public void onShareValidate(View _view)
    {
        ShareTool.Share(this, _selectedFiles);
    }

    public void onBackPressed(View v) {
        this.finish();
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
        refreshGlobalCheckValue();
    }

    /** Check the global checkbox must be checked or unchecked
     */
    public void refreshGlobalCheckValue()
    {
        if(_checkbox_list.size()==0) {
            _global_check.setChecked(false);
            return;
        }
        for(CheckBox c : _checkbox_list)
            if(!c.isChecked())
            {
                _global_check.setChecked(false);
                return;
            }
        _global_check.setChecked(true);
    }

    //      ######################################
    //      #           CUSTOM ADAPTER           #
    //      ######################################

    private class FileAdapter extends ArrayAdapter<String>
    {
        public static final String TAG = "FileAdapter";

        FileAdapter(Context context, List<String> objects) {super(context, 0, objects);}

        @NonNull
        @Override
        public View getView(final int position, View itemView, @NonNull ViewGroup parent)
        {
            ShareHolder sh;
            if(itemView == null) {
                itemView = LayoutInflater.from(getContext()).inflate(R.layout.view_item_share, parent, false);
                sh = new ShareHolder();
                sh.name = (TextView) itemView.findViewById(R.id.file_name);
                sh.selected = (CheckBox) itemView.findViewById(R.id.select_file);
                sh.icon = (ImageView) itemView.findViewById(R.id.img_fold_file);
                itemView.setTag(sh);
            }
            else
                sh = (ShareHolder) itemView.getTag();

            String name_f = getItem(position);
            sh.name.setText(name_f);
            name_f = _current_folder_position+name_f;

            // Checkbox
            sh.selected.setOnCheckedChangeListener(null);
            Log.d(TAG, _selectedFiles.toString());
            sh.selected.setChecked(_selectedFiles.contains(name_f));
            sh.selected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                    add_remove_selected_item(_current_folder_position+_nav_list.get(position));
                    if(!isChecked) ((CheckBox)findViewById(R.id.select_all_files)).setChecked(false);
                    refreshGlobalCheckValue();
                }});

            // Icon, if it's file, changer folder icon by wav file icon
            if((!new File(name_f).isDirectory()))
            {
                sh.icon.setImageResource(R.drawable.wav_file_icon);
                sh.selected.setVisibility(View.VISIBLE);
                _checkbox_list.add(sh.selected);
            }
            else
            {
                sh.icon.setImageResource(R.drawable.folder_close_icon);
                sh.selected.setVisibility(View.INVISIBLE);
            }

            // Go to folder or select item
            itemView.findViewById(R.id.select_item_nav).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v)
                {
                    String nf = _current_folder_position+_nav_list.get(position);
                    if(new File(nf).isDirectory()) {
                        loadFileList(nf + "/");
                        refreshGlobalCheckValue();
                    }
                    else
                    {
                        CheckBox cb = (CheckBox) ((View) v.getParent()).findViewById(R.id.select_file);
                        cb.setChecked(!cb.isChecked());
                    }
                }
            });
            return itemView;
        }
    }
    private static class ShareHolder
    {
        TextView name;
        CheckBox selected;
        ImageView icon;
    }
}