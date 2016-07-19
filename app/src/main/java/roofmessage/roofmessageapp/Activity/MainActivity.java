package roofmessage.roofmessageapp.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.neovisionaries.ws.client.WebSocketState;

import roofmessage.roofmessageapp.R;
import roofmessage.roofmessageapp.dataquery.QueryManager;
import roofmessage.roofmessageapp.io.JSONBuilder;
import roofmessage.roofmessageapp.io.SessionManager;
import roofmessage.roofmessageapp.io.SharedPreferenceManager;
import roofmessage.roofmessageapp.io.WebSocketManager;
import roofmessage.roofmessageapp.utils.PermissionsManager;
import roofmessage.roofmessageapp.utils.Tag;

public class MainActivity extends AppCompatActivity{

    private boolean HAS_ALL_PERMISSIONS = false;
    private IntentFilter intent_filter = new IntentFilter("Websocket connection change");

    private ConnectionKeeper connectionKeeper;

    // UI references.
    private Button mConnectionButton;
    private TextView mStatus;
    private TextView mMessage;

    QueryManager queryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConnectionButton = (Button) findViewById(R.id.connection_button);
        mStatus = (TextView) findViewById(R.id.status);
        mStatus.setText(WebSocketManager.getInstance(this).getLocalizeState());
        mMessage = (TextView) findViewById(R.id.received_data);

        mConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WebSocketManager webSocketManager = WebSocketManager.getInstance(MainActivity.this);
                if (webSocketManager.getState() != WebSocketState.OPEN) {
                    mConnectionButton.setText(MainActivity.this.getString(R.string.disconnect));
                } else {
                    mConnectionButton.setText(MainActivity.this.getString(R.string.connect));
                }
            }
        });

        Button mContact = (Button) findViewById(R.id.post_contact);
        mContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONBuilder jsonBuilder = new JSONBuilder(JSONBuilder.Action.GET_CONTACTS);
                queryManager.addRequest( jsonBuilder );
            }
        });

        Button mConvo = (Button) findViewById(R.id.convo);
        mConvo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONBuilder jsonBuilder = new JSONBuilder(JSONBuilder.Action.GET_CONVERSATIONS);
                queryManager.addRequest(jsonBuilder);
            }
        });

        Button mSMSConvo = (Button) findViewById(R.id.sms_convo);
        mSMSConvo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONBuilder jsonBuilder = new JSONBuilder(JSONBuilder.Action.GET_SMS_CONVO);
                queryManager.addRequest(jsonBuilder);
            }
        });

        Button mMMSConvo = (Button) findViewById(R.id.mms_convo);
        mMMSConvo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONBuilder jsonBuilder = new JSONBuilder(JSONBuilder.Action.GET_MMS_CONVO);
                queryManager.addRequest(jsonBuilder);
            }
        });

        Button getMessages = (Button) findViewById(R.id.messages);
        getMessages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONBuilder jsonBuilder = new JSONBuilder(JSONBuilder.Action.GET_MESSAGES);
                queryManager.addRequest(jsonBuilder);
            }
        });

        Button websocket = (Button) findViewById(R.id.websocket);
        websocket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                SessionManager websocket = SessionManager.getInstance();
            }
        });

        connectionKeeper = new ConnectionKeeper();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        intent_filter.addAction(Tag.ACTION_RECEIVED_MESSAGE);
        intent_filter.addAction(Tag.ACTION_WEBSOC_CHANGE);
        localBroadcastManager.registerReceiver(connectionKeeper,intent_filter);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(connectionKeeper);
        super.onDestroy();
        Log.i(Tag.MAIN_ACTIVITY, "Killed");
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
                        String username = sharedPreferenceManager.getSessionUsername();
                        String password = sharedPreferenceManager.getSessionPassword();
                        SessionManager sessionManager = SessionManager.getInstance(MainActivity.this);
                        sharedPreferenceManager.saveBackgroundState(false);
                        sessionManager.logout(username, password);
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

    private class ConnectionKeeper extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Tag.ACTION_WEBSOC_CHANGE)) {
                final String state = intent.getStringExtra("state");
                MainActivity.this.mStatus.post(new Runnable() {
                    @Override
                    public void run() {
                        mStatus.setText(state);
                    }
                });
            } else if (action.equals(Tag.ACTION_RECEIVED_MESSAGE)) {
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