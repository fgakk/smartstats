package com.fga.smartstats.mobile;

import static com.fga.smartstats.mobile.Constants.*;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener{

	
	private ListPreference mListPreference;
	private SeekBarPreference mSeekBarPreference;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		mListPreference = (ListPreference) getPreferenceScreen().findPreference(KEY_CONNECTION_SELECT);
		mSeekBarPreference = (SeekBarPreference) getPreferenceScreen().findPreference(KEY_DATA_SEND_PERIOD);
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mListPreference.setSummary(sharedPreferences.getString(KEY_CONNECTION_SELECT, ""));
		mSeekBarPreference.setSummary(sharedPreferences.getInt(KEY_DATA_SEND_PERIOD, 1) + " hour(s)");
	}

	@Override
	protected void onResume() {
		
		super.onResume();
		 getPreferenceScreen().getSharedPreferences()
         .registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		 getPreferenceScreen().getSharedPreferences()
         .unregisterOnSharedPreferenceChangeListener(this);
	}
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
	
		if (key.equals(KEY_CONNECTION_SELECT)){
			mListPreference.setSummary(sharedPreferences.getString(key, ""));
		}else if (key.equals(KEY_DATA_SEND_PERIOD)){
			mSeekBarPreference.setSummary(sharedPreferences.getInt(key, 1) + " hour(s)");
		}
		
	}
}
