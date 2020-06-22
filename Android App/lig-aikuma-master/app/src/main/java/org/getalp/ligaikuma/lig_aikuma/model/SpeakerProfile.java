package org.getalp.ligaikuma.lig_aikuma.model;

import android.content.Context;
import android.content.SharedPreferences;

import org.apache.commons.lang3.ArrayUtils;
import org.getalp.ligaikuma.lig_aikuma.ui.RecordingMetadata;

import java.util.ArrayList;

public class SpeakerProfile
{
    public static String TAG = "SpeakerProfile";

    public static String splitKey1="§!§";
    public static String splitKey2="§:§";

    private String _name;
    private int _birthYear;
    private int _gender;

    private Language _recordLang;
    private Language _motherTongue;
    private ArrayList<Language> _otherLanguages;

    private String _region;
    private String _note;
    private boolean _signature;

    private static SpeakerProfile _lastSpeaker = null;

    /** Build profile with all informations
     *
     * @param name speaker name
     * @param birthYear speaker birth year
     * @param gender gender of speaker
     * @param recordLang language of record
     * @param motherTongue mother tongue of speaker
     * @param otherLanguages other tongue of speaker
     * @param region region of record
     * @param note free field for annotation
     */
    public SpeakerProfile(String name, int birthYear, int gender, Language recordLang, Language motherTongue,
                          ArrayList<Language> otherLanguages, String region, String note, boolean signature)
    {
        update(name, birthYear, gender, recordLang, motherTongue, otherLanguages, region, note);
        _signature = signature;
    }

    /** Build new profile with key and context
     *
     * @param c Current context to access to SharedPreferences
     * @param key key of profile
     */
    public SpeakerProfile(Context c, String key)
    {
        SharedPreferences sp = c.getSharedPreferences(key, Context.MODE_PRIVATE);

        String[] tmpL = sp.getString("recordLang"," : ").split(" : ");
        Language recordLang = new Language(tmpL[0],tmpL[1]);

        tmpL = sp.getString("motherTongue"," : ").split(" : ");
        Language motherTongue = new Language(tmpL[0],tmpL[1]);

        ArrayList<Language> otherLanguages = new ArrayList<>();
        String t = sp.getString("otherLanguages", null);
        if(t != null)
            for(String tmpS : t.split(splitKey2))
            {
                tmpL = tmpS.split(" : ");
                otherLanguages.add(new Language(tmpL[0],tmpL[1]));
            }

        update(sp.getString("name",""), sp.getInt("birdthYear",0), sp.getInt("gender",2),
        recordLang, motherTongue, otherLanguages, sp.getString("region",""), sp.getString("note",""));

        _signature = sp.getBoolean("signature",false);
    }

    /** Update a speaker profile with all information
     *
     * @param name speaker name
     * @param birthYear speaker birth year
     * @param gender gender of speaker
     * @param recordLang language of record
     * @param motherTongue mother tongue of speaker
     * @param otherLanguages other tongue of speaker
     * @param region region of record
     * @param note free field for annotation
     */
    public void update(String name, int birthYear, int gender, Language recordLang, Language motherTongue,
                        ArrayList<Language> otherLanguages, String region, String note)
    {
        _name = (name!=null)?name:"";
        _birthYear = birthYear;
        _gender = gender;
        _recordLang = (recordLang!=null)?recordLang:new Language(" "," ");
        _motherTongue = (motherTongue!=null)?motherTongue:new Language(" "," ");
        _otherLanguages = (otherLanguages!=null)?otherLanguages:new ArrayList<Language>();
        _region = (region!=null)?region:"";
        _note = (note!=null)?note:"";
        _lastSpeaker = this;
    }

    /**
     *
     * @return Key used for SharedPreferences
     */
    public String getKey()
    {
        return _name+splitKey1+_recordLang.getCode()+splitKey1+_motherTongue.getCode()+splitKey1+_name+splitKey1+_region+splitKey1+_birthYear+splitKey1+String.valueOf(_gender);
    }

    /** Checks if key exists in SharedPreferences
     *
     * @param c Current context to access to SharedPreferences
     * @param key key of profile
     * @return true if key exist, false otherwise
     */
    public boolean keyExist(Context c, String key)
    {
        return c.getSharedPreferences("_master_speaker_keys", Context.MODE_PRIVATE).getString("_all_keys","").contains(key);
    }

    /** save current profile on SharedPreferences
     *
     * @param c Current context to access to SharedPreferences
     */
    public SpeakerProfile saveOnSharedPreferences(Context c)
    {
        String key = getKey();
        if(keyExist(c, key)) removeProfile(c, key);

        SharedPreferences.Editor edit = c.getSharedPreferences(key, Context.MODE_PRIVATE).edit();
        edit.putString("name", _name)
                .putInt("birdthYear", _birthYear)
                .putInt("gender", _gender)
                .putString("recordLang", _recordLang.toString())
                .putString("region", _region)
                .putString("note", _note)
                .putBoolean("signature", _signature);
        if(_motherTongue!=null)   edit.putString("motherTongue", _motherTongue.toString());
        String l="";
        for(int i=0;i<_otherLanguages.size();i++)
        {
            if(i!=0)    l+=splitKey2;
            l+=_otherLanguages.get(i).toString();
        }
        edit.putString("otherLanguages", l).apply();

        // Save on key register
        SharedPreferences sp = c.getSharedPreferences("_master_speaker_keys", Context.MODE_PRIVATE);
        String s = sp.getString("_all_keys","");
        if(!s.isEmpty())    s+=splitKey2;
        sp.edit().putString("_all_keys",s+getKey()).apply();

        return this;
    }

    /**
     *
     * @param c Current context to access to SharedPreferences
     * @param key key of profile
     */
    public static void removeProfile(Context c, String key)
    {
        // Remove data of preferance
        c.getSharedPreferences(key, Context.MODE_PRIVATE).edit().clear().apply();

        // Remove key
        SharedPreferences sp = c.getSharedPreferences("_master_speaker_keys", Context.MODE_PRIVATE);
        String t = sp.getString("_all_keys","").replace(key,"");
        t=t.replace(splitKey2+splitKey2,splitKey2);
        if(t.matches("^"+splitKey2))t=t.replaceFirst(splitKey2,"");
        if(t.matches(splitKey2+"$"))t=t.substring(0,t.length()-splitKey2.length());
        sp.edit().putString("_all_keys",((t.equals(splitKey2))?"":t)).apply();
    }

    /** Create new profile from profile key
     *
     * @param c Current context to access to SharedPreferences
     * @param key key of profile
     * @return Speaker profile
     */
    public static SpeakerProfile importFromSharedPreferences(Context c, String key)
    {
        SharedPreferences sp = c.getSharedPreferences(key, Context.MODE_PRIVATE);

        String[] tmpL = sp.getString("recordLang"," : ").split(" : ");
        Language recordLang = new Language(tmpL[0],tmpL[1]);

        tmpL = sp.getString("motherTongue","  :  ").split(" : ");
        Language motherTongue = new Language(tmpL[0],tmpL[1]);

        ArrayList<Language> otherLanguages = new ArrayList<>();
        String t = sp.getString("otherLanguages", null);
        if(t != null && !t.isEmpty())
            for(String tmpS : t.split(splitKey2)) {
                tmpL = tmpS.split(" : ");
                otherLanguages.add(new Language(tmpL[0], tmpL[1]));
            }

        return new SpeakerProfile(sp.getString("name",""), sp.getInt("birdthYear",0), sp.getInt("gender", RecordingMetadata.GENDER_UNSPECIFIED),
                recordLang, motherTongue, otherLanguages, sp.getString("region",""), sp.getString("note",""), sp.getBoolean("signature", false));
    }

    /** Get all speaker key
     *
     * @param c Curent context to access to SharedPreferences
     * @return Return all key, null is it's empty
     */
    public static String[] getAllKeys(Context c)
    {
        String t = c.getSharedPreferences("_master_speaker_keys", Context.MODE_PRIVATE).getString("_all_keys",""), t2[] = t.split(splitKey2);
        ArrayUtils.reverse(t2);
        return !t.isEmpty() ?    t2:  null;
    }

    /** get String to display it
     *
     * @return String serialization of SpeakerProfil to display it
     */
    public String toString()
    {
        String r = _name+" | " + _birthYear+" | ";
        switch(_gender)
        {
            case RecordingMetadata.GENDER_MALE:          r+= "Male | "; break;
            case RecordingMetadata.GENDER_FEMALE:        r+= "Female | "; break;
            case RecordingMetadata.GENDER_UNSPECIFIED:   r+= "Unspecified | "; break;
        }
        r+= _recordLang.toString()+" | " + ((_motherTongue!=null)?_motherTongue.toString():"")+" | ";
        if(_otherLanguages!=null)   for(Language l : _otherLanguages)   r+= l.toString()+" | ";
        return r+_region+" | "+_note+((_signature)?" | signed":" | unsigned");
    }

    /** Checks if a speaker profile already exists in the list using the identification key
     *
     * @param c Contexe of activity
     * @param speaker_profile Speaker profile to check
     * @return true if profile exist, false otherwise.
     */
    public static boolean profilExist(Context c, SpeakerProfile speaker_profile)
    {
        String[] ak = getAllKeys(c);
        if(ak == null)  return false;
        String s = speaker_profile.getKey();
        for(String p : ak)  if(p.equals(s)) return true;
        return false;
    }

    //          #####################################
    //          #         GETTERS & SETTERS         #
    //          #####################################

    public static SpeakerProfile    getLastSpeaker()    {return _lastSpeaker;}
    public String                   getName()           {return _name;}
    public String                   getRegion()         {return _region;}
    public String                   getNote()           {return _note;}
    public int                      getBirthYear()      {return _birthYear;}
    public int                      getGender()         {return _gender;}
    public Language                 getRecordLang()     {return _recordLang;}
    public Language                 getMotherTongue()   {return _motherTongue;}
    public ArrayList<Language>      getOtherLanguages() {return _otherLanguages;}
    public boolean                  getSignature()      {return _signature;}

    public SpeakerProfile setSignature(boolean signature)    {_signature=signature;return this;}
}