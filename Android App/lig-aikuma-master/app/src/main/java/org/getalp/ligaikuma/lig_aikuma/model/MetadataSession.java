package org.getalp.ligaikuma.lig_aikuma.model;

import java.util.ArrayList;

public class MetadataSession {
	
	private static MetadataSession ref;
	private Language recordLanguage;
	private Language motherTongue;
	private ArrayList<Language> extralanguages;
	private String regionOrigin;
	private String speakerName;
	private String speakerNote;
	private int speakerAge = 0;
	private int speakerGender = 0;

	private MetadataSession() {
		extralanguages = new ArrayList<>();
		regionOrigin = "";
		speakerName = "";
	}
	
	/**
	 * Ensures that there is a single instance of the session object
	 * @return
	 * - ref, an instance, either empty or full
	 */
	public static MetadataSession getMetadataSession() {
		if(ref == null) ref = new MetadataSession();
		return ref;
	}

	// Never use
	public static void clearSession() {
		ref = null;
	}
	
	public boolean isEmpty(){
		return (recordLanguage == null && motherTongue == null && extralanguages.size() == 0 && regionOrigin.isEmpty()
				&& speakerName.isEmpty() && speakerAge == 0 && speakerGender == 0);
	}
	
	/* getters */
	public Language getRecordLanguage() {
		return recordLanguage;
	}
	
	public Language getMotherTongue() {
		return motherTongue;
	}
	
	public ArrayList<Language> getExtraLanguages() {
		return extralanguages;
	}
	
	public String getRegionOrigin() {
		return regionOrigin;
	}
	
	public String getSpeakerName() {
		return speakerName;
	}

	public String getSpeakerNote() {return speakerNote;}

	public int getSpeakerAge() {
		return speakerAge;
	}
	
	public int getSpeakerGender() {
		return speakerGender;
	}
	
	/* setters */
	public void setSession(Language recordLang, Language motherTong, ArrayList<Language> l, 
			String region, String name, int age, int gender, String note) {
		setRecordLanguage(recordLang);
		setMotherTongue(motherTong);
		setExtraLanguages(l);
		setRegionOrigin(region);
		setSpeakerName(name);
		setSpeakerAge(age);
		setSpeakerGender(gender);
		setSpeakerNote(note);
	}
	
	private void setRecordLanguage(Language recordLang) {
		recordLanguage = (recordLang != null)?	new Language(recordLang.getName(), recordLang.getCode()):
												new Language("", "");
	}
	
	private void setMotherTongue(Language motherTong) {
		motherTongue = (motherTong != null)?	new Language(motherTong.getName(), motherTong.getCode()):
												new Language("", "");
	}
	
	private void setExtraLanguages(ArrayList<Language> l) {
		extralanguages = new ArrayList<>();
		if(l == null)	return;
		for(Language lang : l)
			extralanguages.add(new Language(lang.getName(), lang.getCode()));
	}
	
	private void setRegionOrigin(String r) {
		regionOrigin = r;
	}
	
	private void setSpeakerName(String name) {
		speakerName = name;
	}

	private void setSpeakerNote(String note) { speakerNote = note;}

	private void setSpeakerAge(int age) {
		speakerAge = age;
	}
	
	private void setSpeakerGender(int g) {
		speakerGender = g;
	}
	
	public String toString() {
		return "name : " + getSpeakerName() + " age : " + getSpeakerAge() + "speaker gender : " + getSpeakerGender() +
				" region origin : " + getRegionOrigin() + " record lang : "+ getRecordLanguage().getName() + 
				" mother tongue : " + getMotherTongue().getName() + " rspk languages : " + getExtraLanguages().toString() +
				" note : " + getSpeakerNote();
	}

}
