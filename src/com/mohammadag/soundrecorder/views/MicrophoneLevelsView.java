package com.mohammadag.soundrecorder.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.mohammadag.soundrecorder.R;
import com.mohammadag.soundrecorder.RecordingMode;

public class MicrophoneLevelsView extends View {
	@SuppressWarnings("unused")
	private static final String TAG = MicrophoneLevelsView.class.getSimpleName();

	private static final int REEED = Color.parseColor("#ff4444");
	private static final int BEBEBE = Color.parseColor("#bebebe");
	private static final int MICROPHONE_CIRCLE_RADIUS = 100;

	private Paint mInnerCirclePaint;
	private Paint mOuterCirclePaint;
	private Paint mAudioLevelCirclePaint;
	private Paint mBitmapPaint;

	private Bitmap mMicBitmap;
	private Bitmap mMicLightBitmap;
	private Bitmap mMicPressedBitmap;

	private RecordingMode mRecordingMode = RecordingMode.IDLE;

	private boolean mIsDown;

	private int mTouchRadius = MICROPHONE_CIRCLE_RADIUS;
	private int mAudioLevel = 0;

	public MicrophoneLevelsView(Context context) {
		super(context);
		init(context);
	}

	public MicrophoneLevelsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public MicrophoneLevelsView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context) {
		mMicBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_mic_out_grey);
		mMicLightBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_mic_out_light);
		mMicPressedBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_mic_out_pressed);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean shouldReturnFalse = false;
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			if (isInCircle(event.getX(), event.getY(), getWidth() / 2, getHeight() / 2, mTouchRadius)) {
				mInnerCirclePaint.setColor(BEBEBE);
				mIsDown = true;
			}
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (isInCircle(event.getX(), event.getY(), getWidth() / 2, getHeight() / 2, mTouchRadius)) {
				mInnerCirclePaint.setColor(BEBEBE);
				mIsDown = true;
			} else {
				if (mRecordingMode == RecordingMode.IDLE)
					mInnerCirclePaint.setColor(Color.WHITE);
				else
					mInnerCirclePaint.setColor(REEED);
				shouldReturnFalse = true;
				mIsDown = false;
			}
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			if (isInCircle(event.getX(), event.getY(), getWidth() / 2, getHeight() / 2, mTouchRadius)) {
				mIsDown = false;
			} else {
				if (mRecordingMode == RecordingMode.IDLE)
					mInnerCirclePaint.setColor(Color.WHITE);
				else
					mInnerCirclePaint.setColor(REEED);
				shouldReturnFalse = true;
				mIsDown = false;
			}
		}
		invalidate();
		if (shouldReturnFalse)
			return false;

		return super.onTouchEvent(event);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mInnerCirclePaint == null) {
			// Inner circle with mic
			mInnerCirclePaint = new Paint();
			switch (mRecordingMode) {
			case IDLE:
				mInnerCirclePaint.setColor(Color.WHITE);
				break;
			case RECORDING:
				mInnerCirclePaint.setColor(REEED);
				break;
			}
			mInnerCirclePaint.setStrokeWidth(1);
			mInnerCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);
			mInnerCirclePaint.setAntiAlias(true);

			// Circle around inner circle
			mOuterCirclePaint = new Paint();
			mOuterCirclePaint.setColor(BEBEBE);
			mOuterCirclePaint.setStrokeWidth(5);
			mOuterCirclePaint.setStyle(Paint.Style.STROKE);
			mOuterCirclePaint.setAntiAlias(true);

			mAudioLevelCirclePaint = new Paint();
			mAudioLevelCirclePaint.setColor(BEBEBE);
			mAudioLevelCirclePaint.setStrokeWidth(1);
			mAudioLevelCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);
			mAudioLevelCirclePaint.setAntiAlias(true);

			mBitmapPaint = new Paint();
			mBitmapPaint.setAntiAlias(true);
			mBitmapPaint.setFilterBitmap(true);
			mBitmapPaint.setDither(true);
		}

		if (mAudioLevel > 0 && mRecordingMode != RecordingMode.IDLE) {
			double radius = mAudioLevel * (getWidth() / 3) / 100;
			if (radius > getWidth() / 3)
				radius = getWidth() / 3;
			mTouchRadius = (int) radius;
			canvas.drawCircle(getWidth() / 2, getHeight() / 2, (int) radius, mAudioLevelCirclePaint);
		} else {
			mTouchRadius = MICROPHONE_CIRCLE_RADIUS;
		}

		canvas.drawCircle(getWidth() / 2, getHeight() / 2, MICROPHONE_CIRCLE_RADIUS, mInnerCirclePaint);
		canvas.drawCircle(getWidth() / 2, getHeight() / 2, MICROPHONE_CIRCLE_RADIUS+5, mOuterCirclePaint);

		Bitmap bitmap = null;

		if (mRecordingMode == RecordingMode.RECORDING)
			bitmap = mMicLightBitmap;
		else if (mRecordingMode == RecordingMode.IDLE)
			bitmap = mMicBitmap;

		if (isDown()) {
			bitmap = mMicPressedBitmap;
		}

		if (bitmap != null) {
			/* Draw center microphone */
			canvas.drawBitmap(bitmap,
					(getWidth() / 2) - (mMicBitmap.getWidth() / 2),
					(getHeight() / 2) - (mMicBitmap.getHeight() / 2),
					mBitmapPaint);
		}
	}

	private boolean isDown() {
		return mIsDown;
	}

	private static boolean isInCircle(float x, float y, float circleCenterX, float circleCenterY, float circleRadius) {
		double dx = Math.pow(x - circleCenterX, 2);
		double dy = Math.pow(y - circleCenterY, 2);

		if ((dx + dy) < Math.pow(circleRadius, 2)) {
			return true;
		} else {
			return false;
		}
	}
	private boolean changingValue = false;

	public void setAudioLevel(int audioLevel) {
		if (mAudioLevel == audioLevel)
			return;

		if (mRecordingMode == RecordingMode.IDLE) {
			mRecordingMode = RecordingMode.RECORDING;
		}

		if (changingValue)
			return;

		changingValue = true;

		int diff = Math.abs(mAudioLevel - audioLevel);
		/* Old value = 10
		 * New value = 0
		 * How? 10 9 8 7 6 5 4 3 2 1
		 * 
		 * Old = 10
		 * New = 20
		 * How 10 11 12 13 14
		 */

		while (diff > 0) {
			if (mAudioLevel > audioLevel) {
				mAudioLevel--;
			} else {
				mAudioLevel++;
			}

			diff = Math.abs(mAudioLevel - audioLevel);
			invalidate();
		}

		changingValue = false;

		mAudioLevel = audioLevel;
		invalidate();
	}

	public void setRecordingMode(RecordingMode mode) {
		if (mRecordingMode == mode)
			return;

		mRecordingMode = mode;

		if (mInnerCirclePaint == null)
			return;

		switch (mode) {
		case IDLE:
			mInnerCirclePaint.setColor(Color.WHITE);
			break;
		case RECORDING:
			mInnerCirclePaint.setColor(REEED);
			break;
		}
		invalidate();
	}
}
