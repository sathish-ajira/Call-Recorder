package com.tech42.callrecorder.adapters;

import android.content.Context;

import com.tech42.callrecorder.model.SavedContact;

import io.realm.RealmResults;

/**
 * Created by sathish on 30/06/17.
 */

public class RealmSavedContactsAdapter extends RealmModelAdapter<SavedContact> {

    public RealmSavedContactsAdapter(Context context, RealmResults<SavedContact> realmResults, boolean automaticUpdate) {

        super(context, realmResults, automaticUpdate);
    }
}