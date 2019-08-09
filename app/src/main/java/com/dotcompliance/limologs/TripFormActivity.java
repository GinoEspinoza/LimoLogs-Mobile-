package com.dotcompliance.limologs;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.DvirLog;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.service.LocationUpdateService;
import com.dotcompliance.limologs.util.ImageEncoder;
import com.github.gcacace.signaturepad.views.SignaturePad;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class TripFormActivity extends LimoBaseActivity {
    private final String TAG = "Driver log";

    private EditText editFirstName, editLastName;
    private EditText editVehicleNum;
    private EditText editDate;
    private EditText editHomeTerminal;
    private EditText editCodriver;
    private EditText editTrip, editTrailer;
    private EditText editTotalMiles;
    private SignaturePad signaturePad;

    private int driverlog_id = 0;
    private int log_index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_trip);

        Intent intent = getIntent();
        log_index = intent.getIntExtra("log_index", 0);

        initialize();
    }

    protected void initialize() {
        setRightMenuItem("Save");
        setConnectionStatus(Preferences.isConnected);
        if (Preferences.isFullyActive)
            setLeftMenuItem("Cancel");
        setRightMenuItemFont(15);
        setLeftMenuItemFont(15);
       // setConnectionStatus(Preferences.isConnected);
        editFirstName = (EditText) findViewById(R.id.edit_firstname);
        editLastName = (EditText) findViewById(R.id.edit_lastname);
        editVehicleNum = (EditText) findViewById(R.id.edit_vehicle_num);
        editDate = (EditText) findViewById(R.id.edit_date);
        editHomeTerminal = (EditText) findViewById(R.id.edit_home_terminal);
        editCodriver = (EditText) findViewById(R.id.edit_codriver);
        editTrip = (EditText) findViewById(R.id.edit_trip);
        editTrailer = (EditText) findViewById(R.id.edit_trailer);
        editTotalMiles = (EditText) findViewById(R.id.edit_total_miles);
        signaturePad = (SignaturePad)findViewById(R.id.signature_pad);

        final DriverLog log = Preferences.mDriverLogs.get(log_index);
        driverlog_id = log.driverlog_id;

        if (driverlog_id > 0) {
            editFirstName.setText(log.firstname);
            editLastName.setText(log.lastname);
            editVehicleNum.setText(log.vehicle_nums);
            editDate.setText(log.log_date);
            editHomeTerminal.setText(log.home_terminal);
            editCodriver.setText(log.co_driver);
            editTrip.setText(log.trip);
            editTrailer.setText(log.trailer);
            if(!log.total_miles.equals("0"))
                editTotalMiles.setText(log.total_miles);
            if (log.signature != null) {
                signaturePad.post(new Runnable() {
                    @Override
                    public void run() {
                        signaturePad.setSignatureBitmap(log.signature);
                    }
                });
            }
        }
        else {
            editFirstName.setText(Preferences.mDriver.firstname);
            editLastName.setText(Preferences.mDriver.lastname);
            editDate.setText(log.log_date);
            editHomeTerminal.setText(Preferences.mDriver.company.home_terminal);
        }

        String vehicles = "";
        int total_miles = 0;
        for (int i = 0; i < log.dvirList.size(); i ++) {
            DvirLog dvir = log.dvirList.get(i);
            if (dvir.startOdometer > 0 && dvir.endOdometer > 0)
                total_miles += (dvir.endOdometer - dvir.startOdometer);
            vehicles += dvir.vehicle + ",";
        }
        if (total_miles > 0)
            editTotalMiles.setText("" + total_miles);
        if (vehicles.length() > 1)
            editVehicleNum.setText(vehicles.substring(0, vehicles.length()-1));
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

    @Override
    protected void onMenuItemLeft() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onMenuItemRight() {
        saveForm();
    }

    public void clearSignature(View v) {
        signaturePad.clear();
    }

    private void saveForm() {
        if (editTrip.getText().length() == 0) {
            showMessage("Trip Number cannot be empty.");
            return;
        }
        if (signaturePad.isEmpty())
        {
            showMessage("Please Sign Your Log");
            return;
        }

        RequestParams params = new RequestParams();

        if (driverlog_id > 0)
            params.put("driverlog_id", driverlog_id);
        params.put("firstname", editFirstName.getText().toString());
        params.put("lastname", editLastName.getText().toString());
        params.put("carrier_name", Preferences.mDriver.company.carrier_name);
        params.put("carrier_address", Preferences.mDriver.company.carrier_address);
        params.put("vehicle", editVehicleNum.getText().toString());
        params.put("co_driver", editCodriver.getText().toString());
        params.put("home_terminal", editHomeTerminal.getText().toString());
        params.put("trip", editTrip.getText().toString());
        params.put("trailer", editTrailer.getText().toString());
        if(!editTotalMiles.getText().toString().isEmpty())
            params.put("total_miles", editTotalMiles.getText().toString());
        params.put("log_date", editDate.getText().toString());
        params.put("signature", ImageEncoder.encodeTobase64(signaturePad.getTransparentSignatureBitmap()));
        params.put("vehicle_id", Preferences.mVehicleList.get(Preferences.mSelectedVehicleIndex).vehicle_id);

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.post(Preferences.getUrlWithCredential("/log/save_log"), params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                startLoading();
                Log.d(TAG + " http: ", this.getRequestURI().toString());
            }

            @Override
            public void onFinish() {
                stopLoading();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Log.d(TAG, "save form: " + response.toString(4));
                    int error = response.getInt("error");
                    if (error == 0) {
                        // update today's log
                        DriverLog newLog = Preferences.mDriverLogs.get(log_index);
                        newLog.driverlog_id = response.getInt("driverlog_id");
                        newLog.firstname = editFirstName.getText().toString();
                        newLog.lastname = editLastName.getText().toString();
                        newLog.carrier_name = Preferences.mDriver.company.carrier_name;
                        newLog.carrier_address = Preferences.mDriver.company.carrier_address;
                        newLog.vehicle_nums = editVehicleNum.getText().toString();
                        newLog.co_driver = editCodriver.getText().toString();
                        newLog.home_terminal = editHomeTerminal.getText().toString();
                        newLog.trip = editTrip.getText().toString();
                        newLog.trailer = editTrailer.getText().toString();
                        newLog.total_miles = editTotalMiles.getText().toString();
                        newLog.log_date = editDate.getText().toString();
                        newLog.signature = signaturePad.getTransparentSignatureBitmap();

                        if (log_index > 0) {
                            setResult(RESULT_OK);
                        }
                        else {
                            Intent intent = new Intent(mContext, HomeActivity.class);
                            startActivity(intent);
                        }
                        finish();
                    }
                } catch (JSONException e) {
                    showMessage("Failed to save form: Unexpected message from the server");
                    Log.d(TAG, "unexpected response");
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
                        //e.printStackTrace();
                    }
                    showMessage("Request was failed: " + statusCode);
                }
            }
        });
    }
}
