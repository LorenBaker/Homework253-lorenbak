package com.lbconsulting.homework253_lorenbak;

import java.text.NumberFormat;

import android.app.Activity;
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
import android.widget.TextView;

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
			getStatus();
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
	private Button btnGetStatus;
	private TextView serviceTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		int threadID = android.os.Process.myTid();
		MyLog.d("Main_ACTIVITY", "onCreate() on ThreadID: " + threadID);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/*this.registerReceiver(mGoodbyeTextReceiver, new IntentFilter("com.lbconsulting.homework253_lorenbak.GOODBYE"));*/

		setContentView(R.layout.activity_main);

		serviceTextView = (TextView) findViewById(R.id.serviceTextView);
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

		btnGetStatus = (Button) findViewById(R.id.btnGetStatus);
		btnGetStatus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getStatus();
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

	protected void getStatus() {
		if (mServiceIsBound) {
			// $REMOTE
			StringBuilder sb = new StringBuilder();
			try {
				Bundle statusBundle = mAlarmService.getStatus();
				if (statusBundle != null) {
					mAlarmServiceStatus = statusBundle.getInt("status");

					switch (mAlarmServiceStatus) {
					case AlarmService.ALARM_RUNNING:
						NumberFormat nf = NumberFormat.getInstance();
						long elapsedTime = statusBundle.getLong("elapsedTime");
						long numberOfBeeps = statusBundle.getLong("numberOfBeeps");
						sb.append("Alarm is running:");
						sb.append(System.getProperty("line.separator"));
						sb.append("   with an elapsed time of ");
						sb.append(nf.format(elapsedTime));
						sb.append(" seconds");
						sb.append(System.getProperty("line.separator"));
						sb.append("   and has provided ");
						sb.append(nf.format(numberOfBeeps));
						sb.append(" beeps for your listening enjoyment!");
						break;

					case AlarmService.ALARM_STOPPED:
						sb.append("Alarm is STOPPED.");
						break;

					case AlarmService.ALARM_SERVICE_NOT_BOUND:
						sb.append("!!!!! Alarm servie not bound !!!!!");
						break;

					case AlarmService.ALARM_SERVICE_STARTED:
						sb.append("Alarm service started.");
						break;

					default:
						sb.append("!!!!! Unknown Alarm service status !!!!!");
						break;
					}
				} else {
					// error ... 
					MyLog.e("Main_ACTIVITY", "getStatus(). AlarmService bound but unable to get its status.");
					sb.append("ERROR: AlarmService bound but unable to get its status.");
					mAlarmServiceStatus = AlarmService.ALARM_SERVICE_NOT_BOUND;
				}
				serviceTextView.setText(sb.toString());

			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			mAlarmServiceStatus = AlarmService.ALARM_SERVICE_NOT_BOUND;
			serviceTextView.setText("Unable to get AlarmService status. The AlarmService is NOT bound!");
		}

	}

	protected void startAlarm() {
		MyLog.d("Main_ACTIVITY", "startAlarm()");
		if (mAlarmService != null) {
			try {
				mAlarmService.startAlarm();
				getStatus();
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
				getStatus();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
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
	protected void onRestart() {
		MyLog.i("Main_ACTIVITY", "onRestart()");
		super.onRestart();
	}

	@Override
	protected void onResume() {
		MyLog.i("Main_ACTIVITY", "onResume()");
		super.onResume();
		// TODO retrieve activity state
		// Get the between instance stored values
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
		getStatus();
		// TODO save activity state
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

		// Cleanup our receiver
		/*this.unregisterReceiver(mGoodbyeTextReceiver);*/

		super.onStop();
	}

	/*	@Override
		public boolean onMenuItemSelected(int featureId, MenuItem item) {

			return super.onMenuItemSelected(featureId, item);
		}*/

	@Override
	protected void onDestroy() {
		MyLog.i("Main_ACTIVITY", "onDestroy()");
		// Stop the AlarmService if the activity is being destroyed
		// and the service's alarm is not running.
		// Otherwise, the AlarmService remains alive.
		if (mAlarmServiceStatus != AlarmService.ALARM_RUNNING) {
			stopService(new Intent(this, AlarmService.class));
		}
		super.onDestroy();
	}

	/*	@Override
		public boolean onCreateOptionsMenu(Menu menu) {
			MyLog.i("Main_ACTIVITY", "onCreateOptionsMenu()");
			// Inflate the menu; this adds items to the action bar if it is present.
			getMenuInflater().inflate(R.menu.main, menu);
			return true;
		}*/

}
