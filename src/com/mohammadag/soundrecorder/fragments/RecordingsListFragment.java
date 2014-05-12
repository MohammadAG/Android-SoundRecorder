package com.mohammadag.soundrecorder.fragments;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.cocosw.undobar.UndoBarController.UndoBar;
import com.cocosw.undobar.UndoBarController.UndoListener;
import com.cocosw.undobar.UndoBarStyle;
import com.haarman.listviewanimations.itemmanipulation.OnDismissCallback;
import com.haarman.listviewanimations.itemmanipulation.SwipeDismissAdapter;
import com.mohammadag.soundrecorder.R;
import com.mohammadag.soundrecorder.RecordingItem;
import com.mohammadag.soundrecorder.adapters.RecordingsAdapter;
import com.mohammadag.soundrecorder.views.RoundButton;
import com.mohammadag.soundrecorder.views.RoundButton.Type;

public class RecordingsListFragment extends ListFragment implements OnDismissCallback, OnItemLongClickListener, UndoListener {
	private RecordingsAdapter mAdapter;
	private MediaPlayer mPlayer;
	private TextView mCurrentPositionTextView;
	private SeekBar mCurrentPositionSeekBar;

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mAdapter = new RecordingsAdapter(getActivity());
		SwipeDismissAdapter adapter = new SwipeDismissAdapter(mAdapter, this);
		setListAdapter(adapter);
		getListView().setBackgroundColor(getResources().getColor(R.color.card_gray));
		adapter.setAbsListView(getListView());

		setEmptyText(getString(R.string.no_recordings));

		getListView().setOnItemLongClickListener(this);
		getListView().setDividerHeight(0);
		getListView().setDivider(null);
		getListView().setSelector(new ColorDrawable(android.R.color.transparent));
		getListView().setHeaderDividersEnabled(true);
		getListView().setPadding(getListView().getPaddingLeft(),
				getListView().getPaddingTop() + 20, getListView().getPaddingRight(),
				getListView().getPaddingBottom() + 20);
		getListView().setClipToPadding(false);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> av, View v, int position, long id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final RecordingItem item = mAdapter.getItem(position);
		builder.setTitle(item.getName())
		.setItems(R.array.recording_list_menu_options, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (which == 0) {
					renameItem(item);
				} else if (which == 1) {
					Intent send = new Intent(Intent.ACTION_SEND);  
					send.setType("audio/wav");
					Uri fileUri = Uri.fromFile(new File(item.getFilePath()));
					send.putExtra(Intent.EXTRA_STREAM, fileUri);
					startActivity(send);
				} else if (which == 2) {
					// Delete again? Bad UX?
				}
			}
		});
		builder.show();
		return true;
	}

	protected void renameItem(final RecordingItem item) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(item.getName());
		final EditText input = new EditText(getActivity());
		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT);
		input.setLayoutParams(lp);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				mAdapter.getDatabase().renameItem(item, input.getText().toString());
			}
		});

		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.setView(input);
		builder.show();
	}

	@Override
	public void onListItemClick(android.widget.ListView l, View v, int position, long id) {
		RecordingItem item = mAdapter.getItem(position);
		File file = new File(item.getFilePath());
		if (!file.exists()) {
			Toast.makeText(v.getContext(), R.string.file_not_found, Toast.LENGTH_SHORT).show();
			mAdapter.remove(item);
			return;
		}

		if (mPlayer != null) {
			if (mPlayer.isPlaying())
				mPlayer.stop();

			mPlayer.release();
		}

		mPlayer = new MediaPlayer();
		try {
			mPlayer.setDataSource(item.getFilePath());
			mPlayer.prepare();
			mPlayer.start();
			startTimer();

			showDialog(item);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void showDialog(final RecordingItem item) {
		View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_playback, null);
		final RoundButton pauseButton = (RoundButton) view.findViewById(R.id.pause_button);
		pauseButton.setType(Type.PAUSE);

		final RoundButton stopButton = (RoundButton) view.findViewById(R.id.stop_button);
		stopButton.setType(Type.STOP);
		stopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mPlayer.pause();
				mPlayer.seekTo(0);
				stopButton.setEnabled(false);
				pauseButton.setType(Type.PLAY);
				stopTimer();
			}
		});
		mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer arg0) {
				stopButton.setEnabled(false);
				pauseButton.setType(Type.PLAY);
				stopTimer();
				mCurrentPositionSeekBar.setProgress(mCurrentPositionSeekBar.getMax());
				mCurrentPositionTextView.setText(getTimeText(item.getLength()));
			}
		});

		pauseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				stopButton.setEnabled(true);
				if (mPlayer.isPlaying()) {
					mPlayer.pause();
					pauseButton.setType(Type.PLAY);
					stopTimer();
				} else {
					mPlayer.start();
					pauseButton.setType(Type.PAUSE);
					startTimer();
				}
			}
		});

		TextView recordingName = (TextView) view.findViewById(R.id.recording_name_textview);
		recordingName.setText(item.getName());

		mCurrentPositionSeekBar = (SeekBar) view.findViewById(R.id.seek_bar);
		mCurrentPositionSeekBar.setMax(mPlayer.getDuration());
		mCurrentPositionSeekBar.setProgress(mPlayer.getCurrentPosition());
		mCurrentPositionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) { }
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) { }
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (!fromUser)
					return;

				mPlayer.seekTo(progress);
			}
		});
		mCurrentPositionTextView = (TextView) view.findViewById(R.id.current_position);
		updateSeekBar();

		TextView duration = (TextView) view.findViewById(R.id.total_duration);
		duration.setText(getTimeText(item.getLength()));

		TextView timeAdded = (TextView) view.findViewById(R.id.time_added_textview);
		timeAdded.setVisibility(View.GONE);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				mPlayer.stop();
				mPlayer.release();
				mPlayer = null;
				mCurrentPositionTextView = null;
				mCurrentPositionSeekBar = null;
				stopTimer();
				dialog.dismiss();
			}
		});
		builder.setView(view);
		builder.show();
	}

	private Timer mTimer;
	private TimerTask mProgressTimerTask;
	private void startTimer() {
		mTimer = new Timer();
		mProgressTimerTask = new TimerTask() {
			@Override
			public void run() {
				if (getActivity() == null)
					return;

				getActivity().runOnUiThread(new Runnable() {	
					@Override
					public void run() {
						updateSeekBar();
					}
				});
			}
		};
		mTimer.scheduleAtFixedRate(mProgressTimerTask, 500, 500);
	}

	private void stopTimer() {
		if (mProgressTimerTask != null) {
			mProgressTimerTask.cancel();
			mProgressTimerTask = null;
		}
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}

		updateSeekBar();
	}

	private void updateSeekBar() {
		if (mCurrentPositionTextView != null && mPlayer != null) {
			int position = 0;
			try {
				position = mPlayer.getCurrentPosition();
			} catch (Throwable t) {

			}
			mCurrentPositionTextView.setText(getTimeText(position));
			mCurrentPositionSeekBar.setProgress(position);
		}
	}

	@Override
	public void onDismiss(AbsListView listView, int[] reverseSortedPositions) {
		for (int i : reverseSortedPositions) {
			RecordingItem item = mAdapter.getItem(i);
			mAdapter.remove(item);

			Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
			Animation fadeOutAnimation = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);

			UndoBar undoBar = new UndoBar(getActivity());
			undoBar.style(new UndoBarStyle(R.drawable.ic_undobar_undo, R.string.undo)
			.setAnim(fadeInAnimation, fadeOutAnimation));
			undoBar.listener(this);
			undoBar.message(R.string.recording_deleted);
			undoBar.token(item);
			undoBar.show();

		}
	}

	private static final String getTimeText(int position) {
		SimpleDateFormat mDateFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());
		return mDateFormat.format(position);
	}

	@Override
	public void onUndo(Parcelable token) {
		RecordingItem item = (RecordingItem) token;
		mAdapter.getDatabase().restoreRecording(item);
	}
}
