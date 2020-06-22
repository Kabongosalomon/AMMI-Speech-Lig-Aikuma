/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma.audio;

/**
 * The interface for samplers in the package.
 *
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public interface Sampler {

	/**
	 * Returns the current sample.
	 *
	 * @return	The current sample.
	 */
	long getCurrentSample();

}
