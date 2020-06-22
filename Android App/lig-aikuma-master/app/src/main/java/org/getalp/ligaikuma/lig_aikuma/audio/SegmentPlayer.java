package org.getalp.ligaikuma.lig_aikuma.audio;

import android.app.Activity;

import org.getalp.ligaikuma.lig_aikuma.model.Segments.Segment;
import org.getalp.ligaikuma.lig_aikuma.ui.ThumbRespeakSummary;

import java.io.File;
import java.io.IOException;

public class SegmentPlayer extends MarkedPlayer
{
	
	private Segment segment;
	private OnMarkerReachedListener onMarkerReachedListener;

	public SegmentPlayer(SimplePlayer player, Segment s, boolean playThroughSpeaker,
			final Activity activity) throws IOException {
		super(player, playThroughSpeaker);
		setSegment(s);
		setOnMarkerReachedListener(activity);
	}

	public SegmentPlayer(File recordingFile, long sampleRate,
			boolean playThroughSpeaker, Segment s, final Activity activity) throws IOException {
		super(recordingFile, sampleRate, playThroughSpeaker);
		setSegment(s);
		setOnMarkerReachedListener(activity);
	}
	
	public Segment getSegment() {
		return segment;
	}
	
	public void setSegment(Segment segment) {
		this.segment = segment;
		seekTo(segment);
		setNotificationMarkerPosition(this.segment);
	}
	
	// TODO if we want the seekbar to adjust the length of the segment
	// the getters should be usedul, yet the seekto might not be
//	public int getCurrentMsec() {
//		try {
//			return mediaPlayer.getCurrentPosition() - sampleToMsec(segment.getStartSample());
//		} catch (IllegalStateException e) {
//			//If we get an IllegalStateException because the recording has
//			//finished playing, just return the duration of the recording.
//			return getDurationMsec();
//		}
//	}
//	
//	public long getCurrentSample() {
//		return msecToSample(getCurrentMsec());
//	}
//	
//	public void seekToMsec(int msec) {
//		mediaPlayer.seekTo(msec + sampleToMsec(segment.getStartSample()));
//	}
//	
//	public void seekToSample(long sample) {
//		seekToMsec(sampleToMsec(sample + segment.getStartSample()));
//	}
//	
//	public int getDurationMsec() {
//		return segment.getDuration().intValue();
//	}
	
	

	public void setOnMarkerReachedListener(OnMarkerReachedListener
			onMarkerReachedListener) {
		this.onMarkerReachedListener = onMarkerReachedListener;
	}
	
	
	public void setOnMarkerReachedListener(final Activity activity) {
		OnMarkerReachedListener onEndSegmentMarkerReachedListener =
				new OnMarkerReachedListener() {
			public void onMarkerReached(MarkedPlayer p) {
				((ThumbRespeakSummary) activity).pauseFragment(segment);
//				p.pause();
				p.seekTo(segment);
				setNotificationMarkerPosition(segment);
			}
		};
		super.setOnMarkerReachedListener(onEndSegmentMarkerReachedListener);
	}

}
