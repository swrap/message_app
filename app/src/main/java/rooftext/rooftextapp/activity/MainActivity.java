package rooftext.rooftextapp.activity;

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
import android.widget.TextView;
import android.widget.Toast;

import com.klinker.android.send_message.ApnUtils;

import java.util.ArrayList;
import java.util.List;

import rooftext.rooftextapp.R;
import rooftext.rooftextapp.background.BackgroundManager;
import rooftext.rooftextapp.io.BindListener;
import rooftext.rooftextapp.background.io.SharedPreferenceManager;
import rooftext.rooftextapp.utils.Tag;

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
        Log.d(Tag.MAIN_ACTIVITY, "Starting Main Activity");
        setContentView(R.layout.activity_main);

        connectionReciever = new ConnectionReciever();
        intent_filter.addAction(Tag.ACTION_RECEIVED_MESSAGE);
        intent_filter.addAction(Tag.ACTION_WEBSOC_CHANGE);
        intent_filter.addAction(Tag.ACTION_FAILED_LOGIN);
        registerReceiver(connectionReciever, intent_filter);

        mStatus = (TextView) findViewById(R.id.status);
        bindListener = new BindListener(this);
        bindListener.doBindService();
        updateConnectionUI();

        ActivityManager activityManager = (ActivityManager) this.getSystemService( ACTIVITY_SERVICE );
        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        for (int i = 0; i < procInfos.size(); i++)
        {
            Log.d(Tag.MAIN_ACTIVITY, "Searching for Process Name [" + procInfos.get(i).processName + "] ["
                + "rooftext.rooftextapp" + this.getString(R.string.background_process) + "]");
            if (procInfos.get(i).processName.equals("rooftext.rooftextapp" + this.getString(R.string.background_process)))
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

        findViewById(R.id.logoutBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.closeConnLogout();
            }
        });

        findViewById(R.id.change_apn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ApnUtils.initDefaultApns(MainActivity.this, null, true);
            }
        });

        ArrayList<ApnUtils.APN> apns = ApnUtils.loadApns(this);
        if (apns != null && apns.size() > 1) {
            ApnUtils.initDefaultApns(MainActivity.this, null, true);
        } else {
            findViewById(R.id.change_apn).setVisibility(View.GONE);
        }
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
        closeConnLogout();
    }

    private void closeConnLogout() {
        AlertDialog.Builder permissionsAlert = new AlertDialog.Builder(this);
        permissionsAlert.setMessage(R.string.back_button_dialog)
                .setPositiveButton(R.string.back_button_logout, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SharedPreferenceManager sharedPreferenceManager =
                                SharedPreferenceManager.getInstance(MainActivity.this);
                        Message message = new Message();
                        message.what = BackgroundManager.MSG_LOGOUT_STOP_FOREGROUND;
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

//        Log.d(Tag.LOGIN_ACTIVITY, "request code [" + requestCode + "] [" + Tag.PENDING_INTENT_CLOSE_APP + "]");
//        if (requestCode == Tag.PENDING_INTENT_CLOSE_APP) {
//            Log.d(Tag.LOGIN_ACTIVITY, "request code [" + requestCode + "] YESSSS");
//            //Finish this activity because closing
//            finishAndRemoveTask();
//            //STOP BACKGROUND SERVICE
//            stopService(new Intent(getString(R.string.background_process)));
//            Log.d(Tag.LOGIN_ACTIVITY, "stopped service");
//        }
    }

    @Override
    protected void onPause() {
        Log.d(Tag.MAIN_ACTIVITY, "Pause");
        updateConnectionUI();
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(Tag.MAIN_ACTIVITY, "Resume");
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
            } else if (action.equals(Tag.ACTION_FAILED_LOGIN)) {
                Log.d(Tag.MAIN_ACTIVITY, "Recevied invalid version.");
                int message = intent.getIntExtra(BackgroundManager.KEY_DISPLAY_MESSAGE, -1);
                if (message != -1) {
                    android.support.v7.app.AlertDialog.Builder correctVersionAlert =
                            new android.support.v7.app.AlertDialog.Builder(MainActivity.this);
                    correctVersionAlert.setMessage(R.string.invalid_user_pass)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @TargetApi(16)
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    stopService(new Intent(MainActivity.this, BackgroundManager.class));
                                    MainActivity.this.setResult(55);
                                    MainActivity.this.finishAndRemoveTask();
                                    Log.d(Tag.MAIN_ACTIVITY, "Exiting main");
                                }
                            });
                    correctVersionAlert.create();
                    correctVersionAlert.show();
                }
            }
        }
    }
}