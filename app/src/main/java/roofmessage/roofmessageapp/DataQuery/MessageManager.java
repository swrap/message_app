package roofmessage.roofmessageapp.dataquery;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import roofmessage.roofmessageapp.io.JSONBuilder;
import roofmessage.roofmessageapp.utils.Tag;
import roofmessage.roofmessageapp.utils.Utils;

/**
 * Created by Jesse Saran on 7/4/2016.
 */
public class MessageManager {
    private static MessageManager messageManager = null;
    private static ContentResolver contentResolver;

    protected MessageManager(Context context) {
        contentResolver = context.getContentResolver();
    }

    protected static MessageManager getInstance(Context context) {
        if( messageManager == null) {
            messageManager = new MessageManager(context);
        }

        return messageManager;
    }

    public JSONBuilder getAllConversations() {
        Uri mSMSMMS = Uri.parse("content://mms-sms/conversations?simple=true");

        String [] SMSMMS_COLUMNS = new String[]{
                Telephony.ThreadsColumns.DATE,
                Telephony.ThreadsColumns._ID,
                Telephony.ThreadsColumns.MESSAGE_COUNT,
                Telephony.ThreadsColumns.READ,
                Telephony.ThreadsColumns.RECIPIENT_IDS,
                Telephony.ThreadsColumns.TYPE,
            };

        String ORDERBY = "date DESC";
        Cursor cursor = contentResolver.query(mSMSMMS,
                SMSMMS_COLUMNS,
                null,
                null,
                ORDERBY
        );

        JSONBuilder jsonObject = new JSONBuilder( JSONBuilder.Action.POST_CONVERSATIONS );
        JSONArray jsonArray = new JSONArray();
        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                JSONObject conversation = new JSONObject();
                try {
                    //conversation
                    conversation.put(JSONBuilder.JSON_KEY_CONVERSATION.DATE.name().toLowerCase(),
                            cursor.getString(0));
                    conversation.put(JSONBuilder.JSON_KEY_CONVERSATION.THREAD_ID.name().toLowerCase(),
                            cursor.getString(1));
                    conversation.put(JSONBuilder.JSON_KEY_CONVERSATION.MESSAGE_COUNT.name().toLowerCase(),
                            cursor.getString(2));
                    conversation.put(JSONBuilder.JSON_KEY_CONVERSATION.READ.name().toLowerCase(),
                            cursor.getString(3));

                    //check for found found recipients
                    JSONArray matchRecipients = matchRecipientID(cursor.getString(4).split(" "));
                    if( matchRecipients != null ) {
                        conversation.put(JSONBuilder.JSON_KEY_CONVERSATION.RECIPIENTS.name().toLowerCase(),
                                matchRecipients);
                    }

                    conversation.put(JSONBuilder.JSON_KEY_CONVERSATION.CONVO_TYPE.name().toLowerCase(),
                            cursor.getString(5));

                    jsonArray.put(conversation);
                } catch (JSONException e) {
                    Log.e(Tag.MESSAGE_MANAGER, "Could not add conversation.");
                    e.printStackTrace();
                }

            } while(cursor.moveToNext());
        }
        if(cursor != null){
            cursor.close();
        }
        try {
            jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.CONVERSATIONS.name().toLowerCase(),
                    (Object) jsonArray);
            return jsonObject;
        } catch (JSONException e) {
            Log.e(Tag.MESSAGE_MANAGER,"Could not add array to json.");
            e.printStackTrace();
        }
        return null;
    }

    public JSONBuilder getConversationMessages(String thread_id, int limit, int offset){
        Cursor smsCursor = getSMSMessages(thread_id, limit, offset);
        String start_date = "0";
        String end_date = "0";
        if(smsCursor.getCount() > 0){
            smsCursor.moveToFirst();
            end_date = smsCursor.getString(smsCursor.getColumnIndex(Telephony.TextBasedSmsColumns.DATE));
            smsCursor.moveToLast();
            start_date = smsCursor.getString(smsCursor.getColumnIndex(Telephony.TextBasedSmsColumns.DATE));
        }
        Cursor mmsCursor = getMMSMessages(thread_id, limit,
                Utils.convertSMStoMMSDate(start_date),
                Utils.convertSMStoMMSDate(end_date)
        );

        JSONBuilder jsonObject = new JSONBuilder( JSONBuilder.Action.POST_MESSAGES );
        JSONArray jsonArray = new JSONArray();

        try {
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
                        int compare = smsDate.compareTo(Utils.convertMMStoSMSDate(mmsDate));
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

            jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.MESSAGES.name().toLowerCase(),
                    (Object) jsonArray);
        } catch (JSONException e) {
            Log.e(Tag.MESSAGE_MANAGER,"Could not add array to json.");
            e.printStackTrace();
        }
        return jsonObject;
    }

    private JSONObject getMMSJSON(Cursor cursor) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSONBuilder.Message_Type.TYPE.name().toLowerCase(),
                JSONBuilder.Message_Type.MMS.name().toLowerCase());
        jsonObject.put("Thread_id",
                    cursor.getString(0));
        jsonObject.put("date received",
                Utils.convertMMStoSMSDate(cursor.getString(1)));
        jsonObject.put("date sent",
                    cursor.getString(2));
        jsonObject.put("locked",
                    cursor.getString(3));
        jsonObject.put("seen",
                    cursor.getString(4));
        jsonObject.put("read",
                    cursor.getString(5));
        jsonObject.put("subject",
                    cursor.getString(6));
        jsonObject.put("text_only",
                    cursor.getString(7));
        jsonObject.put("id",
                    cursor.getString(8));
        jsonObject.put("parts", getMMSParts(cursor.getString(8)));
        return jsonObject;
    }

    private JSONObject getSMSJSON(Cursor cursor) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(JSONBuilder.Message_Type.TYPE.name().toLowerCase(),
                JSONBuilder.Message_Type.SMS.name().toLowerCase());
        jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.BODY.name().toLowerCase(),
                cursor.getString(0));
        jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.DATE_RECIEVED.name().toLowerCase(),
                cursor.getString(1));
        jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.DATE_SENT.name().toLowerCase(),
                cursor.getString(2));
        jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.LOCKED.name().toLowerCase(),
                cursor.getString(3));
        jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.SEEN.name().toLowerCase(),
                cursor.getString(4));
        jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.READ.name().toLowerCase(),
                cursor.getString(5));
        jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.SUBJECT.name().toLowerCase(),
                cursor.getString(6));
        jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.THREAD_ID.name().toLowerCase(),
                cursor.getString(7));
        return jsonObject;
    }

    private Cursor getSMSMessages(String thread_id,int limit,int offset){
        Uri contentURI = Telephony.Sms.CONTENT_URI;
        String [] COLUMN = {
                Telephony.TextBasedSmsColumns.BODY,
                Telephony.TextBasedSmsColumns.DATE,
                Telephony.TextBasedSmsColumns.DATE_SENT,
                Telephony.TextBasedSmsColumns.LOCKED,
                Telephony.TextBasedSmsColumns.SEEN,
                Telephony.TextBasedSmsColumns.READ,
                Telephony.TextBasedSmsColumns.SUBJECT,
                Telephony.TextBasedSmsColumns.THREAD_ID,
        };
        String WHERE = Telephony.Sms.Conversations.THREAD_ID + " = ?";
        String [] QUESTIONMARK = {thread_id};
        String ORDERBY =Telephony.Sms.Conversations.DEFAULT_SORT_ORDER + " limit " + limit +
                " offset " + offset;
        Cursor cursor = contentResolver.query(contentURI,
                COLUMN,
                WHERE,
                QUESTIONMARK,
                ORDERBY
        );
        return cursor;
    }

    public JSONBuilder getSMS(String thread_id, int limit, int offset){
        Cursor cursor = getSMSMessages(thread_id, limit, offset);
        JSONBuilder jsonObject = new JSONBuilder( JSONBuilder.Action.POST_MESSAGES );
        JSONArray jsonArray = new JSONArray();
        if(cursor.getCount() > 0) {
            Log.d(Tag.MESSAGE_MANAGER, "AMOUNT: " + cursor.getCount() + " limit: " + limit + " offset: " + offset);
            cursor.moveToFirst();
            do {
                JSONObject message = new JSONObject();
                try {
                    message.put(JSONBuilder.JSON_KEY_CONVERSATION.BODY.name().toLowerCase(),
                            cursor.getString(0));
                    message.put(JSONBuilder.JSON_KEY_CONVERSATION.DATE_RECIEVED.name().toLowerCase(),
                            cursor.getString(1));
                    message.put(JSONBuilder.JSON_KEY_CONVERSATION.DATE_SENT.name().toLowerCase(),
                            cursor.getString(2));
                    message.put(JSONBuilder.JSON_KEY_CONVERSATION.LOCKED.name().toLowerCase(),
                            cursor.getString(3));
                    message.put(JSONBuilder.JSON_KEY_CONVERSATION.SEEN.name().toLowerCase(),
                            cursor.getString(4));
                    message.put(JSONBuilder.JSON_KEY_CONVERSATION.READ.name().toLowerCase(),
                            cursor.getString(5));
                    message.put(JSONBuilder.JSON_KEY_CONVERSATION.SUBJECT.name().toLowerCase(),
                            cursor.getString(6));
                    message.put(JSONBuilder.JSON_KEY_CONVERSATION.THREAD_ID.name().toLowerCase(),
                            cursor.getString(7));
                    jsonArray.put(message);
                } catch (JSONException e) {
                    Log.e(Tag.MESSAGE_MANAGER, "Could not add contact.");
                    e.printStackTrace();
                }
            } while(cursor.moveToNext());
        }

        try {
            jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.MESSAGES.name().toLowerCase(),
                    (Object) jsonArray);
        } catch (JSONException e) {
            Log.e(Tag.MESSAGE_MANAGER,"Could not add array to json.");
            e.printStackTrace();
        }
        if(cursor != null){
            cursor.close();
        }
        return jsonObject;
    }

    public JSONBuilder getMMS(String thread_id, int limit, String start_date, String end_date){
        JSONBuilder jsonObject = new JSONBuilder( JSONBuilder.Action.POST_MESSAGES );
        JSONArray jsonArray = new JSONArray();
        Cursor cursor = getMMSMessages(thread_id, limit, start_date, end_date);
        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                JSONObject message = new JSONObject();
                try {
                    message.put("Thread_id",
                            cursor.getString(0));
                    message.put("date",
                            cursor.getString(1));
                    message.put("date sent",
                            cursor.getString(2));
                    message.put("locked",
                            cursor.getString(3));
                    message.put("seen",
                            cursor.getString(4));
                    message.put("read",
                            cursor.getString(5));
                    message.put("subject",
                            cursor.getString(6));
                    message.put("text_only",
                            cursor.getString(7));
                    message.put("id",
                            cursor.getString(8));
                    message.put("parts", getMMSParts(cursor.getString(8)));
                    jsonArray.put(message);
                } catch (JSONException e) {
                    Log.e(Tag.MESSAGE_MANAGER, "Could not add contact.");
                    e.printStackTrace();
                }
            } while(cursor.moveToNext());
        }

        try {
            jsonObject.put(JSONBuilder.JSON_KEY_CONVERSATION.MESSAGES.name().toLowerCase(),
                    (Object) jsonArray);
        } catch (JSONException e) {
            Log.e(Tag.MESSAGE_MANAGER,"Could not add array to json.");
            e.printStackTrace();
        }
        if(cursor != null){
            cursor.close();
        }
        return jsonObject;
    }

    private Cursor getMMSMessages(String thread_id, int limit, String start_date, String end_date){
        Uri contentURI = Telephony.Mms.CONTENT_URI;
        String [] COLUMN = {
                Telephony.BaseMmsColumns.THREAD_ID,
                Telephony.BaseMmsColumns.DATE,
                Telephony.BaseMmsColumns.DATE_SENT,
                Telephony.BaseMmsColumns.LOCKED,
                Telephony.BaseMmsColumns.SEEN,
                Telephony.BaseMmsColumns.READ,
                Telephony.BaseMmsColumns.SUBJECT,
                Telephony.BaseMmsColumns.TEXT_ONLY,
                Telephony.BaseMmsColumns._ID,
        };
        String WHERE = Telephony.BaseMmsColumns.THREAD_ID + " = ?";
        String [] QUESTIONMARK = null;
        String ORDERBY = "";
        Log.e(Tag.MESSAGE_MANAGER, "LIMIT: " + limit + " DATE: " + start_date + " Date: " + end_date);
        if(start_date.equals("0") && end_date.equals("0")) {
            ORDERBY = Telephony.Mms.DEFAULT_SORT_ORDER + " limit " + limit;
            QUESTIONMARK = new String[]{thread_id};
        } else {
            ORDERBY = Telephony.Mms.DEFAULT_SORT_ORDER;
            QUESTIONMARK = new String[]{thread_id, start_date, end_date};
            WHERE += " AND " + Telephony.BaseMmsColumns.DATE + " BETWEEN ? AND ?";
        }

        Log.e(Tag.MESSAGE_MANAGER, "LIMIT: " + limit + " \n" + ORDERBY +
        " \n" + QUESTIONMARK[0] + " " + QUESTIONMARK[1] + " \n" + WHERE);
        Cursor cursor = contentResolver.query(contentURI,
                COLUMN,
                WHERE,
                QUESTIONMARK,
                ORDERBY
        );
        return cursor;
    }

    private JSONArray getMMSParts(String message_id) throws JSONException {
        Uri contentURI = Uri.parse("content://mms/part");
        String [] COLUMN = {
                Telephony.Mms.Part.CONTENT_TYPE,
                Telephony.Mms.Part.TEXT,
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
                    jsonObject.put("CONTENT_TYPE",
                            cursor.getString(0));
                    jsonObject.put("TEXT",
                            cursor.getString(1));
                    jsonArray.put(jsonObject);
                } while (cursor.moveToNext());
                cursor.close();
            }
        }
        return jsonArray;
    }

    private JSONArray matchRecipientID(String [] recipient_ids) throws JSONException {

        ArrayList<String> strings = new ArrayList<String>(Arrays.asList(recipient_ids));
        JSONArray jsonArray = new JSONArray();

        Cursor cursorCanonical = contentResolver.query(Uri.parse("content://mms-sms/canonical-addresses"),
                new String[] {
                        Telephony.CanonicalAddressesColumns._ID,
                        Telephony.CanonicalAddressesColumns.ADDRESS
                },
                Telephony.CanonicalAddressesColumns._ID + " IN (" + TextUtils.join(",", strings) + ")",
                null,
                null
        );

        if(cursorCanonical.getCount() > 0){
            cursorCanonical.moveToFirst();
            do {
                JSONObject jsonObjectRecipient = new JSONObject();
                jsonObjectRecipient.put(JSONBuilder.JSON_KEY_CONVERSATION.RECIPIENT_ID.name().toLowerCase(),
                        cursorCanonical.getString(0));
                jsonObjectRecipient.put(JSONBuilder.JSON_KEY_CONVERSATION.PHONE_NUMBER.name().toLowerCase(),
                        cursorCanonical.getString(1));
                jsonArray.put(jsonObjectRecipient);
            }while (cursorCanonical.moveToNext());
        }
        if(cursorCanonical != null) {
            cursorCanonical.close();
        }
        return jsonArray;
    }

}