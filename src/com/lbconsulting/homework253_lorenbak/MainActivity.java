package com.lbconsulting.homework253_lorenbak;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.lbconsulting.homework253_lorenbak.services.AlarmService;
import com.lbconsulting.homework253_lorenbak.services.IAlarmService;

public class MainActivity extends Activity {

	// $REMOTE
	private IAlarmService mAlarmService;
	private boolean mServiceIsBound;
	private int mAlarmServiceStatus = AlarmService.ALARM_STOPPED;

	/**
	 * Service Binding callbacks
	 **/
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			MyLog.d("Main_ACTIVITY", "onServiceConnected()");

			// $REMOTE
			mAlarmService = IAlarmService.Stub.asInterface(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			MyLog.d("Main_ACTIVITY", "onServiceDisconnected()");

			mServiceIsBound = false;
			// $REMOTE
			mAlarmService = null;
		}
	};

	private Button btnStartStop;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		int threadID = android.os.Process.myTid();
		MyLog.d("Main_ACTIVITY", "onCreate() on ThreadID: " + threadID);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btnStartStop = (Button) findViewById(R.id.btnStartStop);
		btnStartStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				ToggleButtonText();
			}

			private void ToggleButtonText() {
				String startText = getResources().getString(R.string.btnStart_Text);
				String stopText = getResources().getString(R.string.btnStop_Text);
				if (btnStartStop.getText().toString().equals(startText)) {
					btnStartStop.setText(stopText);
					startAlarm();
				} else {
					btnStartStop.setText(startText);
					stopAlarm();
				}
			}
		});

		// Get the between instance stored values
		SharedPreferences storedStates = getSharedPreferences("HW253", MODE_PRIVATE);
		mAlarmServiceStatus = storedStates.getInt("AlarmServiceStatus", AlarmService.ALARM_STOPPED);

		switch (mAlarmServiceStatus) {
		case AlarmService.ALARM_RUNNING:
			break;
		default:
			// start alarm service
			startService(new Intent(this, AlarmService.class));
			break;
		}

	}

	protected void startAlarm() {
		MyLog.d("Main_ACTIVITY", "startAlarm()");
		if (mAlarmService != null) {
			try {
				mAlarmService.startAlarm();
				mAlarmServiceStatus = AlarmService.ALARM_RUNNING;
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	protected void stopAlarm() {
		MyLog.d("Main_ACTIVITY", "stopAlarm()");
		if (mAlarmService != null) {
			try {
				mAlarmService.stopAlarm();
				clearNotification();
				mAlarmServiceStatus = AlarmService.ALARM_STOPPED;
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	private void clearNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancelAll();

	}

	@Override
	protected void onStart() {
		MyLog.i("Main_ACTIVITY", "onStart()");
		super.onStart();
		// Bind the activity to the AlarmService
		if (mServiceIsBound) {
			MyLog.w("Main_ACTIVITY", "onStart()... Service already bound!");
		} else {
			bindService(new Intent(this, AlarmService.class), mConnection, Context.BIND_AUTO_CREATE);
			mServiceIsBound = true;
		}

	}

	@Override
	protected void onResume() {
		MyLog.i("Main_ACTIVITY", "onResume()");
		super.onResume();
		// retrieve activity state
		SharedPreferences storedStates = getSharedPreferences("HW253", MODE_PRIVATE);
		mAlarmServiceStatus = storedStates.getInt("AlarmServiceStatus", AlarmService.ALARM_STOPPED);

		String startText = getResources().getString(R.string.btnStart_Text);
		String stopText = getResources().getString(R.string.btnStop_Text);
		switch (mAlarmServiceStatus) {
		case AlarmService.ALARM_RUNNING:
			btnStartStop.setText(stopText);
			break;
		default:
			btnStartStop.setText(startText);
			break;
		}
	}

	@Override
	protected void onPause() {
		MyLog.i("Main_ACTIVITY", "onPause()");
		// save activity state
		SharedPreferences preferences = getSharedPreferences("HW253", MODE_PRIVATE);
		SharedPreferences.Editor applicationStates = preferences.edit();
		applicationStates.putInt("AlarmServiceStatus", mAlarmServiceStatus);
		applicationStates.commit();
		super.onPause();
	}

	@Override
	protected void onStop() {
		MyLog.i("Main_ACTIVITY", "onStop()");

		// Unbind the AlarmService
		if (mServiceIsBound) {
			unbindService(mConnection);
			mServiceIsBound = false;
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		MyLog.i("Main_ACTIVITY", "onDestroy()");
		// Stop the AlarmService if the activity is being destroyed
		// AND the service's alarm is not running.
		// Otherwise, the AlarmService remains alive.
		if (mAlarmServiceStatus != AlarmService.ALARM_RUNNING) {
			stopService(new Intent(this, AlarmService.class));
		}
		super.onDestroy();
	}

}
