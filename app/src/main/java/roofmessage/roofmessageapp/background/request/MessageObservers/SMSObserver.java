package roofmessage.roofmessageapp.background.request.MessageObservers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import roofmessage.roofmessageapp.Flush;
import roofmessage.roofmessageapp.io.JSONBuilder;
import roofmessage.roofmessageapp.background.io.WebSocketManager;
import roofmessage.roofmessageapp.utils.Tag;

/**
 * Created by Jesse Saran on 7/19/2016.
 */
public class SMSObserver extends ContentObserver implements Flush{
    private static final Handler handler = new Handler();
    protected Uri uri = Uri.parse("content://sms/");
    private static SMSObserver smsObserver;
    private Context context;
    private WebSocketManager webSocketManager;
    public final SendingFailureReceiver sendingFailureReceiver;
    public final SendingSuccessfullReceiver sendingSuccessfullReceiver;

    private final String MESSAGE_SENT_FAILED = "MESSAGE_SENT_FAILED";
    private final String MESSAGE_SENT = "MESSAGE_SENT";

    private static ConcurrentLinkedQueue<MessageWaiting> messagesWaiting =
            new ConcurrentLinkedQueue<MessageWaiting>();

    private SMSObserver(Context context) {
        super(handler);
        this.context = context;
        webSocketManager = WebSocketManager.getInstance(context);
        sendingFailureReceiver = new SendingFailureReceiver();
        sendingSuccessfullReceiver = new SendingSuccessfullReceiver();
    }

    public static SMSObserver getInstance(Context context) {
        if (smsObserver == null) {
            smsObserver = new SMSObserver(context);
        }
        return smsObserver;
    }

    public void addMessage(MessageWaiting messageWaiting) {
        messagesWaiting.add(messageWaiting);
    }

    public Intent getMessageSentIntent() {
        return new Intent(MESSAGE_SENT);
    }

    public Intent getMessageSentFailedIntent() {
        return new Intent(MESSAGE_SENT_FAILED);
    }

    public void start() {
        Log.d(Tag.SMS_OBSERVER, "Starting");
        if (context != null && context.getContentResolver() != null) {
            context.getContentResolver().registerContentObserver(uri, true, this);
            context.registerReceiver(sendingFailureReceiver, new IntentFilter(MESSAGE_SENT_FAILED));
//            context.getContentResolver().registerContentObserver(uri, true, this); //TODO REMOVED THIS MIGHT BREAK IT. IDK. ALSO IN FLUSH, UNREGISTER
            context.registerReceiver(sendingSuccessfullReceiver, new IntentFilter(MESSAGE_SENT));
        }
        else {
            throw new IllegalStateException(
                    "Current SmsObserver instance is invalid");
        }
    }

    @Override
    public boolean flush() {
        context.getContentResolver().unregisterContentObserver(this);
        if (sendingFailureReceiver != null) {
            context.unregisterReceiver(sendingFailureReceiver);
        }
        if (sendingFailureReceiver != null) {
            context.unregisterReceiver(sendingSuccessfullReceiver);
        }
        return true;
    }

    private class SendingFailureReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String uriString = intent.getStringExtra("uri");
            Uri uri = Uri.parse(uriString);
            String messageId = uri.toString().substring( uri.toString().lastIndexOf('/') + 1 );
            Log.d(Tag.SMS_OBSERVER, "URI: " + uri.toString() + " message_id [" + messageId + "]");
            Log.d(Tag.SMS_OBSERVER, "URI: " + uri.toString());
            String temp_message_id = intent.getStringExtra(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.TEMP_MESSAGE_ID.name().toLowerCase());

            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    break;
                default:
                    Log.e(Tag.SMS_OBSERVER, "Could not send: " + temp_message_id);
                    for (MessageWaiting messageWaiting : messagesWaiting) {
                        Log.d(Tag.SMS_OBSERVER, "FAILED found message. Sending return to websocket. temp_message_id [" + temp_message_id + "] message_id [" + messageId + "]");
                        webSocketManager.sendBytes(messageWaiting.getFailedJSON(messageId).toString().getBytes());
                        messagesWaiting.remove(messageWaiting);
                        break;
                    }
                    break;
            }
        }
    }

    private class SendingSuccessfullReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            Set<String> keySet = bundle.keySet();
            for (String string : keySet) {
                Log.e(Tag.SMS_OBSERVER, "String set: " + string);
            }
            //TODO TESTING CLEAN THIS UP
//            SmsMessage smsMesssage = SmsMessage.createFromPdu(bundle.getByteArray("pdu"), bundle.getString("format"));

//            bundle.get
//            String uriString = intent.getStringExtra("uri");
//            Log.e(Tag.SMS_OBSERVER, "uriString [" + uriString + "]" );
//            Uri uri = Uri.parse(uriString);
//            Log.d(Tag.SMS_OBSERVER, "URI: " + uri.toString());
//            String temp_message_id = intent.getStringExtra(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.TEMP_MESSAGE_ID.name().toLowerCase());
//            switch (getResultCode()) {
//                case Activity.RESULT_OK:
//                    break;
//                default:
//                    Log.e(Tag.SMS_OBSERVER, "Could not send: " + temp_message_id);
//                    break;
//            }
        }
    }

    public SendingFailureReceiver getSendingFailureReceiver() {
        return sendingFailureReceiver;
    }

    public SendingSuccessfullReceiver getSendingSuccessfullReceiver() { return  sendingSuccessfullReceiver; }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        Log.d(Tag.SMS_OBSERVER, "Received change from URI-Path [" + uri.getPath() + "] host [" + uri.getHost() + "] concurrent size [" + messagesWaiting.size() + "] self change [" + selfChange + "]"
        + " uri-to-string [" + uri.toString()+ "]");
        synchronized (this) {
            if (messagesWaiting.size() > 0) {
                Cursor cursor = null;
                try {
                    cursor = context.getContentResolver().query(uri, null, null, null, null);
                    Log.d(Tag.SMS_OBSERVER, "cursor: " + cursor + " " + cursor.getCount());
                    if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                        final int type = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.TYPE));
                        Log.d(Tag.SMS_OBSERVER, "type [" + type + "] " +
                                "\ntypes [" + Telephony.Sms.MESSAGE_TYPE_ALL + "] " +
                                "\ntypes [" + Telephony.Sms.MESSAGE_TYPE_INBOX + "] " +
                                "\ntypes [" + Telephony.Sms.MESSAGE_TYPE_SENT + "] " +
                                "\ntypes [" + Telephony.Sms.MESSAGE_TYPE_DRAFT + "] " +
                                "\ntypes [" + Telephony.Sms.MESSAGE_TYPE_OUTBOX + "] " +
                                "\ntypes [" + Telephony.Sms.MESSAGE_TYPE_FAILED + "] " +
                                "\ntypes [" + Telephony.Sms.MESSAGE_TYPE_QUEUED + "] " +
                                "\ntypes [" + Telephony.Sms.Sent.MESSAGE_TYPE_SENT + "] ");

                        if (type == Telephony.Sms.Sent.MESSAGE_TYPE_SENT) {
                            final String address = cursor.getString(
                                    cursor.getColumnIndex(Telephony.Sms.ADDRESS));
                            final String body = cursor.getString(
                                    cursor.getColumnIndex(Telephony.Sms.BODY));
                            final String threadId = cursor.getString(
                                    cursor.getColumnIndex(Telephony.Sms.THREAD_ID));
                            final String message_id = cursor.getString(
                                    cursor.getColumnIndex(Telephony.Sms._ID));
                            for (MessageWaiting messageWaiting : messagesWaiting) {
                                if (PhoneNumberUtils.compare(address, messageWaiting.numbers) &&
                                        body.equals(messageWaiting.body)) {
                                    Log.d(Tag.SMS_OBSERVER, "Found message. Sending return to websocket. Thread ID [" + threadId + "] id [" + message_id + "] address [" + address + "]");
                                    webSocketManager.sendBytes(messageWaiting.getJSON(
                                            threadId, message_id
                                    ).toString().getBytes());
                                    messagesWaiting.remove(messageWaiting);
                                    break;
                                } else {
                                    Log.v(Tag.SMS_OBSERVER, "Message not found.");
                                }
                            }
                        }
                    } else {
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
    }
}
