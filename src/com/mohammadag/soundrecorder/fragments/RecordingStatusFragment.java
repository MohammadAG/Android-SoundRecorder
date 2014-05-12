package com.mohammadag.soundrecorder.fragments;

import java.text.SimpleDateFormat;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mohammadag.soundrecorder.R;
import com.mohammadag.soundrecorder.RecordingMode;
import com.mohammadag.soundrecorder.RecordingService.OnTimerChangedListener;
import com.mohammadag.soundrecorder.activities.MainActivity;
import com.mohammadag.soundrecorder.listeners.SimpleAnimationListener;
import com.mohammadag.soundrecorder.views.RecordingStateView;
import com.mohammadag.soundrecorder.views.SoundLevelBarsView;

public class RecordingStatusFragment extends Fragment implements OnTimerChangedListener {
	private RecordingStateView mStateView;
	private RecordingMode mRecordingMode;
	private TextView mFilenameView;
	private TextView mTime;
	private SoundLevelBarsView mBars;
	private String mFilename;
	private int mSeconds;
	private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_recording_status, null);

		mFilenameView = (TextView) v.findViewById(R.id.filename_textview);
		mFilenameView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
				alertDialog.setTitle(R.string.recording_name);
				final EditText input = new EditText(getActivity());
				input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.MATCH_PARENT);
				input.setLayoutParams(lp);
				alertDialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						((MainActivity) getActivity()).setPrettyName(input.getText().toString());
					}
				});
				alertDialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {	
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				alertDialog.setView(input);
				alertDialog.show();
			}
		});
		mStateView = (RecordingStateView) v.findViewById(R.id.state_view);
		mTime = (TextView) v.findViewById(R.id.timer_textview);
		mBars = (SoundLevelBarsView) v.findViewById(R.id.bars);

		if (mRecordingMode == RecordingMode.RECORDING) {
			mFilenameView.setVisibility(View.VISIBLE);
			mFilenameView.setText(mFilename);
			mStateView.setRecordingMode(mRecordingMode);
			setTimeFromSeconds(mSeconds);
		}

		return v;
	}

	public void setRecordingMode(final RecordingMode mode) {
		if (mode == mStateView.getRecordingMode())
			return;

		mRecordingMode = mode;

		if (isDetached() || getActivity() == null)
			return;

		switch (mode) {
		case IDLE:
			Animation fadeOutAnimation = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
			fadeOutAnimation.setAnimationListener(new SimpleAnimationListener() {
				@Override
				public void onAnimationEnd(Animation animation) {
					mFilenameView.setVisibility(View.INVISIBLE);
				}
			});
			mFilenameView.startAnimation(fadeOutAnimation);
			mStateView.startAnimation(fadeOutAnimation);
			break;
		case RECORDING:
			Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
			fadeInAnimation.setAnimationListener(new SimpleAnimationListener() {
				@Override
				public void onAnimationEnd(Animation animation) {
					mFilenameView.setVisibility(View.VISIBLE);
				}
			});
			mFilenameView.startAnimation(fadeInAnimation);
			break;
		}

		Animation stateFadeOutTransition = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
		final Animation stateFadeInTransition = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
		stateFadeOutTransition.setAnimationListener(new SimpleAnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				mStateView.setRecordingMode(mode);
				mStateView.startAnimation(stateFadeInTransition);
			}
		});

		mStateView.startAnimation(stateFadeOutTransition);
	}

	public RecordingMode getRecordingMode() {
		return mRecordingMode;
	}

	public void setFileName(String filename) {
		mFilename = filename;
		mFilenameView.setText(filename);
	}

	public void setTimeText(String text) {
		if (mTime != null)
			mTime.setText(text);
	}

	public void setTimeFromSeconds(int seconds) {
		mSeconds = seconds;
		if (mTime != null)
			mTime.setText(mDateFormat.format(seconds*1000));
	}

	public void clearAudioBars() {
		if (mBars != null)
			mBars.clearAudioLevels(true);
	}

	@Override
	public void onTimerChanged(final int seconds) {
		if (getActivity() == null)
			return;

		getActivity().runOnUiThread(new Runnable() {	
			@Override
			public void run() {
				setTimeFromSeconds(seconds);
			}
		});
	}

	public void onAudioLevelChanged(int audioLevel) {
		mBars.addAudioLevel(audioLevel);
	}
}
