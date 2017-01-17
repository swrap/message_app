package rooftext.rooftextapp.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Message;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import rooftext.rooftextapp.background.BackgroundManager;
import rooftext.rooftextapp.io.BindListener;
import rooftext.rooftextapp.R;
import rooftext.rooftextapp.background.io.SharedPreferenceManager;
import rooftext.rooftextapp.utils.PermissionsManager;
import rooftext.rooftextapp.utils.Tag;

/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    private BindListener bindListener;

    // UI references.
    private AutoCompleteTextView mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private Button mUsernameSignInButton;
    private CheckBox mCheckBoxView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO add this in to fix threads
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        //TODO might need to change background state

        //check to see if background manager has started else quit
        ActivityManager activityManager = (ActivityManager) this.getSystemService( ACTIVITY_SERVICE );
        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        for (int i = 0; i < procInfos.size(); i++)
        {
            Log.d(Tag.MAIN_ACTIVITY, "Searching for Process Name [" + procInfos.get(i).processName + "] ["
                    + "rooftext.rooftextapp" + this.getString(R.string.background_process) + "]");
            if (procInfos.get(i).processName.equals("rooftext.rooftextapp" + this.getString(R.string.background_process)))
            {
                Log.d(Tag.LOGIN_ACTIVITY, "Matched Background Service");
                Toast.makeText(getApplicationContext(), "Background service is running", Toast.LENGTH_LONG).show();

                //if the background service is currently running assume that we jumped back in from
                //notify, we need to continue to the next activity
                Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                LoginActivity.this.startActivityForResult(mainIntent, 55);
                break;
            }
            if (i == procInfos.size()-1) {
                Log.d(Tag.LOGIN_ACTIVITY, "Login activity starting background manager.");
                startService(new Intent(this,BackgroundManager.class));
            }
        }

        bindListener = new BindListener(this);
        bindListener.doBindService();
        setContentView(R.layout.activity_login);

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        mCheckBoxView = (CheckBox) findViewById(R.id.remember_checkbox);
        mUsernameView = (AutoCompleteTextView) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        initiateCheckBox();

        // Set up the login form.
        mUsernameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (textView.getId() == R.id.username && mCheckBoxView.isChecked()) {
                    bindListener.sendUserPass(
                            mUsernameView.getText().toString(),
                            mPasswordView.getText().toString(),
                            false);
                    return true;
                }
                return false;
            }
        });

        mUsernameView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (mCheckBoxView.isChecked()) {
                    bindListener.sendUserPass(
                            mUsernameView.getText().toString(),
                            mPasswordView.getText().toString(),
                            false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (textView.getId() == R.id.password) {
                    LoginActivity.this.showKeyboard(false, mLoginFormView);
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mPasswordView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (mCheckBoxView.isChecked()) {
                    bindListener.sendUserPass(
                            mUsernameView.getText().toString(),
                            mPasswordView.getText().toString(),
                            false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        mUsernameSignInButton = (Button) findViewById(R.id.sign_in_button);
        mUsernameSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginActivity.this.showKeyboard(false, mLoginFormView);
                attemptLogin();
            }
        });

        PermissionsManager.checkPermissions(this);

        mCheckBoxView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Message message = new Message();
                message.what = BackgroundManager.MSG_SAVE_REMEMBERME;
                message.arg1 = mCheckBoxView.isChecked() ? 1 : 0;
                bindListener.sendMessage(message);
                if(mCheckBoxView.isChecked()) {
                    bindListener.sendUserPass(
                            mUsernameView.getText().toString(),
                            mPasswordView.getText().toString(),
                            false);
                } else {
                    bindListener.sendUserPass(
                            "",
                            "",
                            false);
                }
            }
        });
    }

    private void showKeyboard(boolean show, View view) {
        InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (show) {
            keyboard.showSoftInput(view, 0);
        } else {
            keyboard.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        //not used on main page
    }

    /**
     * Callback for on activity Result
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //HARD CODED VALUE FOR REQUEST CODE
        Log.d(Tag.LOGIN_ACTIVITY, "resultCode [" + resultCode + "] requestCode [" + requestCode + "]");
        if (resultCode == 57) {
            //Finish this activity because invalid version
            finishAndRemoveTask();
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid username address.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            if (focusView != null) {
                focusView.requestFocus();
            }
        } else {
            //check for permissions
            PermissionsManager.checkPermissions(this);
            if (PermissionsManager.hasAllPermissions(this)) {
                showProgress(true);
                mAuthTask = new UserLoginTask(username, password);
                mAuthTask.execute();
            }
        }
    }

    private void initiateCheckBox() {
        //TODO Preferences in LoginActivity might need to fix
        SharedPreferenceManager sharedPreferenceManager = SharedPreferenceManager.getInstance(this);
        boolean checked = sharedPreferenceManager.getRememberMe();
        mCheckBoxView.setChecked(checked);
        if ( checked ) {
            mCheckBoxView.setTextIsSelectable(true);
            String username = sharedPreferenceManager.getUsername();
            mUsernameView.setText(username);
            mUsernameView.setSelection(username.length());
            String password = sharedPreferenceManager.getPassword();
            mPasswordView.setText(password);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_longAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showProgress(false);
    }

    @Override
    public void onDestroy() {
        Log.d(Tag.LOGIN_ACTIVITY,"CLOSING APP");
        bindListener.doUnbindService();
        super.onDestroy();
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername;
        private final String mPassword;
        private int error = BindListener.ERROR_INVALID_USER_PASS;

        UserLoginTask(String username, String password) {
            mUsername = username;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Log.d(Tag.LOGIN_ACTIVITY, "About to send message to login.");
            //set session use pass and then attempt login
            bindListener.sendUserPass(mUsername, mPassword, true);
            bindListener.attemptLogin(UserLoginTask.this);
            try {
                synchronized (this) {
                    this.wait();
                }
            } catch (InterruptedException e) {
                Log.d(Tag.LOGIN_ACTIVITY, "Waiting to login interrupted");
            }
            Log.d(Tag.LOGIN_ACTIVITY, "Jumping out of return");
            return bindListener.getLoginResponse();
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;

            if (success) {
                bindListener.sendUserPass(mUsername, mPassword, true);
                Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                mainIntent.setAction(BackgroundManager.LOGIN_START);
                //HARD coded value for value of 56 for fine return
                LoginActivity.this.startActivityForResult(mainIntent, 56);
                Log.d(Tag.MAIN_ACTIVITY, "Coming back from Login Activity");
            } else {
                showProgress(false);
                Toast toast;
                AlertDialog.Builder correctVersionAlert;
                switch (error) {
                    case BindListener.ERROR_INVALID_VERSION_PATCH:
                        Log.d(Tag.LOGIN_ACTIVITY, "Adding alert for correct version");
                        correctVersionAlert = new AlertDialog.Builder(LoginActivity.this);
                        correctVersionAlert.setMessage(String.format(LoginActivity.this.getString(R.string.invalid_version_patch)
                                .replaceAll("XX","%"), Tag.VERSION))
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @TargetApi(16)
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                    }
                                });
                        correctVersionAlert.create();
                        correctVersionAlert.show();
                        break;
                    case BindListener.ERROR_INVALID_VERSION_MINOR:
                        Log.d(Tag.LOGIN_ACTIVITY, "Adding alert for correct version");
                        correctVersionAlert = new AlertDialog.Builder(LoginActivity.this);
                        correctVersionAlert.setMessage(String.format(LoginActivity.this.getString(R.string.invalid_version_minor)
                                .replaceAll("XX","%"), Tag.VERSION))
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @TargetApi(16)
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                    }
                                });
                        correctVersionAlert.create();
                        correctVersionAlert.show();
                        break;
                    case BindListener.ERROR_INVALID_VERSION_MAJOR:
                        Log.d(Tag.LOGIN_ACTIVITY, "Adding alert for correct version");
                        correctVersionAlert = new AlertDialog.Builder(LoginActivity.this);
                        correctVersionAlert.setMessage(String.format(LoginActivity.this.getString(R.string.invalid_version_major)
                                .replaceAll("XX","%"), Tag.VERSION))
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @TargetApi(16)
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        LoginActivity.this.finishAndRemoveTask();
                                        System.exit(0);
                                    }
                                });
                        correctVersionAlert.create();
                        correctVersionAlert.show();
                        break;
                    case BindListener.ERROR_NO_WIFI_ERROR:
                        toast = Toast.makeText(LoginActivity.this, R.string.wifi_error_message, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER | Gravity.TOP, 0, 250);
                        toast.show();
                        break;
                    case BindListener.ERROR_INVALID_USER_PASS:
                        toast = Toast.makeText(LoginActivity.this, R.string.invalid_user_pass, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER | Gravity.TOP, 0, 250);
                        toast.show();
                        break;
                    case BindListener.ERROR_INVALID_WEBSOCKET_CONNECT:
                        toast = Toast.makeText(LoginActivity.this, R.string.invalid_websocket_connect, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER | Gravity.TOP, 0, 250);
                        toast.show();
                        break;
                }
            }
            mUsernameView.requestFocus();
            showKeyboard(true, mUsernameView);
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }

        public void setError(int error) {
            this.error = error;
        }
    }
}