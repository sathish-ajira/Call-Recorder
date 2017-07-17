package com.tech42.callrecorder.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Dialog;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.ContactsContract.PhoneLookup;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.tech42.callrecorder.R;
import com.tech42.callrecorder.views.SaveDialogActivity;

import io.realm.Realm;

public class CallRecorderService extends Service {
    public static final String ACTION = "com.tech42.callrecorder.CALL_RECORD";
    public static final String STATE = "STATE";
    public static final String START = "START";
    public static final String STORAGE = "STORAGE";
    public static final String INCOMING = "INCOMING";
    public static final String OUTGOING = "OUTGOING";
    public static final String BEGIN = "BEGIN";
    public static final String END = "END";

    protected static final String TAG = CallRecorderService.class.getName();
    protected static final boolean DEBUG = false;

    private static final String AMR_DIR = "/CallRecorder/";
    private static final String IDLE = "";
    private static final String INCOMING_CALL_SUFFIX = "_i";
    private static final String OUTGOING_CALL_SUFFIX = "_o";

    private Context cntx;
    private volatile String fileNamePrefix = IDLE;
    private volatile MediaRecorder recorder;
    private volatile PowerManager.WakeLock wakeLock;
    private volatile boolean isMounted = false;
    private volatile boolean isInRecording = false;

    private String description, contactName, duration, fileName, filePath, mobileNumber, time, type, status;
    public Dialog recordDialog;
    private Realm realm;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Boolean unknownNumber = false;

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        this.cntx = getApplicationContext();
        this.prepareAmrDir();
        recordDialog = new Dialog(getContext());
        sharedPreferences = getApplicationContext().getSharedPreferences("RecordSettings", 0); // 0 - for private mode
        editor = sharedPreferences.edit();
        log("service create");
    }

    @Override
    public void onDestroy() {
        log("service destory");
        this.stopRecording();
        this.cntx = null;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null == intent || !ACTION.equals(intent.getAction())) {
            return super.onStartCommand(intent, flags, startId);
        }
        String state = intent.getStringExtra(STATE);
        String phoneNo = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        log("state: " + state + " phoneNo: " + phoneNo);
        if (OUTGOING.equals(state)) {
            mobileNumber = phoneNo;
            fileNamePrefix = getContactName(this.getContext(), phoneNo)
                    + OUTGOING_CALL_SUFFIX;
            contactName = getContactName(this.getContext(),phoneNo);
            type = "Outgoing";
        } else if (INCOMING.equals(state)) {
            mobileNumber = phoneNo;
            fileNamePrefix = getContactName(this.getContext(), phoneNo)
                    + INCOMING_CALL_SUFFIX;
            contactName = getContactName(this.getContext(),phoneNo);
            type = "Incoming";
        } else if (BEGIN.equals(state)) {
//            realm = Realm.getDefaultInstance();
//            final RealmResults<SavedContact> selectedContactResults = realm.where(SavedContact.class).equalTo("type", "Selected").findAll();
//            final RealmResults<SavedContact> whiteListResults = realm.where(SavedContact.class).equalTo("type", "WhiteList").findAll();
//            Boolean isWhiteList = false;
//            for (int i = 0; i < whiteListResults.size(); i++){
//                if(whiteListResults.get(i).getContactName().equals(contactName)){
//                    startRecording();
//                    isWhiteList  = true;
//                }
//            }
//            if (!isWhiteList){
//                for (int i = 0; i < selectedContactResults.size(); i++){
//                    if(selectedContactResults.get(i).getContactName().equals(contactName)){
//                        showDialog();
//                    }
//                }
//            }
            if (sharedPreferences.getString("RecordPref","").equals("MyWish")){
                showDialog();
            } else{
                startRecording();
            }
        } else if (END.equals(state)) {
            stopRecording();
            if (recordDialog.isShowing()) {
                recordDialog.dismiss();
            }
        } else if (STORAGE.equals(state)) {
            String mountState = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(mountState)) {
                prepareAmrDir();
            } else {
                isMounted = false;
            }
            if (!isInRecording) {
                stopSelf();
            }
        }
        return START_STICKY;
    }

    public Context getContext() {
        return cntx;
    }

    private void stopRecording() {
        if (isInRecording) {
            isInRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            releaseWakeLock();
            stopSelf();

            Intent intent = new Intent(this,SaveDialogActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Bundle bundle = new Bundle();
            bundle.putLong("id", 2 + System.currentTimeMillis());
            bundle.putString("contactName", contactName);
            bundle.putString("fileName", fileName);
            bundle.putString("filePath", filePath);
            bundle.putString("mobileNumber", mobileNumber);
            bundle.putString("date", getDateTimeString());
            bundle.putString("type", type);
            bundle.putBoolean("unknownNumber", unknownNumber);
            intent.putExtras(bundle);
            this.startActivity(intent);
        }
    }

    private String getDateTimeString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'hh:mm aaa");
        Date now = new Date();
        return sdf.format(now);
    }

    private String getMonthString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
        Date now = new Date();
        return sdf.format(now);
    }

    private String getDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Date now = new Date();
        return sdf.format(now);
    }

    private String getTimeString() {
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
        Date now = new Date();
        return sdf.format(now);
    }

    private void startRecording() {
        if (!isMounted)
            return;
        stopRecording();
        try {
            File amr = new File(Environment.getExternalStorageDirectory()
                    .getAbsolutePath()
                    + AMR_DIR
                    + getDateTimeString()
                    + "_"
                    + fileNamePrefix + ".amr");
            filePath = amr.getAbsolutePath();
            fileName = getDateTimeString()
                    + "_"
                    + fileNamePrefix + ".amr";
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(amr.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            isInRecording = true;
            acquireWakeLock();
            log("Recording in " + amr.getAbsolutePath());
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    private void prepareAmrDir() {
        isMounted = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        if (!isMounted)
            return;
        File amrRoot = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + AMR_DIR);
        if (!amrRoot.isDirectory())
            amrRoot.mkdir();
    }

    private String getContactName(Context cntx, String phoneNo) {
        if (null == phoneNo)
            return "";
        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNo));
        ContentResolver cr = cntx.getContentResolver();
        Cursor c = cr.query(uri, new String[] { PhoneLookup.DISPLAY_NAME },
                null, null, null);
        if (null == c) {
            log("getContactName: The cursor was null when query phoneNo = "
                    + phoneNo);
            unknownNumber = true;
            return phoneNo;
        }
        try {
            if (c.moveToFirst()) {
                String name = c.getString(0);
                name = name.replaceAll("(\\||\\\\|\\?|\\*|<|:|\"|>)", "");
                log("getContactName: phoneNo: " + phoneNo + " name: " + name);
                return name;
            } else {
                log("getContactName: Contact name of phoneNo = " + phoneNo
                        + " was not found.");
                unknownNumber = true;
                return phoneNo;
            }
        } finally {
            c.close();
        }
    }

    private void log(String info) {
        if (DEBUG && isMounted) {
            File log = new File(Environment.getExternalStorageDirectory()
                    .getAbsolutePath()
                    + AMR_DIR
                    + "log_"
                    + getMonthString()
                    + ".txt");
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(log,
                        true));
                try {
                    synchronized (out) {
                        out.write(getDateString()+getTimeString());
                        out.write(" ");
                        out.write(info);
                        out.newLine();
                    }
                } finally {
                    out.close();
                }
            } catch (IOException e) {
                Log.w(TAG, e);
            }
        }
    }

    private void acquireWakeLock() {
        if (wakeLock == null) {
            log("Acquiring wake lock");
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this
                    .getClass().getCanonicalName());
            wakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
            log("Wake lock released");
        }
    }

    private void showDialog(){
        recordDialog.setContentView(R.layout.record_confirm_dialog);
        ImageView recordImage = (ImageView)recordDialog.findViewById(R.id.img_record);
        recordImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
                recordDialog.dismiss();
            }
        });
        recordDialog.setCanceledOnTouchOutside(false);
        recordDialog.setCancelable(true);
        recordDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        recordDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        recordDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);

        DisplayMetrics displayMetrics = cntx.getResources().getDisplayMetrics();
        int dialogWidth = (int)(displayMetrics.widthPixels * 0.19);
        int dialogHeight = (int)(displayMetrics.heightPixels * 0.17);
        recordDialog.getWindow().setLayout(dialogWidth, dialogHeight);
        recordDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        recordDialog.show();
    }
}
