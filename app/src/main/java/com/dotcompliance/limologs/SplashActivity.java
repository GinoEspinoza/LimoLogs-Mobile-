package com.dotcompliance.limologs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.dotcompliance.limologs.ELD.GPSVOXConnect;
import com.dotcompliance.limologs.data.DriverInfo;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.RestTask;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.service.LocationUpdateService;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;

public class SplashActivity extends LimoBaseActivity {
    private Handler handler = new Handler();
    private final int SPLASH_DISPLAY_LENGTH = 1000;
    private boolean isLoadingFinished = false;
    Class<?> classNextActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_splash);

        classNextActivity = LoginActivity.class;
//        GPSVOXConnect.authenticate();
        /* Check the update */
        RestTask.checkForUpdate(new RestTask.TaskCallbackInterface() {
            @Override
            public void onTaskCompleted(Boolean hasNewVersion, String message) {
                if (hasNewVersion) {
                    //Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    //Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    new AlertDialog.Builder(SplashActivity.this)
                            .setMessage("Please update your app from store.")
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialogInterface) {
                                    SplashActivity.this.finish();
                                }
                            })
                            .setPositiveButton("Visit store", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String url = "https://play.google.com/store/apps/details?id=com.dotcompliance.limologs";
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse(url));
                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    SplashActivity.this.finish();
                                }
                            })
                            .show();
                    return;
                }

                checkStatus();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isLoadingFinished) {
                    /* Create an Intent that will start the Menu-Activity. */
                            Intent mainIntent = new Intent(SplashActivity.this, classNextActivity);
                            SplashActivity.this.startActivity(mainIntent);
                            SplashActivity.this.finish();
                        } else {
                            handler.postDelayed(this, SPLASH_DISPLAY_LENGTH);
                        }
                    }
                }, SPLASH_DISPLAY_LENGTH);
            }
        });
    }

    private void checkStatus() {
        RestTask.downloadDefects();

        Preferences.API_TOKEN = Preferences.getSession("TOKEN", mContext);
        if (Preferences.API_TOKEN.isEmpty()) {
            isLoadingFinished = true;
            return;
        }

        if (Preferences.getSession("USER_TYPE", mContext).equals("MECHANIC")) {
            RequestParams params = new RequestParams();
            params.put("mechanic_id", Preferences.getSession("MECHANIC_ID", mContext));
            params.put("token", Preferences.API_TOKEN);

            MyAsyncHttpClient client = new MyAsyncHttpClient();
            client.get(Preferences.API_BASE_PATH + "/limo/mechanic", params, new JsonHttpResponseHandler() {
                @Override
                public void onStart() {
                    Log.d("http: ", this.getRequestURI().toString());
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        int error = response.getInt("error");
                        if (error == 0) { // login success
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

                            classNextActivity = MechanicHomeActivity.class;
                            isLoadingFinished = true;
                        } else {
                            Log.d("Authentication failed: ", response.getString("message"));
                            isLoadingFinished = true;
                        }
                    } catch (JSONException e) {
                        Log.d("Splash: ", "unexpected response");
                        isLoadingFinished = true;
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject response) {
                    if (throwable != null) {
                        Log.e("Network error", " " + throwable.getMessage());
                        showMessage("Network Error: Connection Timeout!");
                    } else {
                        Log.d("network error: ", response.toString());
                        showMessage("Network Error: Code " + statusCode);
                    }
                    //isLoadingFinished = true;
                }
            });
        } else {
            RequestParams params = new RequestParams();
            params.put("driver_id", Preferences.getSession("DRIVER_ID", mContext));
            params.put("token", Preferences.API_TOKEN);

            MyAsyncHttpClient client = new MyAsyncHttpClient();
            client.get(Preferences.API_BASE_PATH + "/limo/driver", params, new JsonHttpResponseHandler() {
                @Override
                public void onStart() {
                    Log.d("http: ", this.getRequestURI().toString());
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        int error = response.getInt("error");
                        if (error == 0) { // login success
                            JSONObject driverObj = response.getJSONObject("driver");
                            if (Preferences.mDriver == null)
                                Preferences.mDriver = new DriverInfo();
                            Preferences.mDriver.driver_id = driverObj.getInt("driver_id");
                            Preferences.mDriver.company_id = driverObj.getInt("company_id");
                            Preferences.mDriver.firstname = driverObj.getString("firstname");
                            Preferences.mDriver.lastname = driverObj.getString("lastname");
                            Preferences.mDriver.email = driverObj.getString("email");
                            Preferences.mDriver.license = driverObj.getString("license");

                            Preferences.mDriver.pc_status = driverObj.getString("pc_status");
                            Preferences.mDriver.ym_status = driverObj.getString("yd_status");

                            JSONObject companyObj = driverObj.getJSONObject("company");
                            Preferences.mDriver.company.company_name = companyObj.getString("name");
                            Preferences.mDriver.company.email = companyObj.getString("email");
                            Preferences.mDriver.company.carrier_name = companyObj.getString("carrier_name");
                            Preferences.mDriver.company.carrier_address = companyObj.getString("carrier_address");
                            Preferences.mDriver.company.home_terminal = companyObj.getString("home_terminal");

                            JSONObject timezoneObj = companyObj.getJSONObject("timezone");
                            Preferences.mDriver.company.timezone = timezoneObj.getString("timezone");

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

                            classNextActivity = WelcomeActivity.class;

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
                                                    isLoadingFinished = true;
                                                } else {
                                                    showMessage("Download logs failed: " + message);
                                                }
                                            }
                                        },"0");

                                        Intent serviceIntent = new Intent(SplashActivity.this, LocationUpdateService.class);
                                        startService(serviceIntent);
                                    }
                                }
                            });
                        } else {
                            Log.d("Authentication failed: ", response.getString("message"));
                            isLoadingFinished = true;
                        }
                    } catch (JSONException e) {
                        Log.d("Splash: ", "unexpected response");
                        isLoadingFinished = true;
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject response) {
                    if (throwable != null) {
                        Log.e("Network error", " " + throwable.getMessage());
                        showMessage("Network Error: Connection Timeout!");
                    } else {
                        Log.d("network error: ", response.toString());
                        showMessage("Network Error: Code " + statusCode);
                    }
                    //isLoadingFinished = true;
                }
            });
        }
    }
}
