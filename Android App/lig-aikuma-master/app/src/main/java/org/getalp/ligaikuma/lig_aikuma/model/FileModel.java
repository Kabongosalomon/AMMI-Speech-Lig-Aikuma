/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Sangyeop Lee
*/
package org.getalp.ligaikuma.lig_aikuma.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.getalp.ligaikuma.lig_aikuma.util.FileIO;

import java.io.File;

/**
 * The file modeled from the viewpoint of GoogleCloud
 * (The parent class of Recording and Speaker / 
 * This can encapsulate the transcript/mapping as well)
 *
 *
 * @author	Sangyeop Lee	<sangl1@student.unimelb.edu.au>
 */
public class FileModel implements Parcelable {

	/**
	 *  Suffix of metadata, maaping and transcript
	 */
	public static final String METADATA_SUFFIX = "-metadata.json";
	public static final String MAPPING_SUFFIX = "-mapping.txt";
	public static final String TRANSCRIPT_SUFFIX = "-transcript.txt";
	public static final String SAMPLE_SUFFIX = "-preview.wav";
	
	/**
	 * Constructor of FileModel
	 * 
	 * @param versionName	versionName of the file-format
	 * @param ownerId		OwnerID(UserID) of the file
	 * @param id			Id of the file
	 * @param type			Type of the file(speaker, mapping, ...)
	 * @param format		Format of the file(jpg, mp4, wav, json, txt)
	 */
	public FileModel(String versionName, String ownerId, String id, String type, String format){
		setVersionName(versionName);
		setOwnerId(ownerId);
		this.id = id;
		this.fileType = type;
		this.format = format;
	}

	public static final Creator<FileModel> CREATOR = new Creator<FileModel>() {
		@Override
		public FileModel createFromParcel(Parcel in) {
			return new FileModel(in);
		}

		@Override
		public FileModel[] newArray(int size) {
			return new FileModel[size];
		}
	};
	
	/**
	 * Constructor for parcel (only used by Speaker)
	 * @param in	Parcel where the speaker will be created
	 */
	protected FileModel(Parcel in) {
		String versionName = in.readString(), ownerId = in.readString();
		setVersionName(versionName);
		setOwnerId(ownerId);
		this.fileType = "speaker";
		this.format = "jpg";
	}
	
	/**
	 * Getter of the file ID
	 * @return	the ID of this file-model
	 */
	public String getId() {
		return id;
	}

	
	/**
	 * Getter of the file's metadata ID + extension
	 * @return	The metadata ID + extension of the file-model
	 */
	public String getMetadataIdExt() {
		return (format.equals("jpg") || (format.equals("wav") && !fileType.equals("preview")))?
			(id + METADATA_SUFFIX): null;
	}
	
	/**
	 * Returns a File that refers to the item's file/metadata-file.
	 * 
	 * @param option	0: file, 1: metadata-file
	 * @return	The file/metadata-file of the item
	 */
	public File getFile(int option)
	{
		if(option != 0 && option != 1)
			return null;
		
		File itemPath;
		if(fileType.equals("speaker")) {
			itemPath = new File(FileIO.getOwnerPath(versionName, ownerId), Speaker.PATH);
			itemPath.mkdirs();
			return new File(itemPath, getId() + "/" + getId() + ((option == 0)?	"-image-small.jpg":	METADATA_SUFFIX));
		}
		itemPath = new File(FileIO.getOwnerPath(versionName, ownerId), Recording.PATH);
		itemPath.mkdirs();

		if(option == 0)
			return new File(itemPath, getId().split("-")[0] + "/" + getId() + getExtension());
		else if(format.equals("txt") || fileType.equals("preview"))	// No metadata for transcript/mapping/preview
			return null;
		return new File(itemPath, getId().split("-")[0] + "/" + getId() + METADATA_SUFFIX);
		
	}

	/**
	 * Get the file's type
	 * TODO: 'respeaking' needs to be changed later to 'respeak'. 'comment','interpret' can be added later
	 * @return	the File-type (source, respeaking, preview, speaker, mapping, transcript)
	 */
	public String getFileType() {
		return fileType;
	}
	
	/**
	 * Get the file's format
	 * @return	the File-format (wav, mp4, jpg, json, txt)
	 */
	public String getFormat() {
		return format;
	}
	
	public String getOwnerId() {
		return ownerId;
	}
	
	public String getVersionName() {
		return versionName;
	}
	
	// Sets the versionName(v0x)
	protected void setVersionName(String versionName) {
		this.versionName = versionName;
	}
	
	// Sets the ownerId(Google account)
	protected void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}	
	
	private String getExtension() {
		switch (format) {
			case "mp4":	return ".mp4";
			case "jpg":	return ".jpg";
			case "txt":	return ".txt";
			default:	return ".wav";
		}
	}
	
	@Override
	public int describeContents() {return 0;}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(versionName);
		dest.writeString(ownerId);
		dest.writeString(id);
		dest.writeString(fileType);
		dest.writeString(format);
	}
	
	/**
	 * The (recording/speaker)'s format version
	 */
	protected String versionName;
	
	/**
	 * The (recording/speaker)'s owner ID
	 */
	protected String ownerId;
	
	/**
	 * The ID of the (recording/speaker).
	 */
	protected String id;
	
	/**
	 * The filetype (speaker, mapping, ...)
	 */
	protected String fileType;

	/**
	 * vnd.wave / mp4 / jpg / txt
	 */
	protected String format;
}
