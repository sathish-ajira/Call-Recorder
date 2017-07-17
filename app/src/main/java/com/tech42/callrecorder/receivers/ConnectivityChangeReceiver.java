package com.tech42.callrecorder.receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import com.tech42.callrecorder.services.CallRecorderService;
import com.tech42.callrecorder.views.AutomaticSyncActivity;
import com.tech42.callrecorder.views.MainActivity;
import com.tech42.callrecorder.views.SaveDialogActivity;

/**
 * Created by sathish on 15/07/17.
 */

public class ConnectivityChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkStateReceiver";

    @Override
    public void onReceive(final Context context, final Intent intent) {

        Log.d(TAG, "Network connectivity change");

        if (intent.getExtras() != null) {
            final ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo ni = connectivityManager.getActiveNetworkInfo();

            if (ni != null && ni.isConnectedOrConnecting()) {
                Toast.makeText(context,  "Network " + ni.getTypeName() + " connected", Toast.LENGTH_SHORT).show();
//                Intent nextIntent = new Intent();
//                nextIntent.setClassName("com.tech42.callrecorder", "com.tech42.callrecorder.views.AutomaticSyncActivity");
//                nextIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                context.startActivity(nextIntent);
                Intent in = new Intent(context,AutomaticSyncActivity.class);
                in.putExtra("interval",5001);
                context.startService(in);
            } else if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
                Toast.makeText(context, "There's no network connectivity", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

