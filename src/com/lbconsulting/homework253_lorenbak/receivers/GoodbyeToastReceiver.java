package com.lbconsulting.homework253_lorenbak.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.lbconsulting.homework253_lorenbak.MyLog;

public class GoodbyeToastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		MyLog.i("GoodbyeToastReceiver", "onReceive() intent: " + intent);
		// If we receive anything it is the GOODBYE action, so just alert the UI

		Toast.makeText(context, "GoodbyeReceiver says Goodbye!!!", Toast.LENGTH_SHORT).show();

	}

}
