package com.lbconsulting.homework253_lorenbak.services;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;

import com.lbconsulting.homework253_lorenbak.MainActivity;
import com.lbconsulting.homework253_lorenbak.MyLog;
import com.lbconsulting.homework253_lorenbak.R;

public class AlarmService extends Service implements OnLoadCompleteListener {

	public static final int ALARM_RUNNING = 10;
	public static final int ALARM_STOPPED = 11;
	public static final int ALARM_SERVICE_NOT_BOUND = 12;
	public static final int ALARM_SERVICE_STARTED = 13;

	private int HW252ALARM = 100;

	private int status = ALARM_SERVICE_NOT_BOUND;
	private Calendar alarmStartTime;
	private long numberOfBeeps = 0;

	private boolean mIsAlarmRunning = false;
	private Handler mHeartbeatHandler = new Handler();
	private AlarmRunnable mAlarmRunnable = new AlarmRunnable();

	private int mBeep08a;
	private SoundPool mSoundPool;
	private HashMap<Integer, SoundResource> mSoundResources = new HashMap<Integer, SoundResource>();

	private class SoundResource {

		public SoundResource(int id, boolean loaded, float volume) {
			this.id = id;
			this.loaded = loaded;
			this.volume = volume;
		}

		public boolean loaded;
		public int id;
		public float volume;
	}

	private class AlarmRunnable implements Runnable {

		@Override
		public void run() {

			if (mIsAlarmRunning) {
				numberOfBeeps++;

				int threadID = android.os.Process.myTid();
				StringBuilder sb = new StringBuilder();
				sb.append("Alarm running: threadID = ");
				sb.append(threadID);
				sb.append("; Number of Beeps = ");
				sb.append(numberOfBeeps);
				MyLog.i("AlarmService", sb.toString());

				soundAlarm();
				sendNotification(); // Yes I know that it is probably too much to send a notification every 5 seconds.
				mHeartbeatHandler.postDelayed(this, 5000);
			}
		}

		private void soundAlarm() {
			final SoundResource sr = mSoundResources.get(mBeep08a);
			if (sr == null) {
				MyLog.e("AlarmService", "soundAlarm() Cound not find SoundResource for ID: " + mBeep08a);
				return;
			}

			if (sr.loaded) {
				mSoundPool.play(sr.id, 0.5f, 0.5f, 1, 0, 1f);
			}
		}
	}

	/** $REMOTE **/
	public class UpdateBinderProxy extends IAlarmService.Stub {

		@Override
		public void startAlarm() throws RemoteException {
			AlarmService.this.startAlarm();

		}

		@Override
		public void stopAlarm() throws RemoteException {
			AlarmService.this.stopAlarm();

		}
	}

	@Override
	public void onCreate() {
		MyLog.d("AlarmService", "onCreate()");
		super.onCreate();

		mSoundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
		mSoundPool.setOnLoadCompleteListener(this);

		AssetManager am = this.getAssets();
		AssetFileDescriptor afd;
		try {
			afd = am.openFd("beep08a.mp3");
			mBeep08a = mSoundPool.load(afd, 1);
			afd.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendNotification() {

		Intent hw253AlarmIntent = new Intent(this, MainActivity.class);

		// Create PendingIntent
		PendingIntent pendingHW253AlarmIntent = PendingIntent.getActivity(
				this,
				HW252ALARM,
				hw253AlarmIntent,
				PendingIntent.FLAG_UPDATE_CURRENT
				);

		// Format the strings to use for the Notification
		String notificationTitle = getString(R.string.alarm_running);
		Calendar now = Calendar.getInstance();
		long elapsedTime = now.getTimeInMillis() - alarmStartTime.getTimeInMillis();
		elapsedTime = elapsedTime / 1000;

		StringBuilder sb = new StringBuilder();
		NumberFormat nf = NumberFormat.getInstance();

		sb.append("Elapsed time: ");
		sb.append(nf.format(elapsedTime));
		sb.append(" seconds; ");
		sb.append(nf.format(numberOfBeeps));
		sb.append(" total beeps!");

		// Create and use a Book Notification

		Notification hw253Notification = new NotificationCompat.Builder(this)
				.setContentTitle(notificationTitle)
				.setContentText(sb.toString())
				.setContentIntent(pendingHW253AlarmIntent)
				.setSmallIcon(android.R.drawable.ic_popup_reminder)
				.build();

		// Use the NotificationManager to notify
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(HW252ALARM, hw253Notification);
		MyLog.d("AlarmService", "sendNotification()");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		MyLog.d("AlarmService", "onStartCommand() intent: " + intent);
		status = ALARM_STOPPED;
		// START_NOT_STICKY - Icky We want to live on, live free!
		return START_STICKY;
	}

	// Call for the actual bind
	@Override
	public IBinder onBind(Intent intent) {
		MyLog.d("AlarmService", "onBind() intent: " + intent);
		status = ALARM_STOPPED;
		return new UpdateBinderProxy();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		MyLog.d("AlarmService", "onUnbind() intent: " + intent);
		status = ALARM_SERVICE_NOT_BOUND;
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		MyLog.d("AlarmService", "onDestroy()");
	}

	// Public methods to expose functionality.
	private void startAlarm() {
		MyLog.i("AlarmService", "startAlarm()");
		if (!mIsAlarmRunning) {
			mIsAlarmRunning = true;
			status = ALARM_RUNNING;
			alarmStartTime = Calendar.getInstance();
			mHeartbeatHandler.postDelayed(mAlarmRunnable, 400);
		}
	}

	private void stopAlarm() {
		MyLog.i("AlarmService", "stopAlarm()");
		mIsAlarmRunning = false;
		status = ALARM_STOPPED;
		numberOfBeeps = 0;
	}

	public Bundle getStatus() {
		MyLog.i("AlarmService", "getStatus()");
		Bundle bundle = new Bundle();
		bundle.putInt("status", status);
		if (status == ALARM_RUNNING) {
			Calendar now = Calendar.getInstance();
			long elapsedTime = now.getTimeInMillis() - alarmStartTime.getTimeInMillis();
			elapsedTime = elapsedTime / 1000;
			bundle.putLong("elapsedTime", elapsedTime);
			bundle.putLong("numberOfBeeps", numberOfBeeps);
		}
		return bundle;
	}

	@Override
	public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
		MyLog.i("AlarmService", "SoundPool.OnLoadCompleteListener: onLoadComplete()");
		mSoundResources.put(sampleId, new SoundResource(sampleId, true, 1f));
	}

}
