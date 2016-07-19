package roofmessage.roofmessageapp.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.StrictMode;
import android.support.annotation.NonNull;
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

import com.neovisionaries.ws.client.WebSocketState;

import roofmessage.roofmessageapp.BackgroundManager;
import roofmessage.roofmessageapp.R;
import roofmessage.roofmessageapp.io.NetworkManager;
import roofmessage.roofmessageapp.io.SharedPreferenceManager;
import roofmessage.roofmessageapp.io.WebSocketManager;
import roofmessage.roofmessageapp.utils.PermissionsManager;
import roofmessage.roofmessageapp.io.SessionManager;
import roofmessage.roofmessageapp.utils.Tag;

/**
 * A login screen that offers login via username/password.
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    private BackgroundManager backgroundManager;

    // UI references.
    private AutoCompleteTextView mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private Button mUsernameSignInButton;
    private CheckBox mCheckBoxView;

    private SharedPreferenceManager sharedPreferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO add this in to fix threads
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        backgroundManager = BackgroundManager.getInstance();
        if (backgroundManager.isRunning()) {
            Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
            LoginActivity.this.startActivity(mainIntent);
        }
        setContentView(R.layout.activity_login);
        startBackgroundManager();
        sharedPreferenceManager = SharedPreferenceManager.getInstance(this);

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
                Log.d(Tag.LOGIN_ACTIVITY, "Saved: " + mUsernameView.getText().toString() + " " +
                        mPasswordView.getText().toString() + " " + mCheckBoxView.isChecked());
                if (textView.getId() == R.id.username && mCheckBoxView.isChecked()) {
                    boolean saved = sharedPreferenceManager.saveUserPass(mUsernameView.getText().toString(),
                            mPasswordView.getText().toString());
                    Log.d(Tag.LOGIN_ACTIVITY, "Saved: " + mUsernameView.getText().toString() + " " +
                            mPasswordView.getText().toString() + " " + saved);
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
                    boolean saved = sharedPreferenceManager.saveUserPass(mUsernameView.getText().toString(),
                            mPasswordView.getText().toString());
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
                    boolean saved = sharedPreferenceManager.saveUserPass(mUsernameView.getText().toString(),
                            mPasswordView.getText().toString());
                    Log.d(Tag.LOGIN_ACTIVITY, "SAVED: " + saved);
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
        NetworkManager.getInstance(this);

        mCheckBoxView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                sharedPreferenceManager.saveRememberMe(mCheckBoxView.isChecked());
                if(mCheckBoxView.isChecked()) {
                    sharedPreferenceManager.saveUserPass(mUsernameView.getText().toString(),
                            mPasswordView.getText().toString());
                } else {
                    sharedPreferenceManager.saveUserPass("","");
                }
            }
        });


    }

    private void startBackgroundManager() {
        boolean serviceStarted = backgroundManager.isRunning();
        if (!serviceStarted) {
            Intent intent = new Intent(LoginActivity.this, BackgroundManager.class);
            intent.setAction(BackgroundManager.LOGIN_START);
            LoginActivity.this.startService(intent);
        }
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
        boolean checked = sharedPreferenceManager.getRememberMe();
        mCheckBoxView.setChecked(checked);
        if ( checked ) {
            mCheckBoxView.setTextIsSelectable(true);
            String username = sharedPreferenceManager.getUsername();
            mUsernameView.setText(username);
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
        if(!sharedPreferenceManager.getRememberMe()) {
            mUsernameView.setText("");
            mPasswordView.setText("");
        }
    }

    @Override
    public void onDestroy() {
        Log.d(Tag.LOGIN_ACTIVITY,"CLOSING APP");
        super.onDestroy();
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername;
        private final String mPassword;
        private SessionManager sessionManager;
        private String mErrorMessage = "";

        UserLoginTask(String username, String password) {
            mUsername = username;
            mPassword = password;
            sessionManager = SessionManager.getInstance(LoginActivity.this);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            mErrorMessage = getString(R.string.error_login);
            boolean retval = false;
            WebSocketManager webSocketManager = WebSocketManager.getInstance(LoginActivity.this);

            NetworkManager networkManager = NetworkManager.getInstance();
            if (networkManager != null && networkManager.isConnected()) {
                //we have internet
                retval = sessionManager.login(mUsername, mPassword);
                if (retval) {
                    //we can login to django
                    WebSocketManager webSocketController = WebSocketManager.getInstance(LoginActivity.this);
                    retval = webSocketController.createConnection();
                    if (!retval) {
                        //we can login but not create a connection
                        Log.e(Tag.LOGIN_ACTIVITY, "Can login, but cannot connect socket.");
                        sessionManager.logout(mUsername, mPassword);
                    }
                }
            } else {
                mErrorMessage = "Not connected to internet";
            }

            return retval;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;

            if (success) {

                if (!sharedPreferenceManager.getBackgroundState()) {
                    //save session username and pass if background wasnt not running before
                    sharedPreferenceManager.saveSessionUsernamePass(mUsername, mPassword);
                    sharedPreferenceManager.saveBackgroundState(true);
                }
                Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                mainIntent.setAction(BackgroundManager.LOGIN_START);
                LoginActivity.this.startActivity(mainIntent);
                showProgress(false);
            } else {
                showProgress(false);
                Toast toast = Toast.makeText(LoginActivity.this, mErrorMessage, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER | Gravity.TOP, 0, 250);
                toast.show();
            }
            mUsernameView.requestFocus();
            showKeyboard(true, mUsernameView);
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}