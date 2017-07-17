package com.tech42.callrecorder.model;

import io.realm.RealmObject;

/**
 * Created by sathish on 07/07/17.
 */

public class SavedContact extends RealmObject {

    private String contactName;
    private String contactNumber;
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

//    public SavedContact(String contactName, String contactNumber, String type) {
//        this.contactName = contactName;
//        this.contactNumber = contactNumber;
//        this.type = type;
//    }
}
