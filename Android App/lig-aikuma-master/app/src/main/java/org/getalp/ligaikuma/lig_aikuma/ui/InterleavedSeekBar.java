/*
	Copyright (C) 2013, The Aikuma Project
	AUTHORS: Oliver Adams and Florian Hanke
*/
package org.getalp.ligaikuma.lig_aikuma.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @author	Oliver Adams	<oliver.adams@gmail.com>
 * @author	Florian Hanke	<florian.hanke@gmail.com>
 */
public class InterleavedSeekBar extends android.support.v7.widget.AppCompatSeekBar {

	public InterleavedSeekBar(Context context) {
		super(context, null, R.attr.seekBarStyle);
		//this.getThumb().setColorFilter(0xff4c516d, Mode.SRC_IN);
		initDawables(context);
	}

	public InterleavedSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs, R.attr.seekBarStyle);
		initDawables(context);
	}

	public InterleavedSeekBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initDawables(context);
	}


	protected void initDawables(Context context)
	{
		Drawable thumb, progressColor;
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
		{
			thumb = context.getResources().getDrawable(R.drawable.thumb_good_quality, context.getTheme());
			progressColor = context.getResources().getDrawable(R.drawable.seekbar_background,context.getTheme());
		}
		else
		{
			thumb = context.getResources().getDrawable(R.drawable.thumb_good_quality);
			progressColor = context.getResources().getDrawable(R.drawable.seekbar_background);
		}

		this.setThumb(thumb);
		this.setThumbOffset(0);
		this.setProgressDrawable(progressColor);
	}

	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(2);
		paint.setColor(Color.rgb(196, 0, 1));

		drawLines(canvas, paint);
	}

	// Draws all the lines on the given canvas with the given paint.
	private void drawLines(Canvas canvas, Paint paint) {
		for(Float line : lines)
			drawLine(line, canvas, paint);
	}

	// Draws a line on the canvas with the given paint
	private void drawLine(Float line, Canvas canvas, Paint paint) {
		Rect bounds = canvas.getClipBounds();
		int barWidth = (bounds.right-16) - (bounds.left+16);
		float pixel = line * ((float) barWidth / 100.0f);

		canvas.drawLine(pixel+16, 0f, pixel+16, 32f, paint);
	}

	/**
	 * Adds a line at the specified point in the seek bar.
	 *
	 * @param	x	The place to put the line, as a percentage of the seekbar.
	 */
	public void addLine(Float x) {
		if(x < 0 || x > 100)
			throw new IllegalArgumentException("The line location must be a percentage of the seekbar between 0 and 100.");
		lines.add(x);
	}

	private List<Float> lines = new ArrayList<>();
}
