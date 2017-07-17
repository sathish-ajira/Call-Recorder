package com.tech42.callrecorder.views;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.tech42.callrecorder.R;
import com.tech42.callrecorder.model.CallRecord;
import com.tech42.callrecorder.realm.RealmController;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.realm.Realm;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class SaveDialogActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        EasyPermissions.PermissionCallbacks {

    private Button cancelButton, saveButton, buttonPersonal, buttonBussiness, allCallsButton, onMyWishButton;
    private TextView contactName, mobilenumber, contactChar;
    private EditText cotextEditText, contactNameEditText;
    private ImageView img_call_type;
    private RatingBar ratingBar;
    private CallRecord callRecord;
    private Realm realm;
    private SharedPreferences sharedPreferences, preferences;
    private SharedPreferences.Editor editor, editorID;
    private Boolean isUnknownNumber = false;
    
    // Drive API
    GoogleAccountCredential mCredential;
    ProgressDialog mProgress;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private String EXISTING_FOLDER_ID;
    private static final int REQUEST_CODE_RESOLUTION = 3;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String TAG = "Google Drive";
    private static final String[] SCOPES = { DriveScopes.DRIVE_METADATA_READONLY };
    private com.google.api.services.drive.Drive mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_dialog);

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        preferences = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        editorID = preferences.edit();


        mProgress = new ProgressDialog(this);
        mProgress.setMessage("File Uploading...");


        EXISTING_FOLDER_ID = preferences.getString("EXISTING_FOLDER_ID","");
        
        cancelButton = (Button) findViewById(R.id.cancel);
        saveButton = (Button) findViewById(R.id.save);
        contactName = (TextView) findViewById(R.id.contact_name);
        mobilenumber = (TextView) findViewById(R.id.mobile_number);
        contactChar = (TextView) findViewById(R.id.contact_char);
        cotextEditText = (EditText) findViewById(R.id.context);
        contactNameEditText = (EditText) findViewById(R.id.edit_contact_name);
        ratingBar = (RatingBar) findViewById(R.id.rating_bar);
        img_call_type = (ImageView) findViewById(R.id.img_call_type);
        buttonPersonal = (Button) findViewById(R.id.personal);
        buttonBussiness = (Button) findViewById(R.id.bussiness);
        allCallsButton = (Button) findViewById(R.id.all_calls);
        onMyWishButton = (Button) findViewById(R.id.on_wish);

        getRealmInstance();
        callRecord = new CallRecord();
        Bundle bundle = getIntent().getExtras();
        callRecord.setContactName(bundle.getString("contactName"));
        callRecord.setId(bundle.getLong("id"));
        callRecord.setFilePath(bundle.getString("filePath"));
        callRecord.setFileName(bundle.getString("fileName"));
        callRecord.setMobileNo(bundle.getString("mobileNumber"));
        callRecord.setTime(bundle.getString("date"));
        callRecord.setType(bundle.getString("type"));
        isUnknownNumber = bundle.getBoolean("unknownNumber");

        if (!isUnknownNumber){
            contactNameEditText.setVisibility(View.GONE);
        }

        contactName.setText(callRecord.getContactName());
        mobilenumber.setText(callRecord.getMobileNo());
        try {
            char c = callRecord.getContactName().charAt(0) == '+' ? 'U' : callRecord.getContactName().charAt(0);
            contactChar.setText(Character.toString(c));
            callRecord.setCategory("Personal");
            callRecord.setStorage("Local");
        } catch(Exception e){
            System.out.println("Error: "+e.getMessage());
        }

        sharedPreferences = getApplicationContext().getSharedPreferences("RecordSettings", 0); // 0 - for private mode
        editor = sharedPreferences.edit();

        if (sharedPreferences.getString("RecordPref", "").equals("MyWish")){
            allCallsButton.setBackgroundResource(R.drawable.unselect_button_background_left);
            allCallsButton.setTextColor(getApplicationContext().getResources().getColor(R.color.colorHint));

            onMyWishButton.setBackgroundResource(R.drawable.select_button_background_right);
            onMyWishButton.setTextColor(getApplicationContext().getResources().getColor(R.color.white));
        } else {
            allCallsButton.setBackgroundResource(R.drawable.select_button_background);
            allCallsButton.setTextColor(getApplicationContext().getResources().getColor(R.color.white));

            onMyWishButton.setBackgroundResource(R.drawable.unselect_button_background);
            onMyWishButton.setTextColor(getApplicationContext().getResources().getColor(R.color.colorHint));
        }

        buttonPersonal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callRecord.setCategory("Personal");
                buttonPersonal.setBackgroundResource(R.drawable.select_button_background);
                buttonPersonal.setTextColor(getApplicationContext().getResources().getColor(R.color.white));

                buttonBussiness.setBackgroundResource(R.drawable.unselect_button_background);
                buttonBussiness.setTextColor(getApplicationContext().getResources().getColor(R.color.colorHint));
            }
        });

        buttonBussiness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callRecord.setCategory("Bussiness");
                buttonPersonal.setBackgroundResource(R.drawable.unselect_button_background_left);
                buttonPersonal.setTextColor(getApplicationContext().getResources().getColor(R.color.colorHint));

                buttonBussiness.setBackgroundResource(R.drawable.select_button_background_right);
                buttonBussiness.setTextColor(getApplicationContext().getResources().getColor(R.color.white));
            }
        });

        allCallsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString("RecordPref", "AllCalls");
                editor.commit();
                allCallsButton.setBackgroundResource(R.drawable.select_button_background);
                allCallsButton.setTextColor(getApplicationContext().getResources().getColor(R.color.white));

                onMyWishButton.setBackgroundResource(R.drawable.unselect_button_background);
                onMyWishButton.setTextColor(getApplicationContext().getResources().getColor(R.color.colorHint));
            }
        });

        onMyWishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString("RecordPref", "MyWish");
                editor.commit();
                allCallsButton.setBackgroundResource(R.drawable.unselect_button_background_left);
                allCallsButton.setTextColor(getApplicationContext().getResources().getColor(R.color.colorHint));

                onMyWishButton.setBackgroundResource(R.drawable.select_button_background_right);
                onMyWishButton.setTextColor(getApplicationContext().getResources().getColor(R.color.white));
            }
        });

        if(callRecord.getType().toString().equals("Incoming")){
            img_call_type.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_leftarrow));
            img_call_type.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.green));
        } else{
            img_call_type.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_rightarrow));
            img_call_type.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.red));
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callRecord.setPriority("" + ratingBar.getRating());
                callRecord.setDescription(cotextEditText.getText().toString());
                if (isUnknownNumber && !contactNameEditText.getText().toString().isEmpty()){
                    addContact(contactNameEditText.getText().toString(), callRecord.getMobileNo());
                    callRecord.setContactName(contactNameEditText.getText().toString());
                }
                getResultsFromApi();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCallRecord();
            }
        });
    }

    private void getRealmInstance(){
        this.realm = RealmController.with(this).getRealm();
    }

    private void addCallRecord(){
        realm.beginTransaction();
        realm.copyToRealm(callRecord);
        realm.commitTransaction();
        Toast.makeText(getApplicationContext(),"Call record stored successfully",Toast.LENGTH_SHORT).show();
    }

    private void addContact(String name, String phone) {
        ContentValues values = new ContentValues();
        values.put(Contacts.People.NUMBER, phone);
        values.put(Contacts.People.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM);
        values.put(Contacts.People.LABEL, name);
        values.put(Contacts.People.NAME, name);
        Uri dataUri = getContentResolver().insert(Contacts.People.CONTENT_URI, values);
        Uri updateUri = Uri.withAppendedPath(dataUri, Contacts.People.Phones.CONTENT_DIRECTORY);
        values.clear();
        values.put(Contacts.People.Phones.TYPE, Contacts.People.TYPE_MOBILE);
        values.put(Contacts.People.NUMBER, phone);
        updateUri = getContentResolver().insert(updateUri, values);
    }

    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            Toast.makeText(getApplicationContext(), "No network connection available. So this record stored in your mobile", Toast.LENGTH_LONG).show();
            addCallRecord();
            finish();
        } else {
            if (!preferences.getString("EXISTING_FOLDER_ID","").isEmpty()){
                new SaveDialogActivity.AddFileTask(mCredential).execute();
            }
        }
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName  = preferences.getString("ACCOUNT_NAME","");
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
                            "Google Play Services on your device and relaunch this app.",Toast.LENGTH_LONG).show();
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName  = preferences.getString("ACCOUNT_NAME","");
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
                SaveDialogActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    public String AddFile(){
        String id = "";
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.drive.Drive.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName("Crucial Conversation App")
                .build();
        try {
            com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
            fileMetadata.setName(callRecord.getFileName());
            EXISTING_FOLDER_ID = preferences.getString("EXISTING_FOLDER_ID","");
            fileMetadata.setParents(Collections.singletonList(EXISTING_FOLDER_ID));
            java.io.File filePath = new java.io.File(callRecord.getFilePath());
            FileContent mediaContent = new FileContent("audio/amr", filePath);
            com.google.api.services.drive.model.File file = mService.files().create(fileMetadata, mediaContent)
                    .setFields("id, parents")
                    .execute();
            id = file.getId();
            callRecord.setFileId(id);
            System.out.println("File ID: " + id);
           // ArrayList<String> filePathArray = new ArrayList<>();
//                filePathArray.add(0,"/storage/emulated/0/CallRecorder/2017-07-13 04:55 PM_T42 Mari_o.amr");
//                filePathArray.add(1,"/storage/emulated/0/CallRecorder/2017-07-13 04:54 PM_Jio Complaint - Helpline_o.amr");
//                for (int i = 0; i < filePathArray.size(); i++){
//                    java.io.File filePath = new java.io.File(filePathArray.get(i));
//                    FileContent mediaContent = new FileContent("audio/amr", filePath);
//                    com.google.api.services.drive.model.File file = mService.files().create(fileMetadata, mediaContent)
//                            .setFields("id, parents")
//                            .execute();
//                    id = file.getId();
////                callRecord.setFileId(id);
//                    System.out.println("File ID: " + id);
//                }
        } catch(Exception e) {
            System.out.println("Error -------->" + e.getMessage());
            addCallRecord();
        }
        return id;
    }

    private class AddFileTask extends AsyncTask<Void, Void, String> {

        AddFileTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Drive API Android Quickstart")
                    .build();
        }
        @Override
        protected String doInBackground(Void... params) {
            try {
                return AddFile();
            } catch (Exception e) {
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        @Override
        protected void onPostExecute(String output) {
            callRecord.setStorage("Cloud");
            addCallRecord();
            removeCallRecord();
            mProgress.hide();
            finish();
        }
    }

    private void removeCallRecord(){
        File file = new File(callRecord.getFilePath());
        file.delete();
        finish();
    }
}
