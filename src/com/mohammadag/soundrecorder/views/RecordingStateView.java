package com.mohammadag.soundrecorder.views;

import com.mohammadag.soundrecorder.RecordingMode;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class RecordingStateView extends View {

	private Paint mPaint;
	private RecordingMode mMode = RecordingMode.IDLE;

	public RecordingStateView(Context context) {
		super(context);
	}

	public RecordingStateView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RecordingStateView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void setRecordingMode(RecordingMode mode) {
		if (mMode == mode)
			return;

		mMode = mode;
		invalidate();
	}

	public RecordingMode getRecordingMode() {
		return mMode;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mPaint == null) {
			mPaint = new Paint();
			mPaint.setColor(Color.RED);
			mPaint.setStrokeWidth(1);
			mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
			mPaint.setAntiAlias(true);
		}

		switch (mMode) {
		case IDLE:
			canvas.drawRect(0, 0, getHeight(), getWidth(), mPaint);
			break;
		case RECORDING:
			canvas.drawCircle(getHeight() / 2, getWidth() / 2, getHeight() / 2, mPaint);
			break;
		}
	}

}
