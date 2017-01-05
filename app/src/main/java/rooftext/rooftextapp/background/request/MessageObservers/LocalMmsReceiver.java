package rooftext.rooftextapp.background.request.MessageObservers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import rooftext.rooftextapp.utils.Tag;

/**
 * Created by Jesse Saran on 9/30/2016.
 */

public class LocalMmsReceiver extends BroadcastReceiver {
    private final IntentFilter localMMSReceiverIntentFilter = new IntentFilter();
    private static final String TEMP_MESSAGE_ID = "TEMP_MESSAGE_ID";

    public LocalMmsReceiver(Context context) {
        localMMSReceiverIntentFilter.addAction(Tag.ACTION_LOCAL_MMS_SEND_RECEIVER);
        LocalBroadcastManager.getInstance(context).registerReceiver(this, localMMSReceiverIntentFilter);
        Log.e("RM:MMO","Created Local mms receiver.");
    }

    public static Intent getIntent(String tempMessageID) {
        Intent intent = new Intent(Tag.ACTION_LOCAL_MMS_SEND_RECEIVER);
        intent.putExtra(TEMP_MESSAGE_ID, tempMessageID);
        Log.e("RM:MMO","Created Local mms inent.");
        return new Intent(Tag.ACTION_LOCAL_MMS_SEND_RECEIVER);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("RM:LOCALSENT", "Local message sent [" + intent.getStringExtra(TEMP_MESSAGE_ID) + "] [" +
            intent.getStringExtra("uri") + "]");
    }
}