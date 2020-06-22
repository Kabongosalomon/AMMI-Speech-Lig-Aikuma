package org.getalp.ligaikuma.lig_aikuma.audio;

import android.app.Activity;

import org.getalp.ligaikuma.lig_aikuma.model.Recording;
import org.getalp.ligaikuma.lig_aikuma.model.Segments.Segment;
import org.getalp.ligaikuma.lig_aikuma.model.TempTranscript;

import java.io.IOException;

/**
 * A player that plays a recording and additionally presents an existing
 * transctiption in an appropriate activity.
 *
 * @author	Oliver Adams	<oliver.adams@gmail.com
 */
public class TranscriptPlayer extends MarkedPlayer
{
	/**
	 * Constructor
	 *
	 * @param	recording	The recording to be played
	 * @param	activity	The activity to modify as the transcriptions
	 * change.
	 * @throws	IOException	If there is an issue reading transcriptions or the
	 * recording
	 */
	public TranscriptPlayer(Recording recording, final Activity activity)
			throws IOException {
		super(recording, true);

		this.activity = activity;

		transcript = recording.getTranscript();

		if(transcript != null) {
			updateTranscriptStatus(getCurrentSample());
			OnMarkerReachedListener onTranscriptMarkerReachedListener =
					new OnMarkerReachedListener() {
				public void onMarkerReached(MarkedPlayer p) {
					activity.runOnUiThread(new Runnable() {
						public void run() {
							updateTranscriptStatus(getCurrentSample());
						}
					});
				}
			};
			setOnMarkerReachedListener(onTranscriptMarkerReachedListener);
		}
		
	}

	@Override
	public void play() {
		super.play();
		if(transcript != null)	updateTranscriptStatus(getCurrentSample());
	}

	// Updates the transcript Ui and prepares the notification marker position.
	private void updateTranscriptStatus(long sample) {
		Segment segment = transcript.getSegmentOfSample(sample);
		setNotificationMarkerPosition(segment);
	}


	@Override
	public void seekToMsec(int msec) {
		super.seekToMsec(msec);
		//Find the transcript segment that corresponds to this point in time
		// and update the UI to reflect the current transcript.
		updateTranscriptStatus(msecToSample(msec));
	}

	//private static Segment segment;
	private TempTranscript transcript;
	private Activity activity;
}
