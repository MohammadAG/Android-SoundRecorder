package com.mohammadag.soundrecorder.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.mohammadag.soundrecorder.AnimationHelpers;
import com.mohammadag.soundrecorder.R;
import com.mohammadag.soundrecorder.RecordingMode;
import com.mohammadag.soundrecorder.listeners.OnRecordingStateChangedListener;
import com.mohammadag.soundrecorder.views.MicrophoneLevelsView;

public class RecordingControlsFragment extends Fragment implements OnRecordingStateChangedListener {
	private MicrophoneLevelsView mRecordButton;
	private OnClickListener mRecordButtonOnClickListener;
	private View mRecordingHintView;
	private RecordingMode mRecordingMode = RecordingMode.IDLE;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_record_buttons, null);
		mRecordingHintView = v.findViewById(R.id.recording_hint_textview);
		mRecordButton = (MicrophoneLevelsView) v.findViewById(R.id.button1);
		mRecordButton.setOnClickListener(mRecordButtonOnClickListener);

		if (mRecordingMode == RecordingMode.RECORDING) {
			mRecordingHintView.setVisibility(View.INVISIBLE);
		}
		return v;
	}

	public void setRecordButtonCallback(OnClickListener onClickListener) {
		mRecordButtonOnClickListener = onClickListener;
		if (mRecordButton != null)
			mRecordButton.setOnClickListener(onClickListener);
	}

	@Override
	public void onRecordingStateChanged(RecordingMode mode) {
		if (mRecordingHintView == null)
			return;

		mRecordingMode = mode;

		switch (mode) {
		case RECORDING:
			AnimationHelpers.setVisibilityWithTransition(mRecordingHintView, View.INVISIBLE);
			break;
		case IDLE:
			AnimationHelpers.setVisibilityWithTransition(mRecordingHintView, View.VISIBLE);
			break;
		}

		if (mRecordButton != null)
			mRecordButton.setRecordingMode(mode);
	}

	public void onAudioLevelChanged(int percentage) {
		if (mRecordButton != null) {
			mRecordButton.setAudioLevel(percentage);
		}
	}
}
