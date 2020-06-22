/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import com.google.common.base.Charsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.BuildConfig;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;
import org.getalp.ligaikuma.lig_aikuma.model.Language;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * Utility class that offers various File IO related methods.
 *
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public final class FileIO {

	public static final String TAG = "FileIO";

	/**
	 * Private constructor to prevent instantiation.
	 */
	private FileIO() {}

	/**
	 * The application's top level path in the external storage.
	 */
	static final String APP_ROOT_PATH = "ligaikuma";

	/**
	 * The path in the external storage for files that are not to be synced.
	 */
	static final String NO_SYNC_PATH = "aikuma-no-sync/";

	/**
	 * Returns the path to the application's data.
	 *
	 * @return	A File representing the applications base directory (the "aikuma"
	 * directory)
	 */
	public static File getAppRootPath(){
		File path = new File(Environment.getExternalStorageDirectory(), APP_ROOT_PATH);
		if(!path.exists()){
			path.mkdirs();
			new File(path, "recordings").mkdir();
			new File(path, "check_transcription_mode").mkdir();
			new File(path, AikumaSettings.getLatestVersion()).mkdir();
		}
		return path;
	}
	
	/**
	 * Returns the path to the directory containing files that are not to be
	 * synced.
	 *
	 * @return	A File representing the application's directory for files not
	 * to be synced
	 */
	public static File getNoSyncPath(){
		File path = new File(Environment.getExternalStorageDirectory(), NO_SYNC_PATH);
		path.mkdirs();
		return path;
	}
	
	/**
	 * Returns the path to the directory containing files of the owner
	 * 
	 * @param versionName	versionName of the files
	 * @param ownerId		Owner's ID for the path
	 * @return	A File representing the owner's directory of versionName
	 */
	public static File getOwnerPath(String versionName, String ownerId) {
		String ownerIdDirName = IdUtils.getOwnerDirName(ownerId);
		String ownerDirStr = (APP_ROOT_PATH + versionName + "/" + 
				ownerIdDirName.substring(0, 1) + "/" + 
				ownerIdDirName.substring(0, 2) + "/" + ownerId + "/");	
		File path = new File(Environment.getExternalStorageDirectory(), ownerDirStr);
		path.mkdirs();
		return path;
	}
	
	/**
	 * Returns the path to the directory of recordings
	 * LIG Version
	 * @return A File representing the recordings directory
	 */
	public static File getOwnerPath() {
		File path = new File(Environment.getExternalStorageDirectory(), APP_ROOT_PATH);
		path.mkdirs();
		return path;
	}

	/**
	 * Takes a file path (relative to the aikuma directory or absolute) and some
	 * data and writes the data into the file in the aikuma directory in external
	 * storage.
	 *
	 * @param	path	The path to the file in which the data is to be
	 * written.
	 * @param	data	The data that is to be written to the file.
	 * @throws	IOException	If there is an I/O issue when writing the data to
	 * file.
	 */
	public static void write(String path, String data) throws IOException {
		FileUtils.writeStringToFile((path.startsWith("/"))?new File(path):new File(getAppRootPath(), path), data, Charsets.UTF_8);
	}

	/**
	 * Takes an absolute file path, some data and
	 * writes the data into the file in the aikuma directory in external storage.
	 *
	 * @param	path	The path to the file in which the data is to be
	 * written.
	 * @param	data	The data that is to be written to the file.
	 * @throws	IOException	If there is an I/O issue when writing the data to file.
	 *
	 */
	public static void write(File path, String data) throws IOException {
		FileUtils.writeStringToFile(path, data, Charsets.UTF_8);
	}

	/**
	 * Takes a File representation of a path and a JSONObject and writes that
	 * JSONObject to the file.
	 *
	 * @param	path	The File where the JSONObject will be written.
	 * @param	jsonObj	The JSONObject to be written.
	 * @throws	IOException if there is an I/O issue when writing the
	 * JSONObject to file.
	 */
	public static void writeJSONObject(File path, JSONObject jsonObj) throws IOException {
		StringWriter stringWriter = new StringWriter();
		jsonObj.writeJSONString(stringWriter);
		write(path, stringWriter.toString());
	}
	
	/**
	 * Takes a file path (relative to the aikuma directory) and
	 * returns a string containing the file's contents.
	 *
	 * @param	path	The path to the file in which the data lies.
	 * @throws	IOException	if there is an issue reading from the file.
	 * @return	A string containing the file's contents;
	 */
	public static String read(String path) throws IOException {

		return FileUtils.readFileToString((path.startsWith("/"))?new File(path):new File(getAppRootPath(), path), Charsets.UTF_8);
	} 

	/**
	 * Takes a file path (relative to the aikuma directory) and
	 * returns a string containing the file's contents.
	 *
	 * @param	path	The path to the file in which the data lies.
	 * @throws	IOException	If there is an issue reading from the file.
	 * @return	A string containing the file's contents;
	 */
	public static String read(File path) throws IOException {
		return FileUtils.readFileToString(path, Charsets.UTF_8);
	}

	/**
	 * Takes a file path and reads a JSONObject from that file.
	 *
	 * @param	path	The path to the file where the JSON is stored.
	 * @throws	IOException	If there is a problem parsing the JSON.
	 * @return	A JSONObject representing the JSON in the supplied File.
	 */
	public static JSONObject readJSONObject(File path) throws IOException {
		try {
			String jsonStr = read(path);
			if(BuildConfig.DEBUG)Log.i("FileIO", "json content: " + jsonStr);
			return (JSONObject) new JSONParser().parse(jsonStr);
		} catch (org.json.simple.parser.ParseException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Loads the ISO 639-3 language codes from the original text file.
	 *
	 * @param	resources	resources so that the iso 639-3 text file can be
	 * retrieved.
	 * @param languages			[Output] a list of languages having names and corresponding codes
	 * @param languageCodeMap	[Output] a map of a language code to its name
	 * @throws	IOException	If there is an issue reading from the langcodes
	 * file.
	 */
	public static void readLangCodes(Resources resources, List<Language> languages, Map<String, String> languageCodeMap) throws IOException
	{
		languages.clear();
		languageCodeMap.clear();

		StringWriter writer = new StringWriter();
		IOUtils.copy(resources.openRawResource(R.raw.iso_639_3), writer, Charsets.UTF_8);
		String s[] = writer.toString().split("\n"), elements[];
		for(int i=1,j=s.length;i<j;i++) {
			elements = s[i].split("(?=\t)");
			languages.add(new Language(elements[6].trim(), elements[0].trim()));
			languageCodeMap.put(elements[0].trim(), elements[6].trim());
		}
	}

	/**
	 * Deletes the file having path
	 * 
	 * @param path		The path to the file
	 * @throws IOException	if delete fails
	 */
	public static void delete(File path) throws IOException {
		FileUtils.forceDelete(path);
	}

	/** Copy all file from Assets to an other ligaikuma/examples
	 *
	 * @param c Context used to find Assets directory
	 */
	public static void copyFilesFromAssets(Context c)
	{
		AssetManager assetManager = c.getAssets();
		String[] files = null;
		try {
			files = assetManager.list("");
		} catch (IOException e) {
			Log.e(TAG, "Failed to get asset file list.", e);
		}
		InputStream in = null;
		OutputStream out = null;
		if(files != null)
			for(String filename : files)
			{
				if(!filename.contains("."))	continue;
				try {
					in = assetManager.open(filename);
					out = new FileOutputStream(new File(FileIO.getAppRootPath()+"/examples/"+filename));
					copyFile(in, out);
				} catch(IOException e) {
					Log.e(TAG, "Failed to copy asset file: " + filename, e);
				}
				finally {
					try {
						if(in != null)	in.close();
						if(out != null)	out.close();
					}
					catch (IOException ignored){}
				}
			}
	}

	private static void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		for(int read;(read = in.read(buffer)) != -1;)
			out.write(buffer, 0, read);
	}
}
