package com.mohammadag.soundrecorder.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import com.mohammadag.soundrecorder.R;

public class SettingsFragment extends PreferenceFragment {
	private static final String URL_MY_APPS = "market://search?q=pub:Mohammad Abu-Garbeyyeh";
	public static final String KITKAT_TRANSLUCENCY_KEY = "pref_kitkat_translucent_navbar";
	public static final String BACK_BUTTON_ALWAYS_QUITS = "pref_back_button_always_quits";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
		initCopyright();
		initKitKatTranslucencySettings();
	}

	private void initKitKatTranslucencySettings() {
		Preference kitKatPreference = findPreference("pref_kitkat_translucent_navbar");

		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
			getPreferenceScreen().removePreference(kitKatPreference);
		} else {
			kitKatPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {	
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Toast.makeText(getActivity(), R.string.requires_restart, Toast.LENGTH_SHORT).show();
					return true;
				}
			});
		}
	}

	private void initCopyright() {
		Preference copyrightPreference = findPreference("copyright_key");
		copyrightPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setPackage("com.android.vending");
				Uri uri = Uri.parse(URL_MY_APPS);
				try {
					startActivity(intent.setData(uri));
				} catch (ActivityNotFoundException e) {
					intent.setPackage("");
					startActivity(intent);
				}
				return false;
			}
		});
	}
}
