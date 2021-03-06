package rooftext.rooftextapp.background.request;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rooftext.rooftextapp.io.JSONBuilder;
import rooftext.rooftextapp.utils.Tag;
import rooftext.rooftextapp.utils.Utils;

/**
 * Created by Jesse Saran on 7/4/2016.
 */
public class MessageQuery {
    private static MessageQuery messageQuery = null;
    private static ContentResolver contentResolver;

    protected MessageQuery(Context context) {
        contentResolver = context.getContentResolver();
    }

    protected static MessageQuery getInstance(Context context) {
        if( messageQuery == null) {
            messageQuery = new MessageQuery(context);
        }

        return messageQuery;
    }

    public void sendAllConversationsAsync(final Context context) {
        Log.d(Tag.MESSAGE_MANAGER, "ASYNC CONVERSATION START");
        new AsyncTask<Void,Void,Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                Uri mSMSMMS = Uri.parse("content://mms-sms/conversations?simple=true");

                String [] SMSMMS_COLUMNS = new String[]{
                        Telephony.ThreadsColumns.DATE,
                        Telephony.ThreadsColumns._ID,
                        Telephony.ThreadsColumns.RECIPIENT_IDS,
                        Telephony.ThreadsColumns.TYPE,
                };

                String ORDERBY = "date DESC";
                if (Tag.LOCAL_HOST) {
                    ORDERBY += " LIMIT 5";
                }
                Cursor cursor = contentResolver.query(mSMSMMS,
                        SMSMMS_COLUMNS,
                        null,
                        null,
                        ORDERBY
                );

                try {
                    int GROUP = 60, count = 0;
                    if(cursor != null && cursor.moveToFirst()) {
                        Log.d(Tag.MESSAGE_MANAGER, "GI [" + (cursor.getCount()/GROUP)+1 + "] ["
                            + cursor.getCount() + "] ["+ GROUP + "]");
                        int parts = (cursor.getCount()/GROUP);
                        if (cursor.getCount() >  GROUP) {
                            //if there is less than 60 parts then we need to continue to the last part
                            parts++;
                        } else {
                            parts = 1;
                        }
                        for(int gI = 0; gI < parts; gI++) {
                            JSONBuilder jsonObject = new JSONBuilder( JSONBuilder.Action.POST_CONVERSATIONS );
                            JSONArray jsonArray = new JSONArray();
                            ArrayList<String []> eachConvo = new ArrayList<String[]>(cursor.getCount());
                            HashSet<String> recipId = new HashSet<String>(cursor.getCount());
                            do {
//                                Log.d(Tag.MESSAGE_MANAGER, "GI CONT[" + count + "] ["
//                                        + cursor.getCount() + "] ["+ GROUP + "]");
                                JSONObject conversation = new JSONObject();
                                JSONObject conversationInfo = new JSONObject();
                                try {
                                    conversation.put(cursor.getString(1), conversationInfo);
                                    conversationInfo.put(JSONBuilder.JSON_KEY_CONVERSATION.DATE.name().toLowerCase(),
                                            cursor.getString(cursor.getColumnIndex(Telephony.ThreadsColumns.DATE)));
                                    conversationInfo.put(JSONBuilder.JSON_KEY_CONVERSATION.CONVO_TYPE.name().toLowerCase(),
                                            cursor.getString(cursor.getColumnIndex(Telephony.ThreadsColumns.TYPE)));
                                    ArrayList<String> list = new ArrayList<String>();
                                    String s = cursor.getString(cursor.getColumnIndex(Telephony.ThreadsColumns.RECIPIENT_IDS));
                                    if (!s.equals("")) {
                                        Pattern p = Pattern.compile("-?\\d+");
                                        Matcher m = p.matcher(s);
                                        while(m.find()) {
                                            String t = m.group();
                                            recipId.add(t);
                                            list.add(t);
                                        }
                                        eachConvo.add(list.toArray(new String[list.size()]));
                                        //if the list is not empty then add it the convo else do not
                                        jsonArray.put(conversation);
                                    }
                                } catch (JSONException e) {
                                    Log.e(Tag.MESSAGE_MANAGER, "Could not add conversation.");
                                    e.printStackTrace();
                                }
                                count++;
                            } while(cursor.moveToNext() && count < GROUP);
                            count = 0;

                            Log.d(Tag.MESSAGE_MANAGER, "ASYNC CONVERSATION MIDDLE START");
                            HashMap<String, JSONObject> recipsIdPhoneNum = matchRecipientId(recipId);
                            for(int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jo = (JSONObject)jsonArray.get(i);
                                JSONArray arrT = new JSONArray();
                                String [] strings = eachConvo.get(i);
                                for(String s : strings) {
                                    arrT.put(recipsIdPhoneNum.get(s));
                                }
                                String key = jo.keys().next();
                                ((JSONObject)jo.get(key)).put(JSONBuilder.JSON_KEY_CONVERSATION.RECIPIENTS.name().toLowerCase(),
                                        arrT);
                            }
                            Log.d(Tag.MESSAGE_MANAGER, "ASYNC CONVERSATION MIDDLE END");
                            jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.CONVERSATIONS.name().toLowerCase(),
                                    jsonArray);
                            Intent intent = new Intent(Tag.ACTION_LOCAL_SEND_MESSAGE);
                            intent.putExtra(Tag.KEY_SEND_JSON_STRING, jsonObject.toString());
                            Log.d(Tag.MESSAGE_MANAGER, "ASYNC CONVERSATION ABOUT TO SEND");
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                            Log.d(Tag.MESSAGE_MANAGER, "ASYNC CONVERSATION SENT");
                        }
                    }
                } catch (JSONException e) {
                    Log.e(Tag.MESSAGE_MANAGER,"Could not add array to json. or other problems... internal");
                    e.printStackTrace();
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void getConversationMessagesAsync(final String thread_id,final int limit,final String offset,
                                             final int period,final Context context,final Intent intent) {
        Log.d(Tag.MESSAGE_MANAGER, "GETTING CONVERSATIONS ASYNC");
        new AsyncTask<Void,Void,Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                Log.d(Tag.MESSAGE_MANAGER, "Starting conversation async");
                Cursor smsCursor = getSMSMessages(thread_id,limit,offset,period);
                Cursor mmsCursor = getMMSMessages(thread_id, limit,
                        Utils.convertSMStoMMSDate(offset),
                        period
                );

                //used to check if end of messages
                int count = 0;
                if (smsCursor != null) {
                    count += smsCursor.getCount();
                }
                if (mmsCursor != null) {
                    count += mmsCursor.getCount();
                }

                JSONBuilder jsonObject = new JSONBuilder( JSONBuilder.Action.POST_MESSAGES );
                JSONArray jsonArray = new JSONArray();

                try {
                    jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.THREAD_ID.name().toLowerCase(),
                            thread_id);
                    Log.e(Tag.MESSAGE_MANAGER, smsCursor + " " + smsCursor.getCount() + " " +
                            mmsCursor + " " + mmsCursor.getCount());

                    if(smsCursor != null && mmsCursor != null && smsCursor.getCount() > 0 && mmsCursor.getCount() > 0) {
                        smsCursor.moveToFirst();
                        mmsCursor.moveToFirst();

                        String smsDate = smsCursor.getString(smsCursor.getColumnIndex(Telephony.TextBasedSmsColumns.DATE));
                        String mmsDate = Utils.convertMMStoSMSDate(mmsCursor.getString(mmsCursor.getColumnIndex(Telephony.BaseMmsColumns.DATE)));
                        for (int i = 0; i < limit; i++) {
                            Log.e(Tag.MESSAGE_MANAGER, "date: " + smsDate + " " + mmsDate);
                            if (smsDate != null && mmsDate != null) {
                                int compare = smsDate.compareTo(mmsDate);
                                Log.e(Tag.MESSAGE_MANAGER, "COMPARE: " + compare);
                                if (compare > 0) {
                                    jsonArray.put(getSMSJSON(smsCursor));
                                    if (smsCursor.moveToNext()) {
                                        smsDate = smsCursor.getString(smsCursor.getColumnIndex(Telephony.TextBasedSmsColumns.DATE));
                                    } else {
                                        smsDate = null;
                                    }
                                } else {
                                    Log.e(Tag.MESSAGE_MANAGER, "IN");
                                    jsonArray.put(getMMSJSON(mmsCursor));
                                    if (mmsCursor.moveToNext()) {
                                        mmsDate = Utils.convertMMStoSMSDate(mmsCursor.getString(mmsCursor.getColumnIndex(Telephony.BaseMmsColumns.DATE)));
                                    } else {
                                        mmsDate = null;
                                    }
                                }
                            } else if (smsDate != null && mmsDate == null) {
                                jsonArray.put(getSMSJSON(smsCursor));
                                if (smsCursor.moveToNext()) {
                                    smsDate = smsCursor.getString(smsCursor.getColumnIndex(Telephony.TextBasedSmsColumns.DATE));
                                } else {
                                    smsDate = null;
                                }
                            } else if (smsDate == null && mmsDate != null) {
                                jsonArray.put(getMMSJSON(mmsCursor));
                                if (mmsCursor.moveToNext()) {
                                    mmsDate = Utils.convertMMStoSMSDate(mmsCursor.getString(mmsCursor.getColumnIndex(Telephony.BaseMmsColumns.DATE)));
                                } else {
                                    mmsDate = null;
                                }
                            }
                        }
                    } else if(smsCursor != null && (mmsCursor == null || mmsCursor.getCount() == 0)){
                        Log.e(Tag.MESSAGE_MANAGER, "here: " + smsCursor + " " + mmsCursor);
                        if(smsCursor.getCount() > 0) {
                            smsCursor.moveToFirst();
                            do {
                                jsonArray.put(getSMSJSON(smsCursor));
                            } while(smsCursor.moveToNext());
                        }
                    } else if((smsCursor == null || smsCursor.getCount() == 0) && mmsCursor != null){
                        Log.e(Tag.MESSAGE_MANAGER, "it: " + smsCursor + " " + mmsCursor);
                        if(mmsCursor.getCount() > 0) {
                            mmsCursor.moveToFirst();
                            do {
                                jsonArray.put(getMMSJSON(mmsCursor));
                            } while(mmsCursor.moveToNext());
                        }
                    }
                    jsonObject.put(thread_id, (Object) jsonArray);

                    //puts in ending
                    if (count < limit) {
                        jsonObject.put("end", "t");
                    }
                } catch (JSONException e) {
                    Log.e(Tag.MESSAGE_MANAGER,"Could not add array to json.");
                    e.printStackTrace();
                }
                if (mmsCursor != null) {
                    mmsCursor.close();
                }
                if (smsCursor != null) {
                    smsCursor.close();
                }
                Log.e(Tag.MESSAGE_MANAGER, "MessengerQuery: " + jsonObject);
                if (jsonObject != null) {
                    intent.putExtra(Tag.KEY_SEND_JSON_STRING, jsonObject.toString());
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                }
                Log.d(Tag.MESSAGE_MANAGER, "Ending conversation async");
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static JSONObject getMMSJSON(Cursor cursor) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        JSONObject message = new JSONObject();
        message.put(cursor.getString(cursor.getColumnIndex(Telephony.BaseMmsColumns._ID)), jsonObject);

        jsonObject.put(JSONBuilder.Message_Type.TYPE.name().toLowerCase(),
                JSONBuilder.Message_Type.MMS.name().toLowerCase());
        jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.DATE_RECIEVED.name().toLowerCase(),
                Utils.convertMMStoSMSDate(cursor.getString(cursor.getColumnIndex(Telephony.BaseMmsColumns.DATE))));
        jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.TEXT_ONLY.name().toLowerCase(),
                cursor.getString(cursor.getColumnIndex(Telephony.BaseMmsColumns.TEXT_ONLY)));
        jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.MESSAGE_TYPE.name().toLowerCase(),
                cursor.getString(cursor.getColumnIndex(Telephony.BaseMmsColumns.MESSAGE_BOX)));
        jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.PARTS.name().toLowerCase(),
                getMMSParts(cursor.getString(cursor.getColumnIndex(Telephony.BaseMmsColumns._ID))));
        Log.d(Tag.MESSAGE_MANAGER, "PUTTING IN CONTACT ID");
        String [] addressId = getMMSAddress(cursor.getString(cursor.getColumnIndex(Telephony.BaseMmsColumns._ID)));
        jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.CONTACT_ID.name().toLowerCase(),
                addressId[0]);
        //NOTE ID IS CHANGED  IN METHOD CALL
        jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.ADDRESS.name().toLowerCase(),
                addressId[1]);
        return message;
    }

    public static JSONObject getSMSJSON(Cursor cursor) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        JSONObject message = new JSONObject();
        message.put(cursor.getString(cursor.getColumnIndex(Telephony.Sms._ID)), jsonObject);

        jsonObject.put(JSONBuilder.Message_Type.TYPE.name().toLowerCase(),
                JSONBuilder.Message_Type.SMS.name().toLowerCase());
        jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.BODY.name().toLowerCase(),
                cursor.getString(cursor.getColumnIndex(Telephony.TextBasedSmsColumns.BODY)));
        jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.DATE_RECIEVED.name().toLowerCase(),
                cursor.getString(cursor.getColumnIndex(Telephony.TextBasedSmsColumns.DATE)));
        jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.MESSAGE_TYPE.name().toLowerCase(),
                cursor.getString(cursor.getColumnIndex(Telephony.TextBasedSmsColumns.TYPE)));
        return message;
    }

    //NOTE CURSOR MUST BE CLOSED BY RECEIVED COMMAND
    public static Cursor getSMSMessage(String messageId) {
        Uri contentURI = Telephony.Sms.CONTENT_URI;
        String [] COLUMN = {
                Telephony.TextBasedSmsColumns.BODY,
                Telephony.TextBasedSmsColumns.DATE,
                Telephony.TextBasedSmsColumns.THREAD_ID,
                Telephony.Sms._ID,
                Telephony.Sms.TYPE,
        };
        String WHERE = Telephony.Sms._ID + " = ? ";
        String [] QUESTIONMARK = {messageId};
        String ORDERBY = Telephony.Sms.Conversations.DEFAULT_SORT_ORDER + " limit " + 1;
        return contentResolver.query(contentURI,
                COLUMN,
                WHERE,
                QUESTIONMARK,
                ORDERBY
        );
    }

    //NOTE CURSOR MUST BE CLOSED BY RECEIVED COMMAND
    public static Cursor getSMSMessage(String address, String date) {
        Uri contentURI = Telephony.Sms.CONTENT_URI;
        String [] COLUMN = {
                Telephony.TextBasedSmsColumns.BODY,
                Telephony.TextBasedSmsColumns.DATE,
                Telephony.TextBasedSmsColumns.THREAD_ID,
                Telephony.Sms._ID,
                Telephony.Sms.TYPE,
        };
        String WHERE = Telephony.TextBasedSmsColumns.DATE + " = ? AND "
                + Telephony.TextBasedSmsColumns.ADDRESS + " = ? ";
        String [] QUESTIONMARK = {date, address};
        String ORDERBY = Telephony.Sms.Conversations.DEFAULT_SORT_ORDER + " limit " + 1;
        return contentResolver.query(contentURI,
                COLUMN,
                WHERE,
                QUESTIONMARK,
                ORDERBY
        );
    }

    private Cursor getSMSMessages(String thread_id, int limit, String offset, int period){
        Uri contentURI = Telephony.Sms.CONTENT_URI;
        String [] COLUMN = {
                Telephony.TextBasedSmsColumns.BODY,
                Telephony.TextBasedSmsColumns.DATE,
                Telephony.TextBasedSmsColumns.THREAD_ID,
                Telephony.Sms._ID,
                Telephony.Sms.TYPE,
        };
        String WHERE = Telephony.Sms.Conversations.THREAD_ID + " = ? ";
        if(!offset.equals("-1")) {
            WHERE += " AND " + Telephony.TextBasedSmsColumns.DATE + " " + (period == 0 ? "<" : ">") + " ? ";
        }
        String [] QUESTIONMARK;
        if(offset.equals("-1")) {
            QUESTIONMARK = new String[1];
            QUESTIONMARK[0] = thread_id;
        } else {
            QUESTIONMARK = new String[2];
            QUESTIONMARK[0] = thread_id;
            QUESTIONMARK[1] = offset;
        }
        String ORDERBY = Telephony.Sms.Conversations.DEFAULT_SORT_ORDER + " limit " + limit;
        return contentResolver.query(contentURI,
                COLUMN,
                WHERE,
                QUESTIONMARK,
                ORDERBY
        );
    }

    private static String[] getMMSAddress(String messageId) {
        final String[] COLUMN =  new String[] {
                Telephony.Mms.Addr.ADDRESS,
        };
        //FROM PDU HEADER
        final String WHERE = "TYPE = 137";

        Uri.Builder builder = Uri.parse("content://mms").buildUpon();
        builder.appendPath(String.valueOf(messageId)).appendPath("addr");

        Cursor cursor = contentResolver.query(
                builder.build(),
                COLUMN,
                WHERE,
                null, null);
        String tempAddr = "";
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                tempAddr = cursor.getString(0);
            }
            cursor.close();
        }

        String [] temp = matchPhoneNumberToContactId(tempAddr);
        temp[1] = tempAddr;
        return temp;
    }

    private Cursor getMMSMessages(String thread_id, int limit, String offset, int period) {
        Uri contentURI = Telephony.Mms.CONTENT_URI;
        String [] COLUMN = {
                Telephony.BaseMmsColumns.THREAD_ID,
                Telephony.BaseMmsColumns.DATE,
                Telephony.BaseMmsColumns.TEXT_ONLY,
                Telephony.BaseMmsColumns._ID,
                Telephony.BaseMmsColumns.MESSAGE_BOX,
        };
        String WHERE = Telephony.BaseMmsColumns.THREAD_ID + " = ?";
        String [] QUESTIONMARK = null;
        String ORDERBY = "";

        if(!offset.equals("-1")) {
            WHERE += " AND " + Telephony.BaseMmsColumns.DATE + " " + (period == 0 ? "<" : ">") + " ? ";
        }
        if(offset.equals("-1")) {
            QUESTIONMARK = new String[1];
            QUESTIONMARK[0] = thread_id;
        } else {
            QUESTIONMARK = new String[2];
            QUESTIONMARK[0] = thread_id;
            QUESTIONMARK[1] = offset;
        }
        ORDERBY = Telephony.Mms.DEFAULT_SORT_ORDER + " limit " + limit;

        Cursor cursor = contentResolver.query(contentURI,
                COLUMN,
                WHERE,
                QUESTIONMARK,
                ORDERBY
        );
        return cursor;
    }

    public static JSONArray getMMSParts(String message_id) throws JSONException {
        Uri contentURI = Uri.parse("content://mms/part");
        String [] COLUMN = {
                Telephony.Mms.Part.CONTENT_TYPE,
                Telephony.Mms.Part.TEXT,
                Telephony.Mms.Part._ID,
        };
        String WHERE = Telephony.Mms.Part.MSG_ID + " = ?";
        String [] QUESTIONMARK = {message_id};
        Cursor cursor = contentResolver.query(contentURI,
                COLUMN,
                WHERE,
                QUESTIONMARK,
                null
        );

        JSONArray jsonArray = new JSONArray();
            if(cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(JSONBuilder.Parts.CONTENT_TYPE.toString().toUpperCase(),
                            cursor.getString(0));
                    jsonObject.put(JSONBuilder.Parts.TEXT.toString().toUpperCase(),
                            cursor.getString(1));
                    jsonObject.put(JSONBuilder.Parts.ID.toString().toLowerCase(),
                            cursor.getString(2));
                    jsonObject.put(JSONBuilder.Parts.MESSAGE_ID.toString().toLowerCase(),
                            message_id);
                    jsonArray.put(jsonObject);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return jsonArray;
    }

    private HashMap<String, JSONObject> matchRecipientId(HashSet<String> recipient_ids) throws JSONException {

        HashMap<String, JSONObject> recips = null;
        Cursor cursorCanonical = contentResolver.query(Uri.parse("content://mms-sms/canonical-addresses"),
                new String[] {
                        Telephony.CanonicalAddressesColumns._ID,
                        Telephony.CanonicalAddressesColumns.ADDRESS
                },
                Telephony.CanonicalAddressesColumns._ID + " IN (" + TextUtils.join(",", recipient_ids) + ")",
                null,
                null
        );

        if (cursorCanonical != null) {
            if(cursorCanonical.getCount() > 0){
                recips = new HashMap<String,JSONObject>((int)(recipient_ids.size()*1.5));
                cursorCanonical.moveToFirst();
                do {
                    JSONObject jsonObjectRecipient = new JSONObject();
                    String phoneNumber = cursorCanonical.getString(cursorCanonical.getColumnIndex(Telephony.CanonicalAddressesColumns.ADDRESS));
                    jsonObjectRecipient.put(JSONBuilder.JSON_KEY_CONVERSATION.PHONE_NUMBER.name().toLowerCase(),
                            phoneNumber);
                    String [] numberContactId = matchPhoneNumberToContactId(phoneNumber);
                    if (numberContactId[0] == null || numberContactId[0].isEmpty() || numberContactId[1] == null || numberContactId[1].isEmpty()) {
                        Log.d(Tag.MESSAGE_MANAGER, "Oh no! Message query just detected one of these was null. [" + numberContactId[0] + "] [" + numberContactId[1] + "]");
                    }
                    jsonObjectRecipient.put(JSONBuilder.JSON_KEY_CONVERSATION.FULL_NAME.name().toLowerCase(),
                            numberContactId[0]);
                    jsonObjectRecipient.put(JSONBuilder.JSON_KEY_CONVERSATION.CONTACT_ID.name().toLowerCase(),
                            numberContactId[1]);
                    recips.put(cursorCanonical.getString(cursorCanonical.getColumnIndex(Telephony.CanonicalAddressesColumns._ID)),
                            jsonObjectRecipient);
                }while (cursorCanonical.moveToNext());
            }
            cursorCanonical.close();
        }
        Log.d(Tag.MESSAGE_MANAGER, "ThreadId [" + recips + "]");
        return recips;
    }

    private JSONArray matchRecipientID(String [] recipient_ids) throws JSONException {

        JSONArray jsonArray = new JSONArray();

        Cursor cursorCanonical = contentResolver.query(Uri.parse("content://mms-sms/canonical-addresses"),
                new String[] {
                        Telephony.CanonicalAddressesColumns.ADDRESS
                },
                Telephony.CanonicalAddressesColumns._ID + " IN (" + TextUtils.join(",", recipient_ids) + ")",
                null,
                null
        );

        if (cursorCanonical != null) {
            if(cursorCanonical.getCount() > 0){
                cursorCanonical.moveToFirst();
                do {
                    JSONObject jsonObjectRecipient = new JSONObject();
                    String phoneNumber = cursorCanonical.getString(cursorCanonical.getColumnIndex(Telephony.CanonicalAddressesColumns.ADDRESS));
                    jsonObjectRecipient.put(JSONBuilder.JSON_KEY_CONVERSATION.PHONE_NUMBER.name().toLowerCase(),
                            phoneNumber);
                    String [] numberContactId = matchPhoneNumberToContactId(phoneNumber);
                    if (numberContactId[0] == null || numberContactId[0].isEmpty() || numberContactId[1] == null || numberContactId[1].isEmpty()) {
                        Log.d(Tag.MESSAGE_MANAGER, "Oh no! Message query just detected one of these was null. [" + numberContactId[0] + "] [" + numberContactId[1] + "]");
                    }
                    jsonObjectRecipient.put(JSONBuilder.JSON_KEY_CONVERSATION.FULL_NAME.name().toLowerCase(),
                            numberContactId[0]);
                    jsonObjectRecipient.put(JSONBuilder.JSON_KEY_CONVERSATION.CONTACT_ID.name().toLowerCase(),
                            numberContactId[1]);
                    jsonArray.put(jsonObjectRecipient);
                }while (cursorCanonical.moveToNext());
            }
            cursorCanonical.close();
        }
        Log.d(Tag.MESSAGE_MANAGER, "ThreadId [" + jsonArray + "]");
        return jsonArray;
    }

    private static String [] matchPhoneNumberToContactId(String phoneNumber) {
        String [] numberContactId = new String[2];
//        Log.d(Tag.MESSAGE_MANAGER, "Number searching for [" + phoneNumber + "]");
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

//        Log.d(Tag.MESSAGE_MANAGER, "Number searching for [" + uri.toString() + "]");
        Cursor contactLookup = null;
        try {
            contactLookup = contentResolver.query(
                    uri,
                    new String[] {
                            ContactsContract.PhoneLookup.DISPLAY_NAME,
                            android.provider.BaseColumns._ID
                    },
                    null,
                    null,
                    null);
            if (contactLookup != null) {
                if (contactLookup.getCount() > 0) {
                    contactLookup.moveToFirst();
                    numberContactId[0] = contactLookup.getString(0);
                    numberContactId[1] = contactLookup.getString(1);
                } else {
                    Log.d(Tag.MESSAGE_MANAGER, "COULDNT FIND NAME FOR [" + phoneNumber + "]");
                }
                contactLookup.close();
            }
        } catch (Exception e) {
            Log.e(Tag.MESSAGE_MANAGER, "ERROR OPENNING SPECIFC NUMBER");
        }

        return numberContactId;
    }

    public void queryDataAndSendAsync(final String partId, final String type,
                                      final String messageId,
                                      final Context context, final Intent intent) {

        new AsyncTask<Integer, Void, Void>() {

            @Override
            protected Void doInBackground(Integer ... params) {
                try {
                    if ("image/jpeg".equals(type) || "image/bmp".equals(type) ||
                            "image/gif".equals(type) || "image/jpg".equals(type) ||
                            "image/png".equals(type)) {
                        Uri uri = Uri.parse("content://mms/part/" + partId);
                        Log.e(Tag.MESSAGE_MANAGER, "TAG DATA [" + uri.getPath() + "]");
                        InputStream is = contentResolver.openInputStream(uri);
                        Bitmap bmp = null;
                        ByteArrayOutputStream stream = null;
                        Base64OutputStream base64OutputStream = null;
                        try {
                            bmp = BitmapFactory.decodeStream(is);
                            stream = new ByteArrayOutputStream();
                            base64OutputStream = new Base64OutputStream(stream,Base64.DEFAULT);
                            Log.d(Tag.MESSAGE_MANAGER, "STREAM START SIZE [" + stream.size() + "][" + is.available() + "]");
                            bmp.compress(Bitmap.CompressFormat.JPEG, 75, base64OutputStream);
                            Log.d(Tag.MESSAGE_MANAGER, "STREAM SIZE [" + stream.size() + "][" + is.available() + "]");
                            byte [] bytes = stream.toByteArray();
                            Log.d(Tag.MESSAGE_MANAGER, "DATA Finished compressing");
                            final int CHUNK_SIZE = 500000;
                            final int size = bytes.length/CHUNK_SIZE;
                            int sentSize = size+1;
                            if (bytes.length%CHUNK_SIZE == 0) {
                                sentSize--;
                            }
                            if (size > 0) {
                                for(int i = 0; i < size; i++) {
                                    String tempS = new String(bytes,(i*CHUNK_SIZE),CHUNK_SIZE);
                                    Log.d(Tag.MESSAGE_MANAGER, "Reading byte: " + i + " of: " + size
                                            + " [" + (i*CHUNK_SIZE) + "] [" + CHUNK_SIZE + "] [" + tempS.length() + "]");
                                    JSONBuilder jsonBuilder = new JSONBuilder(JSONBuilder.Action.POST_DATA);
                                    jsonBuilder.put(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.PART_ID.name().toLowerCase(),
                                            partId);
                                    jsonBuilder.put(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.MESSAGE_ID.name().toLowerCase(),
                                            messageId);
                                    jsonBuilder.put(JSONBuilder.JSON_KEY_CONVERSATION.DATA.name().toLowerCase(),
                                            tempS);
                                    jsonBuilder.put("order",i);
                                    jsonBuilder.put("size",sentSize);
                                    intent.putExtra(Tag.KEY_SEND_JSON_STRING, jsonBuilder.toString());
                                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                                }
                            }
                            if (bytes.length%CHUNK_SIZE != 0) {
                                //send remainder
                                String tempS = new String(bytes, (size * CHUNK_SIZE), bytes.length - (size * CHUNK_SIZE));
                                Log.d(Tag.MESSAGE_MANAGER, "Reading last byte of: " + size
                                        + " [" + (size * CHUNK_SIZE) + "] ["
                                        + (bytes.length - (size * CHUNK_SIZE)) + "] ["
                                        + tempS.length() + "] [" + sentSize + "]");
                                JSONBuilder jsonBuilder = new JSONBuilder(JSONBuilder.Action.POST_DATA);
                                jsonBuilder.put(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.PART_ID.name().toLowerCase(),
                                        partId);
                                jsonBuilder.put(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.MESSAGE_ID.name().toLowerCase(),
                                        messageId);
                                jsonBuilder.put(JSONBuilder.JSON_KEY_CONVERSATION.DATA.name().toLowerCase(),
                                        tempS);
                                jsonBuilder.put("order", sentSize-1);
                                jsonBuilder.put("size", sentSize);
                                intent.putExtra(Tag.KEY_SEND_JSON_STRING, jsonBuilder.toString());
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                            }
                            if (is != null) {
                                is.close();
                            }
                            if (stream != null) {
                                stream.close();
                            }
                            if (base64OutputStream != null) {
                                base64OutputStream.close();
                            }
                        } catch (JSONException e) {
                            Log.d(Tag.MESSAGE_MANAGER, "Unable to add data", e);
                        } finally {
                            if (bmp != null) {
                                bmp.recycle();
                            }
                        }
                    } else {
                        //data is not image return fail
                        try{
                            Log.d(Tag.MESSAGE_MANAGER, "About to send failed");
                            JSONBuilder jsonBuilder = new JSONBuilder(JSONBuilder.Action.POST_DATA);
                            jsonBuilder.put(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.MESSAGE_ID.name().toLowerCase(),
                                    partId);
                            jsonBuilder.put("fail", "true");
                            intent.putExtra(Tag.KEY_SEND_JSON_STRING, jsonBuilder.toString());
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                            Log.d(Tag.MESSAGE_MANAGER, "Sent failed");
                        } catch (JSONException e) {
                            Log.d(Tag.MESSAGE_MANAGER, "Unable to finish json failed context", e);
                        }
                    }
                    System.gc();
                } catch (IOException e) {
                    Log.e(Tag.MESSAGE_MANAGER, "ERROR READNG DATA!!", e);
                }
                return null;
            }
            //NOTE MUST BE SERIAL SO IT DOES NOT RUN OUT OF MEMORY WITH MULTIPLE REQUESTS
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
//        Cursor cursor = contentResolver.query(contentURI,
//                COLUMN,
//                WHERE,
//                QUESTIONMARK,
//                null
//        );
//
//        try {
//            if(cursor != null) {
//                if (cursor.getCount() > 0) {
//                    cursor.moveToFirst();
//                    do {
//                        String id = cursor.getString(cursor.getColumnIndex(Telephony.Mms.Part._ID));
//                        String type = cursor.getString(cursor.getColumnIndex(Telephony.Mms.Part.CONTENT_TYPE));
//                        if ("image/jpeg".equals(type) || "image/bmp".equals(type) ||
//                                "image/gif".equals(type) || "image/jpg".equals(type) ||
//                                "image/png".equals(type)) {
//                            Uri uri = Uri.parse("content://mms/part/" + id);
//                            Log.e(Tag.MESSAGE_MANAGER, "TAG DATA [" + uri.getPath() + "]");
//                            InputStream is = contentResolver.openInputStream(uri);
//                            Bitmap bmp = BitmapFactory.decodeStream(is);
//                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
//                            try {
//                                jsonBuilder.put(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.MESSAGE_ID.name().toLowerCase(),
//                                        message_id);
////                                jsonBuilder.put(JSONBuilder.JSON_KEY_CONVERSATION.DATA.name().toLowerCase(),
////                                        stream.toString("UTF-8"));
//                            } catch (JSONException e) {
//                                Log.d(Tag.MESSAGE_MANAGER, "Unable to add data", e);
//                            }
////                            } catch (UnsupportedEncodingException e) {
////                                Log.d(Tag.MESSAGE_MANAGER, "Unable to add data unsopported encoding", e);
////                            }
//
//                        }
//                    } while (cursor.moveToNext());
//                    cursor.close();
//                }
//            }
//        } catch (FileNotFoundException e) {
//            Log.e(Tag.MESSAGE_MANAGER, "ERROR READNG DATA!!", e);
//        }
//        return jsonBuilder;
    }
}