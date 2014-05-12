package com.mohammadag.soundrecorder;

import com.mohammadag.soundrecorder.listeners.SimpleAnimationListener;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;

public class AnimationHelpers {
	public static void setVisibilityWithTransition(final View view, final int visibility) {
		AnimationSet set = new AnimationSet(true);

		final AnimationListener listener = new SimpleAnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				view.setVisibility(visibility);
			}
		};

		if (visibility == View.GONE || visibility == View.INVISIBLE) {
			Animation fadeOutAnim = AnimationUtils.loadAnimation(view.getContext(), android.R.anim.fade_out);
			fadeOutAnim.setAnimationListener(listener);
			set.addAnimation(fadeOutAnim);

			Animation shrinkAnimation = new ScaleAnimation(1.0f, 1.0f, 1.0f, 0.0f);
			shrinkAnimation.setDuration(fadeOutAnim.getDuration());
			set.addAnimation(shrinkAnimation);

			view.startAnimation(set);
		} else if (visibility == View.VISIBLE) {
			view.setVisibility(visibility);
			Animation fadeInAnimation = AnimationUtils.loadAnimation(view.getContext(), android.R.anim.fade_in);
			fadeInAnimation.setAnimationListener(listener);
			set.addAnimation(fadeInAnimation);
			view.startAnimation(set);
		}
	}
}
