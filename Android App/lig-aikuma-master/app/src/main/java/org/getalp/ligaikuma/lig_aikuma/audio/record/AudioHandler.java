/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma.audio.record;

/**
 * Interface for anything that handles (saves/processes) audio.
 *
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public interface AudioHandler {

	/**
	 * The caller has detected non-silent audio.
	 *
	 * @param buffer An array of samples.
	 * @param justChanged Whether the caller has just changed
	 *        from silence to audio.
	 */
	void audioTriggered(short[] buffer, boolean justChanged);

	/**
	 * The caller has detected silent audio.
	 *
	 * @param buffer An array of samples.
	 * @param justChanged Whether the caller has just changed
	 *        from audio to silence.
	 */
	void silenceTriggered(short[] buffer, boolean justChanged);
}
