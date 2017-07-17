package com.tech42.callrecorder.adapters;

import android.content.Context;

import com.tech42.callrecorder.model.CallRecord;

import io.realm.RealmResults;

/**
 * Created by sathish on 30/06/17.
 */

public class RealmCallRecordsAdapter extends RealmModelAdapter<CallRecord> {

    public RealmCallRecordsAdapter(Context context, RealmResults<CallRecord> realmResults, boolean automaticUpdate) {

        super(context, realmResults, automaticUpdate);
    }
}