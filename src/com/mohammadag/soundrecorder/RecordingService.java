package com.mohammadag.soundrecorder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import com.mohammadag.soundrecorder.activities.MainActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class RecordingService extends Service {
	private static final String TAG = RecordingService.class.getSimpleName();

	public static final String INTENT_RECORDING_STARTED = "com.mohammadag.soundrecorder.STARTED_RECORDING";
	public static final String INTENT_RECORDING_STOPPED = "com.mohammadag.soundrecorder.STOPPED_RECORDING";

	public static final int SAMPLING_RATE = 44100;
	public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
	public static final int CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	public static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNEL_IN_CONFIG, AUDIO_FORMAT);
	public static final int SAMPLE_DELAY = 1000 / 60;

	private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());

	private int mElpasedSeconds = 0;

	private Timer mTimer = null;
	private TimerTask mIncrementTimerTask = null;

	private OnTimerChangedListener onTimerChangedListener = null;
	private OnAudioLevelChangedListener mAudioChangedListener = null;

	private String mFilePath = null;
	private String mPrettyRecordingName = null;

	private AudioRecord mAudioRecord = null;

	private Thread mRecordingThread = null;
	private Thread mAnalyzingThread = null;

	byte mAudioData[] = null;

	private boolean mIsRecording = false;

	private final IBinder mBinder = new ServiceBinder();

	private RecordingsDatabase mDatabase;

	private RecordingMode mRecordingMode = RecordingMode.IDLE;

	public class ServiceBinder extends Binder {
		public RecordingService getService() {
			return RecordingService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public interface OnTimerChangedListener {
		void onTimerChanged(int seconds);
	}

	public interface OnAudioLevelChangedListener {
		void onAudioLevelChanged(int percentage);
	}

	public void setOnAudioLevelChanged(OnAudioLevelChangedListener listener) {
		mAudioChangedListener = listener;
	}

	public void setOnTimerChangedListener(OnTimerChangedListener listener) {
		onTimerChangedListener = listener;
	}

	public RecordingService getService() {
		return RecordingService.this;
	}
	
	private BroadcastReceiver mShutdownReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			stopRecording();
		}
	};

	public void onCreate() {
		mDatabase = new RecordingsDatabase(getApplicationContext());
		sendBroadcast(new Intent("com.mohammadag.soundrecorder.SERVICE_STARTED"));
		
		IntentFilter iF = new IntentFilter();
		iF.addAction(Intent.ACTION_SHUTDOWN);
		iF.addAction("android.intent.action.QUICKBOOT_POWEROFF");
		registerReceiver(mShutdownReceiver, iF);
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null)
			return START_STICKY;

		if ("com.mohammadag.soundrecorder.START_RECORDING".equals(intent.getAction())) {
			startRecording();
		} else if ("com.mohammadag.soundrecorder.STOP_RECORDING".equals(intent.getAction())) {
			stopRecording();
		}
		return START_STICKY;
	}

	public void startRecording() {
		if (mAudioRecord != null)
			return;

		File file = generateFile();
		if (file == null) {
			return;
		}

		mFilePath = file.getAbsolutePath();
		mPrettyRecordingName = file.getName().replace(".pcm", "");

		mAudioRecord = new AudioRecord(AUDIO_SOURCE,
				SAMPLING_RATE, CHANNEL_IN_CONFIG,
				AUDIO_FORMAT, BUFFER_SIZE);

		mAudioData = new byte[BUFFER_SIZE];
		mAudioRecord.startRecording();
		mIsRecording = true;

		createRecordingThread();
		mRecordingThread.start();

		createAnalyzingThread();
		mAnalyzingThread.start();

		mRecordingMode = RecordingMode.RECORDING;

		Intent startedRecording = new Intent(INTENT_RECORDING_STARTED);
		startedRecording.putExtra("filename", file.getName());
		sendBroadcast(startedRecording);

		startTimer();

		startForeground(1, createNotification());
	}

	public boolean isRecording() {
		return mIsRecording;
	}

	public String getFilename() {
		return new File(mFilePath).getName();
	}

	private void startTimer() {
		mTimer = new Timer();
		mIncrementTimerTask = new TimerTask() {
			@Override
			public void run() {
				mElpasedSeconds++;
				if (onTimerChangedListener != null)
					onTimerChangedListener.onTimerChanged(mElpasedSeconds);

				NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				mgr.notify(1, createNotification());
			}
		};
		mTimer.scheduleAtFixedRate(mIncrementTimerTask, 1000, 1000);
	}

	public double round(double d, int decimalPlace) {
		// see the Javadoc about why we use a String in the constructor
		// http://java.sun.com/j2se/1.5.0/docs/api/java/math/BigDecimal.html#BigDecimal(double)
		try {
			BigDecimal bd = new BigDecimal(Double.toString(d));
			bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
			return bd.doubleValue();
		} catch (Throwable t) {
			int i = (int) d;
			return i;
		}
	}
	
	@Override
	public void onDestroy() {
		if (mIsRecording)
			stopRecording();
		unregisterReceiver(mShutdownReceiver);
		super.onDestroy();
	}

	public void pauseRecording() {
		if (mIncrementTimerTask != null) {
			mIncrementTimerTask.cancel();
			mIncrementTimerTask = null;
		}

		if (mAudioRecord != null) {
			mIsRecording = false;
			mAudioRecord.stop();
			mRecordingThread = null;
			mAnalyzingThread = null;
		}
	}

	private void createRecordingThread() {
		mRecordingThread = new Thread(new Runnable() {
			public void run() {
				BufferedOutputStream os = null;
				try {
					os = new BufferedOutputStream(new FileOutputStream(mFilePath));
				} catch (FileNotFoundException e) {
					Log.e(TAG, "File not found for recording ", e);
				}
				while (mIsRecording) {
					int status = mAudioRecord.read(mAudioData, 0, mAudioData.length);

					if (status == AudioRecord.ERROR_INVALID_OPERATION ||
							status == AudioRecord.ERROR_BAD_VALUE) {
						Log.e(TAG, "Error reading audio data!");
						return;
					}

					try {
						os.write(mAudioData, 0, BUFFER_SIZE);
					} catch (IOException e) {
						Log.e(TAG, "Error saving recording ", e);
						return;
					}
				}

				try {
					os.close();
				} catch (Throwable t) {

				}
			}
		});
	}

	private Notification createNotification() {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
		builder.setContentTitle(getString(R.string.state_recording));
		builder.setContentText(mDateFormat.format(mElpasedSeconds*1000));
		builder.setSmallIcon(R.drawable.ic_action_mic_out_light);
		builder.setOngoing(true);

		builder.setContentIntent(PendingIntent.getActivities(getApplicationContext(), 0,
				new Intent[] { new Intent(getApplicationContext(), MainActivity.class) }, 0));

		Intent stopIntent = new Intent(getApplicationContext(), RecordingService.class);
		stopIntent.setAction("com.mohammadag.soundrecorder.STOP_RECORDING");
		builder.addAction(R.drawable.ic_stat_stop, getString(R.string.stop),
				PendingIntent.getService(getApplicationContext(), 0, stopIntent, 0));

		return builder.build();
	}

	private void createAnalyzingThread() {
		mAnalyzingThread = new Thread(new Runnable() {
			public void run() {
				while (mIsRecording) {
					try {
						Thread.sleep(SAMPLE_DELAY);
					} catch(InterruptedException ie) {
						ie.printStackTrace();
					}

					//					double splValue = 0.0;
					double rmsValue = 0.0;

					for (int i = 0; i < BUFFER_SIZE - 1; i++) {
						rmsValue += mAudioData[i] * mAudioData[i];
					}
					rmsValue = rmsValue / BUFFER_SIZE;
					rmsValue = Math.sqrt(rmsValue);

					//					splValue = 20 * Math.log10(rmsValue / 0.000002);
					//					splValue = splValue + -80;
					//					splValue = round(splValue, 2);

					if (mAudioChangedListener != null)
						mAudioChangedListener.onAudioLevelChanged((int) rmsValue);
				}
			}
		});
	}

	public void resumeRecording() {
		mAudioRecord.startRecording();
		mIsRecording = true;
		startTimer();

		createRecordingThread();
		mRecordingThread.start();

		createAnalyzingThread();
		mAnalyzingThread.start();
	}

	public void stopRecording() {
		if (mIncrementTimerTask != null) {
			mIncrementTimerTask.cancel();
			mIncrementTimerTask = null;
		}

		if (mAudioRecord != null) {
			mIsRecording = false;
			mAudioRecord.stop();
			mAudioRecord.release();
			mAudioRecord = null;
			mRecordingThread = null;
			mAnalyzingThread = null;
		}

		File tempFile = new File(mFilePath);
		if (!tempFile.exists()) {
			mElpasedSeconds = 0;
			mFilePath = null;
			return;
		}

		String destFilePath = mFilePath.replace("pcm", "wav");
		WavConverter.copyWaveFile(mFilePath, destFilePath, BUFFER_SIZE);
		File file = new File(mFilePath);
		if (file.exists())
			file.delete();

		File newFile = new File(destFilePath);

		mRecordingMode = RecordingMode.IDLE;

		Intent stoppedRecording = new Intent(INTENT_RECORDING_STOPPED);
		stoppedRecording.putExtra("total_time", mElpasedSeconds);
		stoppedRecording.putExtra("filepath", mFilePath.replace("pcm", "wav"));
		sendBroadcast(stoppedRecording);

		mFilePath = null;

		int length = getWavDuration(newFile);
		mElpasedSeconds = 0;

		mDatabase.addRecording(mPrettyRecordingName,
				newFile.getAbsolutePath(), length);

		stopForeground(true);
	}

	public void setNextPrettyRecordingName(String fileName) {
		mPrettyRecordingName = fileName;
	}

	public RecordingMode getRecordingMode() {
		return mRecordingMode;
	}

	public int getWavDuration(File file) {
		MediaPlayer mp = new MediaPlayer();
		try {
			mp.setDataSource(file.getAbsolutePath());
			mp.prepare(); 
			int length = mp.getDuration();
			mp.release();
			return length;
		} catch (Exception e) {
			e.printStackTrace();

			return mElpasedSeconds * 1000;
		}
	}

	private File generateFile() {
		String filename = Environment.getExternalStorageDirectory().getAbsolutePath();
		filename += "/SoundRecorder";

		File dir = new File(filename);
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				// failed to create dir
				Toast.makeText(getApplicationContext(), R.string.failed_to_create_dir, Toast.LENGTH_SHORT).show();
				return null; 
			}
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH);
		String currentDateandTime = sdf.format(new Date());
		File file = new File(dir, "REC_" + currentDateandTime + ".pcm");
		int count = 1;
		while (file.exists()) {
			file = new File(dir, "REC_" + currentDateandTime + "_" + String.valueOf(count) + ".pcm");
		}

		return file;
	}
}
