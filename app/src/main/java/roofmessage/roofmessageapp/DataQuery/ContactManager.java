package roofmessage.roofmessageapp.DataQuery;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import roofmessage.roofmessageapp.JSONBuilder;
import roofmessage.roofmessageapp.PhoneNumberFormatter;

/**
 * Created by Jesse Saran on 6/22/2016.
 */
public class ContactManager {

    private static ContentResolver contentResolver;
    private static ContactManager contactManager;
    private final static String TAG = "ROOFMESSAGE:CManager: ";


    protected ContactManager(Context context) {
        this.contentResolver = context.getContentResolver();
    };

    public static ContactManager getInstance(Context context) {
        if( contactManager == null) {
            contactManager = new ContactManager(context);
        }

        return contactManager;
    }

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
                Log.e(TAG,"Could not add contact.");
                e.printStackTrace();
            }
        }
        try {
            Log.v(TAG,jsonObject.toString());
            return jsonObject;
        } catch (Exception e) {
            Log.e(TAG,"Could not add array to json.");
            e.printStackTrace();
        }
        return null;
    }

    public JSONBuilder getContacts() {
        Uri contentURI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String [] COLUMN = {
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        String WHERE = ContactsContract.CommonDataKinds.Phone.TYPE + " = " +
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
        String [] QUESTIONMARK = null; //=  {"%" + name[0] + "%"};
        String ORDERBY = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY;
        Cursor cursor = contentResolver.query(contentURI,
                COLUMN,
                WHERE,
                QUESTIONMARK,
                ORDERBY
        );

        JSONBuilder jsonObject = new JSONBuilder( JSONBuilder.Action.post_contacts );
        JSONArray jsonArray = new JSONArray();
        if(cursor.getColumnCount() > 0) {
            cursor.moveToFirst();
            do {
                if(!cursor.getString(1).equals(cursor.getString(2))) {
                    JSONObject contact = new JSONObject();
                    try {
                        contact.put(JSONBuilder.JSON_KEY_CONTACT.ANDROID_ID.name().toLowerCase(), cursor.getString(0));
                        contact.put(JSONBuilder.JSON_KEY_CONTACT.FULL_NAME.name().toLowerCase(), cursor.getString(1));
                        contact.put(JSONBuilder.JSON_KEY_CONTACT.PHONE_NUMBER.name().toLowerCase(),
                                PhoneNumberFormatter.toSingleNumber(cursor.getString(2)));
                        jsonArray.put(contact);
                    } catch (JSONException e) {
                        Log.e(TAG,"Could not add contact.");
                        e.printStackTrace();
                    }
                }
            } while(cursor.moveToNext());
        }
        try {
            jsonObject.put("contacts", (Object) jsonArray);
            Log.v(TAG,jsonObject.toString());
            return jsonObject;
        } catch (JSONException e) {
            Log.e(TAG,"Could not add array to json.");
            e.printStackTrace();
        }
        return null;
    }

    public Cursor updateContact( JSONObject jsonObject ) {
        try {
            JSONObject contact = jsonObject.getJSONObject(JSONBuilder.JSON_KEY_CONTACT.CONTACT.name().toLowerCase());
            Object android_id = contact.get(JSONBuilder.JSON_KEY_CONTACT.ANDROID_ID.name().toLowerCase());

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
}
