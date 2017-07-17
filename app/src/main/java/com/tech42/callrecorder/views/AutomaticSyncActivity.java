package com.tech42.callrecorder.views;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.DriveId;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.tech42.callrecorder.adapters.CallRecordAdapter;
import com.tech42.callrecorder.adapters.RealmCallRecordsAdapter;
import com.tech42.callrecorder.model.CallRecord;
import com.tech42.callrecorder.realm.RealmController;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by sathish on 15/07/17.
 */

public class AutomaticSyncActivity extends Service implements GoogleApiClient.ConnectionCallbacks{
    // Drive API
    GoogleAccountCredential mCredential;
    ProgressDialog mProgress;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {DriveScopes.DRIVE_METADATA_READONLY};
    private com.google.api.services.drive.Drive mService = null;

    private int size, totalSize = 0, rowIndex = 0;
    private CallRecordAdapter callRecordAdapter;
    private Realm realm;
    private CallRecord callRecordUpdate;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    private String EXISTING_FOLDER_ID;
    private static final int REQUEST_CODE_RESOLUTION = 3;

    private GoogleApiClient mGoogleApiClient;
    private DriveId mFolderDriveId;
    private String FILE_NAME, FILEPATH;
    private static final String TAG = AutomaticSyncActivity.class.getName();
    
    @Override
    public void onCreate() {

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        callRecordAdapter = new CallRecordAdapter(getApplicationContext());

        pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        editor = pref.edit();

        EXISTING_FOLDER_ID = pref.getString("EXISTING_FOLDER_ID", "");
        getRealmInstance();
        refreshRealmInstance();
        callRecordUpdate = new CallRecord();

        getResultsFromApi();
    }

    public void getRealmInstance() {
        realm = RealmController.with(getApplication()).getRealm();
    }

    public void refreshRealmInstance() {
        RealmController.with(getApplication()).refresh();

        setRealmAdapter(RealmController.with(getApplication()).queryedCallRecords("storage", "Local"));
        size = (RealmController.with(getApplication()).queryedCallRecords("storage", "Local").size());
    }

    public void setRealmAdapter(RealmResults<CallRecord> CallRecords) {
        RealmCallRecordsAdapter realmAdapter = new RealmCallRecordsAdapter(getApplicationContext(), CallRecords, true);
        // Set the data and tell the RecyclerView to draw
        callRecordAdapter.setRealmAdapter(realmAdapter);
        callRecordAdapter.notifyDataSetChanged();
    }

    public void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {
            Toast.makeText(getApplicationContext(), "No network connection available. So this record stored in your mobile", Toast.LENGTH_LONG).show();
        } else {
            if (!pref.getString("EXISTING_FOLDER_ID", "").isEmpty()) {
                totalSize = callRecordAdapter.getItemCount();
                for (int i = 0; i < totalSize; i++) {
                    new AutomaticSyncActivity.AddFileTask(mCredential, callRecordAdapter.getItem(0).getFileName()).execute();
                }
            }
        }
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = pref.getString("ACCOUNT_NAME", "");
            if (!accountName.isEmpty()) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }


    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "API client connected.");

    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
        }
    }

    public String AddFile(String fileName, String filePath) {
        String id = "";
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.drive.Drive.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName("Crucial Conversation App")
                .build();
        System.out.println("File Name : " + fileName);
        System.out.println("File Path : " + filePath);
        try {
            com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
            fileMetadata.setName(fileName);
            EXISTING_FOLDER_ID = pref.getString("EXISTING_FOLDER_ID", "");
            fileMetadata.setParents(Collections.singletonList(EXISTING_FOLDER_ID));
            java.io.File audioFilePath = new java.io.File(filePath);
            FileContent mediaContent = new FileContent("audio/amr", audioFilePath);
            com.google.api.services.drive.model.File file = mService.files().create(fileMetadata, mediaContent)
                    .setFields("id, parents")
                    .execute();
            id = file.getId();
            System.out.println("File ID: " + id);
        } catch (Exception e) {
            System.out.println("Error -------->" + e.getMessage());
        }
        return id;
    }

    private class AddFileTask extends AsyncTask<Void, Void, String> {

        String FILE_NAME = "";

        AddFileTask(GoogleAccountCredential credential, String fileName) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Drive API Android Quickstart")
                    .build();
            FILE_NAME = fileName;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                FILEPATH = "/storage/emulated/0/CallRecorder/" + FILE_NAME;
                return AddFile(FILE_NAME, FILEPATH);
            } catch (Exception e) {
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(String output) {
            updateRecord(output);
        }
    }

    private void updateRecord(String id) {
        callRecordUpdate = callRecordAdapter.getItem(rowIndex);
        realm.beginTransaction();
        callRecordUpdate.setFileId(id);
        callRecordUpdate.setStorage("Cloud");
        realm.copyToRealmOrUpdate(callRecordUpdate);
        realm.commitTransaction();
        if (callRecordAdapter.getItemCount() == 0) {
            Toast.makeText(getApplicationContext(), "Sync Completed Successfully", Toast.LENGTH_SHORT).show();
            String dir = pref.getString("FOLDER_PATH","");
            System.out.println("--------->"+ dir);
            File fileList = new File( dir );
            if (fileList != null) {
                File[] filenames = fileList.listFiles();
                for (File tmpf : filenames) {
                    tmpf.delete();
                }
            }
            rowIndex = 0;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "Service onBind");
        return null;
    }
}

