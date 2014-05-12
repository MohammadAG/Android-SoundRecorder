package com.mohammadag.soundrecorder.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.mohammadag.soundrecorder.R;
import com.mohammadag.soundrecorder.RecordingMode;
import com.mohammadag.soundrecorder.RecordingService;
import com.mohammadag.soundrecorder.RecordingService.OnAudioLevelChangedListener;
import com.mohammadag.soundrecorder.activities.FragmentHolderActivity.ActivityType;
import com.mohammadag.soundrecorder.adapters.DrawerMenuArrayAdapter;
import com.mohammadag.soundrecorder.fragments.AboutFragment;
import com.mohammadag.soundrecorder.fragments.RecordingControlsFragment;
import com.mohammadag.soundrecorder.fragments.RecordingStatusFragment;
import com.mohammadag.soundrecorder.fragments.RecordingsListFragment;
import com.mohammadag.soundrecorder.fragments.SettingsFragment;

public class MainActivity extends Activity implements OnAudioLevelChangedListener, OnItemClickListener {
	@SuppressWarnings("unused")
	private static final String TAG = MainActivity.class.getSimpleName();

	private static final int REQUEST_CODE_SETTINGS = 0;

	private RecordingService mRecordingService;

	private RecordingStatusFragment mRecordingStatusFragment;
	private RecordingControlsFragment mRecordingControlsFragment;
	private RecordingsListFragment mRecordingsListFragment;
	private AboutFragment mAboutFragment;
	private SettingsFragment mSettingsFragment;

	private boolean mRecordingQueued = false;
	private boolean mIsKitKatTranslucencyEnabled = false;
	private boolean mBackButtonAlwaysQuits = false;
	private boolean mIsBound = false;

	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;

	private DrawerMenuArrayAdapter mDrawerArrayAdapter;

	private BroadcastReceiver mStateChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (RecordingService.INTENT_RECORDING_STARTED.equals(intent.getAction())) {
				String filename = intent.getStringExtra("filename");
				mRecordingStatusFragment.setRecordingMode(RecordingMode.RECORDING);
				mRecordingStatusFragment.setFileName(filename.replace(".pcm", ""));
				mRecordingControlsFragment.onRecordingStateChanged(RecordingMode.RECORDING);
				getActionBar().setTitle(R.string.state_recording);
			} else if (RecordingService.INTENT_RECORDING_STOPPED.equals(intent.getAction())) {
				mRecordingStatusFragment.setRecordingMode(RecordingMode.IDLE);
				mRecordingControlsFragment.onRecordingStateChanged(RecordingMode.IDLE);
				getActionBar().setTitle(R.string.app_name);
			}
		}
	};

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mRecordingService = ((RecordingService.ServiceBinder)service).getService();
			mRecordingService.setOnTimerChangedListener(mRecordingStatusFragment);
			mRecordingService.setOnAudioLevelChanged(MainActivity.this);
			if (mRecordingQueued) {
				mRecordingService.startRecording();
				mRecordingQueued = false;
			}

			if (mRecordingService.isRecording()) {
				mRecordingStatusFragment.setRecordingMode(RecordingMode.RECORDING);
				mRecordingStatusFragment.setFileName(mRecordingService.getFilename().replace(".pcm", ""));
				mRecordingControlsFragment.onRecordingStateChanged(RecordingMode.RECORDING);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			mRecordingService = null;
		}
	};

	void doBindService() {
		bindService(new Intent(MainActivity.this, 
				RecordingService.class), mConnection, Context.BIND_AUTO_CREATE);

		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@TargetApi(android.os.Build.VERSION_CODES.KITKAT)
	private void enableKitKatTranslucency() {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
					WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mBackButtonAlwaysQuits = prefs.getBoolean(SettingsFragment.BACK_BUTTON_ALWAYS_QUITS, false);
		mIsKitKatTranslucencyEnabled = prefs.getBoolean(SettingsFragment.KITKAT_TRANSLUCENCY_KEY, true);
		prefs = null;
		if (mIsKitKatTranslucencyEnabled)
			enableKitKatTranslucency();

		startService(new Intent(this, RecordingService.class));
		doBindService();

		if (savedInstanceState == null) {
			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			mRecordingStatusFragment = new RecordingStatusFragment();
			mRecordingStatusFragment.setRetainInstance(true);

			mRecordingControlsFragment = new RecordingControlsFragment();
			mRecordingControlsFragment.setRetainInstance(true);

			mRecordingControlsFragment.setRecordButtonCallback(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					RecordingMode mode;
					if (mRecordingService == null)
						mode = mRecordingStatusFragment.getRecordingMode();
					else
						mode = mRecordingService.getRecordingMode();
					switch (mode) {
					case IDLE:
						if (mRecordingStatusFragment != null) {
							mRecordingStatusFragment.setTimeFromSeconds(0);
							mRecordingStatusFragment.clearAudioBars();
						}
						MediaPlayer openPlayer = MediaPlayer.create(getApplicationContext(), R.raw.open);
						if (openPlayer == null) {
							startRecording();
						} else {
							openPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
								@Override
								public void onCompletion(MediaPlayer mp) {
									startRecording();
								}
							});
							openPlayer.start();
						}
						break;
					case RECORDING:
					default:
						mRecordingService.stopRecording();
						MediaPlayer successPlayer = MediaPlayer.create(getApplicationContext(), R.raw.success);
						if (successPlayer != null) {
							successPlayer.start();
						}
						break;
					}
				}
			});
			transaction.add(R.id.container, mRecordingControlsFragment);
			transaction.add(R.id.status_container, mRecordingStatusFragment);
			transaction.commit();
		}

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerToggle = new ActionBarDrawerToggle(
				this, mDrawerLayout, R.drawable.ic_navigation_drawer, 
				R.string.open_drawer, R.string.close_drawer) {

			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				if (mRecordingService != null && mRecordingService.getRecordingMode() != RecordingMode.RECORDING)
					getActionBar().setTitle(mDrawerArrayAdapter.getCurrentTitle());
				else
					getActionBar().setTitle(R.string.state_recording);
			}

			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				if (mRecordingService != null && mRecordingService.getRecordingMode() != RecordingMode.RECORDING)
					getActionBar().setTitle(R.string.app_name);
				else
					getActionBar().setTitle(R.string.state_recording);
			}
		};

		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		ListView listView = (ListView) findViewById(R.id.left_drawer);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT
				&& mIsKitKatTranslucencyEnabled) {
			listView.setPadding(listView.getPaddingLeft(),
					listView.getPaddingTop() + getNavDrawerPadding(),
					listView.getPaddingRight(), listView.getPaddingBottom());
		}
		mDrawerArrayAdapter = new DrawerMenuArrayAdapter(getApplicationContext());
		listView.setAdapter(mDrawerArrayAdapter);
		listView.setOnItemClickListener(this);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
	}

	private int getNavDrawerPadding() {
		TypedValue tv = new TypedValue();
		int actionBarHeight = 0;
		if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
			actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
		}

		return getStatusBarHeight() + actionBarHeight;
	}

	private int getStatusBarHeight() { 
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		} 
		return result;
	} 

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	private void switchToNewRecording() {
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		removeAllFragments(transaction);
		transaction.add(R.id.container, mRecordingControlsFragment);
		transaction.add(R.id.status_container, mRecordingStatusFragment);
		transaction.commit();
	}

	private void switchToRecordings() {
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		removeAllFragments(transaction);
		transaction.add(R.id.container, getRecordingsListFragment());
		transaction.commit();
	}

	private void removeAllFragments(FragmentTransaction transaction) {
		transaction.remove(mRecordingStatusFragment);
		transaction.remove(mRecordingControlsFragment);
		transaction.remove(getRecordingsListFragment());

		if (mAboutFragment != null) {
			transaction.remove(mAboutFragment);
			mAboutFragment = null;
		}

		if (mSettingsFragment != null) {
			transaction.remove(mSettingsFragment);
		}
	}

	private RecordingsListFragment getRecordingsListFragment() {
		if (mRecordingsListFragment == null)
			mRecordingsListFragment = new RecordingsListFragment();

		return mRecordingsListFragment;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	protected void startRecording() {
		if (mRecordingService != null)
			mRecordingService.startRecording();
		else
			mRecordingQueued = true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		if (!mBackButtonAlwaysQuits
				&& mDrawerArrayAdapter.getSelectedItemPosition() != DrawerMenuArrayAdapter.NEW_RECORDING_POS) {
			onItemClick(null, null, 0, 0);
			return;
		}
		super.onBackPressed();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_SETTINGS) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			mBackButtonAlwaysQuits = prefs.getBoolean(SettingsFragment.BACK_BUTTON_ALWAYS_QUITS, false);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
		unregisterReceiver(mStateChangedReceiver);
		if (mRecordingService.getRecordingMode() == RecordingMode.IDLE) {
			mRecordingService.stopSelf();
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter iF = new IntentFilter();
		iF.addAction(RecordingService.INTENT_RECORDING_STARTED);
		iF.addAction(RecordingService.INTENT_RECORDING_STOPPED);
		registerReceiver(mStateChangedReceiver, iF);

		if (mRecordingService == null)
			return;

		if (mRecordingStatusFragment != null)
			mRecordingStatusFragment.setRecordingMode(mRecordingService.getRecordingMode());

		if (mRecordingControlsFragment != null)
			mRecordingControlsFragment.onRecordingStateChanged(mRecordingService.getRecordingMode());

		if (mRecordingService.getRecordingMode() == RecordingMode.IDLE)
			getActionBar().setTitle(R.string.app_name);
		else if (mRecordingService.getRecordingMode() == RecordingMode.RECORDING)
			getActionBar().setTitle(R.string.state_recording);
	}

	@Override
	public void onAudioLevelChanged(final int percentage) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (mRecordingControlsFragment != null)
					mRecordingControlsFragment.onAudioLevelChanged(percentage);

				if (mRecordingStatusFragment != null)
					mRecordingStatusFragment.onAudioLevelChanged(percentage);
			}
		});
	}

	private void showAbout() {
		FragmentHolderActivity.startActivity(this, ActivityType.ABOUT, null);
	}

	private void showSettings() {
		Bundle bundle = FragmentHolderActivity.getBundleOfColor(Color.parseColor("#666666"));
		FragmentHolderActivity.startActivityForResult(this,
				ActivityType.SETTINGS, REQUEST_CODE_SETTINGS, bundle);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		if (mDrawerArrayAdapter.getSelectedItemPosition() == position) {
			mDrawerLayout.closeDrawers();
			return;
		}

		mDrawerLayout.closeDrawers();

		switch (position) {
		case DrawerMenuArrayAdapter.NEW_RECORDING_POS:
			switchToNewRecording();
			break;
		case DrawerMenuArrayAdapter.RECORDINGS_LIST_POS:
			switchToRecordings();
			break;
		case DrawerMenuArrayAdapter.SETTINGS_POS:
			showSettings();
			return;
		case DrawerMenuArrayAdapter.ABOUT_POS:
			showAbout();
			return;
		}

		mDrawerArrayAdapter.setItemSelected(position);

		if (mRecordingService != null && mRecordingService.getRecordingMode() != RecordingMode.RECORDING)
			getActionBar().setTitle(mDrawerArrayAdapter.getCurrentTitle());
	}

	public void setPrettyName(String string) {
		mRecordingService.setNextPrettyRecordingName(string);
		mRecordingStatusFragment.setFileName(string);
	}
}
