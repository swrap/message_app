package roofmessage.roofmessageapp.dataquery;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import roofmessage.roofmessageapp.io.JSONBuilder;
import roofmessage.roofmessageapp.utils.PhoneNumberFormatter;
import roofmessage.roofmessageapp.utils.Tag;

/**
 * Created by Jesse Saran on 6/22/2016.
 */
public class ContactManager {

    private static ContentResolver contentResolver;
    private static ContactManager contactManager;


    protected ContactManager(Context context) {
        this.contentResolver = context.getContentResolver();
    };

    protected static ContactManager getInstance(Context context) {
        if( contactManager == null) {
            contactManager = new ContactManager(context);
        }

        return contactManager;
    }
/**
    public JSONBuilder getContact(String name) {
        Uri contentURI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String [] COLUMN = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        String WHERE = ContactsContract.CommonDataKinds.Phone.TYPE + " = " +
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE + " AND " +
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?";
        String [] QUESTIONMARK = {name};

        Cursor cursor = contentResolver.query(contentURI,
                COLUMN,
                WHERE,
                QUESTIONMARK,
                null
                );

        JSONBuilder jsonObject = new JSONBuilder( JSONBuilder.Action.post_contacts );
        if(cursor.getColumnCount() > 0) {
            cursor.moveToFirst();
            JSONObject contact = new JSONObject();
            try {
                contact.put(JSONBuilder.JSON_KEY_CONTACT.ANDROID_ID.name().toLowerCase(), cursor.getString(0));
                contact.put(JSONBuilder.JSON_KEY_CONTACT.FULL_NAME.name().toLowerCase(), cursor.getString(1));
                contact.put(JSONBuilder.JSON_KEY_CONTACT.PHONE_NUMBER.name().toLowerCase(),
                        PhoneNumberFormatter.toSingleNumber(cursor.getString(2)));
                jsonObject.put(JSONBuilder.JSON_KEY_CONTACT.CONTACT.name().toLowerCase(),
                        contact);
            } catch (JSONException e) {
                Log.e(Tag.CONTACT_MANAGER,"Could not add contact.");
                e.printStackTrace();
            }
        }
        try {
            Log.v(Tag.CONTACT_MANAGER,jsonObject.toString());
            return jsonObject;
        } catch (Exception e) {
            Log.e(Tag.CONTACT_MANAGER,"Could not add array to json.");
            e.printStackTrace();
        }
        return null;
    }*/

    public JSONBuilder getContacts() {
        Uri contentURI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String [] COLUMN = {
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        String WHERE = ContactsContract.CommonDataKinds.Phone.TYPE + " = " +
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
        String [] QUESTIONMARK = null;
        String ORDERBY = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY + " DESC";
        Cursor cursor = contentResolver.query(contentURI,
                COLUMN,
                WHERE,
                QUESTIONMARK,
                ORDERBY
        );

        JSONBuilder jsonObject = new JSONBuilder( JSONBuilder.Action.POST_CONTACTS );
        JSONArray jsonArray = new JSONArray();
        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            String previousName = "";
            do {
                if(!cursor.getString(1).equals(previousName)) {
                    JSONObject contact = new JSONObject();
                    try {
                        contact.put(JSONBuilder.JSON_KEY_CONTACT.ANDROID_ID.name().toLowerCase(), cursor.getString(0));
                        contact.put(JSONBuilder.JSON_KEY_CONTACT.FULL_NAME.name().toLowerCase(), cursor.getString(1));
                        contact.put(JSONBuilder.JSON_KEY_CONTACT.PHONE_NUMBER.name().toLowerCase(),
                                cursor.getString(2));
                        jsonArray.put(contact);
                    } catch (JSONException e) {
                        Log.e(Tag.CONTACT_MANAGER,"Could not add contact.");
                        e.printStackTrace();
                    }
                }
                previousName = cursor.getString(1);
            } while(cursor.moveToNext());
        }
        try {
            jsonObject.put("contacts", (Object) jsonArray);
        } catch (JSONException e) {
            Log.e(Tag.CONTACT_MANAGER,"Could not add array to json.");
            e.printStackTrace();
        }
        if(cursor != null){
            cursor.close();
        }
        return jsonObject;
    }
}
