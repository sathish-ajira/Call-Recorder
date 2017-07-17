package com.tech42.callrecorder.realm;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.v4.app.Fragment;

import com.tech42.callrecorder.model.CallRecord;
import com.tech42.callrecorder.model.SavedContact;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;


public class RealmController {

    private static RealmController instance;
    private final Realm realm;

    public RealmController(Application application) {
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(application.getApplicationContext()).build();
        Realm.setDefaultConfiguration(realmConfiguration);
        realm = Realm.getDefaultInstance();
    }

    public static RealmController with(Fragment fragment) {

        if (instance == null) {
            instance = new RealmController(fragment.getActivity().getApplication());
        }
        return instance;
    }

    public static RealmController with(Activity activity) {

        if (instance == null) {
            instance = new RealmController(activity.getApplication());
        }
        return instance;
    }

    public static RealmController with(Application application) {

        if (instance == null) {
            instance = new RealmController(application);
        }
        return instance;
    }

    public static RealmController getInstance() {

        return instance;
    }

    public Realm getRealm() {

        return realm;
    }

    //Refresh the realm istance
    public void refresh() {
        realm.refresh();
    }

    //clear all objects from CallRecord.class
    public void clearAll() {

        realm.beginTransaction();
        realm.clear(CallRecord.class);
        realm.commitTransaction();
    }

    //find all objects in the CallRecord.class
    public RealmResults<CallRecord> getCallRecords() {

        return realm.where(CallRecord.class).findAll();
    }

    //find all objects in the SavedContact.class
    public RealmResults<SavedContact> getSavedContactRecords(String type) {

        return realm.where(SavedContact.class).equalTo("type", type).findAll();
    }

    //query a single item with the given id
    public CallRecord getCallRecord(String id) {

        return realm.where(CallRecord.class).equalTo("id", id).findFirst();
    }

    //check if CallRecord.class is empty
    public boolean hasCallRecords() {

        return !realm.allObjects(CallRecord.class).isEmpty();
    }

    //query example
    public RealmResults<CallRecord> queryedCallRecords(String type) {
        return realm.where(CallRecord.class)
                .contains("type", type)
                .findAll();
    }

    public RealmResults<CallRecord> queryedCallRecords(String firstKey, String firstValue) {
        return realm.where(CallRecord.class)
                .contains(firstKey, firstValue)
                .findAll();
    }

    public RealmResults<CallRecord> queryedCallRecords(String firstKey, String firstValue, String secondKey, String secondValue) {
        return realm.where(CallRecord.class)
                .beginGroup()
                .equalTo(firstKey, firstValue)
                .endGroup()
                .beginGroup()
                .equalTo(secondKey, secondValue)
                .endGroup()
                .findAll();
    }

    public RealmResults<CallRecord> queryedCallRecords(String firstKey, String firstValue, String secondKey, String secondValue,
                                                       String thirdKey, String thirdValue) {
        return realm.where(CallRecord.class)
                .beginGroup()
                .equalTo(firstKey, firstValue)
                .endGroup()
                .beginGroup()
                .equalTo(secondKey, secondValue)
                .endGroup()
                .beginGroup()
                .equalTo(thirdKey, thirdValue)
                .endGroup()
                .findAll();
    }

    public RealmResults<CallRecord> queryedCallRecords(String firstKey, String firstValue, String secondKey, String secondValue,
                                                       String thirdKey, String thirdValue, String fourthKey, String fourthValue) {
        return realm.where(CallRecord.class)
                .beginGroup()
                .equalTo(firstKey, firstValue)
                .endGroup()
                .beginGroup()
                .equalTo(secondKey, secondValue)
                .endGroup()
                .beginGroup()
                .equalTo(thirdKey, thirdValue)
                .endGroup()
                .beginGroup()
                .equalTo(fourthKey, fourthValue)
                .endGroup()
                .findAll();
    }

    public RealmResults<CallRecord> queryedCallRecords(String firstKey, String firstValue, String secondKey, String secondValue,
                                                       String thirdKey, String thirdValue, String fourthKey, String fourthValue,
                                                       String fifthKey, String fifthValue) {
        return realm.where(CallRecord.class)
                .beginGroup()
                .equalTo(firstKey, firstValue)
                .endGroup()
                .beginGroup()
                .equalTo(secondKey, secondValue)
                .endGroup()
                .beginGroup()
                .equalTo(thirdKey, thirdValue)
                .endGroup()
                .beginGroup()
                .equalTo(fourthKey, fourthValue)
                .endGroup()
                .beginGroup()
                .equalTo(fifthKey, fifthValue)
                .endGroup()
                .findAll();
    }

    public RealmResults<CallRecord> queryedCallRecords(String firstKey, String firstValue, String secondKey, String secondValue,
                                                       String thirdKey, String thirdValue, String fourthKey, String fourthValue,
                                                       String fifthKey, String fifthValue, String sixthKey, String sixthValue) {
        return realm.where(CallRecord.class)
                .beginGroup()
                .equalTo(firstKey, firstValue)
                .endGroup()
                .beginGroup()
                .equalTo(secondKey, secondValue)
                .endGroup()
                .beginGroup()
                .equalTo(thirdKey, thirdValue)
                .endGroup()
                .beginGroup()
                .equalTo(fourthKey, fourthValue)
                .endGroup()
                .beginGroup()
                .equalTo(fifthKey, fifthValue)
                .endGroup()
                .beginGroup()
                .equalTo(sixthKey, sixthValue)
                .endGroup()
                .findAll();
    }

    public RealmResults<CallRecord> queryedCallRecords(String firstKey, String firstValue, String secondKey, String secondValue,
                                                       String thirdKey, String thirdValue, String fourthKey, String fourthValue,
                                                       String fifthKey, String fifthValue, String sixthKey, String sixthValue, String seventhKey, String seventhValue) {
        return realm.where(CallRecord.class)
                .beginGroup()
                .equalTo(firstKey, firstValue)
                .endGroup()
                .beginGroup()
                .equalTo(secondKey, secondValue)
                .endGroup()
                .beginGroup()
                .equalTo(thirdKey, thirdValue)
                .endGroup()
                .beginGroup()
                .equalTo(fourthKey, fourthValue)
                .endGroup()
                .beginGroup()
                .equalTo(fifthKey, fifthValue)
                .endGroup()
                .beginGroup()
                .equalTo(sixthKey, sixthValue)
                .endGroup()
                .beginGroup()
                .equalTo(seventhKey, seventhValue)
                .endGroup()
                .findAll();
    }

}
