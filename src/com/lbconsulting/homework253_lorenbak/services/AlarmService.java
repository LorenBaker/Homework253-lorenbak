package com.lbconsulting.homework253_lorenbak.services;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;

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

import com.lbconsulting.homework253_lorenbak.MyLog;

public class AlarmService extends Service implements OnLoadCompleteListener {

	public static final int ALARM_RUNNING = 10;
	public static final int ALARM_STOPPED = 11;
	private int status = ALARM_STOPPED;
	private Calendar alarmStartTime;
	private long numberOfBeeps = 0;

	private boolean mIsAlarmRunning = false;
	private Handler mHeartbeatHandler = new Handler();
	private AlarmRunnable mAlarmRunnable = new AlarmRunnable();

	private int mBeep08a;
	private SoundPool mSoundPool;
	private HashMap<Integer, SoundResource> mSoundResources = new HashMap<Integer, SoundResource>();

	//private AudioManager mAudioManager;

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
				int threadID = android.os.Process.myTid();
				MyLog.i("AlarmService", "Alarm running from threadID: " + threadID);
				soundAlarm();
				numberOfBeeps++;
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
		public void getGoodbye() throws RemoteException {
			AlarmService.this.getGoodbye();
		}

		@Override
		public String getHello() throws RemoteException {
			return AlarmService.this.getHello();
		}

		@Override
		public Bundle getStatus() throws RemoteException {
			return AlarmService.this.getStatus();
		}
	}

	// Call for the actual bind
	@Override
	public IBinder onBind(Intent intent) {
		MyLog.d("AlarmService", "onBind() intent: " + intent);
		startAlarm();
		return new UpdateBinderProxy();
	}

	/**
	 * Public method to expose functionality.
	 * 
	 * @return
	 */
	public void getGoodbye() {
		MyLog.d("AlarmService", "getGoodbye()");
		Intent goodbyeIntent = new Intent();
		goodbyeIntent.setAction("com.lbconsulting.homework253_lorenbak.GOODBYE");
		this.sendBroadcast(goodbyeIntent);
	}

	private void startAlarm() {
		mIsAlarmRunning = true;
		status = ALARM_RUNNING;
		alarmStartTime = Calendar.getInstance();
		mHeartbeatHandler.postDelayed(mAlarmRunnable, 500);
	}

	private void stopAlarm() {
		mIsAlarmRunning = false;
		status = ALARM_STOPPED;
		numberOfBeeps = 0;
	}

	/**
	 * Public method to expose functionality.
	 * 
	 * @return
	 */
	public String getHello() {
		MyLog.d("AlarmService", "getHello()");

		return "Hello";
	}

	public Bundle getStatus() {
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
			//afd.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		MyLog.d("AlarmService", "onStart() intent: " + intent);
		startAlarm();
		super.onStart(intent, startId);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		MyLog.d("AlarmService", "onUnbind() intent: " + intent);
		return super.onUnbind(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		MyLog.d("AlarmService", "onStartCommand() intent: " + intent);

		// START_NOT_STICKY - Icky We want to live on, live free!
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		MyLog.d("AlarmService", "onDestroy()");
		stopAlarm();

		// LEARN: We are forcing an exit here just to show the :service process disappears
		System.exit(0);

	}

	@Override
	public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
		MyLog.i("AlarmService", "SoundPool.OnLoadCompleteListener: onLoadComplete");
		mSoundResources.put(sampleId, new SoundResource(sampleId, true, 1f));
	}

}
