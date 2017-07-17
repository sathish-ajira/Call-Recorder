package com.tech42.callrecorder.adapters;

/**
 * Created by sathish on 07/07/17.
 */

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tech42.callrecorder.R;
import com.tech42.callrecorder.app.Prefs;
import com.tech42.callrecorder.model.CallRecord;
import com.tech42.callrecorder.model.SavedContact;
import com.tech42.callrecorder.realm.RealmController;
import com.tech42.callrecorder.views.AudioPlayer;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.realm.Realm;
import io.realm.RealmResults;

public class ContactsAdapter extends RealmRecyclerViewAdapter<SavedContact> {

    final Context context;
    private Realm realm;
    private LayoutInflater inflater;

    public ContactsAdapter(Context context) {

        this.context = context;
    }

    // create new views (invoked by the layout manager)
    @Override
    public ContactsCardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // inflate a new card view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.selected_contact_card, parent, false);
        return new ContactsCardViewHolder(view);
    }

    // replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int position) {

        realm = RealmController.getInstance().getRealm();

        // get the article
        final SavedContact SavedContact = getItem(position);
        // cast the generic view holder to our specific one
        final ContactsCardViewHolder holder = (ContactsCardViewHolder) viewHolder;

        char c = SavedContact.getContactName().charAt(0) == '+' ? 'U' : SavedContact.getContactName().charAt(0);
        holder.contactChar.setText(Character.toString(c));

        holder.contactName.setText(SavedContact.getContactName());
        //update single match from realm
        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
            }
        });

        holder.removeImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(
                        context);
                alertDialogBuilder.setMessage("Are you sure?");
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("Delete",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                final RealmResults<SavedContact> results = realm.where(SavedContact.class).equalTo("contactName",SavedContact.getContactName()).findAll();
                                realm.beginTransaction();
                                if(!results.isEmpty()) {
                                    for(int i = results.size() - 1; i >= 0; i--) {
                                        results.get(i).removeFromRealm();
                                    }
                                }
                                realm.commitTransaction();
                                notifyDataSetChanged();
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });
                android.app.AlertDialog alertDialogAndroid = alertDialogBuilder.create();
                alertDialogAndroid.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
                alertDialogAndroid.show();
            }
        });
    }

    // return the size of your data set (invoked by the layout manager)
    public int getItemCount() {

        if (getRealmAdapter() != null) {
            return getRealmAdapter().getCount();
        }
        return 0;
    }

    private static class ContactsCardViewHolder extends RecyclerView.ViewHolder {

        private TextView contactName;
//        private TextView contactNumber;
        private TextView contactChar;
        private CardView card;
        private ImageView removeImg;

        private ContactsCardViewHolder(View itemView) {
            // standard view holder pattern with Butterknife view injection
            super(itemView);

            card = (CardView) itemView.findViewById(R.id.cardView);
            contactName = (TextView) itemView.findViewById(R.id.contact_name);
//            contactNumber = (TextView) itemView.findViewById(R.id.number);
            contactChar = (TextView) itemView.findViewById(R.id.contact_char);
            removeImg = (ImageView) itemView.findViewById(R.id.remove);
        }
    }

}

