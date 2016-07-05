package roofmessage.roofmessageapp.DataQuery;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import roofmessage.roofmessageapp.JSONBuilder;
import roofmessage.roofmessageapp.PhoneNumberFormatter;

/**
 * Created by Jesse Saran on 7/4/2016.
 */
public class MessageManager {
    private static MessageManager messageManager = null;
    private static ContentResolver contentResolver;
    private final static String TAG = "ROOFMESSAGE:MManager: ";

    protected MessageManager(Context context) {
        contentResolver = context.getContentResolver();
    }

    public static MessageManager getInstance(Context context) {
        if( messageManager == null) {
            messageManager = new MessageManager(context);
        }

        return messageManager;
    }


    public JSONBuilder getAll_SMS_MMS() {
        Uri mSMSMMSMessage = Uri.parse("content://mms-sms/conversations/");
        String SMS_READ_COLUMN = "read";
        String WHERE_CONDITION = SMS_READ_COLUMN + " = 0";
        String SORT_ORDER = "date DESC";
        Cursor cursor = contentResolver.query(mSMSMMSMessage,
                new String[]{
                        JSONBuilder.JSON_KEY_MESSAGE._ID.name().toLowerCase(),
                        JSONBuilder.JSON_KEY_MESSAGE.THREAD_ID.name().toLowerCase(),
                        JSONBuilder.JSON_KEY_MESSAGE.ADDRESS.name().toLowerCase(),
                        JSONBuilder.JSON_KEY_MESSAGE.PERSON.name().toLowerCase(),
                        JSONBuilder.JSON_KEY_MESSAGE.DATE.name().toLowerCase(),
                        JSONBuilder.JSON_KEY_MESSAGE.BODY.name().toLowerCase(),
                },
                WHERE_CONDITION,
                null,
                SORT_ORDER
        );

        JSONBuilder jsonObject = new JSONBuilder( JSONBuilder.Action.post_contacts );
        JSONArray jsonArray = new JSONArray();
        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                if(!cursor.getString(1).equals(cursor.getString(2))) {
                    JSONObject contact = new JSONObject();
                    try {
                        String[] columns = new String[] {
                                "address",
                                "person",
                                "date",
                                "body",
                                "type"
                        };
                        while (cursor.moveToNext()){
                            contact.put(JSONBuilder.JSON_KEY_CONTACT.ANDROID_ID.name().toLowerCase(), cursor.getString(0));
                            contact.put(JSONBuilder.JSON_KEY_CONTACT.FULL_NAME.name().toLowerCase(), cursor.getString(1));
                            contact.put(JSONBuilder.JSON_KEY_CONTACT.PHONE_NUMBER.name().toLowerCase(),
                                    PhoneNumberFormatter.toSingleNumber(cursor.getString(2)));

                            String address = cursor.getString(cursor.getColumnIndex(columns[0]));
                            String name = cursor.getString(cursor.getColumnIndex(columns[1]));
                            String date = cursor.getString(cursor.getColumnIndex(columns[2]));
                            String msg = cursor.getString(cursor.getColumnIndex(columns[3]));
                            String type = cursor.getString(cursor.getColumnIndex(columns[4]));
                        }
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
}
