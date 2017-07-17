package com.tech42.callrecorder.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.tech42.callrecorder.R;
import com.tech42.callrecorder.adapters.ContactsAdapter;
import com.tech42.callrecorder.adapters.RealmSavedContactsAdapter;
import com.tech42.callrecorder.model.SavedContact;
import com.tech42.callrecorder.realm.RealmController;
import com.tech42.callrecorder.utils.AppConstant;

import io.realm.Realm;
import io.realm.RealmResults;

public class WhiteListActivity extends AppCompatActivity {

    private static RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private RecyclerView recyclerView;
    public View.OnClickListener myOnClickListener;
    private Realm mRealm;
    public static final int PICK_CONTACT = 1;
    private ContactsAdapter ContactsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_white_list);
        AppConstant.activityName = "WhiteList";
        myOnClickListener = new MyOnClickListener(this);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        setupRecyclerView();
        getRealmInstance();
        setupRecyclerView();
        refreshRealmInstance();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }


    public void getRealmInstance(){
        this.mRealm = RealmController.with(this).getRealm();
    }

    public void setRealmAdapter(RealmResults<SavedContact> SavedContact) {
        RealmSavedContactsAdapter realmAdapter = new RealmSavedContactsAdapter(getApplicationContext(), SavedContact, true);
        // Set the data and tell the RecyclerView to draw
        ContactsAdapter.setRealmAdapter(realmAdapter);
        ContactsAdapter.notifyDataSetChanged();
    }

    public void refreshRealmInstance(){
        RealmController.with(this).refresh();
        setRealmAdapter(RealmController.with(this).getSavedContactRecords("WhiteList"));
        int size = (RealmController.with(this).getSavedContactRecords("WhiteList")).size();
    }

    public void setupRecyclerView() {
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager since the cards are vertically scrollable
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        // create an empty adapter and add it to the recycler view
        ContactsAdapter = new ContactsAdapter(getApplicationContext());
        recyclerView.setAdapter(ContactsAdapter);
    }

    private static class MyOnClickListener implements View.OnClickListener {

        private final Context context;

        private MyOnClickListener(Context context) {
            this.context = context;
        }

        @Override
        public void onClick(View v) {
            removeItem(v);
        }

        private void removeItem(View v) {
//            int selectedItemPosition = recyclerView.getChildPosition(v);
//            RecyclerView.ViewHolder viewHolder
//                    = recyclerView.findViewHolderForPosition(selectedItemPosition);
//            TextView textViewName
//                    = (TextView) viewHolder.itemView.findViewById(R.id.textViewName);
//            String selectedName = (String) textViewName.getText();
//            int selectedItemId = -1;
//            for (int i = 0; i < MyData.nameArray.length; i++) {
//                if (selectedName.equals(MyData.nameArray[i])) {
//                    selectedItemId = MyData.id_[i];
//                }
//            }
//            removedItems.add(selectedItemId);
//            data.remove(selectedItemPosition);
//            adapter.notifyItemRemoved(selectedItemPosition);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contact_menu, menu);
        MenuItem addContact = menu.findItem(R.id.add_contact);
        addContact.setActionView(R.layout.add_contact_menu_layout);
        final ImageView addImage = (ImageView) menu.findItem(R.id.add_contact).getActionView().findViewById(R.id.add);

        addImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickContactNumber();
            }
        });
        return true;
    }

    public void pickContactNumber() {
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
                        String contactNumber= phone.getString(phone.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                        Toast.makeText(getApplicationContext(),contactNumberName +" : "+contactNumber,Toast.LENGTH_LONG).show();
                        mRealm.beginTransaction();
                        SavedContact savedContact = new SavedContact();
                        savedContact.setContactName(contactNumberName);
                        savedContact.setContactNumber(contactNumber);
                        savedContact.setType("WhiteList");
                        mRealm.copyToRealm(savedContact);
                        mRealm.commitTransaction();
                    }
                }
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshRealmInstance();
    }

}
