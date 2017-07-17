package com.tech42.callrecorder.adapters;

/**
 * Created by sathish on 30/06/17.
 */

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tech42.callrecorder.views.MainActivity;
import com.tech42.callrecorder.R;
import com.tech42.callrecorder.app.Prefs;
import com.tech42.callrecorder.model.CallRecord;
import com.tech42.callrecorder.realm.RealmController;
import com.tech42.callrecorder.views.AudioPlayer;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.realm.Realm;
import io.realm.RealmResults;

public class CallRecordAdapter extends RealmRecyclerViewAdapter<CallRecord> {

    final Context context;
    private Realm realm;
    private LayoutInflater inflater;
    private SharedPreferences sharedPreferences;
    private RealmResults<CallRecord> results;


    public CallRecordAdapter(Context context) {

        this.context = context;
    }

    // create new views (invoked by the layout manager)
    @Override
    public CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // inflate a new card view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.call_record_item, parent, false);
        return new CardViewHolder(view);
    }

    // replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int position) {

        realm = RealmController.getInstance().getRealm();
        sharedPreferences = context.getSharedPreferences("MyPref", 0);// 0 - for private mode

        // get the article
        final CallRecord CallRecord = getItem(position);
        // cast the generic view holder to our specific one
        final CardViewHolder holder = (CardViewHolder) viewHolder;

        // set the title and the snippet
        holder.textContactName.setText(CallRecord.getContactName());
        holder.textCategory.setText(CallRecord.getCategory());
        holder.textNumber.setText(CallRecord.getMobileNo());
        char c = CallRecord.getContactName().charAt(0) == '+' ? 'U' : CallRecord.getContactName().charAt(0);
        holder.contactChar.setText(Character.toString(c));
        holder.date.setText(CallRecord.getTime());
        if (CallRecord.getStorage().equals("Cloud")){
            holder.cloudImage.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.cloud_done));
        } else{
            holder.cloudImage.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.cloud_not_done));
        }

        if(CallRecord.getType().toString().equals("Incoming")){
            holder.img_call_type.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_leftarrow));
            holder.img_call_type.setColorFilter(ContextCompat.getColor(context, R.color.green));
        } else{
            holder.img_call_type.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_rightarrow));
            holder.img_call_type.setColorFilter(ContextCompat.getColor(context, R.color.red));
        }

        //update single match from realm
        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupWindow(v, position);
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

    public static class CardViewHolder extends RecyclerView.ViewHolder {

        public CardView card;
        public TextView textContactName;
        public TextView textNumber;
        public TextView textCategory;
        public  TextView contactChar;
        public  TextView date;
        public ImageView img_call_type;
        public ImageView cloudImage;

        public CardViewHolder(View itemView) {
            // standard view holder pattern with Butterknife view injection
            super(itemView);

            card = (CardView) itemView.findViewById(R.id.cardView);
            textContactName = (TextView) itemView.findViewById(R.id.contact_name);
            textNumber = (TextView) itemView.findViewById(R.id.number);
            textCategory = (TextView) itemView.findViewById(R.id.category);
            contactChar = (TextView) itemView.findViewById(R.id.contact_char);
            date = (TextView) itemView.findViewById(R.id.date);
            img_call_type = (ImageView) itemView.findViewById(R.id.img_call_type);
            cloudImage = (ImageView) itemView.findViewById(R.id.cloud_img);
        }
    }

    public void showPopupWindow(View view,final int position) {
        PopupMenu popup = new PopupMenu(context, view);
        try {
            Field[] fields = popup.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popup);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                if (sharedPreferences.getString("Activity","").equals("All")){
                    results = realm.where(CallRecord.class).findAll();
                } else if(sharedPreferences.getString("Activity","").equals("Cloud")){
                    results = realm.where(CallRecord.class).contains("storage", "Cloud").findAll();
                }else{
                    results = realm.where(CallRecord.class).contains("storage", "Local").findAll();
                }
                final CallRecord callRecord = results.get(position);
                String title = item.getTitle().toString();
                String PATH_TO_FILE = callRecord.getFilePath();
                switch(title){
                    case "Call":
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(context, "Please allow call permission", Toast.LENGTH_SHORT).show();
                        }else{
                            Intent intent2 = new Intent(Intent.ACTION_CALL);
                            intent2.setData(Uri.parse("tel:" + callRecord.getMobileNo() ));
                            context.startActivity(intent2);
                        }
                        break;
                    case "Play":
                        if (callRecord.getStorage().equals("Cloud")){
                            try {
                                Intent i = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://drive.google.com/open?id=" + callRecord.getFileId()));
                                context.startActivity(i);
                            } catch(Exception er){
                                Toast.makeText(context,"Error : " + er,Toast.LENGTH_SHORT).show();
                            }
                        } else{
                            Intent intent = new Intent(context,AudioPlayer.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("path", PATH_TO_FILE);
                            bundle.putString("name", callRecord.getContactName());
                            bundle.putString("fileId", callRecord.getFileId());
                            intent.putExtras(bundle);
                            context.startActivity(intent);
                        }
                        break;

                    case "Share":
                        if (callRecord.getStorage().equals("Cloud")){
                            try {
                                Intent intent2 = new Intent(); intent2.setAction(Intent.ACTION_SEND);
                                intent2.setType("text/plain");
                                intent2.putExtra(Intent.EXTRA_TEXT, "https://drive.google.com/open?id=" + callRecord.getFileId());
                                context.startActivity(Intent.createChooser(intent2, "Share via"));
                            } catch(Exception er){
                                Toast.makeText(context,"Error : " + er,Toast.LENGTH_SHORT).show();
                            }
                        } else{
                            File f=new File(callRecord.getFilePath());
                            Uri uri = Uri.parse("file://"+f.getAbsolutePath());
                            Intent share = new Intent(Intent.ACTION_SEND);
                            share.putExtra(Intent.EXTRA_STREAM, uri);
                            share.setType("audio/*");
                            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            context.startActivity(Intent.createChooser(share, "Share audio File"));
                        }
                        break;

                    case "Delete":
                        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(
                                context);
                        alertDialogBuilder.setMessage("Are you sure?");
                        alertDialogBuilder
                                .setCancelable(false)
                                .setPositiveButton("Delete",new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        if (callRecord.getStorage().equals("Cloud")){
                                            try {
                                                Intent intent = new Intent(context,MainActivity.class);
                                                Bundle bundle = new Bundle();
                                                bundle.putString("fileId", callRecord.getFileId());
                                                intent.putExtras(bundle);
                                                realm.beginTransaction();
                                                results.remove(position);
                                                realm.commitTransaction();
                                                context.startActivity(intent);
                                            } catch(Exception er){
                                                Toast.makeText(context,"Error : " + er,Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            String contact = callRecord.getContactName();

                                            File file = new File(callRecord.getFilePath());
                                            file.delete();

                                            realm.beginTransaction();
                                            results.remove(position);
                                            realm.commitTransaction();

                                            if (results.size() == 0) {
                                                Prefs.with(context).setPreLoad(false);
                                            }
                                            notifyDataSetChanged();
                                            dialog.cancel();
                                            Toast.makeText(context, contact + " call record was deleted", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                })
                                .setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        dialog.cancel();
                                    }
                                });
                        android.app.AlertDialog alertDialogAndroid = alertDialogBuilder.create();
                        alertDialogAndroid.show();
                        break;

                    case "Details":
                        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(context);
                        View mView = layoutInflaterAndroid.inflate(R.layout.details_layout, null);

                        android.app.AlertDialog.Builder alertDialogBuilderUserInput = new android.app.AlertDialog.Builder(context);
                        alertDialogBuilderUserInput.setView(mView);

                        final EditText contactName = (EditText)mView.findViewById(R.id.contact);
                        final EditText contactNumber = (EditText)mView.findViewById(R.id.contact_number);
                        final EditText category = (EditText)mView.findViewById(R.id.category_edit);
                        final EditText date = (EditText)mView.findViewById(R.id.date);
                        final EditText context = (EditText)mView.findViewById(R.id.context);
                        final EditText filePath = (EditText) mView.findViewById(R.id.file_path);
                        final Button cancel = (Button) mView.findViewById(R.id.cancel);

                        contactName.setText(callRecord.getContactName());
                        contactNumber.setText(callRecord.getMobileNo());
                        category.setText(callRecord.getCategory());
                        date.setText(callRecord.getTime());
                        context.setText(callRecord.getDescription());
                        filePath.setText(callRecord.getFilePath());

                        final android.app.AlertDialog alertDialogAnd = alertDialogBuilderUserInput.create();
                        alertDialogAnd.setCanceledOnTouchOutside(false);
                        cancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                alertDialogAnd.cancel();
                            }
                        });
                        alertDialogAnd.show();
                        break;

                    default:
                        break;
                }
                return true;
            }
        });
        popup.show();
    }


}

