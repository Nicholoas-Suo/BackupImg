package tech.xiaosuo.com.bmob;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;

import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.LogInListener;
import tech.xiaosuo.com.bmob.bean.UserInfo;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity  {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private static final String TAG = "LoginActivity";
    // UI references.
    private AutoCompleteTextView mUserNameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private ProgressDialog progressDialog;
    private Context mContext;
    private Context mActivity;
    private static final int LOGIN_SUCCESS = 0;
    private static final int LOGIN_FAIL=-1;
    private static final int SERVER_ERROR = 1;
    private static final int IO_EXCEPTION = 2;
    BackUpApplication mApp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.d(TAG," wangsm LoginActivity onCreate");
        mContext = getApplicationContext();
        mActivity = this;
        mApp = (BackUpApplication)getApplication();
        // Set up the login form.
        mUserNameView = (AutoCompleteTextView) findViewById(R.id.user_name);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mUserSignInButton = (Button) findViewById(R.id.user_login_button);
        mUserSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
        Button registerButton = (Button)findViewById(R.id.user_register_button);
        registerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this,RegisterActivity.class);
                startActivity(intent);
                //finish();
            }
        });
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }




    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mUserNameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUserNameView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(username)) {
            mUserNameView.setError(getString(R.string.error_field_required));
            focusView = mUserNameView;
            cancel = true;
        } else if (!isUsernameValid(username)) {
            mUserNameView.setError(getString(R.string.error_invalid_username));
            focusView = mUserNameView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            BmobUser.loginByAccount(username, password, new LogInListener<UserInfo>() {

                @Override
                public void done(UserInfo user, BmobException e) {
                    showProgress(false);
                    if(user!=null){
                        Log.d(TAG,"login success");
                        Intent mainIntent = new Intent(LoginActivity.this,MainActivity.class);
                        startActivity(mainIntent);
                        finish();
                    }else{
                        Log.d(TAG,"login fail " + e.toString());
                        new AlertDialog.Builder(mActivity).setTitle(R.string.dialog_title).setMessage(e.toString()).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).show();
                    }
                }
            });

        }
    }

    private boolean isUsernameValid(String email) {
        //TODO: Replace this with your own logic
        return true;//email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */

    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
         if(progressDialog == null){
             progressDialog = new ProgressDialog(this);
         }
        if(show){
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage(getString(R.string.login_progress_message));
            progressDialog.setCancelable(true);
            progressDialog.setCanceledOnTouchOutside(false);

            progressDialog.show();
        }else{
            progressDialog.cancel();
            progressDialog = null;
        }

       /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

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
        }*/
    }

}

