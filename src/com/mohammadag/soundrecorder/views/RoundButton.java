package com.mohammadag.soundrecorder.views;

import com.mohammadag.soundrecorder.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class RoundButton extends View {
	private static final float CIRCLE_RADIUS = 90;
	private static final int SQUARE_WIDTH = (int) (CIRCLE_RADIUS / 2);

	private Paint mCirclePaint;
	private Paint mInnerThingyPaint;
	private Paint mBitmapPaint;

	private Type mType = Type.PAUSE;
	public enum Type {
		PAUSE, STOP, PLAY
	}

	private boolean mIsDown = false;
	private Bitmap mPlayBitmap;
	private Bitmap mScaledBitmap;

	public RoundButton(Context context) {
		super(context);
		init(context);
	}

	public RoundButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public RoundButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	public void init(Context context) {
		mPlayBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_play);
	}

	public boolean isDown() {
		return mIsDown;
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (mInnerThingyPaint != null){
			if (enabled)
				mInnerThingyPaint.setColor(Color.BLACK);
			else
				mInnerThingyPaint.setColor(Color.GRAY);
		}
		invalidate();
		super.setEnabled(enabled);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled())
			return super.onTouchEvent(event);

		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			if (!isInCircle(event.getX(), event.getY(), getWidth() / 2, getHeight() / 2, CIRCLE_RADIUS)) {
				return false;
			}
			if (isEnabled()) {
				mCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);
				invalidate();
			}
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (isInCircle(event.getX(), event.getY(), getWidth() / 2, getHeight() / 2, CIRCLE_RADIUS) && !isEnabled()) {
				mCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);
				invalidate();
			} else {
				mCirclePaint.setStyle(Paint.Style.STROKE);
				invalidate();
			}
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			mCirclePaint.setStyle(Paint.Style.STROKE);
			invalidate();
			if (!isInCircle(event.getX(), event.getY(), getWidth() / 2, getHeight() / 2, CIRCLE_RADIUS)) {
				return false;
			}
		}

		return super.onTouchEvent(event);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mCirclePaint == null) {
			mCirclePaint = new Paint();
			mCirclePaint.setColor(Color.GRAY);
			mCirclePaint.setStyle(Paint.Style.STROKE);
			mCirclePaint.setStrokeWidth(1);

			mInnerThingyPaint = new Paint();
			mInnerThingyPaint.setColor(Color.BLACK);
			mInnerThingyPaint.setStyle(Paint.Style.FILL_AND_STROKE);
			mInnerThingyPaint.setStrokeWidth(1);

			mBitmapPaint = new Paint();
			mBitmapPaint.setAntiAlias(true);
			mBitmapPaint.setFilterBitmap(true);
			mBitmapPaint.setDither(true);
		}

		canvas.drawCircle(getWidth() / 2, getHeight() / 2, CIRCLE_RADIUS, mCirclePaint);

		switch (mType) {
		case STOP:
			int squareLeft = getWidth() / 2 - (SQUARE_WIDTH/2);
			int squareTop = getHeight() / 2 - (SQUARE_WIDTH/2);
			int squareRight = squareLeft + SQUARE_WIDTH;
			int squareBottom = squareTop + SQUARE_WIDTH;
			canvas.drawRect(squareLeft, squareTop, squareRight, squareBottom, mInnerThingyPaint);
			break;
		case PAUSE:
			int rectLeft = (getWidth() / 2) - (SQUARE_WIDTH/2);
			int rectTop = getHeight() / 2 - (SQUARE_WIDTH/2);
			int rectRight = (rectLeft + (SQUARE_WIDTH/2));
			int rectBottom = rectTop + SQUARE_WIDTH;
			int width = rectRight - rectLeft;
			rectLeft -= (width / 2) - (SQUARE_WIDTH / 8);
			rectRight -= (width / 2) - (SQUARE_WIDTH / 8);
			canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, mInnerThingyPaint);

			rectLeft += (SQUARE_WIDTH / 2) + (SQUARE_WIDTH / 4);
			rectRight += (SQUARE_WIDTH / 2) + (SQUARE_WIDTH / 4);
			canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, mInnerThingyPaint);

			break;
		case PLAY:
			if (mScaledBitmap != null) {
				/* Draw mic */
				canvas.drawBitmap(mScaledBitmap,
						(getWidth() / 2) - (mScaledBitmap.getWidth() / 2),
						(getHeight() / 2) - (mScaledBitmap.getHeight() / 2),
						mBitmapPaint);
			}
			break;
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mScaledBitmap = Bitmap.createScaledBitmap(mPlayBitmap, getWidth() / 3, getHeight()/3, false);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(200, 200);
		if (mScaledBitmap == null && !isInEditMode())
			mScaledBitmap = Bitmap.createScaledBitmap(mPlayBitmap, getWidth() / 3, getHeight()/3, false);
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

	public void setType(Type type) {
		mType = type;
		invalidate();
	}
}
