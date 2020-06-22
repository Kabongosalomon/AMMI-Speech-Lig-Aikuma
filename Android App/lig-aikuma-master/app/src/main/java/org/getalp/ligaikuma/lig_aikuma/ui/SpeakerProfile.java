package org.getalp.ligaikuma.lig_aikuma.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;

import java.util.ArrayList;
import java.util.List;

import static org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile.splitKey1;


public class SpeakerProfile extends AikumaActivity
{
    public static final String TAG = "SpeakerProfile";

    private ArrayList<String> _listValue;
    private ProfileAdapter _adapteurKey;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speaker);

        // Construct list and adapteur from keys values
        String[] keys = org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile.getAllKeys(this);
        _listValue = new ArrayList<>();
        if(keys != null)
            for(String key : keys)
                if(key.split(splitKey1).length==7)
                    _listValue.add(key);
        _adapteurKey = new ProfileAdapter(this, _listValue);

        // Set ListView controles
        ((ListView) findViewById(R.id.list_key)).setAdapter(_adapteurKey);
    }


    /** Show a dialog to ask a user for deleting speaker profile
     *
     * @param position position of deleted speaker profile
     */
    public static int _position=0;
    public void showSuppressDialog(int position) {
        _position = position;
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.do_you_want_to_delete_this_profile))
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Delete on shardPref
                                org.getalp.ligaikuma.lig_aikuma.model.SpeakerProfile.removeProfile(SpeakerProfile.this, _listValue.get(_position));

                                //Delete on listView
                                _listValue.remove(SpeakerProfile._position);
                                _adapteurKey.notifyDataSetChanged();
                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // CUSTOM ADAPTEUR
    private class ProfileAdapter extends ArrayAdapter<String>
    {
        ProfileAdapter(Context context, List<String> objects) {super(context, 0, objects);}

        /** build a new view for array list
         *
         * @param position Position of item in memory list
         * @param itemView View to build
         * @param parent View list
         * @return Builded View
         */
        @NonNull @Override
        public View getView(final int position, View itemView, @NonNull ViewGroup parent)
        {
            if(itemView == null)
                itemView = LayoutInflater.from(getContext()).inflate(R.layout.view_profil_list, parent, false);
            String[] key = getItem(position).split(splitKey1);
            ((TextView) itemView.findViewById(R.id.item_name)).setText(key[0]);
            String t = key[1]+" : "+key[2]+" : "+key[3]+" : "+key[4]+" : "+key[5]+" : ";
            switch(Integer.parseInt(key[6]))
            {
                case RecordingMetadata.GENDER_MALE:          t+=getString(R.string.male);    break;
                case RecordingMetadata.GENDER_FEMALE:        t+=getString(R.string.female);  break;
                default:                                        t+=getString(R.string.unspecified);
            }
            ((TextView) itemView.findViewById(R.id.item_lang)).setText(t);
            itemView.findViewById(R.id.delete_profil_button).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    showSuppressDialog(position);
                }});

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.putExtra("profileKey", _listValue.get(position));
                    setResult(RESULT_OK, intent);
                    SpeakerProfile.this.finish();
                }
            });
            return itemView;
        }
    }

    public void onBackPressed(View prev) {
        this.finish();
    }
}
