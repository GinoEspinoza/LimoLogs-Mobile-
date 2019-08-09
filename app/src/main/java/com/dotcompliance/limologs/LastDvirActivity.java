package com.dotcompliance.limologs;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.dotcompliance.limologs.data.DvirLog;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.network.RestTask;
import com.dotcompliance.limologs.service.LocationUpdateService;
import com.dotcompliance.limologs.util.DataManager;
import com.dotcompliance.limologs.view.DvirPreview;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import cz.msebera.android.httpclient.Header;

public class LastDvirActivity extends LimoBaseActivity {

    ScrollView containerView;
    CheckBox checkBox;

    private int log_index = 0;
    private int vehicle_index;
    private int is_certify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_last_dvir);
        log_index = getIntent().getIntExtra("log_index", 0);
        vehicle_index = getIntent().getIntExtra("vehicle_index", 0);
        initialize();
    }

    protected void initialize() {
        setConnectionStatus(Preferences.isConnected);
        setTitle("Last DVIR");
        setLeftMenuItem("Back");
        setRightMenuItem("Next");

        containerView = (ScrollView) findViewById(R.id.scrollview_container);
        checkBox = (CheckBox) findViewById(R.id.check_box);

        loadLastDvir();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.common, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_sign_out) {
//            Preferences.clearSession(mContext);
//            Preferences.mDriverLogs.clear();
//            Preferences.mVehicleList.clear();
//            stopService(new Intent(mContext, LocationUpdateService.class));
//            startActivity(new Intent(mContext, LoginActivity.class));
//            finish();
            onSaveDuty();
            //checkCertification();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onMenuItemLeft() {
        Intent intent = new Intent(mContext, VehicleListActivity.class);
        intent.putExtra("log_index", log_index);
        startActivity(intent);
//        finish();
    }

    @Override
    protected void onMenuItemRight() {
        if (checkBox.isChecked()) {
            Intent intent = new Intent(mContext, CombinedSheetActivity.class);
            intent.putExtra("log_index", log_index);
            intent.putExtra("vehicle_index", vehicle_index);
            startActivity(intent);
            finish();
        }
        else {
            showMessage("Please check the box");
        }
    }

    public void loadLastDvir() {
        RequestParams params = new RequestParams();
        params.put("vehicle_no", Preferences.mVehicleList.get(vehicle_index).vehicle_no);
        params.put("limited_date", Preferences.mDriverLogs.get(log_index).log_date);

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.post(Preferences.getUrlWithCredential("/log/last_dvir"), params, new JsonHttpResponseHandler() {
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
                    Log.d("save dvir", response.toString(4));
                    int error = response.getInt("error");
                    if (error == 0) {
                        DvirLog dvir = new DvirLog(response.getJSONObject("dvir"));
                        DvirPreview dvirView = new DvirPreview(mContext);
                        dvirView.setData(dvir);
                        dvirView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        containerView.addView(dvirView);
                    }
                    else {
                        onMenuItemRight();
                    }
                }
                catch (JSONException e) {
                    Log.d("last dvir: ", "unexpected response");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse)
            {
                if (throwable != null) {
                    Log.d("Network error", " " +  throwable.getMessage());
                    showMessage("Failed to get last duty. " + throwable.getMessage());
                }
                else {
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


    public void onSaveDuty() {
//         DutyStatus firstStatus = mDriverLog.statusList.get(0);
//        if (firstStatus.Id == 0) { // save first state
//            RestTask.saveStatus(mDriverLog.driverlog_id, firstStatus, null);
//        }
        DataManager.getInstance().showProgressMessage(LastDvirActivity.this);
        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
        int curr_time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        curr_time -= (curr_time % 15);
        // if (curr_time > mLastStateTime) {
        final int last_starttime = curr_time;
        RestTask.saveNewStatus(0, 7, curr_time,
                "",
                "",
                new RestTask.TaskCallbackInterface() {
                    @Override
                    public void onTaskCompleted(Boolean success, String message) {
                        if (success) {
                            checkCertification();

                        } else {
                            showMessage("Sorry, was unable to set new status: " + message);
                            DataManager.getInstance().hideProgressMessage();
                        }
                    }
                });


    }

    public int checkCertification() {

       DataManager.getInstance().showProgressMessage(LastDvirActivity.this);

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
                                startActivity(new Intent(LastDvirActivity.this, CertifyLogsActivity.class));
                                LastDvirActivity.this.finish();
                            } else {
                                Preferences.clearSession(mContext);
                                Preferences.mDriverLogs.clear();
                                Preferences.mVehicleList.clear();
                                Preferences.isConnected = false;
                                stopTimer();
                                stopService(new Intent(mContext, LocationUpdateService.class));
                                startActivity(new Intent(mContext, LoginActivity.class));
                                finish();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable
                    , org.json.JSONObject errorResponse) {
                Log.e("Connection Response", "onFailure: " + errorResponse.toString());
                DataManager.getInstance().hideProgressMessage();


            }
        });

        return is_certify;
    }
}
