package com.dotcompliance.limologs;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ToggleButton;

import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class SendLogActivity extends LimoBaseActivity {
    private ToggleButton toggleEmail;
    private ToggleButton toggleFax;
    private EditText editLogAddr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_sendlog);

    //    log_filename = getIntent().getStringExtra("log_filename");

        initialize();
    }

    @Override
    protected void onMenuItemLeft() {
        finish();
    }

    protected void initialize() {
        setLeftMenuItem("Back");

        editLogAddr = (EditText)findViewById(R.id.edit_log_address);
        toggleEmail = (ToggleButton)findViewById(R.id.toggle_email);
        toggleFax = (ToggleButton)findViewById(R.id.toggle_fax);

        toggleEmail.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleFax.setChecked(!isChecked);
            }
        });
        toggleFax.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleEmail.setChecked(!isChecked);
            }
        });
    }

    public void onSendFMCSAButtonClick(View v) {

        /*if (editLogAddr.getText().toString().isEmpty()) {
            showMessage("Please enter email.");
            editLogAddr.performClick();
            return;
        }*/

        RequestParams params = new RequestParams();
        params.put("driver_id", Preferences.mDriverLogs.get(0).driver_id);

        new MyAsyncHttpClient().post(Preferences.getUrlWithCredential("/log/send_eld"), params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                startLoading();
                Log.d("InspectionActivity", this.getRequestURI().toString());
            }

            @Override
            public void onFinish() {
                stopLoading();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int error = response.getInt("error");
                    if (error == 0) {
                        showMessage("Thank you your request has been submitted");
                    } else {
                        showMessage("Failed to send logs: " + response.getString("message"));
                    }
                } catch (JSONException e) {
                    showMessage("Failed to send logs: JSONException - " + e.getMessage());
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                if (throwable != null) {
                    showMessage(throwable.getMessage());
                } else {
                    showMessage("Request was failed: " + statusCode);
                }
            }
        });
    }

    public void onSendButtonClick(View v) {
        Boolean isByEmail = toggleEmail.isChecked();

        RequestParams params = new RequestParams();
        params.put("address", editLogAddr.getText().toString());
        params.put("is_email", isByEmail ? 1 : 0);
        params.put("first_date", Preferences.mDriverLogs.get(6).log_date);
        params.put("dvir_id", Preferences.mDvirId);

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.get(Preferences.getUrlWithCredential("/log/report2"), params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
                startLoading();
            }

            @Override
            public void onFinish() {
                stopLoading();
            }

//            @Override
//            public void onFailure(int statusCode, Header[] headers, JSONObject response, Throwable throwable) {
//
//                try {
//                    int error = response.getInt("error");
//
//                    if (error == 0) { // login success
//                        Log.d("report : ", " Success" );
//                    }
//                } catch (JSONException e) {
//                    Log.d("report: ", "unexpected response");
//                    //if (callback != null) callback.onTaskCompleted(false, "Unexpected response");
//                }
//
//
//            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                try {
                    //Log.d("network error:yash1505 ", errorResponse.toString(4));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) { //  String responseString
                //Log.d("report", responseString);
                try {
                    int error = response.getInt("error");
                    if (error == 0) { // login success
                        Log.d("reportyash1505 : ", " Success" );
                    }

                } catch (JSONException e) {
                    Log.d("yash1505: ", "unexpected response");
                    //if (callback != null) callback.onTaskCompleted(false, "Unexpected response");
                }


            }
        });

    }
}
