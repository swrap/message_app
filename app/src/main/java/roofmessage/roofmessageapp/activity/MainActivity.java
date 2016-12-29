package roofmessage.roofmessageapp.activity;

import android.annotation.TargetApi;
import android.app.ActivityManager;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

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

    private EditText mIp;//TODO TEST REMOVE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectionReciever = new ConnectionReciever();
        intent_filter.addAction(Tag.ACTION_RECEIVED_MESSAGE);
        intent_filter.addAction(Tag.ACTION_WEBSOC_CHANGE);
        intent_filter.addAction(Tag.ACTION_LOCAL_INVALID_VERSION);
        registerReceiver(connectionReciever, intent_filter);

        mStatus = (TextView) findViewById(R.id.status);
        mMessage = (TextView) findViewById(R.id.received_data);
        bindListener = new BindListener(this);
        bindListener.doBindService();
        updateConnectionUI();

        ActivityManager activityManager = (ActivityManager) this.getSystemService( ACTIVITY_SERVICE );
        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        for (int i = 0; i < procInfos.size(); i++)
        {
            Log.d(Tag.MAIN_ACTIVITY, "Searching for Process Name [" + procInfos.get(i).processName + "] ["
                + "roofmessage.roofmessageapp" + this.getString(R.string.background_process) + "]");
            if (procInfos.get(i).processName.equals("roofmessage.roofmessageapp" + this.getString(R.string.background_process)))
            {
                Log.d(Tag.MAIN_ACTIVITY, "Matched Background Service");
                Toast.makeText(getApplicationContext(), "Background service is running", Toast.LENGTH_LONG).show();
                break;
            }
            if (i == procInfos.size()-1 ) {
                Log.d(Tag.MAIN_ACTIVITY, "Starting Background manager");
                startService(new Intent(this,BackgroundManager.class));
            }
        }

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

        mIp = (EditText) findViewById(R.id.connectionIp);
        Button resetConnection = (Button) findViewById(R.id.resetConnection);
        resetConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(Tag.MAIN_ACTIVITY, "Resetting connection.");
                Message message = new Message();
                message.what = BackgroundManager.MSG_RESET_CONNECTION;
                Bundle bundle = new Bundle();
                bundle.putString(BackgroundManager.KEY_CONNECTION_IP, mIp.getText().toString());
                message.setData(bundle);
                bindListener.sendMessage(message);
            }
        });
        ////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////
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
                    Log.d(Tag.MAIN_ACTIVITY, "Asking for update ui websocket state update ["
                        + retval + "]");
                    if (!retval) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Log.e(Tag.MAIN_ACTIVITY, "Waiting to update UI. Error should not occur", e);
                        }
                    }
                }
            }
        }).start();
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
    protected void onPause() {
        updateConnectionUI();
        super.onPause();
    }

    @Override
    protected void onResume() {
        updateConnectionUI();
        super.onResume();
    }

    private class ConnectionReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Tag.MAIN_ACTIVITY, "Changed State Received.");
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
            } else if (action.equals(Tag.ACTION_LOCAL_INVALID_VERSION)) {
                    Log.d(Tag.MAIN_ACTIVITY, "Recevied invalid version.");
                    final String message = intent.getStringExtra(BackgroundManager.KEY_DISPLAY_MESSAGE);
                    MainActivity.this.mStatus.post(new Runnable() {
                        @Override
                        public void run() {
                            android.support.v7.app.AlertDialog.Builder correctVersionAlert =
                                    new android.support.v7.app.AlertDialog.Builder(MainActivity.this);
                            correctVersionAlert.setMessage(message)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        @TargetApi(16)
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            //HARD coded value for value of 57 for invalid version
                                            MainActivity.this.setResult(57);
                                            MainActivity.this.finishAndRemoveTask();
                                            Log.d(Tag.MAIN_ACTIVITY, "EXITING APP");
                                            //TODO MAKE THIS FINISH MORE CLEANELY. Right now it goes to login when finished
//                                            System.exit(0);
                                        }
                                    });
                            correctVersionAlert.create();
                            correctVersionAlert.show();
                        }
                    });
                }
        }
    }
}