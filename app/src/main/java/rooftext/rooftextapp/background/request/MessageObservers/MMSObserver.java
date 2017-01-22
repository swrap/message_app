package rooftext.rooftextapp.background.request.MessageObservers;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.util.Log;

import com.klinker.android.send_message.ApnUtils;
import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Settings;
import com.klinker.android.send_message.Transaction;

import org.json.JSONArray;
import org.json.JSONException;

import rooftext.rooftextapp.background.io.WebSocketManager;
import rooftext.rooftextapp.background.request.MessageQuery;
import rooftext.rooftextapp.background.request.RequestManager;
import rooftext.rooftextapp.io.JSONBuilder;
import rooftext.rooftextapp.utils.Tag;

/**
 * Created by Jesse Saran on 8/24/2016.
 */
public class MMSObserver extends ContentObserver {

    private static final Handler handler = new Handler();
    protected Uri uri = Uri.parse("content://mms/");
    private static MMSObserver mmsObserver = null;
    private final Context context;
    private WebSocketManager webSocketManager;
    private LocalMmsReceiver localMMSReceiver;

    public MMSObserver (Context context) {
        super(handler);
        this.context = context;
        this.webSocketManager = WebSocketManager.getInstance(context);
        this.localMMSReceiver = new LocalMmsReceiver(context);
        context.getContentResolver().registerContentObserver(uri, true, this);
    }

    public static MMSObserver getInstance(final Context context) {
        if (mmsObserver == null) {
            mmsObserver = new MMSObserver(context);
            ApnUtils.initDefaultApns(context, null, false);
        }
        return mmsObserver;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        Log.d(Tag.SMS_OBSERVER, "Received change from URI-Path [" + uri.getPath() + "] host [" + uri.getHost() + "] self change [" + selfChange + "]"
                + " uri-to-string [" + uri.toString()+ "]");
        synchronized (this) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, null, null, null, null);
                Log.d(Tag.MMS_OBSERVER, "cursor: " + cursor + " " + cursor.getCount() + " " + uri.getHost() + " " + uri.getPath());
                if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                    Log.d(Tag.MMS_OBSERVER, "Was received. [" + cursor.getString(cursor.getColumnIndex(Telephony.Mms.MESSAGE_BOX)) + "] [" + Telephony.BaseMmsColumns.MESSAGE_BOX_SENT + "] " +
                            cursor.getString(cursor.getColumnIndex(Telephony.BaseMmsColumns.MESSAGE_BOX)).equals(""+Telephony.BaseMmsColumns.MESSAGE_BOX_SENT));
                    try {
                        if(cursor.getString(cursor.getColumnIndex(Telephony.BaseMmsColumns.MESSAGE_BOX)).equals(""+Telephony.BaseMmsColumns.MESSAGE_BOX_SENT)) {
                            final String threadId = cursor.getString(
                                    cursor.getColumnIndex(Telephony.BaseMmsColumns.THREAD_ID));
                            final String message_id = cursor.getString(
                                    cursor.getColumnIndex(Telephony.BaseMmsColumns._ID));
                            final String body = MessageQuery.getMMSParts(message_id).getJSONObject(1).getString("TEXT");
                            Log.e(Tag.MMS_OBSERVER, "Body [" + body + "]");
                            Log.d(Tag.SMS_OBSERVER, "Length [" + RequestManager.messagesWaiting.size()  + "]");
                            for (MessageWaiting messageWaiting : RequestManager.messagesWaiting) {
                                Log.d(Tag.SMS_OBSERVER, "Length [" + RequestManager.messagesWaiting.size()  + "]");
                                if (threadId.equals("" + threadId) && body.equals(messageWaiting.body)) {
                                    Log.d(Tag.MMS_OBSERVER, "Found message. Sending return to websocket. Thread ID [" + threadId + "] id [" + message_id + "] temp message id [" +
                                            messageWaiting.temp_message_id + "]");
                                    JSONBuilder jsonBuilder = messageWaiting.getJSON(threadId, message_id);
                                    try {
                                        jsonBuilder.put(threadId, new JSONArray().put(MessageQuery.getMMSJSON(cursor)));
                                    } catch (JSONException e) {
                                        Log.d(Tag.MMS_OBSERVER, "Unable to get messages for received successfully sent.");
                                    }
                                    webSocketManager.sendString(jsonBuilder.toString());
                                    RequestManager.messagesWaiting.remove(messageWaiting);
                                    break;
                                } else {
                                    Log.v(Tag.MMS_OBSERVER, "Message not found.");
                                }
                            }
                        } else {
                            JSONBuilder jsonBuilder = new JSONBuilder(JSONBuilder.Action.RECEIVED_MESSAGE);
                            String thread_id = cursor.getString(cursor.getColumnIndex(Telephony.BaseMmsColumns.THREAD_ID));
                            Log.d(Tag.MMS_OBSERVER, "Thread id [" + thread_id + "]");
                            jsonBuilder.put(JSONBuilder.JSON_KEY_CONVERSATION.THREAD_ID.name().toLowerCase(),
                                    thread_id);
                            jsonBuilder.put(thread_id, new JSONArray().put(MessageQuery.getMMSJSON(cursor)));
                            Log.d(Tag.MMS_OBSERVER, "Received message, sending away! [" + jsonBuilder.toString() + "]");
                            webSocketManager.sendString(jsonBuilder.toString());
                        }
                    } catch (JSONException e) {
                        Log.d(Tag.MMS_OBSERVER, "Unable to query message for incoming message.", e);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    public void sendMMS(final Context context, final String body, final String [] numbers, final String tempMessageID) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Settings sendSettings = new Settings();
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                sendSettings.setMmsc(sharedPreferences.getString("mmsc_url", ""));
                sendSettings.setProxy(sharedPreferences.getString("mms_proxy", ""));
                sendSettings.setPort(sharedPreferences.getString("mms_port", ""));
                sendSettings.setUseSystemSending(true);

                Transaction transaction = new Transaction(context, sendSettings);
                Log.e("RM:MMO","ABout to send.");
                Message message = new Message(body, numbers);

//                if (imageToSend.isEnabled()) {
//                    message.setImage(BitmapFactory.decodeResource(getResources(), R.drawable.android));
//                }
                transaction.sendNewMmsMessage(message, LocalMmsReceiver.getIntent(tempMessageID), null);
                Log.e("RM:MMO","sending");
            }
        }).start();
    }
}