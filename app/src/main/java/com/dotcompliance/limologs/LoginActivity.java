package com.dotcompliance.limologs;

import android.content.Intent;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import com.dotcompliance.limologs.data.DriverInfo;
import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.DutyStatus;
import com.dotcompliance.limologs.network.RestTask;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.service.LocationUpdateService;
import com.dotcompliance.limologs.util.DataManager;
import com.dotcompliance.limologs.util.ImageEncoder;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

import com.dotcompliance.limologs.data.Preferences;
import com.orhanobut.hawk.Hawk;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class LoginActivity extends LimoBaseActivity {
    private EditText editEmail;
    private EditText editPassword;
    private RadioButton radioDriver;
    private int need_to_certify;
    public int mLastStateTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_login);
        initialize();
    }

    protected void initialize() {
        editEmail = (EditText) findViewById(R.id.text_email);
        editPassword = (EditText) findViewById(R.id.text_password);
        radioDriver = (RadioButton) findViewById(R.id.radio_driver);

        editEmail.setText(Hawk.get("email", ""));

        if (!editEmail.getText().toString().isEmpty()) {
            editPassword.requestFocus();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    public void clickSignIn(View v) {
        RequestParams params = new RequestParams();
        params.put("email", editEmail.getText().toString());
        params.put("password", editPassword.getText().toString());

        if (radioDriver.isChecked()) {
            MyAsyncHttpClient client = new MyAsyncHttpClient();
            client.get(Preferences.loginPath(), params, new JsonHttpResponseHandler() {
                @Override
                public void onStart() {
                    Log.d("http: ", this.getRequestURI().toString());
                    //startLoading();
                    DataManager.getInstance().showProgressMessage(LoginActivity.this);
                }

                @Override
                public void onFinish() {
                    //stopLoading();
                    // DataManager.getInstance().hideProgressMessage();
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        Log.e("LoginResponse", "onSuccess: " + response.toString());
                        int error = response.getInt("error");
                        if (error == 0) { // login success
                            String token = response.getString("token");
                            Preferences.API_TOKEN = token;
                            Preferences.saveSession("TOKEN", token, LoginActivity.this);

                            JSONObject driverObj = response.getJSONObject("driver");
                            if (Preferences.mDriver == null)
                                Preferences.mDriver = new DriverInfo();

                            Hawk.put("email", editEmail.getText().toString());

                            Preferences.saveSession("DRIVER_ID", driverObj.getString("driver_id"), LoginActivity.this);
                            Preferences.mDriver.driver_id = driverObj.getInt("driver_id");
                            Preferences.mDriver.company_id = driverObj.getInt("company_id");
                            Preferences.mDriver.firstname = driverObj.getString("firstname");
                            Preferences.mDriver.lastname = driverObj.getString("lastname");
                            Preferences.mDriver.email = driverObj.getString("email");
                            Preferences.mDriver.license = driverObj.getString("license");
                            Preferences.mDriver.isDriver = true;
                            Preferences.mDriver.ym_status = driverObj.getString("yd_status");
                            Preferences.mDriver.pc_status = driverObj.getString("pc_status");
                            JSONObject companyObj = driverObj.getJSONObject("company");
                            Preferences.mDriver.company.company_name = companyObj.getString("name");
                            Preferences.mDriver.company.email = companyObj.getString("email");
                            Preferences.mDriver.company.carrier_name = companyObj.getString("carrier_name");
                            Preferences.mDriver.company.carrier_address = companyObj.getString("carrier_address");
                            Preferences.mDriver.company.home_terminal = companyObj.getString("home_terminal");

                            JSONObject timezoneObj = companyObj.getJSONObject("timezone");
                            Preferences.mDriver.company.timezone = timezoneObj.getString("timezone");
                            need_to_certify = response.optInt("need_to_certify");
                            JSONObject ruleObj = companyObj.getJSONObject("rule");
                            if (ruleObj != null) {
                                Preferences.OFFDUTY_HOUR = ruleObj.getInt("off_duty") * 60;
                                Preferences.DRIVING_HOUR = ruleObj.getInt("driving_hour") * 60;
                                Preferences.ONDUTY_HOUR = ruleObj.getInt("on_duty") * 60;
                                Preferences.isREST_BREAK = ruleObj.getInt("rest_break") == 1;
                                if (ruleObj.getInt("cycle_rule") == 0) { // USA 70 hour / 8 day
                                    Preferences.CYCLE_HOUR = 70 * 60;
                                    Preferences.CYCLE_DAY = 8;
                                } else {
                                    Preferences.CYCLE_HOUR = 60 * 60;
                                    Preferences.CYCLE_DAY = 7;
                                }
                            }

                            if (response.has("last_log_date")) {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                                sdf.setTimeZone(Preferences.getDriverTimezone());
                                try {
                                    Preferences.mLastLogDate = sdf.parse(response.getString("last_log_date"));
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }

                            RestTask.downloadVehicleData(new RestTask.TaskCallbackInterface() {
                                @Override
                                public void onTaskCompleted(Boolean success, String message) {
                                    if (success) {
                                        RestTask.downloadLogs(new RestTask.TaskCallbackInterface() {
                                            @Override
                                            public void onTaskCompleted(Boolean success, String message) {
                                                if (success) {
                                                    int size = Preferences.mDriverLogs.size();
//                                                    new Handler().postDelayed(new Runnable() {
//                                                        @Override
//                                                        public void run() {
//                                                            onSaveDutyClick();
//                                                            // saveLogsheet();
//                                                        }
//                                                    }, 3000);
                                                  /*  if (Preferences.mDriverLogs.get(size - 1).driverlog_id == 0) {
                                                        new Handler().postDelayed(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                onSaveDutyClick();
                                                               // saveLogsheet();
                                                            }
                                                        }, 3000);
                                                    } else {
                                                        new Handler().postDelayed(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                onSaveDutyClick();
                                                                //saveLogsheet();
                                                            }
                                                        }, 3000);
                                                    }*/

                                                    DataManager.getInstance().hideProgressMessage();
                                                    if (need_to_certify == 1) {
                                                        Preferences.isCertify = true;
                                                        Intent intent = new Intent(LoginActivity.this,
                                                                CertifyLogsActivity.class);
                                                        startActivity(intent);
                                                        finish();
                                                    } else {

                                                     /*   new Handler().postDelayed(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                               onSaveDutyClick();
                                                             //   saveLogsheet();
                                                            }
                                                        }, 3000);*/

                                                        DataManager.getInstance().hideProgressMessage();
                                                        startActivity(new Intent(LoginActivity.this, WelcomeActivity.class));
                                                        LoginActivity.this.finish();
                                                    }
                                                } else {
                                                    showMessage("Download logs failed: " + message);
                                                }
                                            }
                                        },"1");

                                        Intent serviceIntent = new Intent(LoginActivity.this, LocationUpdateService.class);
                                        startService(serviceIntent);
                                    }
                                }
                            });

                        } else {
                            Log.d("Login response: ", response.getString("message"));
                            DataManager.getInstance().hideProgressMessage();
                            Snackbar.make(findViewById(android.R.id.content), response.getString("message"), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        }
                    } catch (JSONException e) {
                        Snackbar.make(findViewById(android.R.id.content), "Unexpected response", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    }
                }


                @Override
                public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                    if (throwable != null) {
                        Log.d("Network error", " " + throwable.getMessage());
                        showMessage("Network Error: " + throwable.getMessage());
                    } else {
                        try {
                            Log.d("network error: ", errorResponse.toString(4));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        showMessage("Network Error: " + statusCode);
                    }
                }
            });
        } else {
            MyAsyncHttpClient client = new MyAsyncHttpClient();
            client.get(Preferences.API_BASE_PATH + "/limo/mechanic_login", params, new JsonHttpResponseHandler() {
                @Override
                public void onStart() {
                    Log.d("http: ", this.getRequestURI().toString());
                    startLoading();
                }

                @Override
                public void onFinish() {
                    stopLoading();
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        int error = response.getInt("error");
                        if (error == 0) { // login success
                            String token = response.getString("token");
                            Preferences.API_TOKEN = token;
                            Preferences.saveSession("TOKEN", token, LoginActivity.this);

                            JSONObject jsonObject = response.getJSONObject("mechanic");
                            Preferences.saveSession("USER_TYPE", "MECHANIC", mContext);
                            Preferences.saveSession("MECHANIC_ID", jsonObject.getString("mechanic_id"), mContext);

                            JSONObject mechanicObj = response.getJSONObject("mechanic");
                            if (Preferences.mDriver == null)
                                Preferences.mDriver = new DriverInfo();

                            Preferences.mDriver.driver_id = mechanicObj.getInt("mechanic_id");
                            Preferences.mDriver.company_id = mechanicObj.getInt("company_id");
                            Preferences.mDriver.firstname = mechanicObj.getString("firstname");
                            Preferences.mDriver.lastname = mechanicObj.getString("lastname");
                            Preferences.mDriver.email = mechanicObj.getString("email");
                            Preferences.mDriver.isDriver = false;

                            JSONObject companyObj = mechanicObj.getJSONObject("company");
                            Preferences.mDriver.company.company_name = companyObj.getString("name");
                            Preferences.mDriver.company.email = companyObj.getString("email");
                            Preferences.mDriver.company.carrier_name = companyObj.getString("carrier_name");
                            Preferences.mDriver.company.carrier_address = companyObj.getString("carrier_address");
                            Preferences.mDriver.company.home_terminal = companyObj.getString("home_terminal");

                            JSONObject timezoneObj = companyObj.getJSONObject("timezone");
                            Preferences.mDriver.company.timezone = timezoneObj.getString("timezone");

                            startActivity(new Intent(mContext, MechanicHomeActivity.class));
                            finish();
                        } else {
                            Log.d("Login response: ", response.getString("message"));
                            Snackbar.make(findViewById(android.R.id.content),
                                    response.getString("message"),
                                    Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        }
                    } catch (JSONException e) {
                        Snackbar.make(findViewById(android.R.id.content),
                                "Unexpected response", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                    if (throwable != null) {
                        Log.d("Network error", " " + throwable.getMessage());
                        showMessage("Network Error: " + throwable.getMessage());
                    } else {
                        try {
                            Log.d("network error: ", errorResponse.toString(4));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        showMessage("Network Error: " + statusCode);
                    }
                }
            });
        }
    }


    public void onSaveDutyClick() {
//         DutyStatus firstStatus = mDriverLog.statusList.get(0);
//        if (firstStatus.Id == 0) { // save first state
//            RestTask.saveStatus(mDriverLog.driverlog_id, firstStatus, null);
//        }

        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
        int curr_time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        curr_time -= (curr_time % 15);
        if (curr_time > mLastStateTime) {
            final int last_starttime = curr_time;
            RestTask.saveNewStatus(0, 6, curr_time,
                    "",
                    "",
                    new RestTask.TaskCallbackInterface() {
                        @Override
                        public void onTaskCompleted(Boolean success, String message) {
                            if (success) {


                            } else {
                                showMessage("Sorry, was unable to set new status: " + message);
                                DataManager.getInstance().hideProgressMessage();
                            }
                        }
                    });


        }
    }


    private void saveLogsheet() {
        RequestParams params = new RequestParams();

        int size = Preferences.mDriverLogs.size();
        if (Preferences.mDriver.driver_id > 0)
            params.put("driverlog_id", Preferences.mDriverLogs.get(size - 1).driverlog_id);
        params.put("firstname", Preferences.mDriver.firstname);
        params.put("lastname", Preferences.mDriver.lastname);
        params.put("carrier_name", Preferences.mDriver.company.carrier_name);
        params.put("carrier_address", Preferences.mDriver.company.carrier_address);
        params.put("vehicle", "");
        params.put("co_driver", "N/A");
        params.put("home_terminal", "N/A");
        params.put("trip", "");
        params.put("trailer", "N/A");
        // if (editEndOdo.getText().toString().equals(null) || editEndOdo.getText().toString().equals("")) {
        params.put("total_miles", "");
        // } else if ((editStartOdo.getText().toString().equals(null) || editStartOdo.getText().toString().equals(""))) {
        //     params.put("total_miles", mDriverLog.total_miles);
        //  } else {
//            try {
//                int miles = Integer.parseInt(editEndOdo.getText().toString().trim()) - Integer.parseInt(editStartOdo.getText().toString().trim());
//                mDriverLog.total_miles = "" + miles;
//                params.put("total_miles", mDriverLog.total_miles);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        //    }
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        Log.e("date", "saveLogsheet: " + dateFormat.format(date));
        params.put("log_date", dateFormat.format(date));
        params.put("signature", "");

        params.put("vehicle_id", Preferences.mVehicleList.get(Preferences.mSelectedVehicleIndex).vehicle_id);
        //params.put("ignition", "");

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.post(Preferences.getUrlWithCredential("/log/save_log"), params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                // DataManager.getInstance().showProgressMessage(LoginActivity.this);
                Log.d("SaveLog" + " http: ", this.getRequestURI().toString());
            }

            @Override
            public void onFinish() {
                //    DataManager.getInstance().hideProgressMessage();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Log.d("SaveLog", "save form: " + response.toString(4));
                    int error = response.getInt("error");
       //             onSaveDutyClick();
//                    if (error == 0) {
//                        // update today's log
//                        mDriverLog.driverlog_id = response.getInt("driverlog_id");
//                        mDriverLog.firstname = editFirstName.getText().toString();
//                        mDriverLog.lastname = editLastName.getText().toString();
//                        mDriverLog.carrier_name = Preferences.mDriver.company.carrier_name;
//                        mDriverLog.carrier_address = Preferences.mDriver.company.carrier_address;
//                        mDriverLog.vehicle_nums = editVehicles.getText().toString();
//                        mDriverLog.trip = editTrip.getText().toString();
//                        mDriverLog.signature = signaturePad.getTransparentSignatureBitmap();
//
//                        if (log_index > 0) {
//                            setResult(RESULT_OK);
//                        } else {
//                            Preferences.mVehicleIndex = vehicle_index;
//                            Intent intent = new Intent(mContext, HomeActivity.class);
//                            startActivity(intent);
//                        }
//                        finish();
//                    } else {
//                        showMessage("Failed to save logsheet");
//                    }
                } catch (JSONException e) {
                    showMessage("Failed to save logsheet: Server error");
                    Log.d("SaveLog", "unexpected response");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                if (throwable != null) {
                    Log.d("Network error", " " + throwable.getMessage());
                    showMessage(throwable.getMessage());
                } else {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                    showMessage("Request was failed: " + statusCode);
                }
            }
        });
    }
}
