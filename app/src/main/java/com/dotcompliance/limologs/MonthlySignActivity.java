package com.dotcompliance.limologs;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.network.RestTask;
import com.dotcompliance.limologs.service.LocationUpdateService;
import com.dotcompliance.limologs.util.DataManager;
import com.dotcompliance.limologs.util.DateUtils;
import com.dotcompliance.limologs.util.ImageEncoder;
import com.github.gcacace.signaturepad.views.SignaturePad;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class MonthlySignActivity extends LimoBaseActivity {
    private TextView textViewMonth;
    private TextView textViewDuration;
    private SignaturePad signaturePad;
    Date startDate, endDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_sign);
        initialize();
    }

    @Override
    public void onMenuItemRight() {
        if (signaturePad.isEmpty()) {
            showMessage("Please sign your log");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(Preferences.getDriverTimezone());
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("start_date", sdf.format(startDate));
        params.put("end_date", sdf.format(endDate));
        params.put("signature", ImageEncoder.encodeTobase64(signaturePad.getTransparentSignatureBitmap()));
        client.post(Preferences.getUrlWithCredential("/log/bundle_sign"), params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                super.onStart();
                startLoading();
            }

            @Override
            public void onFinish() {
                super.onFinish();
                stopLoading();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int error = response.getInt("error");
                    if (error == 0) { // login success
                        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
                        if (DateUtils.getDifferenceInDays(cal.getTime(), endDate) <= 1) {
                            startLoading();
                            RestTask.downloadLogs(new RestTask.TaskCallbackInterface() {
                                @Override
                                public void onTaskCompleted(Boolean success, String message) {
                                    stopLoading();
                                    finish();
                                    Preferences.mLastLogDate = null;
                                }
                            },"0");
                        }

                        cal.setTime(endDate);
                        cal.add(Calendar.DATE, 1);
                        startDate = cal.getTime();

                        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)); // last day of month
                        endDate = cal.getTime();

                        if (DateUtils.getDifferenceInDays(Calendar.getInstance(Preferences.getDriverTimezone()).getTime(), endDate) < 0) {
                            cal = Calendar.getInstance(Preferences.getDriverTimezone());
                            cal.add(Calendar.DATE, -1);
                            endDate = cal.getTime();
                        }

                        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.US);
                        sdf.setTimeZone(Preferences.getDriverTimezone());
                        textViewMonth.setText(sdf.format(startDate));

                        sdf.applyPattern("MM/dd/yyyy");
                        textViewDuration.setText(sdf.format(startDate) + " - " + sdf.format(endDate));
                        signaturePad.clear();
                    }
                    else {
                        Log.d("request failed: ", response.getString("message"));
                        showMessage("Failed to save log");
                    }
                } catch (JSONException e) {
                    Log.d("json error", "json error");
                    showMessage("Failed to save logs");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                if (throwable != null) {
                    Log.d("Network error", " " +  throwable.getMessage());
                    showMessage(throwable.getMessage());
                }
                else {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    showMessage("Request was failed: " + statusCode);
                }
            }
        }) ;
    }

    private void initialize() {
        setRightMenuItem("Next");

        textViewMonth = (TextView)findViewById(R.id.text_month);
        textViewDuration = (TextView)findViewById(R.id.text_duration);
        signaturePad = (SignaturePad)findViewById(R.id.signature_pad);

        if (Preferences.mLastLogDate == null) {
            finish();
        }

        startDate = DateUtils.addDaysToDate(Preferences.mLastLogDate, 1);

        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
        cal.setTime(startDate);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)); // last day of month
        endDate = cal.getTime();

        if (DateUtils.getDifferenceInDays(Calendar.getInstance(Preferences.getDriverTimezone()).getTime(), endDate) <= 0) {
            endDate = DateUtils.addDaysToDate(Calendar.getInstance(Preferences.getDriverTimezone()).getTime(), -1);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.US);
        sdf.setTimeZone(Preferences.getDriverTimezone());
        textViewMonth.setText(sdf.format(startDate));

        sdf.applyPattern("MM/dd/yyyy");
        textViewDuration.setText(sdf.format(startDate) + " - " + sdf.format(endDate));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.common_extra, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.nav_recap) {
            startActivity(new Intent(mContext, RecapActivity.class));
        }
        else if (id == R.id.nav_editlogs) {
            DataManager.getInstance().setClassname("monthlysign");
            startActivity(new Intent(mContext, LogsListActivity.class));

        } else if (id == R.id.nav_inspection) {
            startActivity(new Intent(mContext, InspectionActivity.class));
            finish();
        }
        else if (id == R.id.action_sign_out) {
            Preferences.clearSession(mContext);
            Preferences.mDriverLogs.clear();
            Preferences.mVehicleList.clear();
            stopService(new Intent(mContext, LocationUpdateService.class));
            startActivity(new Intent(mContext, LoginActivity.class));
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void clearSignature(View v) {
        signaturePad.clear();
    }
}
