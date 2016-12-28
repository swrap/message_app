package roofmessage.roofmessageapp.background.request;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;

import roofmessage.roofmessageapp.background.request.MessageObservers.MMSObserver;
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

    private ArrayList<String> bodyArrList;
    private String body;
    private String strNumbers;
    private String [] numbers;
    private SmsManager smsManager = SmsManager.getDefault();
    private MessageType messageType = null;
    private String temp_message_id;
    private SMSObserver smsObserver;
    private MMSObserver mmsObserver;

    public SmsMmsDelivery(String body, String temp_message_id, String [] numbers, SMSObserver smsObserver, MMSObserver mmsObserver) {
        this.smsObserver = smsObserver;
        this.mmsObserver = mmsObserver;
        this.body = body;
        this.bodyArrList = smsManager.divideMessage(body);
        this.strNumbers = formatNumbers(numbers);
        this.numbers = numbers;
        this.temp_message_id = temp_message_id;
        if (numbers.length == 1) {
            if (this.bodyArrList.size() == 1) {
                messageType = MessageType.SMS_SINGLE;
            } else if (this.bodyArrList.size() > 1) {
                //TODO: changuing to mms multi recipient for now
//                messageType = MessageType.SMS_MULTI;
                messageType = MessageType.MMS_MULTI_RECIPIENT;
            }
        } else if (numbers.length > 1) {
            if (this.bodyArrList.size() > 0) {
                messageType = MessageType.MMS_MULTI_RECIPIENT;
            }
        }
    }

    private String formatNumbers(String [] numbers) {
        //Todo add in validation check on strNumbers
        String numbersCombined = "";
        for (int i = 0; i < numbers.length; i++) {
            numbersCombined += numbers[i];
            if (i < numbers.length-1 && numbers.length > 1) {
                numbersCombined += ", ";
            }
        }
        Log.d(Tag.SMSMMSDELIVERY, "Numbers [" + numbersCombined + "]");
        return numbersCombined;
    }

    public ArrayList<MessageWaiting> prepareMessage() {
        ArrayList<MessageWaiting> messageWaiting = new ArrayList<MessageWaiting>();
        if (messageType == MessageType.SMS_SINGLE) {
            messageWaiting.add(new MessageWaiting(bodyArrList.get(0), temp_message_id, strNumbers));
        } else if (messageType == MessageType.SMS_MULTI) {
            //TODO change this to the right method of sending
            for(String tempBody : bodyArrList) {
                messageWaiting.add(new MessageWaiting(tempBody, temp_message_id, strNumbers));
            }
        } else if (messageType == MessageType.MMS_MULTI_RECIPIENT) {
            messageWaiting.add(new MessageWaiting(body, temp_message_id, strNumbers));
        }
        return messageWaiting;
    }
//(context, sentPendingIntent, sentFailedPendingIntent);
    public void sendMessage(Context context) {
        Log.e(Tag.SMSMMSDELIVERY, "TYPE: " + messageType + " " + MessageType.SMS_SINGLE);
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
            Log.e(Tag.SMSMMSDELIVERY, "Body [" + bodyArrList.get(0) + "]");
            smsManager.sendTextMessage(strNumbers, null, bodyArrList.get(0), sentFailedPendingIntent, null);
        } else if (messageType == MessageType.SMS_MULTI) {
            //TODO change this to the right method of sending
//            intent.putExtra(JSONBuilder.JSON_KEY_MESSAGE_DELIVERY.TEMP_MESSAGE_ID.name().toLowerCase(),
//                    temp_message_id);
//            for(String tempBody : bodyArrList) {
//                smsManager.sendTextMessage(strNumbers, null, tempBody, null, null);
//            }
        } else if (messageType == MessageType.MMS_MULTI_RECIPIENT) {
            Log.e(Tag.SMSMMSDELIVERY, "Sending mms.");
            mmsObserver.sendMMS(context, body, numbers, temp_message_id);
        }
    }
}
