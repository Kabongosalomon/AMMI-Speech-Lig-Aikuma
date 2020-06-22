package org.getalp.ligaikuma.lig_aikuma.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;


public class ShareTool
{
    public static final String TAG = "ShareTool";

    /** Share one file
     *
     * @param c Current context
     * @param file File to share
     */
    public static void Share(Context c, String file)
    {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType(getBetterType(file));
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(file)));
        c.startActivity(intent);
    }

    /** Share multiple file
     *
     * @param c Current context
     * @param files Files to share
     */
    public static void Share(Context c, ArrayList<String> files)
    {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        //intent.setType(getBetterType(files));
        intent.setType("audio/wav");
        ArrayList<Uri> al = new ArrayList<>();
        for(String f : files)
            al.add(Uri.fromFile(new File(f)));
        Log.d(TAG, al.toString());
        intent.putExtra(Intent.EXTRA_STREAM, al);
        c.startActivity(intent);
    }

    /** Get better MIME type for stream
     *
     * @param file File to choose the better type
     * @return MIME type used for stream file
     */
    private static String getBetterType(String file)
    {
        // You can add other type, this list is not full
        switch(FilenameUtils.getExtension(file))
        {
            // To optimize, use only those you need
            case"wav" :     return "audio/wav";
            case"map" :     return "text/plain";
            case"json":     return "text/plain";
            case"txt":      return "text/plain";
            case"pdf" :     return "*/*";
            case"mp3" :     return "audio/mpeg";
            case"aac" :     return "audio/aac";
            case"ogg" :     return "audio/ogg";
            case"mid" :
            case"midi":     return "audio/midi";
            case"mp4" :     return "video/mp4";
            case"png" :     return "image/png";
            case"jpg" :
            case"jpeg":     return "image/jpeg";
            case"gif" :     return "image/gif";
            case"html":     return "text/html";
            default   :     return "*/*";
        }
    }

    /** Get better MIME type for stream
     *
     * @param files Files to choose the better type
     * @return MIME type used for stream files
     */
    private static String getBetterType(ArrayList<String> files)
    {
        if(files.size()==0) return "*/*";
        if(files.size()==1) return getBetterType(files.get(0));
        ArrayList<String> tl = new ArrayList<>();
        for(String f : files)   tl.add(getBetterType(f));
        String t = tl.get(0);
        boolean tok = true;
        for(String f : tl)  if(!t.equals(f))    {tok=false;break;}
        if(tok) return t;
        t=tl.get(0).split("/")[0];
        for(String f : tl)  if(!t.equals(f.split("/")[0]))  {tok=true;break;}
        return (!tok)?t+"/*":"*/*";
    }
}
