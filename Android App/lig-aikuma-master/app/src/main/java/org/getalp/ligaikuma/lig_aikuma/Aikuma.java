/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings.Secure;

import org.getalp.ligaikuma.lig_aikuma.model.Language;
import org.getalp.ligaikuma.lig_aikuma.util.FileIO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Offers a collection of static methods that require a context independently
 * of an Activity.
 *
 * Sources and caveats:
 * http://stackoverflow.com/questions/987072/using-application-context-everywhere
 * http://stackoverflow.com/questions/2002288/static-way-to-get-context-on-android
 * 
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public class Aikuma extends android.app.Application {

	private static Aikuma instance;
	private static Map<String, String> languageCodeMap;
	private static List<Language> languages;

	public static Context appContext = null;

	/**
	 * The constructor.
	 */
	public Aikuma() {
		instance = this;
	}

	/**
	 * Static method that provides a context when needed by code not bound to
	 * any meaningful context.
	 *
	 * @return	A Context-> Still null from migration on Android Studio
	 */
	public static Context getContext() { return instance; }

	/**
	 * Gets the phone model name
	 *
	 * @return	The device name (manufacturer + model)
	 */
	public static String getDeviceName() {
		return Build.MANUFACTURER + "-" + Build.MODEL;
	}

	/**
	 * Set the application context, use it before call all other functions (except getLanguages)
	 *
	 * @param context
	 */
	public static void setAppContext(Context context) { appContext = context;}

	/**
	 * Gets the android ID of the phone.
	 *
	 * @return	The android ID as a String.
	 */
	@SuppressLint("HardwareIds")
	public static String getAndroidID() {
		return Secure.getString(appContext.getContentResolver(), Secure.ANDROID_ID);
	}

	/**
	 * A function which return 3 first charactes of ANDROID_ID address
	 * @return 3 first characters of ANDROID_ID Address
	 */
	public static String getDeviceId() {
		return getAndroidID().substring(0, 3);
	}

	/**
	 * Returns the ISO 639-3 languages once they are loaded.
	 * 
	 * @return	the languages
	 */
	public static List<Language> getLanguages(Context context) {
		if(languages != null) return languages;
		loadLanguages(context);
		while(languages == null);
		return languages;
	}
	
	/**
	 * Returns the ISO 639-3 language map (code - name)
	 * 
	 * @return	the languageCodeMap
	 */
	public static Map<String, String> getLanguageCodeMap() {
		if (languageCodeMap != null) return languageCodeMap;
		loadLanguages(appContext);	// Potential error if appContext == null
		while(languageCodeMap == null);//Wait patiently
		return languageCodeMap;
	}

	/**
	 * Loads the ISO 639-3 languages.
	 */
	public static void loadLanguages(Context context) {
		if(languages != null)
			return;
		languages = new ArrayList<>();
		languageCodeMap = new HashMap<>();
		setAppContext(context);
		if(loadLangCodesThread == null || !loadLangCodesThread.isAlive()) {
			loadLangCodesThread = new Thread(new Runnable() {
				public void run() {
					try {
						if(appContext==null) return;
						FileIO.readLangCodes(appContext.getResources(), languages, languageCodeMap);
					} catch (IOException e) {
						// This should never happen.
						throw new RuntimeException("Cannot load languages");
					}
				}
			});
			loadLangCodesThread.start();
		}
	}

	/**
	 * The thread used to load the language codes without interrupting the
	 * main thread.
	 */
	public static Thread loadLangCodesThread;
}