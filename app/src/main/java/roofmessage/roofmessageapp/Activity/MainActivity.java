package roofmessage.roofmessageapp.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import roofmessage.roofmessageapp.R;
import roofmessage.roofmessageapp.background.BackgroundManager;
import roofmessage.roofmessageapp.io.BindListener;
import roofmessage.roofmessageapp.io.JSONBuilder;
import roofmessage.roofmessageapp.background.io.SharedPreferenceManager;
import roofmessage.roofmessageapp.utils.Tag;

public class MainActivity extends AppCompatActivity{

    private IntentFilter intent_filter = new IntentFilter("Websocket connection change");

    private ConnectionReciever connectionReciever;
    private BindListener bindListener;

    // UI references.
    private TextView mStatus;
    private TextView mMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStatus = (TextView) findViewById(R.id.status);
        mMessage = (TextView) findViewById(R.id.received_data);
        bindListener = new BindListener(this);
        bindListener.doBindService();
        updateConnectionUI();

        //TODO FIX ALL THE SINGLETON PATTERNS

        ////////////////////////////////////////////////////////////////////////////////
        //TODO REMOVE ALL BELOW AFTER TESTING///////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////
        Button mContact = (Button) findViewById(R.id.post_contact);
        mContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message message = new Message();
                message.what = BackgroundManager.MSG_REQUEST;
                Bundle bundle = new Bundle();
                bundle.putString(BackgroundManager.KEY_ACTION, JSONBuilder.Action.GET_CONTACTS.name());
                message.setData(bundle);
                bindListener.sendMessage(message);
            }
        });

        Button mConvo = (Button) findViewById(R.id.convo);
        mConvo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONBuilder jsonBuilder = new JSONBuilder(JSONBuilder.Action.GET_CONVERSATIONS);
                Message message = new Message();
                message.what = BackgroundManager.MSG_REQUEST;
                Bundle bundle = new Bundle();
                bundle.putString(BackgroundManager.KEY_ACTION, JSONBuilder.Action.GET_CONVERSATIONS.name());
                message.setData(bundle);
                bindListener.sendMessage(message);
            }
        });

        Button mSMSConvo = (Button) findViewById(R.id.sms_convo);
        mSMSConvo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message message = new Message();
                message.what = BackgroundManager.MSG_REQUEST;
                Bundle bundle = new Bundle();
                bundle.putString(BackgroundManager.KEY_ACTION, JSONBuilder.Action.GET_SMS_CONVO.name());
                message.setData(bundle);
                bindListener.sendMessage(message);
            }
        });

        Button mMMSConvo = (Button) findViewById(R.id.mms_convo);
        mMMSConvo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message message = new Message();
                message.what = BackgroundManager.MSG_REQUEST;
                Bundle bundle = new Bundle();
                bundle.putString(BackgroundManager.KEY_ACTION, JSONBuilder.Action.GET_MMS_CONVO.name());
                message.setData(bundle);
                bindListener.sendMessage(message);
            }
        });

        Button getMessages = (Button) findViewById(R.id.messages);
        getMessages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message message = new Message();
                message.what = BackgroundManager.MSG_REQUEST;
                Bundle bundle = new Bundle();
                bundle.putString(BackgroundManager.KEY_ACTION, JSONBuilder.Action.GET_MESSAGES.name());
                message.setData(bundle);
                bindListener.sendMessage(message);
            }
        });

        Button sendMessage = (Button) findViewById(R.id.send_msg);
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(Tag.MAIN_ACTIVITY, "Sending message.");
                Message message = new Message();
                message.what = BackgroundManager.MSG_REQUEST;
                Bundle bundle = new Bundle();
                bundle.putString(BackgroundManager.KEY_ACTION, JSONBuilder.Action.SEND_MESSAGES.name());
                message.setData(bundle);
                bindListener.sendMessage(message);
            }
        });
        ////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////

        connectionReciever = new ConnectionReciever();
        intent_filter.addAction(Tag.ACTION_RECEIVED_MESSAGE);
        intent_filter.addAction(Tag.ACTION_WEBSOC_CHANGE);
        registerReceiver(connectionReciever, intent_filter);
    }

    private void updateConnectionUI() {
        //ask to send update to correct ui on load
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean retval = false;
                while (!retval) {
                    Message message = new Message();
                    message.what = BackgroundManager.MSG_POST_WEBSOCKET_UPDATE;
                    retval = bindListener.sendMessage(message);
                    if (!retval) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Log.e(Tag.MAIN_ACTIVITY, "Waiting to update UI. Error should not occur", e);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        //TODO Might need to separate connectionReceiver to separate instances
        LocalBroadcastManager.getInstance(this).unregisterReceiver(connectionReciever);
        unregisterReceiver(connectionReciever);
        bindListener.doUnbindService();
        Log.i(Tag.MAIN_ACTIVITY, "Killed");
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder permissionsAlert = new AlertDialog.Builder(this);
        permissionsAlert.setMessage(R.string.back_button_dialog)
                .setPositiveButton(R.string.back_button_logout, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SharedPreferenceManager sharedPreferenceManager =
                                SharedPreferenceManager.getInstance(MainActivity.this);
                        Message message = new Message();
                        message.what = BackgroundManager.MSG_LOGOUT;
                        bindListener.sendMessage(message);
                        Log.d(Tag.MAIN_ACTIVITY, "Logging out.");
                        MainActivity.this.finish();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {}
                });
        permissionsAlert.create();
        permissionsAlert.show();
    }

    @Override
    protected void onResume() {
        updateConnectionUI();
        super.onResume();
    }

    private class ConnectionReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Tag.ACTION_WEBSOC_CHANGE)) {
                Log.d(Tag.MAIN_ACTIVITY, "State changed received.");
                final String state = intent.getStringExtra("state");
                MainActivity.this.mStatus.post(new Runnable() {
                    @Override
                    public void run() {
                        mStatus.setText(state);
                    }
                });
            } else if (action.equals(Tag.ACTION_RECEIVED_MESSAGE)) {
                Log.d(Tag.MAIN_ACTIVITY, "Recevied message.");
                final String message = intent.getStringExtra("message");
                MainActivity.this.mStatus.post(new Runnable() {
                    @Override
                    public void run() {
                        mMessage.setText(message);
                    }
                });
            }
        }
    }
}