package com.tech42.callrecorder.receivers;

/**
 * Created by sathish on 30/06/17.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.tech42.callrecorder.services.CallRecorderService;

public class CallRecorderReceiver extends BroadcastReceiver {
    SharedPreferences pref;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (null == intent)
            return;
        Intent i = new Intent(CallRecorderService.ACTION);
        if (Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) {
            i.putExtra(CallRecorderService.STATE, CallRecorderService.OUTGOING);
            i.putExtra(Intent.EXTRA_PHONE_NUMBER,
                    intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER));
        } else if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent
                .getAction())) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                i.putExtra(CallRecorderService.STATE,
                        CallRecorderService.INCOMING);
                i.putExtra(Intent.EXTRA_PHONE_NUMBER, intent
                        .getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));
            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                i.putExtra(CallRecorderService.STATE, CallRecorderService.BEGIN);
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                i.putExtra(CallRecorderService.STATE, CallRecorderService.END);
            }
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            i.putExtra(CallRecorderService.STATE, CallRecorderService.START);
        } else if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)
                || intent.getAction().equals(Intent.ACTION_MEDIA_REMOVED)) {
            i.putExtra(CallRecorderService.STATE, CallRecorderService.STORAGE);
        } else {
            return;
        }
        pref = context.getSharedPreferences("MyPref", 0); // 0 - for private mode
        Boolean isEnable = pref.getBoolean("callRecord",true);
        i.setPackage("com.tech42.callrecorder");
        if (isEnable) {
            context.startService(i);
        } else{
            context.stopService(i);
        }
        Log.d("service",
                "AutoRunReceiver startService "
                        + i.getStringExtra(CallRecorderService.STATE) + ":"
                        + i.getStringExtra(Intent.EXTRA_PHONE_NUMBER));
    }
}

