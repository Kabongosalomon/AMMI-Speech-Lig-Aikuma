/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Sangyeop Lee
*/
package org.getalp.ligaikuma.lig_aikuma.util;


/**
 * The class storing setting parameters which can be accessed
 * by all Android components in the application
 * (Global Parameters)
 *
 * @author	Sangyeop Lee	    <sangl1@student.unimelb.edu.au>
 * @author	Baudson Guillaume	<baudson.guillaume@hotmail.fr>
 */
public class AikumaSettings {

	public static boolean isProximityOn = false;

	private static String DEFAULT_USER_ID = null;
	
	// Latest version name.
	private static final String DEFAULT_VERSION = "/v03";
	
	
	/**
	 * Return current file-format version
	 * @return	String of version
	 */
	public static String getLatestVersion(){
		return DEFAULT_VERSION;
	}
	
	/**
	 * Return current default owner account
	 * @return	String of default owner account
	 */
	public static String getCurrentUserId() {
		return DEFAULT_USER_ID;
	}
	/**
	 * Set the default owner account
	 * @param Id	String of default owner account
	 */
	public static void setUserId(String Id) {
		DEFAULT_USER_ID = Id;
	}
}

