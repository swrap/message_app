package roofmessage.roofmessageapp.background.request;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;

import roofmessage.roofmessageapp.background.request.MessageObservers.SMSObserver;
import roofmessage.roofmessageapp.io.JSONBuilder;
import roofmessage.roofmessageapp.background.request.MessageObservers.MessageWaiting;
import roofmessage.roofmessageapp.utils.Tag;

/**
 * Created by Jesse Saran on 7/19/2016.
 */
public class SmsMmsDelivery {

    private enum MessageType {
        MMS_DATA,
        MMS_MULTI_RECIPIENT,
        SMS_SINGLE,
        SMS_MULTI,
    }

    private ArrayList<String> body;
    private String numbers;
    private SmsManager smsManager = SmsManager.getDefault();
    private MessageType messageType = null;
    private String temp_message_id;

    public SmsMmsDelivery(String body, String temp_message_id, String [] numbers) {
        this.body = smsManager.divideMessage(body);
        this.numbers = formatNumbers(numbers);
        this.temp_message_id = temp_message_id;
        if (numbers.length == 1) {
            if (this.body.size() == 1) {
                messageType = MessageType.SMS_SINGLE;
            } else if (this.body.size() > 1) {
                messageType = MessageType.SMS_MULTI;
            }
        } else if (numbers.length > 1) {
            if (this.body.size() > 0) {
                messageType = MessageType.MMS_MULTI_RECIPIENT;
            }
        }
    }

    private String formatNumbers(String [] numbers) {
        //Todo add in validation check on numbers
        String numbersCombined = "";
        for (String number : numbers) {
            numbersCombined += number;
        }
        return numbersCombined;
    }

    public ArrayList<MessageWaiting> prepareMessage() {
        ArrayList<MessageWaiting> messageWaiting = new ArrayList<MessageWaiting>();
        if (messageType == MessageType.SMS_SINGLE) {
            messageWaiting.add(new MessageWaiting(body.get(0), temp_message_id, numbers));
        } else if (messageType == MessageType.SMS_MULTI) {
            //TODO change this to the right method of sending
            for(String tempBody : body) {
                messageWaiting.add(new MessageWaiting(tempBody, temp_message_id, numbers));
            }
        }
        return messageWaiting;
    }
//(context, sentPendingIntent, sentFailedPendingIntent);
    public void sendMessage(Context context, SMSObserver smsObserver) {
        Log.e(Tag.SMS_OBSERVER, "TYPE: " + messageType + " " + MessageType.SMS_SINGLE);
        if (messageType == MessageType.SMS_SINGLE) {
            //successful intent
//            Intent sentIntent = smsObserver.getMessageSentIntent();
//            sentIntent.putExtra(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.TEMP_MESSAGE_ID.name().toLowerCase(),
//                    temp_message_id);
            //TODO TESTING CLEAN THIS UP
//            PendingIntent sentPendingIntent = PendingIntent.getBroadcast(
//                    context, 0, sentIntent,
//                    PendingIntent.FLAG_UPDATE_CURRENT);
            //failed intent
            Intent sentFailedIntent = smsObserver.getMessageSentFailedIntent();
            sentFailedIntent.putExtra(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.TEMP_MESSAGE_ID.name().toLowerCase(),
                    temp_message_id);
            PendingIntent sentFailedPendingIntent = PendingIntent.getBroadcast(
                    context, 0, sentFailedIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            Log.e(Tag.SMS_OBSERVER, "Body [" + body.get(0) + "]");
            smsManager.sendTextMessage(numbers, null, body.get(0), sentFailedPendingIntent, null);
        } else if (messageType == MessageType.SMS_MULTI) {
            //TODO change this to the right method of sending
//            intent.putExtra(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.TEMP_MESSAGE_ID.name().toLowerCase(),
//                    temp_message_id);
//            for(String tempBody : body) {
//                smsManager.sendTextMessage(numbers, null, tempBody, null, null);
//            }
        }
    }
}
