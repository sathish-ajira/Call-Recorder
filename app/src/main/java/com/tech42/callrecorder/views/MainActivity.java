package com.tech42.callrecorder.views;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.utils.FileUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.drive.DriveId;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.tech42.callrecorder.R;
import com.tech42.callrecorder.adapters.CallRecordAdapter;
import com.tech42.callrecorder.adapters.RealmCallRecordsAdapter;
import com.tech42.callrecorder.model.CallRecord;
import com.tech42.callrecorder.realm.RealmController;
import com.tech42.callrecorder.services.CallRecorderService;
import com.tech42.callrecorder.utils.AppConstant;
import com.tech42.callrecorder.viewpager.ViewPagerAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, ConnectionCallbacks,
        OnConnectionFailedListener, EasyPermissions.PermissionCallbacks {

    private ViewPager viewPager;
    private DrawerLayout drawer;
    private TabLayout tabLayout;
    private String[] pageTitle = {"All", "Your Gmail", "Mobile"};
    private static final String TAG = MainActivity.class.getName();
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    private String EXISTING_FOLDER_ID;

    private static final int REQUEST_CODE_CAPTURE_IMAGE = 1;
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;

    private GoogleApiClient mGoogleApiClient;
    private DriveId mFolderDriveId;
    private String FILE_NAME, FILEPATH;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        callRecordAdapter = new CallRecordAdapter(getApplicationContext());

        pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        editor = pref.edit();
        editor.putString("FOLDER_PATH",Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/CallRecorder");
        editor.commit();

        EXISTING_FOLDER_ID = pref.getString("EXISTING_FOLDER_ID", "");
        getRealmInstance();
        refreshRealmInstance();
        callRecordUpdate = new CallRecord();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null){
            System.out.println("--------");
            deleteFilesFromAPI(bundle.getString("fileId"));
        }

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Inserting File...");

        viewPager = (ViewPager) findViewById(R.id.view_pager);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        drawer = (DrawerLayout) findViewById(R.id.drawerLayout);

        setSupportActionBar(toolbar);

        //create default navigation drawer toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //setting Tab layout (number of Tabs = number of ViewPager pages)
        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        for (int i = 0; i < 3; i++) {
            tabLayout.addTab(tabLayout.newTab().setText(pageTitle[i]));
        }

        //set gravity for tab bar
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        //handling navigation view item event
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        assert navigationView != null;
        navigationView.setNavigationItemSelectedListener(this);

        //set viewpager adapter
        ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);

        //change Tab selection when swipe ViewPager
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        //change ViewPager page when tab selected
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    public void getRealmInstance() {
        realm = RealmController.with(MainActivity.this).getRealm();
    }

    public void refreshRealmInstance() {
        RealmController.with(this).refresh();

        setRealmAdapter(RealmController.with(MainActivity.this).queryedCallRecords("storage", "Local"));
        size = (RealmController.with(MainActivity.this).queryedCallRecords("storage", "Local").size());
    }

    public void setRealmAdapter(RealmResults<CallRecord> CallRecords) {
        RealmCallRecordsAdapter realmAdapter = new RealmCallRecordsAdapter(getApplicationContext(), CallRecords, true);
        // Set the data and tell the RecyclerView to draw
        callRecordAdapter.setRealmAdapter(realmAdapter);
        callRecordAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.cloud) {
            if (size > 0) {
                getResultsFromApi();
            } else {
                Toast.makeText(this, "Local Storage is empty", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.settings) {
            showDialog();
        } else if (id == R.id.about) {
            aboutDialog();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        assert drawer != null;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
        AppConstant.filterList.clear();
        finishAffinity();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem recorderToggle = menu.findItem(R.id.myswitch);
        recorderToggle.setActionView(R.layout.recorder_toggle);
        final Boolean isEnable = pref.getBoolean("callRecord", true);
        final Switch switchToggle = (Switch) menu.findItem(R.id.myswitch).getActionView().findViewById(R.id.recorderSwitch);

        MenuItem filterImage = menu.findItem(R.id.filter);
        filterImage.setActionView(R.layout.filter_layout);
        final ImageView filter = (ImageView) menu.findItem(R.id.filter).getActionView().findViewById(R.id.filter_img);

        filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppConstant.filterList.clear();
                Intent intent = new Intent(MainActivity.this, FilterActivity.class);
                startActivity(intent);
            }
        });

        if (isEnable) {
            switchToggle.setChecked(true);
        } else {
            switchToggle.setChecked(false);
        }
        switchToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent i = new Intent(CallRecorderService.ACTION);
                if (isChecked) {
                    editor.putBoolean("callRecord", true);
                    editor.commit();
                    Toast.makeText(getApplicationContext(), "Call Recorder Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    editor.putBoolean("callRecord", false);
                    editor.commit();
                    Toast.makeText(getApplicationContext(), "Call Recorder Disabled", Toast.LENGTH_SHORT).show();
                }
                if (isEnable) {
                    if (!isServiceRunning()) {
                        i.setPackage("com.tech42.callrecorder");
                        startService(i);
                    }
                } else {
                    if (isServiceRunning()) {
                        i.setPackage("com.tech42.callrecorder");
                        stopService(i);
                    }
                }
            }
        });

        return true;
    }

    private boolean isServiceRunning() {
        ActivityManager myManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(30);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().equals(
                    CallRecorderService.class.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppConstant.filterList.clear();
    }

    private void showDialog() {
        final SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("RecordSettings", 0); // 0 - for private mode
        final SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.settings_dialog, null);
        dialogBuilder.setView(dialogView);

        final Switch allCalls = (Switch) dialogView.findViewById(R.id.all_calls);
        final Switch myWish = (Switch) dialogView.findViewById(R.id.my_wish);
        final Button apply = (Button) dialogView.findViewById(R.id.apply);
        final Button cancel = (Button) dialogView.findViewById(R.id.cancel);

        final AlertDialog alertDialog = dialogBuilder.create();

        if (sharedPreferences.getString("RecordPref", "").equals("MyWish")) {
            allCalls.setChecked(false);
            myWish.setChecked(true);
        } else {
            allCalls.setChecked(true);
            myWish.setChecked(false);
        }

        allCalls.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    allCalls.setChecked(true);
                    myWish.setChecked(false);
                } else {
                    allCalls.setChecked(false);
                    myWish.setChecked(true);
                }
            }
        });

        myWish.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    allCalls.setChecked(false);
                    myWish.setChecked(true);
                } else {
                    allCalls.setChecked(true);
                    myWish.setChecked(false);
                }
            }
        });

        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (allCalls.isChecked()) {
                    prefEditor.putString("RecordPref", "AllCalls");
                    prefEditor.commit();
                } else {
                    prefEditor.putString("RecordPref", "MyWish");
                    prefEditor.commit();
                }
                alertDialog.dismiss();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
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
                    new AddFileTask(mCredential, callRecordAdapter.getItem(0).getFileName()).execute();
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
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
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
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "This app requires Google Play Services. Please install " +
                            "Google Play Services on your device and relaunch this app.", Toast.LENGTH_LONG).show();
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName = pref.getString("ACCOUNT_NAME", "");
                    if (!accountName.isEmpty()) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this, result.getErrorCode(), 0).show();
            return;
        }
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
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
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
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
            refresh();
            rowIndex = 0;
        }
    }

    private void deleteFilesFromAPI(String fileId) {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            String accountName = pref.getString("ACCOUNT_NAME", "");
            if (!accountName.isEmpty()) {
                mCredential.setSelectedAccountName(accountName);
                deleteFiles(fileId);
            }
        } else if (!isDeviceOnline()) {
            Toast.makeText(getApplicationContext(), "No network connection available", Toast.LENGTH_LONG).show();
        } else {
            new DeleteFileTask(mCredential, fileId).execute();
        }
    }

    public String deleteFiles(final String fileId) {
            System.out.println("File ID " + fileId);
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, mCredential)
                    .setApplicationName("Drive API Android Quickstart")
                    .build();
            Thread CrearEventoHilo = new Thread(){
                public void run(){
                    //do something that retrun "Calling this from your main thread can lead to deadlock"
                    try {
                        mService.files().delete(fileId).execute();
                    }catch (IOException e) {
                        System.out.println("An error occurred: " + e);
                    }
                }
            };
            CrearEventoHilo.start();
            CrearEventoHilo.interrupt();
        return fileId;
    }


    private class DeleteFileTask extends AsyncTask<Void, Void, String> {

        String FILE_ID = "";

        DeleteFileTask(GoogleAccountCredential credential, String fileId) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Drive API Android Quickstart")
                    .build();
            FILE_ID = fileId;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                return deleteFiles(FILE_ID);
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

        }
    }


    private void refresh(){
        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        startActivity(intent);
    }

    private void aboutDialog(){
        // Create custom dialog object
        final Dialog dialog = new Dialog(MainActivity.this);
        // Include dialog.xml file
        dialog.setContentView(R.layout.about_dialog);

        final TextView cancel = (TextView) dialog.findViewById(R.id.cancel);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}


