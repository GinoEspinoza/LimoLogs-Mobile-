package com.dotcompliance.limologs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.network.RestTask;
import com.dotcompliance.limologs.service.LocationUpdateService;
import com.dotcompliance.limologs.util.DataManager;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Timer;

import cz.msebera.android.httpclient.Header;

public abstract class LimoBaseActivity extends AppCompatActivity {

    private static final String TAG = "LimoBaseActivity";
    protected Context mContext;
    public Dialog mDialog;

    protected Snackbar snackbar;

    TextView textTitle;
    TextView menuItemLeft;
    TextView menuItemRight;
    TextView txt_isConnected;

    int is_certify = 0;

    private Timer timer;

    protected FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LimoApplication.addActiveActivity(this);
        super.onCreate(savedInstanceState);
        mContext = this;

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
            LayoutInflater inflater = LayoutInflater.from(this);

            View viewActionBar = inflater.inflate(R.layout.actionbar_layout, null);
            textTitle = (TextView) viewActionBar.findViewById(R.id.actionbar_text_title);
            menuItemLeft = (TextView) viewActionBar.findViewById(R.id.actionbar_menu_left);
            menuItemRight = (TextView) viewActionBar.findViewById(R.id.actionbar_menu_right);
            txt_isConnected = (TextView) viewActionBar.findViewById(R.id.txt_isConnected);


            String label = getPackageManager().getApplicationLabel(getApplicationInfo()).toString();
//            try {
//                label = getResources().getString(getPackageManager().getActivityInfo(getComponentName(), 0).labelRes);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
            if (label.isEmpty())
                textTitle.setText(getString(R.string.app_name));
            else
                textTitle.setText(label);

            menuItemLeft.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onMenuItemLeft();
                }
            });

            menuItemRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onMenuItemRight();
                }
            });

            actionBar.setCustomView(viewActionBar);
            actionBar.setDisplayShowCustomEnabled(true);
        }

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        timer = getTimer();
    }

    @Override
    protected void onDestroy() {
        LimoApplication.removeActiveActivity(this);
        super.onDestroy();
    }

    protected void onMenuItemLeft() {
    }

    protected void onMenuItemRight() {
    }

    protected void setLeftMenuItem(String title) {
        if (menuItemLeft != null) {
            menuItemLeft.setText(title);
            menuItemLeft.setVisibility(View.VISIBLE);
        }
    }


    protected void setLeftMenuItemFont(float size) {
        if (menuItemLeft != null) {
            menuItemLeft.setTextSize(size);
            //  menuItemLeft.setVisibility(View.VISIBLE);
        }
    }


    protected void setRightMenuItemFont(float size) {
        if (menuItemRight != null) {
            menuItemRight.setTextSize(size);
            //     menuItemLeft.setVisibility(View.VISIBLE);
        }
    }

    protected void setRightMenuItem(String title) {
        if (menuItemRight != null) {
            menuItemRight.setText(title);
            menuItemRight.setVisibility(View.VISIBLE);
        }
    }

    public void setTitle(String title) {
        textTitle.setText(title);
    }

    public void setConnectionStatus(boolean status) {
        setConnectionStatus(txt_isConnected, status);

    }

    public void setConnectionStatus(TextView txt_isConnected, boolean status) {
        if (status)
            txt_isConnected.setBackgroundResource(0);
        else
            txt_isConnected.setBackgroundResource(R.drawable.horizontallineontext);

    }

    protected void startLoading() {
        //mContext = getApplicationContext();
        //mContext = LimoBaseActivity.this;
        //mContext = this;
//        if(mContext == null)
//            mContext = getBaseContext();
        if (mDialog == null) {
            View view = View.inflate(mContext, R.layout.loading_progress, null);
            mDialog = new Dialog(mContext, R.style.LoadingDialog);
            mDialog.setContentView(view);
            mDialog.setCancelable(false);
        }
        mDialog.show();
    }

    protected void stopLoading() {
        if (mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    protected void showMessage(String message) {
        showMessage(message, false);
    }

    protected void showMessage(String message, Boolean isDefinite) {
        snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        snackbar.setAction("Dismiss", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
            }
        });

        View snackbarView = snackbar.getView();
        TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setMaxLines(2);

        if (isDefinite)
            snackbar.setDuration(Snackbar.LENGTH_LONG);
        else
            snackbar.setDuration(Snackbar.LENGTH_INDEFINITE);

        snackbar.show();
    }

    protected void setupUI(View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(mContext);
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

    public void hideSoftKeyboard(Context context) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.
                INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }


    public void onSaveDuty() {
//         DutyStatus firstStatus = mDriverLog.statusList.get(0);
//        if (firstStatus.Id == 0) { // save first state
//            RestTask.saveStatus(mDriverLog.driverlog_id, firstStatus, null);
//        }
        DataManager.getInstance().showProgressMessage(this);
        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
        int curr_time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        curr_time -= (curr_time % 15);
        // if (curr_time > mLastStateTime) {
        final int last_starttime = curr_time;
        Log.e(TAG, "onSaveDuty: Current Time " + curr_time);
        RestTask.saveNewStatus(0, 7, curr_time,
                "",
                "",
                new RestTask.TaskCallbackInterface() {
                    @Override
                    public void onTaskCompleted(Boolean success, String message) {
                        if (success) {
                            Log.e(TAG, "onTaskCompleted: " + message);
                            checkCertification();

                        } else {
                            showMessage("Sorry, was unable to set new status: " + message);
                            DataManager.getInstance().hideProgressMessage();
                        }
                    }
                });


    }


    public int checkCertification() {

        DataManager.getInstance().showProgressMessage(this);

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.get(Preferences.getCertificatioLog("/log/check_certification"), new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                if (response != null) {
                    try {
                        DataManager.getInstance().hideProgressMessage();
                        int error = response.optInt("error");
                        if (error == 0) {
                            is_certify = response.optInt("need_to_certify");
                            if (is_certify == 1) {
                                Preferences.isCertify = false;
                                startActivity(new Intent(LimoBaseActivity.this, CertifyLogsActivity.class));
                                //finish();
                            } else {
                                Preferences.clearSession(mContext);
                                Preferences.mDriverLogs.clear();
                                Preferences.mVehicleList.clear();
                                Preferences.isConnected = false;
                                stopTimer();
                                stopService(new Intent(mContext, LocationUpdateService.class));
                                startActivity(new Intent(mContext, LoginActivity.class));
                                //finish();
                            }
                            LimoApplication.closeAllActivities();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable
                    , org.json.JSONObject errorResponse) {
                try {
                    Log.e("Connection Response", "onFailure: " + errorResponse.toString());
                    DataManager.getInstance().hideProgressMessage();
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });

        return is_certify;
    }

    public void stopTimer() {
        if (timer != null) {
            Log.e("181017", " StopTimer...");
            if (timer != null) {
                Log.e("181017", " StopTimer...not null");
                timer.cancel();
                timer = null;
            }
        }
    }

    protected Timer getTimer() {
        return null;
    }

}
