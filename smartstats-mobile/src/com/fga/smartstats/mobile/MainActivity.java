package com.fga.smartstats.mobile;

import static com.fga.smartstats.mobile.Constants.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";
	private static final int UI_REFRESH_PERIOD = 1000;
	private static final int MS_FACTOR = 3600000;
	private Handler mHandler = new Handler();
	private PackageManager pm;
	private ActivityManager activity_manager = null;
	private List<RunningAppProcessInfo> recentTasks;
	private HashMap<String, RunningAppProcessInfo> packageList = new HashMap<String, ActivityManager.RunningAppProcessInfo>();
	private int batteryLevel;
	private int charging;
	private ArrayAdapter<Object> mAdapter;
	private SharedPreferences sharedPref;
	private int sendPeriod = MS_FACTOR;
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
		intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
		registerReceiver(batteryBroadcastReceiver, new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED));
		activity_manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		recentTasks = activity_manager.getRunningAppProcesses();
		pm = getPackageManager();
		sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		sendPeriod = sharedPref.getInt(KEY_DATA_SEND_PERIOD, 1) * MS_FACTOR;
		mHandler.postDelayed(updateUITask, UI_REFRESH_PERIOD);
		mHandler.postDelayed(sendDataTask, sendPeriod);
		fillList();
	
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		recentTasks = activity_manager.getRunningAppProcesses();
		fillList();
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(batteryBroadcastReceiver);
		mHandler.removeCallbacks(updateUITask);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch(item.getItemId()){
		case R.id.action_settings:
			startActivity(new Intent(this, SettingsActivity.class));
		}
		return super.onOptionsItemSelected(item);
	}
	

	private final BroadcastReceiver batteryBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.i(TAG, "Received info");
			charging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			
			if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
				batteryLevel = getBatteryLevel(intent);
			}
		}
	};
	
	private Runnable updateUITask = new Runnable() {
		
		@Override
		public void run() {
			updateUI();
			mHandler.postDelayed(updateUITask, UI_REFRESH_PERIOD);
			
		}
		
	}; 
	
	private Runnable sendDataTask = new Runnable() {
		
		@Override
		public void run() {
			new SendDataAsyncTask().execute();
			mHandler.postDelayed(sendDataTask, sendPeriod);
			
		}
	};
	
	private class SendDataAsyncTask extends AsyncTask<String, Integer, String>{

		@Override
		protected String doInBackground(String... params) {
			
			sendData();
			return null;
		}
		
	}
	
	private void sendData(){
		
		String data = prepareData();
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost postData = new HttpPost("http://smartstats.info/smartstats-web");
		try {
			// create a list to store HTTP variables and their values
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	                // add an HTTP variable and value pair
			nameValuePairs.add(new BasicNameValuePair("data", data));
			postData.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	                // send the variable and value, in other words post, to the URL
			httpClient.execute(postData);
		} catch (ClientProtocolException e) {
			Log.e(TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}finally{
			if (postData != null){
				postData.abort();
			}
		}
		
	}
	
	
	
	private String prepareData(){

		// Prepare data
		JSONObject data = new JSONObject();
		try {
			data.put("batteryLevel", batteryLevel);
		
			JSONArray runningApps = new JSONArray();
			for (Entry<String, RunningAppProcessInfo> entry : packageList.entrySet()){
				RunningAppProcessInfo processInfo = entry.getValue();
				CharSequence appName;
				try{
					appName = pm.getApplicationLabel(pm.getApplicationInfo(
							processInfo.processName, PackageManager.GET_META_DATA));
					
				}catch (Exception e) {
					continue;
				}
				JSONObject obj = new JSONObject();
				obj.put("packageName", processInfo.processName);
				obj.put("applicationName", appName);
				runningApps.put(obj);
			}
			
				
		
			data.put("date", new Date().getTime());
			data.put("apps", runningApps);
			data.put("model", android.os.Build.MODEL);
			data.put("manifacturer", android.os.Build.MANUFACTURER);
			data.put("version", android.os.Build.VERSION.RELEASE);
		} catch (JSONException e) {
			Log.e(TAG,
					"Error while getting battery information or running applications");
			Log.w(TAG, "Skipping send data");
		}
		
		return data.toString();
			
	}
	
	private void updateUI(){
		
		StringBuffer sb = new StringBuffer();
		sb.append("Battery Level: ");
		sb.append(batteryLevel);
		sb.append("% ");
		TextView batteryView = (TextView) findViewById(R.id.batterystatus);
		ProgressBar batteryBar = (ProgressBar) findViewById(R.id.progressbar);
		batteryBar.setProgress(batteryLevel);
	
		if (charging == BatteryManager.BATTERY_STATUS_CHARGING){
			sb.append(" Charging");
			
		}else if (charging == BatteryManager.BATTERY_STATUS_NOT_CHARGING){
			sb.append(" Not Charging");
			
		}else{
			sb.append(" Charging complete");
		}
		
		batteryView.setText(String.valueOf(sb.toString()));

	}
	
	private void fillList(){
		ListView listView = (ListView) findViewById(R.id.appslist);

		
		Iterator<RunningAppProcessInfo> iter = recentTasks.iterator();
		RunningAppProcessInfo info = null;
		
		while(iter.hasNext()){
			info = iter.next();
			if (info.processName.equals(this.getPackageName()) || info.importance == RunningAppProcessInfo.IMPORTANCE_BACKGROUND){
				continue;
			}
			try{
				pm.getApplicationLabel(pm.getApplicationInfo(
						info.processName, PackageManager.GET_META_DATA));
				
			}catch (Exception e) {
				continue;
			}
			 	
			
			packageList.put(info.processName, info);
		
		}
		mAdapter = new AppsAdapter(this, packageList.keySet().toArray());
		listView.setAdapter(mAdapter);
		
		
	}

	private static int getBatteryLevel(final Intent intent) {
		int rawlevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		int level = -1;
		if (rawlevel >= 0 && scale > 0) {
			level = (rawlevel * 100) / scale;
		}
	
		return level;
	}
	
	
		
	

}
