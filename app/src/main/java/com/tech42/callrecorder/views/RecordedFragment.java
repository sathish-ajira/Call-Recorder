package com.tech42.callrecorder.views;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tech42.callrecorder.R;
import com.tech42.callrecorder.adapters.CallRecordAdapter;
import com.tech42.callrecorder.adapters.RealmCallRecordsAdapter;
import com.tech42.callrecorder.model.CallRecord;
import com.tech42.callrecorder.realm.RealmController;
import com.tech42.callrecorder.utils.AppConstant;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class RecordedFragment extends Fragment {

    private CallRecordAdapter callRecordAdapter;
    private LayoutInflater layoutInflater;
    private Realm realm;
    private RecyclerView recyclerView;
    private List<String> list;
    private int size;
    private TextView noRecordWarning;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recorded, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        noRecordWarning = (TextView)view.findViewById(R.id.text_view);
        noRecordWarning.setVisibility(View.GONE);
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setVisibility(View.VISIBLE);
        sharedPreferences = getContext().getSharedPreferences("MyPref", 0);// 0 - for private mode
        editor = sharedPreferences.edit();
        editor.putString("Activity","All");
        editor.commit();
        setupRecyclerView();
        getRealmInstance();
        setupRecyclerView();
        refreshRealmInstance();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshRealmInstance();
    }

    public void getRealmInstance(){
        this.realm = RealmController.with(this).getRealm();
    }

    public void refreshRealmInstance(){
        RealmController.with(this).refresh();

        list = AppConstant.filterList;
        RealmResults<CallRecord> callRecords = null;
        if (list.size() == 0){
            callRecords = RealmController.with(this).getCallRecords();
            size = (RealmController.with(this).getCallRecords().size());
        } else{
            if (list.size() == 2){
                callRecords = RealmController.with(this).queryedCallRecords(list.get(0), list.get(1));
                size = callRecords.size();
            } else if (list.size() == 4){
                callRecords = RealmController.with(this).queryedCallRecords(list.get(0), list.get(1), list.get(2), list.get(3));
                size = callRecords.size();
            } else if (list.size() == 6){
                callRecords = RealmController.with(this).queryedCallRecords(list.get(0), list.get(1), list.get(2), list.get(3),
                        list.get(4), list.get(5));
                size = callRecords.size();
            } else if (list.size() == 8){
                callRecords = RealmController.with(this).queryedCallRecords(list.get(0), list.get(1), list.get(2), list.get(3),
                        list.get(4), list.get(5),list.get(6), list.get(7));
                size = callRecords.size();
            } else if (list.size() == 10){
                callRecords = RealmController.with(this).queryedCallRecords(list.get(0), list.get(1), list.get(2), list.get(3),
                        list.get(4), list.get(5),list.get(6), list.get(7),list.get(8), list.get(9));
                size = callRecords.size();
            } else if (list.size() == 12){
                callRecords = RealmController.with(this).queryedCallRecords(list.get(0), list.get(1), list.get(2), list.get(3),
                        list.get(4), list.get(5),list.get(6), list.get(7),list.get(8), list.get(9), list.get(10), list.get(11));
                size = callRecords.size();
            }
            else if (list.size() == 14){
                callRecords = RealmController.with(this).queryedCallRecords(list.get(0), list.get(1), list.get(2), list.get(3),
                        list.get(4), list.get(5),list.get(6), list.get(7),list.get(8), list.get(9), list.get(10), list.get(11));
                size = callRecords.size();
            }
        }

        if (size == 0){
            noRecordWarning.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else{
            noRecordWarning.setVisibility(View.GONE);
            if(sharedPreferences.getBoolean("NameSort", false)) {
                callRecords.sort("contactName", true);
                editor.putBoolean("NameSort",false);
                editor.commit();
            }
            if(sharedPreferences.getBoolean("DateSort", false)) {
                callRecords.sort("time", true);
                editor.putBoolean("DateSort",false);
                editor.commit();
            }
            setRealmAdapter(callRecords);
        }
    }

    public void setRealmAdapter(RealmResults<CallRecord> CallRecords) {
        RealmCallRecordsAdapter realmAdapter = new RealmCallRecordsAdapter(getContext(), CallRecords, true);
        // Set the data and tell the RecyclerView to draw
        callRecordAdapter.setRealmAdapter(realmAdapter);
        callRecordAdapter.notifyDataSetChanged();
    }
    
    public void setupRecyclerView() {
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager since the cards are vertically scrollable
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        // create an empty adapter and add it to the recycler view
        callRecordAdapter = new CallRecordAdapter(getContext());
        recyclerView.setAdapter(callRecordAdapter);
    }

}