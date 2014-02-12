package com.lbconsulting.homework253_lorenbak;

import java.text.NumberFormat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
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

	private Handler mGoodbyeHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			TextView tv = (TextView) findViewById(R.id.serviceTextView);
			tv.setText(tv.getText().toString() + " Goodbye ");
			super.handleMessage(msg);
		}
	};

	private GoodbyeTextReceiver mGoodbyeTextReceiver = new GoodbyeTextReceiver();

	private class GoodbyeTextReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			MyLog.i("Main_ACTIVITY", "onReceive() intent: " + intent);
			mGoodbyeHandler.sendEmptyMessage(1);
		}
	}

	/**
	 * Service Binding callbacks
	 **/
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			MyLog.d("Main_ACTIVITY", "onServiceConnected");

			// $REMOTE
			mAlarmService = IAlarmService.Stub.asInterface(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			MyLog.d("Main_ACTIVITY", "onServiceDisconnected");

			mServiceIsBound = false;
			// $REMOTE
			mAlarmService = null;
		}
	};

	private Button btnStartStop;
	private Button btnGetStatus;
	/*private Button getHelloButton;*/
	private TextView serviceTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		int threadID = android.os.Process.myTid();
		MyLog.d("Main_ACTIVITY", "onCreate() on ThreadID: " + threadID);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		this.registerReceiver(mGoodbyeTextReceiver, new IntentFilter("com.lbconsulting.homework253_lorenbak.GOODBYE"));

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
					startService();
				} else {
					btnStartStop.setText(startText);
					stopService();
				}
			}
		});

		btnGetStatus = (Button) findViewById(R.id.btnGetStatus);
		btnGetStatus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mServiceIsBound) {
					// $REMOTE
					StringBuilder sb = new StringBuilder();
					try {
						Bundle statusBundle = mAlarmService.getStatus();
						if (statusBundle != null) {
							int status = statusBundle.getInt("status");

							switch (status) {
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

							default:
								break;
							}
						}
						serviceTextView.setText(sb.toString());

					} catch (RemoteException e) {
						e.printStackTrace();
					}
				} else {
					serviceTextView.setText("Alarm is STOPPED. Alarm service is NOT bound!");
				}
			}
		});

		/*		getGoodbyeButton = (Button) findViewById(R.id.getGoodbyeButton);
				getGoodbyeButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (mServiceIsBound) {
							// $REMOTE
							try {
								mAlarmService.getGoodbye();
							} catch (RemoteException e) {
								e.printStackTrace();
							}
						}
					}
				});*/
	}

	protected void startService() {
		MyLog.d("Main_ACTIVITY", "startService");
		if (mServiceIsBound) {
			MyLog.w("Main_ACTIVITY", "startService()... Service already bound!");
		} else {
			bindService(new Intent(this, AlarmService.class), mConnection, Context.BIND_AUTO_CREATE);
			mServiceIsBound = true;
			/*findViewById(R.id.getGoodbyeButton).setEnabled(mServiceIsBound);
			findViewById(R.id.getHelloButton).setEnabled(mServiceIsBound);*/
		}
	}

	protected void stopService() {
		MyLog.d("Main_ACTIVITY", "stopService");

		if (mServiceIsBound) {
			unbindService(mConnection);
			mServiceIsBound = false;
			/*findViewById(R.id.getGoodbyeButton).setEnabled(mServiceIsBound);
			findViewById(R.id.getHelloButton).setEnabled(mServiceIsBound);*/
		}
	}

	@Override
	protected void onStart() {
		MyLog.i("Main_ACTIVITY", "onStart");
		super.onStart();
	}

	@Override
	protected void onRestart() {
		MyLog.i("Main_ACTIVITY", "onRestart");
		super.onRestart();
	}

	@Override
	protected void onResume() {
		MyLog.i("Main_ACTIVITY", "onResume");
		super.onResume();

	}

	@Override
	protected void onPause() {
		MyLog.i("Main_ACTIVITY", "onPause");
		super.onPause();
	}

	@Override
	protected void onStop() {
		MyLog.i("Main_ACTIVITY", "onStop");

		// Unbind the service
		if (mServiceIsBound) {
			unbindService(mConnection);
			mServiceIsBound = false;
		}

		/*findViewById(R.id.getGoodbyeButton).setEnabled(mServiceIsBound);
		findViewById(R.id.getHelloButton).setEnabled(mServiceIsBound);*/

		// Cleanup our receiver
		this.unregisterReceiver(mGoodbyeTextReceiver);

		super.onStop();
	}

	/*	@Override
		public boolean onMenuItemSelected(int featureId, MenuItem item) {

			return super.onMenuItemSelected(featureId, item);
		}*/

	@Override
	protected void onDestroy() {
		MyLog.i("Main_ACTIVITY", "onDestroy");
		super.onDestroy();
	}

	/*	@Override
		public boolean onCreateOptionsMenu(Menu menu) {
			MyLog.i("Main_ACTIVITY", "onCreateOptionsMenu");
			// Inflate the menu; this adds items to the action bar if it is present.
			getMenuInflater().inflate(R.menu.main, menu);
			return true;
		}*/

}
