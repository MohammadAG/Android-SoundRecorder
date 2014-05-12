package com.mohammadag.soundrecorder.views;

import java.util.ArrayList;

import com.mohammadag.soundrecorder.listeners.SimpleAnimationListener;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class SoundLevelBarsView extends View {
	@SuppressWarnings("unused")
	private static final String TAG = SoundLevelBarsView.class.getSimpleName();

	// We only need a new value each 500ms
	private static final int DELAY = 500;
	private static final int LIGHT_GRAY = Color.parseColor("#f7f3f7");
	private static final int MID_GRAY = Color.parseColor("#dedbde");
	private static final int DARK_GRAY = Color.parseColor("#bdbebd");

	// dp values
	private static final int DOT_RADIUS = 2;
	private static final int DOT_MARGIN = 10;
	private static final int DOT_SPACING = 5;

	private static int mMaxAudioLevels;
	private static int mCurrentOverrideIndex = 0;

	private static float mMargin;
	private static float mSpacing;
	private static float mRadius;
	private Paint mPaint;

	private ArrayList<Integer> mAudioLevels = new ArrayList<Integer>();

	private long mLastSetAudioLevelTime;

	public SoundLevelBarsView(Context context) {
		super(context);
		init(context);
	}

	public SoundLevelBarsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public SoundLevelBarsView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context) {
		mRadius = convertDpToPixel(DOT_RADIUS, context);
		mMargin = convertDpToPixel(DOT_MARGIN, context);
		mSpacing = convertDpToPixel(DOT_SPACING, context);

		if (isInEditMode()) {
			int examples[] = { 10, 20, 30, 50, 80, 100, 100, 90, 75, 60, 50, 30, 10, 20, 30, 50 };
			for (int i : examples) {
				mAudioLevels.add(i);
			}
		}
	}

	public void clearAudioLevels(boolean animated) {
		if (!animated) {
			mAudioLevels.clear();
			invalidate();
			return;
		}

		Animation fadeOutAnim = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out);
		fadeOutAnim.setAnimationListener(new SimpleAnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				clearAudioLevels(false);
				startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
			}
		});
		startAnimation(fadeOutAnim);
	}

	public static float convertDpToPixel(float dp, Context context){
		Resources resources = context.getResources();
		DisplayMetrics metrics = resources.getDisplayMetrics();
		float px = dp * (metrics.densityDpi / 160f);
		return px;
	}

	public void addAudioLevel(int audioLevel) {
		if (mLastSetAudioLevelTime != 0 && System.currentTimeMillis() - mLastSetAudioLevelTime < DELAY) {
			return;
		}

		mLastSetAudioLevelTime = System.currentTimeMillis();

		if (mAudioLevels.size() >= mMaxAudioLevels) {
			if (mCurrentOverrideIndex >= mMaxAudioLevels)
				mCurrentOverrideIndex = 0;
			mAudioLevels.set(mCurrentOverrideIndex, audioLevel);
			mCurrentOverrideIndex++;
		} else {
			mAudioLevels.add(audioLevel);
		}
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mPaint == null) {
			mPaint = new Paint();
			mPaint.setStrokeWidth(1);
			mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
			mPaint.setAntiAlias(true);
		}
		float y = getHeight() - mMargin;

		int x = (int) (0 + mRadius);
		for (int percentage : mAudioLevels) {
			int numbersOfBars = percentage / 20;
			if (numbersOfBars == 0)
				numbersOfBars = 1;
			if (percentage > 95)
				numbersOfBars = 5;

			for (int i = 0; i < numbersOfBars; i++) {
				if (i == 0 || i == 1)
					mPaint.setColor(DARK_GRAY);
				else if (i == 2 || i == 3)
					mPaint.setColor(MID_GRAY);
				else if (i == 4 || i == 5)
					mPaint.setColor(LIGHT_GRAY);
				canvas.drawCircle(x, y, mRadius, mPaint);
				y -= mRadius + mSpacing;
			}

			y = getHeight() - mMargin;
			x += mSpacing + mRadius;
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int sizeOfOneBall = (int) (mSpacing + mRadius);
		mMaxAudioLevels = width / sizeOfOneBall;
		setMeasuredDimension(width, (int) (5 * (mRadius + mSpacing) + mMargin));
	}
}
