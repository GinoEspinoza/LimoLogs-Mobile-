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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.DvirLog;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.network.RestTask;
import com.dotcompliance.limologs.service.LocationUpdateService;
import com.dotcompliance.limologs.util.DataManager;
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

public class CombinedSheetActivity extends LimoBaseActivity implements LocationListener {
    private final String TAG = "CombinedSheet";
    private boolean odometerboolean = true;
    private EditText editFirstName, editLastName;
    private EditText editVehicleNo;
    private EditText editLocation;
    private EditText editStartOdo, editEndOdo;

    private CheckBox chkConfirm;

    private EditText editVehicles;
    private EditText editTrip;

    private Button btnDefects;
    private Button btnInspection;
    private Button btnChecklist;
    private ImageButton btnGpsTracker;
    private ProgressBar progressTracking;

    private SignaturePad signaturePad;
    private static String odometerValue;
    private LocationManager locationManager;

    private DvirLog mDvir = null;
    private DriverLog mDriverLog;

    private int log_index = 0;
    private int vehicle_index = -1;
    private int dvir_index = -1;
    private Boolean isPostTrip;
    Boolean isDefectAffected = true;
    List<Map<String, String>> mListDefects = new ArrayList<Map<String, String>>();

    int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private int is_certify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_combined_sheet);

        Intent intent = getIntent();
        log_index = intent.getIntExtra("log_index", 0);
        isPostTrip = intent.getBooleanExtra("post_trip", false);
        vehicle_index = getIntent().getIntExtra("vehicle_index", Preferences.mVehicleIndex);

        mDriverLog = Preferences.mDriverLogs.get(log_index);
        setConnectionStatus(Preferences.isConnected);
        setTitle("DVIR/LOG");
        setLeftMenuItem("Back");
        setRightMenuItem("Next");
        setConnectionStatus(Preferences.isConnected);
        initialize();
    }

    protected void initControls() {
        editFirstName = (EditText) findViewById(R.id.edit_firstname);
        editLastName = (EditText) findViewById(R.id.edit_lastname);

        editVehicleNo = (EditText) findViewById(R.id.edit_vehicle_num);
        editLocation = (EditText) findViewById(R.id.edit_location);
        editStartOdo = (EditText) findViewById(R.id.edit_odometer_start);
        editEndOdo = (EditText) findViewById(R.id.edit_odometer_end);
        chkConfirm = (CheckBox) findViewById(R.id.check_confirm);

        editVehicles = (EditText) findViewById(R.id.edit_vehicles);
        editTrip = (EditText) findViewById(R.id.edit_trip);

        signaturePad = (SignaturePad) findViewById(R.id.signature_pad);

        btnGpsTracker = (ImageButton) findViewById(R.id.button_tracker);
        progressTracking = (ProgressBar) findViewById(R.id.progressGps);

        btnDefects = (Button) findViewById(R.id.button_defects);
        btnInspection = (Button) findViewById(R.id.button_inspection);
        btnChecklist = (Button) findViewById(R.id.button_checklist);

        btnDefects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, DefectActivity.class);
                intent.putExtra("log_index", log_index);
                intent.putExtra("dvir_index", dvir_index);
                startActivityForResult(intent, 1);
            }
        });


    }

    protected void initialize() {
        initControls();

        btnGpsTracker.performClick();

        if (vehicle_index > -1) {
            for (int i = 0; i < mDriverLog.dvirList.size(); i++) {
                if (Preferences.mVehicleList.get(vehicle_index).vehicle_no.equals(mDriverLog.dvirList.get(i).vehicle)) {
                    dvir_index = i;
                    break;
                }
            }
            if (dvir_index > -1) {

                mDvir = mDriverLog.dvirList.get(dvir_index);
                //mDriverLog.lastDvir = Preferences.mDriverLogs.get(log_index).dvirList.get(dvir_index);

                editVehicleNo.setText(mDvir.vehicle);
                editLocation.setText(mDvir.location);

                if (mDvir.startOdometer > 0)
                    editStartOdo.setText("" + mDvir.startOdometer);
                if (mDvir.endOdometer > 0)
                    editEndOdo.setText("" + mDvir.endOdometer);

                if (mDvir.defectList.size() > 0) {
                    chkConfirm.setText("Defect(s) was found");
                }

                for (int i = 0; i < mDvir.defectList.size(); i++) {
                    Map<String, String> defect = new HashMap<String, String>();
                    defect.put("defect_id", mDvir.defectList.get(i).defect_id);
                    defect.put("defect_name", mDvir.defectList.get(i).defectName);
                    defect.put("comment", mDvir.defectList.get(i).comment);
                    mListDefects.add(defect);
                }
            } else {

                // editVehicleNo.setText(Preferences.getCurrentVehicle().vehicle_no); // Commenting this code to remove issue with vehicle no at DVIR/LOG screen MANGOIT ANDROID TEAM 28-09-2017
                editVehicleNo.setText(Preferences.mVehicleList.get(Preferences.mSelectedVehicleIndex).vehicle_no);
                editLocation.setText(Preferences.mCurrentLocation);

                mDvir = new DvirLog();

                mDvir.vehicle = Preferences.mVehicleList.get(vehicle_index).vehicle_no;
                mDvir.carrierName = Preferences.mDriver.company.carrier_name;
                mDvir.carrierAddress = Preferences.mDriver.company.carrier_address;
                mDvir.logDate = mDriverLog.log_date;

                String deviceId = Preferences.mVehicleList.get(vehicle_index).obdDeviceID;
                if (deviceId != null) {
                    getOdometer(deviceId);
                }
            }
        /*
            if (mDriverLog.lastDvir != null && mDriverLog.lastDvir.vehicle.equals(Preferences.mVehicleList.get(vehicle_index).vehicle_no)) {
                mDvir = mDriverLog.lastDvir;
                editVehicleNo.setText(mDvir.vehicle);
                editLocation.setText(mDvir.location);
                if (mDvir.startOdometer > 0)
                    editStartOdo.setText("" + mDvir.startOdometer);
                if (mDvir.endOdometer > 0)
                    editEndOdo.setText("" + mDvir.endOdometer);

                if (mDvir.defectList.size() > 0) {
                    chkConfirm.setText("Defect(s) was found");
                }

                if (log_index == 0) {
                    Preferences.mOutOfService = mDvir.isDefected;
                    if (mDvir.mechanicSign != null)
                        Preferences.mOutOfService = false;
                }

                for (int i = 0; i < mDvir.defectList.size(); i ++) {
                    Map<String, String> defect = new HashMap<String, String>();
                    defect.put("defect_id", mDvir.defectList.get(i).defect_id);
                    defect.put("defect_name", mDvir.defectList.get(i).defectName);
                    defect.put("comment", mDvir.defectList.get(i).comment);
                    mListDefects.add(defect);
                }
            }
            else {
                // Initiate form values
                editVehicleNo.setText(Preferences.getCurrentVehicle().vehicle_no);
                editLocation.setText(Preferences.mCurrentLocation);

                mDvir = new DvirLog();

                mDvir.vehicle = Preferences.mVehicleList.get(vehicle_index).vehicle_no;
                mDvir.carrierName = Preferences.mDriver.company.carrier_name;
                mDvir.carrierAddress = Preferences.mDriver.company.carrier_address;
                mDvir.logDate = mDriverLog.log_date;

                //Preferences.mOutOfService = false;

            }
        */
        } else { // no vehicle selected, only enable logsheet fields
            editVehicleNo.setEnabled(false);
            editLocation.setEnabled(false);
            editStartOdo.setEnabled(false);
            editEndOdo.setEnabled(false);
            btnGpsTracker.setEnabled(false);
            btnDefects.setEnabled(false);
            btnInspection.setEnabled(false);
            signaturePad.setEnabled(false);
            btnChecklist.setEnabled(false);

            if (log_index == 0) {
                Preferences.mVehicleIndex = -1;
            }
        }

        if (mDriverLog.driverlog_id > 0) {
            editFirstName.setText(mDriverLog.firstname);
            editLastName.setText(mDriverLog.lastname);
            editVehicles.setText(mDriverLog.vehicle_nums);
            //      editTrip.setText(mDriverLog.trip);
            if (mDriverLog.signature != null) {
                signaturePad.post(new Runnable() {
                    @Override
                    public void run() {
                        signaturePad.setSignatureBitmap(mDriverLog.signature);
                    }
                });
            }
        } else {
            editFirstName.setText(Preferences.mDriver.firstname);
            editLastName.setText(Preferences.mDriver.lastname);
        }

        String vehicles = "";
        int total_miles = 0;
        for (int i = 0; i < mDriverLog.dvirList.size(); i++) {
            DvirLog dvir = mDriverLog.dvirList.get(i);
            if (dvir.startOdometer > 0 && dvir.endOdometer > 0)
                total_miles += (dvir.endOdometer - dvir.startOdometer);
            vehicles += dvir.vehicle + ",";
        }
        if (mDvir != null) {
            vehicles += mDvir.vehicle + ",";
        }
        if (total_miles > 0)
            mDriverLog.total_miles = "" + total_miles;
        else
            mDriverLog.total_miles = "";

        if (vehicles.length() > 1)
            editVehicles.setText(vehicles.substring(0, vehicles.length() - 1));
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
            //onSaveDuty();
            checkCertification();
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
        if (mDvir == null) {
            saveLogsheet();
        } else {
            saveForm();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                } else {
                    isDefectAffected = false;
                    chkConfirm.setText("No defect was found");
                }
                if (log_index == 0)
                    Preferences.mOutOfService = isDefectAffected;
            } else if (resultCode == RESULT_CANCELED) {
                // nothing to do
            }
        }
    }

    public void clearSignature(View v) {
        signaturePad.clear();
    }

    public void inspectVehicleBody(View v) {
        Intent intent = new Intent(mContext, BodyInspectionActivity.class);
        intent.putExtra("vehicle_index", vehicle_index);
        startActivity(intent);
    }

    public void viewChecklist(View v) {
        Intent intent = new Intent(mContext, DriversChecklistActivity.class);
        startActivity(intent);
    }

    int tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            // Log exception.
            return 0;
        }
    }

    private void saveForm() {
        if (!chkConfirm.isChecked()) {
            showMessage("Please check the box or list defects found.");
            return;
        }

        if (vehicle_index > -1) {
            if (editStartOdo.getText().toString().isEmpty()) {
                showMessage("Start Odometer field is required.");
                return;
            }
            if (isPostTrip && editEndOdo.getText().toString().isEmpty()) {
                showMessage("Please input end odometer.");
                return;
            }
        }

        if (editLocation.getText().length() == 0) {
            showMessage("Location cannot be empty.");
            return;
        }
        if (editTrip.getText().length() == 0) {
            showMessage("Trip Number cannot be empty.");
            return;
        }
        if (signaturePad.isEmpty()) {
            showMessage("Your signature is required.");
            return;
        }

        if (vehicle_index > -1) {
            // save dvir
            RequestParams params = new RequestParams();
            final int start_odometer = tryParseInt(editStartOdo.getText().toString());
            final int end_odometer = tryParseInt(editEndOdo.getText().toString());

            params.put("start_odometer", start_odometer);
            if (!editEndOdo.getText().toString().isEmpty()) {
                if (start_odometer > end_odometer) {
                    showMessage("End odometer must be greater than start odometer.");
                    return;
                }
                params.put("end_odometer", end_odometer);
            }

            if (mDvir.dvir_id > 0)
                params.put("dvir_id", mDvir.dvir_id);
            params.put("company_id", Preferences.mDriver.company_id);
            params.put("firstname", editFirstName.getText().toString());
            params.put("lastname", editLastName.getText().toString());
            params.put("carrier_name", mDvir.carrierName);
            params.put("carrier_address", mDvir.carrierAddress);
            params.put("vehicle_no", editVehicleNo.getText().toString());
            params.put("location", editLocation.getText().toString());
            params.put("log_date", mDvir.logDate);

            Calendar calendar = Calendar.getInstance(Preferences.getDriverTimezone());
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a zzz", Locale.US);
            sdf.setTimeZone(Preferences.getDriverTimezone());
            final String dvir_time = sdf.format(calendar.getTime());

            params.put("dvir_time", dvir_time);

            if (!signaturePad.isEmpty()) {
                params.put("signature", ImageEncoder.encodeTobase64(signaturePad.getTransparentSignatureBitmap()));
            }

            if (!mListDefects.isEmpty()) {
                params.put("defect_affected", isDefectAffected ? 1 : 0);
                params.put("defects", mListDefects);
            }

            MyAsyncHttpClient client = new MyAsyncHttpClient();
            client.post(Preferences.getUrlWithCredential("/log/save_dvir"), params, new JsonHttpResponseHandler() {
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
                            mDvir.vehicle = editVehicleNo.getText().toString();
                            mDvir.location = editLocation.getText().toString();
                            mDvir.startOdometer = start_odometer;
                            mDvir.endOdometer = end_odometer;
                            mDvir.logTime = dvir_time;
                            mDvir.driverSign = signaturePad.getTransparentSignatureBitmap();
                            mDvir.isDefected = isDefectAffected;
                            // add defects
                            mDvir.defectList.clear();
                            for (Map<String, String> defect : mListDefects) {
                                mDvir.addNewDefect(defect.get("defect_id"), defect.get("defect_name"), defect.get("comment"));
                            }

                            mDriverLog.lastDvir = mDvir;

                            if (dvir_index < 0) {
                                mDriverLog.dvirList.add(mDvir);
                                dvir_index = mDriverLog.dvirList.size() - 1;
                            }
                            //DriverLog mDriverLog = Preferences
                            if (log_index == 0)
                                Preferences.mDvirId = mDvir.dvir_id;
                            Preferences.mVehicles = response.getString("vehicle_nums");
                            Preferences.mTotalMiles = response.getString("total_miles");

                            saveLogsheet();
                        }
                    } catch (JSONException e) {
                        Log.d("save dvir: ", "unexpected response");
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    showMessage("Request was failed: " + statusCode);
                }
            });
        } else {
            saveLogsheet();
        }
    }

    private void saveLogsheet() {
        RequestParams params = new RequestParams();

        if (mDriverLog.driverlog_id > 0)
            params.put("driverlog_id", mDriverLog.driverlog_id);
        params.put("firstname", editFirstName.getText().toString());
        params.put("lastname", editLastName.getText().toString());
        params.put("carrier_name", Preferences.mDriver.company.carrier_name);
        params.put("carrier_address", Preferences.mDriver.company.carrier_address);
        params.put("vehicle", editVehicles.getText().toString());
        params.put("co_driver", "N/A");
        params.put("home_terminal", "N/A");
        params.put("trip", editTrip.getText().toString());
        params.put("trailer", "N/A");
        if (editEndOdo.getText().toString().equals(null) || editEndOdo.getText().toString().equals("")) {
            params.put("total_miles", mDriverLog.total_miles);
        } else if ((editStartOdo.getText().toString().equals(null) || editStartOdo.getText().toString().equals(""))) {
            params.put("total_miles", mDriverLog.total_miles);
        } else {
            try {
                int miles = Integer.parseInt(editEndOdo.getText().toString().trim()) - Integer.parseInt(editStartOdo.getText().toString().trim());
                mDriverLog.total_miles = "" + miles;
                params.put("total_miles", mDriverLog.total_miles);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        params.put("log_date", mDriverLog.log_date);
        params.put("signature", ImageEncoder.encodeTobase64(signaturePad.getTransparentSignatureBitmap()));

        params.put("vehicle_id", Preferences.mVehicleList.get(Preferences.mSelectedVehicleIndex).vehicle_id);
        //params.put("ignition", "");

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
                        mDriverLog.driverlog_id = response.getInt("driverlog_id");
                        mDriverLog.firstname = editFirstName.getText().toString();
                        mDriverLog.lastname = editLastName.getText().toString();
                        mDriverLog.carrier_name = Preferences.mDriver.company.carrier_name;
                        mDriverLog.carrier_address = Preferences.mDriver.company.carrier_address;
                        mDriverLog.vehicle_nums = editVehicles.getText().toString();
                        mDriverLog.trip = editTrip.getText().toString();
                        mDriverLog.signature = signaturePad.getTransparentSignatureBitmap();

                        if (log_index > 0) {
                            setResult(RESULT_OK);
                        } else {
                            Preferences.mVehicleIndex = vehicle_index;
                            Intent intent = new Intent(mContext, HomeActivity.class);
                            startActivity(intent);
                        }
                        finish();
                    } else {
                        showMessage("Failed to save logsheet");
                    }
                } catch (JSONException e) {
                    showMessage("Failed to save logsheet: Server error");
                    Log.d(TAG, "unexpected response");
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

    public void trackGpsLocation(View v) {
        if (locationManager == null)
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                return;
            }
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        btnGpsTracker.setVisibility(View.GONE);
        progressTracking.setVisibility(View.VISIBLE);
    }


    @Override
    public void onLocationChanged(Location location) {
        // Called when a new location is found by the network location provider.
        Log.e(TAG, "onLocationChanged: " + location.getLongitude() + "Latitude :----" + location.getLatitude());
        Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses.size() > 0) {
                Address addr = addresses.get(0);
                String strAddress = addr.getLocality() + ", " + addr.getAdminArea();
                editLocation.setText(strAddress);
                Preferences.mCurrentLocation = strAddress;
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(this);
        } catch (IOException e) {
            Log.d("Geocoder: ", e.getLocalizedMessage());
        }

        btnGpsTracker.setVisibility(View.VISIBLE);
        progressTracking.setVisibility(View.GONE);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void onSaveDuty() {
//         DutyStatus firstStatus = mDriverLog.statusList.get(0);
//        if (firstStatus.Id == 0) { // save first state
//            RestTask.saveStatus(mDriverLog.driverlog_id, firstStatus, null);
//        }
        DataManager.getInstance().showProgressMessage(CombinedSheetActivity.this);
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
                        }
                    }
                });


    }


    public int checkCertification() {
        DataManager.getInstance().showProgressMessage(CombinedSheetActivity.this);
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
                                startActivity(new Intent(CombinedSheetActivity.this, CertifyLogsActivity.class));
                                CombinedSheetActivity.this.finish();
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


    public int getOdometer(String deviceID) {
        /*DataManager.getInstance().showProgressMessage(CombinedSheetActivity.this);
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.get(Preferences.getOdoMeter("/log/get_odometer_value", deviceID), new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                DataManager.getInstance().hideProgressMessage();
                if (response != null) {
                    try {
                        int error = response.optInt("error");
                        if (error == 0) {
                            String odometer = response.optString("odometer_value");
                            if (editEndOdo.getText().toString().equals("")) {
                                editStartOdo.setText("" + odometer);
                                odometerValue = "" + odometer;
                            } else {
                                if(odometer.equals("")){

                                }else {
                                    editStartOdo.setText(odometerValue);
                                    editEndOdo.setText(""+odometer);
                                }
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
        */
        return is_certify;
    }
}
