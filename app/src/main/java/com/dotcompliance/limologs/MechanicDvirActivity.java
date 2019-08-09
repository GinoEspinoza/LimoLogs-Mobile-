package com.dotcompliance.limologs;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.dotcompliance.limologs.data.DvirLog;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.util.ImageEncoder;
import com.github.gcacace.signaturepad.views.SignaturePad;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

public class MechanicDvirActivity extends LimoBaseActivity implements LocationListener {

    private EditText editFirstName, editLastName;
    private EditText editCarrier, editCarrierAddress;
    private AppCompatSpinner spnVehicle;
    private EditText editDate;
    private EditText editLocation;
    private EditText editStartOdo, editEndOdo;
    private CheckBox chkConfirm;

    private ImageButton btnGpsTracker;
    private ProgressBar progressTracking;

    private Button btnDefects;
    private SignaturePad signaturePad, signatureMechanicPad;

    private LocationManager locationManager;

    private DvirLog mDvir = new DvirLog();
    private ArrayList<String> mArrVehicle = new ArrayList<>();
    Boolean isDefectAffected = true;

    List<Map<String, String>> mListDefects = new ArrayList<Map<String, String>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_mechanic_dvir);

        initialize();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                mListDefects.clear();
                ArrayList<Integer> listCheckedIDs = data.getIntegerArrayListExtra("checked_ids");
                ArrayList<String> listComments = data.getStringArrayListExtra("comments");
                ArrayList<String> listDefects = data.getStringArrayListExtra("defects");
                for (int i = 0; i < listCheckedIDs.size(); i++) {
                    Map<String, String> defect = new HashMap<String, String>();
                    defect.put("defect_id", "" + listCheckedIDs.get(i));
                    defect.put("defect_name", listDefects.get(i));
                    defect.put("comment", listComments.get(i));
                    mListDefects.add(defect);
                    //    mDvir.defectList.add(new DvirLog.DvirDefect(listDefects.get(i), listComments.get(i)));
                    //    mDvir.addNewDefect(listDefects.get(i), listComments.get(i));
                }

                if (mListDefects.size() > 0) {
                    isDefectAffected = data.getBooleanExtra("defect_affectable", true);
                    chkConfirm.setText("Defect(s) was found");
                }
                else {
                    isDefectAffected = false;
                    chkConfirm.setText("No defect was found");
                }
            }
            else if (resultCode == RESULT_CANCELED) {
                // nothing to do
            }
        }
    }

    protected void initialize() {
        setLeftMenuItem("Back");
        setRightMenuItem("Save");
        setConnectionStatus(Preferences.isConnected);
        // Configure components
        editFirstName = (EditText) findViewById(R.id.edit_firstname);
        editLastName = (EditText) findViewById(R.id.edit_lastname);
        editCarrier = (EditText) findViewById(R.id.edit_carrier);
        editCarrierAddress = (EditText) findViewById(R.id.edit_carrier_addr);
        spnVehicle = (AppCompatSpinner) findViewById(R.id.spin_vehicle);
        editDate = (EditText) findViewById(R.id.edit_date);
        editLocation = (EditText) findViewById(R.id.edit_location);
        editStartOdo = (EditText) findViewById(R.id.edit_odometer_start);
        editEndOdo = (EditText) findViewById(R.id.edit_odometer_end);
        chkConfirm = (CheckBox) findViewById(R.id.check_confirm);

        btnGpsTracker = (ImageButton) findViewById(R.id.button_tracker);
        progressTracking = (ProgressBar) findViewById(R.id.progressGps);

        btnDefects = (Button) findViewById(R.id.button_defects);
        btnDefects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, DefectActivity.class);
                startActivityForResult(intent, 1);
            }
        });

        signaturePad = (SignaturePad) findViewById(R.id.signature_pad);
        signatureMechanicPad = (SignaturePad) findViewById(R.id.signature_mechanic_pad);

        configureSpinner();

        // Initiate form values
        editFirstName.setText(Preferences.mDriver.firstname);
        editLastName.setText(Preferences.mDriver.lastname);
        editCarrier.setText(Preferences.mDriver.company.carrier_name);
        editCarrierAddress.setText(Preferences.mDriver.company.carrier_address);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(Preferences.getDriverTimezone());
        editDate.setText(sdf.format(Calendar.getInstance().getTime()));
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
            startActivity(new Intent(mContext, LoginActivity.class));
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onMenuItemLeft() {
        finish();
    }

    @Override
    protected void onMenuItemRight() {
        saveForm();
    }

    protected void configureSpinner() {
        for (int i = 0; i < Preferences.mVehicleList.size(); i ++) {
            mArrVehicle.add(Preferences.mVehicleList.get(i).vehicle_no);
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_dropdown_item, mArrVehicle);
        adapter.setDropDownViewResource(R.layout.simple_spinner_item);
        spnVehicle.setAdapter(adapter);
        spnVehicle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void clearSignature(View v) {
        signaturePad.clear();
    }

    public void clearMechanicSignature(View v) {
        signatureMechanicPad.clear();
    }

    int tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch(NumberFormatException nfe) {
            // Log exception.
            return 0;
        }
    }

    protected void saveForm() {
        if (editFirstName.getText().toString().isEmpty() || editLastName.getText().toString().isEmpty()) {
            showMessage("Please type your full name");
            return;
        }

        if (!chkConfirm.isChecked())
        {
            showMessage("Please check the box or list defects found");
            return;
        }
        if (signaturePad.isEmpty())
        {
            showMessage("Please Sign Your Log");
            return;
        }

        RequestParams params = new RequestParams();

        final int start_odometer = tryParseInt(editStartOdo.getText().toString());
        final int end_odometer = tryParseInt(editEndOdo.getText().toString());

        if (!editStartOdo.getText().toString().isEmpty()) {
            params.put("start_odometer", start_odometer);
            if (!editEndOdo.getText().toString().isEmpty()) {
                if (start_odometer > end_odometer) {
                    showMessage("End odometer must be greater than start");
                    return;
                }
                params.put("end_odometer", end_odometer);
            }
        }
        else if(!editEndOdo.getText().toString().isEmpty()) {
            showMessage("No start odometer entered.");
            return;
        }

        if (mDvir.dvir_id > 0)
            params.put("dvir_id", mDvir.dvir_id);
        params.put("firstname", editFirstName.getText().toString());
        params.put("lastname", editLastName.getText().toString());
        params.put("carrier_name", editCarrier.getText().toString());
        params.put("carrier_address", editCarrierAddress.getText().toString());
        params.put("vehicle_no", mArrVehicle.get(spnVehicle.getSelectedItemPosition()));
        params.put("location", editLocation.getText().toString());
        params.put("log_date", editDate.getText().toString());

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a zzz");
        final String dvir_time = sdf.format(calendar.getTime());

        params.put("dvir_time", dvir_time);

        params.put("signature", ImageEncoder.encodeTobase64(signaturePad.getTransparentSignatureBitmap()));

        if (!mListDefects.isEmpty()) {
            params.put("defect_affected", isDefectAffected ? 1: 0);
            params.put("defects", mListDefects);
        }

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.post(Preferences.getUrlWithCredential("/mechanic/save_dvir"), params, new JsonHttpResponseHandler() {
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
                        mDvir.dvir_id = response.getInt("dvir_id");
                        mDvir.firstName = editFirstName.getText().toString();
                        mDvir.lastName = editLastName.getText().toString();
                        mDvir.carrierName = editCarrier.getText().toString();
                        mDvir.carrierAddress = editCarrierAddress.getText().toString();
                        mDvir.vehicle = mArrVehicle.get(spnVehicle.getSelectedItemPosition());
                        mDvir.location = editLocation.getText().toString();
                        mDvir.startOdometer = start_odometer;
                        mDvir.endOdometer = end_odometer;
                        mDvir.logDate = editDate.getText().toString();
                        mDvir.logTime = dvir_time;
                        mDvir.driverSign = signaturePad.getTransparentSignatureBitmap();
                        mDvir.isDefected = isDefectAffected;
                        // add defects
                        mDvir.defectList.clear();
                        for (Map<String, String> defect : mListDefects) {
                            mDvir.addNewDefect(defect.get("defect_id"), defect.get("defect_name"), defect.get("comment"));
                        }

                        Preferences.mDvirList.add(mDvir);

                        finish();
                    }
                } catch (JSONException e) {
                    Log.d("save dvir: ", "unexpected response");
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
        });
    }

    public void trackGpsLocation(View v) {
        if (locationManager == null)
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                return;
            }
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        btnGpsTracker.setVisibility(View.GONE);
        progressTracking.setVisibility(View.VISIBLE);
    }

    public void onLocationChanged(Location location) {
        // Called when a new location is found by the network location provider.
        Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 3);
            if (addresses.size() > 0) {
                editLocation.setText(addresses.get(0).getAddressLine(1) + ", " + addresses.get(0).getAdminArea());
                Preferences.mCurrentLocation = editLocation.getText().toString();
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(this);
        }
        catch (IOException e) {
            Log.d("Geocoder: ", e.getLocalizedMessage());
        }

        btnGpsTracker.setVisibility(View.VISIBLE);
        progressTracking.setVisibility(View.GONE);
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {}

    public void onProviderEnabled(String provider) {}

    public void onProviderDisabled(String provider) {}
}
