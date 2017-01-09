package rooftext.rooftextapp.background.request;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import rooftext.rooftextapp.io.JSONBuilder;
import rooftext.rooftextapp.utils.Tag;

/**
 * Created by Jesse Saran on 6/22/2016.
 */
public class ContactQuery {

    private static ContentResolver contentResolver;
    private static ContactQuery contactQuery;


    protected ContactQuery(Context context) {
        this.contentResolver = context.getContentResolver();
    };

    protected static ContactQuery getInstance(Context context) {
        if( contactQuery == null) {
            contactQuery = new ContactQuery(context);
        }

        return contactQuery;
    }

    public void getContactsAsync(final Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {Uri contentURI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
                Log.d(Tag.MESSAGE_MANAGER, "Starting contact async");
                String [] COLUMN = {
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                };
                String WHERE = ContactsContract.CommonDataKinds.Phone.TYPE + " = " +
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
                String ORDERBY = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY;
                Cursor cursor = contentResolver.query(contentURI,
                        COLUMN,
                        WHERE,
                        null,
                        ORDERBY
                );

                JSONBuilder jsonObject = new JSONBuilder( JSONBuilder.Action.POST_CONTACTS );
                JSONArray jsonArray = new JSONArray();
                if(cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    String previousName = "";
                    do {
                        if(!cursor.getString(1).equals(previousName)) {
                            JSONObject contact = new JSONObject();
                            JSONObject contactInfo = new JSONObject();
                            try {
                                contact.put(cursor.getString(0), contactInfo);
                                contactInfo.put(JSONBuilder.JSON_KEY_CONTACT.FULL_NAME.name().toLowerCase(), cursor.getString(1));
                                contactInfo.put(JSONBuilder.JSON_KEY_CONTACT.PHONE_NUMBER.name().toLowerCase(),
                                        cursor.getString(2));
                                jsonArray.put(contact);
                            } catch (JSONException e) {
                                Log.d(Tag.CONTACT_MANAGER,"Could not add contact.");
                                e.printStackTrace();
                            }
                        }
                        previousName = cursor.getString(1);
                    } while(cursor.moveToNext());
                }
                try {
                    jsonObject.put("contacts", (Object) jsonArray);
                } catch (JSONException e) {
                    Log.e(Tag.CONTACT_MANAGER,"Could not add array to json.", e);
                }
                if(cursor != null){
                    cursor.close();
                }
                Intent intent = new Intent(Tag.ACTION_LOCAL_SEND_MESSAGE);
                intent.putExtra(Tag.KEY_SEND_JSON_STRING, jsonObject.toString());
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                Log.d(Tag.MESSAGE_MANAGER, "Stoping contact async");
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
