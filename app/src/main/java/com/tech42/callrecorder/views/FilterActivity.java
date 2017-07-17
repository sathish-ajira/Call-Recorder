package com.tech42.callrecorder.views;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.tech42.callrecorder.R;
import com.tech42.callrecorder.model.CallRecord;
import com.tech42.callrecorder.realm.RealmController;
import com.tech42.callrecorder.utils.AppConstant;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmResults;

public class FilterActivity extends Activity {

    public CheckBox checkContactName, checkDate, checkMobileNumber, checkCategory, checkCallType, checkContext, checkShowAll,
            checkImportance, checkNameSort, checkDateSort;
    public EditText editDate, editMobileNumber;
    public RatingBar ratingBar;
    public Spinner spinnerCategory, spinnerCallType;
    public AutoCompleteTextView autoCompleteContext, editContactName;
    public Button cancel, filter;
    public String spinnerCategoryString, spinnerCallTypeString;
    public static final int PICK_CONTACT = 1;
    private List<String> contextArray = new ArrayList<>();
    private List<String> contactArray = new ArrayList<>();
    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

         checkContactName = (CheckBox) findViewById(R.id.check_contact_name);
         checkDate = (CheckBox) findViewById(R.id.check_date);
         checkMobileNumber = (CheckBox) findViewById(R.id.check_mobile_number);
         checkCategory = (CheckBox) findViewById(R.id.check_category);
         checkCallType = (CheckBox) findViewById(R.id.check_call_type);
         checkContext = (CheckBox) findViewById(R.id.check_context);
         checkShowAll = (CheckBox) findViewById(R.id.show_all);
         checkImportance = (CheckBox) findViewById(R.id.importance);
         checkNameSort = (CheckBox) findViewById(R.id.sort_by_name);
         checkDateSort = (CheckBox) findViewById(R.id.sort_by_date);
         ratingBar = (RatingBar) findViewById(R.id.rating_bar);

         editContactName = (AutoCompleteTextView) findViewById(R.id.edit_contact_name);
         editDate = (EditText) findViewById(R.id.edit_date);
         editMobileNumber = (EditText) findViewById(R.id.edit_mobile_number);
         spinnerCategory = (Spinner) findViewById(R.id.spinner_category);
         spinnerCallType = (Spinner) findViewById(R.id.spinner_call_type);
         autoCompleteContext = (AutoCompleteTextView) findViewById(R.id.edit_context);

         cancel = (Button) findViewById(R.id.cancel);
         filter = (Button) findViewById(R.id.filter);

        editContactName.setVisibility(View.GONE);
        editDate.setVisibility(View.GONE);
        editMobileNumber.setVisibility(View.GONE);
        spinnerCategory.setVisibility(View.GONE);
        spinnerCallType.setVisibility(View.GONE);
        autoCompleteContext.setVisibility(View.GONE);
        ratingBar.setVisibility(View.GONE);

        List<String> categories = new ArrayList<String>();
        categories.add("Personal");
        categories.add("Bussiness");

        List<String> callType = new ArrayList<String>();
        callType.add("Incoming");
        callType.add("Outgoing");

        getRealmInstance();
        refreshRealmInstance();

        ArrayAdapter<String> categoriesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        categoriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoriesAdapter);

        ArrayAdapter<String> callTypeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, callType);
        callTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCallType.setAdapter(callTypeAdapter);

        checkContactName.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    pickAContactNumber();
                    editContactName.setVisibility(View.VISIBLE);
                } else {
                    editContactName.setVisibility(View.GONE);
                    editContactName.setText("");
                }
            }
        });

        checkDate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Calendar mcurrentDate=Calendar.getInstance();
                int mYear=mcurrentDate.get(Calendar.YEAR);
                int mMonth=mcurrentDate.get(Calendar.MONTH);
                int mDay=mcurrentDate.get(Calendar.DAY_OF_MONTH);

                if(isChecked) {
                    DatePickerDialog mDatePicker = new DatePickerDialog(FilterActivity.this, R.style.datepicker, new DatePickerDialog.OnDateSetListener() {
                        public void onDateSet(DatePicker datepicker, int year, int month, int day) {
                            // TODO Auto-generated method stub
                            String Date = year + "-" + String.format("%02d", (month + 1)) + "-" + String.format("%02d", day);
                            editDate.setVisibility(View.VISIBLE);
                            editDate.setText(Date);
                        }
                    }, mYear, mMonth, mDay);
                    mDatePicker.show();
                    mDatePicker.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_NEGATIVE) {
                                // Do Stuff
                                checkDate.setChecked(false);
                            }
                        }
                    });
                }
                else{
                    editDate.setVisibility(View.GONE);
                    editDate.setText("");
                }
            }
        });

        checkMobileNumber.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    editMobileNumber.setVisibility(View.VISIBLE);
                else {
                    editMobileNumber.setVisibility(View.GONE);
                    editMobileNumber.setText("");
                }
            }
        });

        checkCategory.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    spinnerCategory.setVisibility(View.VISIBLE);
                else
                    spinnerCategory.setVisibility(View.GONE);
            }
        });

        checkCallType.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    spinnerCallType.setVisibility(View.VISIBLE);
                else
                    spinnerCallType.setVisibility(View.GONE);
            }
        });

        checkContext.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                    autoCompleteContext.setVisibility(View.VISIBLE);
                else{
                    autoCompleteContext.setVisibility(View.GONE);
                    autoCompleteContext.setText("");
                }
            }
        });

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinnerCategoryString = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinnerCategoryString = "Personal";
            }
        });

        spinnerCallType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinnerCallTypeString = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinnerCallTypeString = "Incoming";
            }
        });

        checkImportance.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    ratingBar.setVisibility(View.VISIBLE);
                } else {
                    ratingBar.setVisibility(View.GONE);
                }
            }
        });

        checkDateSort.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    checkNameSort.setChecked(false);
                }
            }
        });

        checkNameSort.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    checkDateSort.setChecked(false);
                }
            }
        });

        checkShowAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    editContactName.setVisibility(View.GONE);
                    editDate.setVisibility(View.GONE);
                    editMobileNumber.setVisibility(View.GONE);
                    spinnerCategory.setVisibility(View.GONE);
                    spinnerCallType.setVisibility(View.GONE);
                    autoCompleteContext.setVisibility(View.GONE);
                    ratingBar.setVisibility(View.GONE);

                    checkImportance.setChecked(false);
                    checkContactName.setChecked(false);
                    checkDate.setChecked(false);
                    checkMobileNumber.setChecked(false);
                    checkCategory.setChecked(false);
                    checkCallType.setChecked(false);
                    checkContext.setChecked(false);
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(checkShowAll.isChecked()){
                    AppConstant.filterList.clear();
                } else {
                    if (checkContactName.isChecked()) {
                        AppConstant.filterList.add("contactName");
                        AppConstant.filterList.add(editContactName.getText().toString());
                    }
                    if (checkContext.isChecked()) {
                        AppConstant.filterList.add("description");
                        AppConstant.filterList.add(autoCompleteContext.getText().toString());
                    }
                    if (checkDate.isChecked()) {
                        AppConstant.filterList.add("time");
                        AppConstant.filterList.add(editDate.getText().toString());
                    }
                    if (checkCategory.isChecked()) {
                        AppConstant.filterList.add("category");
                        AppConstant.filterList.add(spinnerCategoryString);
                    }
                    if (checkCallType.isChecked()) {
                        AppConstant.filterList.add("type");
                        AppConstant.filterList.add(spinnerCallTypeString);
                    }
                    if (checkMobileNumber.isChecked()) {
                        AppConstant.filterList.add("mobileNo");
                        AppConstant.filterList.add(editMobileNumber.getText().toString());
                    }
                    if (checkImportance.isChecked()){
                        AppConstant.filterList.add("priority");
                        AppConstant.filterList.add(""+ratingBar.getRating());
                    }
                    SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    if(checkNameSort.isChecked()){
                        editor.putBoolean("NameSort", true);
                        editor.commit();
                    } else{
                        editor.putBoolean("NameSort", false);
                        editor.commit();
                    }

                    if(checkDateSort.isChecked()){
                        editor.putBoolean("DateSort", true);
                        editor.commit();
                    } else{
                        editor.putBoolean("DateSort", false);
                        editor.commit();
                    }

                    Set<String> set = new HashSet<String>();
                    set.addAll(AppConstant.filterList);
                    editor.putStringSet("filterList", set);
                    editor.commit();
                }
                Toast.makeText(getApplicationContext(),"Filtered Call Records",Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    public void getRealmInstance(){
        this.realm = RealmController.with(this).getRealm();
    }

    public void refreshRealmInstance() {
        RealmController.with(this).refresh();
        RealmResults<CallRecord> realmResults = RealmController.with(this).getCallRecords();
        int size = RealmController.with(this).getCallRecords().size();
        for(int i = 0; i < size; i++){
            contextArray.add(realmResults.get(i).getDescription());
            contactArray.add(realmResults.get(i).getContactName());
        }
        ArrayAdapter<String> contactAdapter = new ArrayAdapter<String>
                (this, android.R.layout.select_dialog_item, contactArray);
        editContactName.setThreshold(1);
        editContactName.setAdapter(contactAdapter);

        ArrayAdapter<String> contextAdapter = new ArrayAdapter<String>
                (this, android.R.layout.select_dialog_item, contextArray);
        autoCompleteContext.setThreshold(1);
        autoCompleteContext.setAdapter(contextAdapter);

        autoCompleteContext.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        editContactName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    public void pickAContactNumber() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT);
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        switch (reqCode) {
            case (PICK_CONTACT):
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    Cursor phone = getContentResolver().query(contactData, null, null, null, null);
                    if (phone.moveToFirst()) {
                        String contactNumberName = phone.getString(phone.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        // Todo something when contact number selected
                        editContactName.setVisibility(View.VISIBLE);
                        editContactName.setText(contactNumberName);
                    }
                }
                break;
        }
    }
}
