package rooftext.rooftextapp.io;

import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ObbInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import rooftext.rooftextapp.activity.LoginActivity;
import rooftext.rooftextapp.background.BackgroundManager;
import rooftext.rooftextapp.utils.Tag;

/**
 * Created by Jesse Saran on 7/22/2016.
 * Created by Jesse Saran on 7/22/2016.
 */
public class BindListener {
    //****NOTE DO NOT CHANGE THESE*****
    public static final int ERROR_INVALID_VERSION_PATCH = 0,
            ERROR_INVALID_VERSION_MINOR = 1,
            ERROR_INVALID_VERSION_MAJOR = 2,
            ERROR_NO_WIFI_ERROR = 3,
            ERROR_INVALID_USER_PASS = 4,
            ERROR_INVALID_WEBSOCKET_CONNECT = 5;

    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    private Messenger mService = null;
    private boolean mIsBound;
    private Context context;

    private LoginActivity.UserLoginTask userLoginTask;
    private boolean loginResponse = false;

    public BindListener(Context context) {
        this.context = context;
    }

    public void sendUserPass(String username, String password, boolean session) {
        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putStringArray(BackgroundManager.KEY_USERNAME_PASS, new String [] {
                username, password,
        });
        message.setData(bundle);
        if (session) {
            message.what = BackgroundManager.MSG_SAVE_SESSION_USERNAME_PASS;
        } else {
            message.what = BackgroundManager.MSG_SAVE_USERNAME_PASS;
        }
        sendMessage(message);
    }

    public void attemptLogin(LoginActivity.UserLoginTask userLoginTask) {
        this.userLoginTask = userLoginTask;
        Message message = new Message();
        message.what = BackgroundManager.MSG_ATTEMPT_LOGIN;
        sendMessage(message);
    }

    public boolean sendMessage(Message message) {
        message.replyTo = mMessenger;
        if (mService != null) {
            try {
                mService.send(message);
                return true;
            } catch (RemoteException e) {
                Log.d(Tag.BIND_LISTENER, "Could not send message, what [" + message.what + "]", e);
            }
        }
        return false;
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BackgroundManager.MSG_SET_VALUE:
                    Log.d(Tag.BIND_LISTENER, "Received from service: " + msg.arg1);
                    break;
                case BackgroundManager.MSG_ATTEMPT_LOGIN:
                    if (userLoginTask != null) {
                        synchronized (userLoginTask) {
                            Log.d(Tag.BIND_LISTENER, "Login response [" + msg.arg1 +
                                    "] userLoginTask [" + userLoginTask + "] [" +
                                    msg.arg2 + "] [" + (userLoginTask != null) + "]");
                            if (userLoginTask != null) {
                                loginResponse = (msg.arg1 != 0);
                                userLoginTask.setError(msg.arg2);
                                userLoginTask.notifyAll();
                                userLoginTask = null;
                            }
                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    public boolean getLoginResponse() {
        return loginResponse;
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            Log.d(Tag.BIND_LISTENER, "Attached.");

            try {
                Message msg = Message.obtain(null, BackgroundManager.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                msg = Message.obtain(null, BackgroundManager.MSG_SET_VALUE, this.hashCode(), 0);
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }

//            Toast.makeText(context, R.string.remote_service_connected,
//                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            Log.d(Tag.BIND_LISTENER, "Disconnected.");
//            Toast.makeText(BindListener.this.context, R.string.remote_service_disconnected,
//                    Toast.LENGTH_SHORT).show();
        }
    };

    public void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        context.bindService(new Intent(BindListener.this.context, BackgroundManager.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        Log.e(Tag.BIND_LISTENER, "Binding.");
    }

    public void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, BackgroundManager.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            context.unbindService(mConnection);
            mIsBound = false;
            Log.d(Tag.BIND_LISTENER, "Unbinding.");
        }
    }
}
