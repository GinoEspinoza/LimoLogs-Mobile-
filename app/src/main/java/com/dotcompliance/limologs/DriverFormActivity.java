package com.dotcompliance.limologs;

/*
* Driver Vehicle Inspection Report
*
 */

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
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.dotcompliance.limologs.ELD.AvlEvent;
import com.dotcompliance.limologs.ELD.GPSVOXConnect;
import com.dotcompliance.limologs.ELD.GPSVOXRetrofitConnect;
import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.DvirLog;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
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

public class DriverFormActivity extends LimoBaseActivity implements LocationListener {

    private EditText editFirstName, editLastName;
    private EditText editVehicleNo;
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
    private int log_index = 0;
    private int vehicle_index = 0;
    private int dvir_index = -1;
    private Boolean isPostTrip;
    Boolean isDefectAffected = true;
    List<Map<String, String>> mListDefects = new ArrayList<Map<String, String>>();

    private DriverLog driverLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_form);
        Intent intent = getIntent();
        log_index = intent.getIntExtra("log_index", 0);
        isPostTrip = intent.getBooleanExtra("post_trip", false);

        vehicle_index = getIntent().getIntExtra("vehicle_index", Preferences.mVehicleIndex);
        driverLog = Preferences.mDriverLogs.get(log_index);

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

//                if (isDefectAffected) {
//                    LinearLayout layoutMechanic = (LinearLayout) findViewById(R.id.layout_mechanic_sign);
//                    layoutMechanic.setVisibility(View.VISIBLE);
//                }
                if (mListDefects.size() > 0) {
                    isDefectAffected = data.getBooleanExtra("defect_affectable", true);
                    chkConfirm.setText("Defect(s) was found");
                }
                else {
                    isDefectAffected = false;
                    chkConfirm.setText("No defect was found");
                }
				if (log_index == 0)
					Preferences.mOutOfService = isDefectAffected;
            }
            else if (resultCode == RESULT_CANCELED) {
                // nothing to do
            }
        }
    }

    protected void initialize() {
        setLeftMenuItem("Back");
        setRightMenuItem("Save");
        setRightMenuItemFont(15);
        setLeftMenuItemFont(15);
        setConnectionStatus(Preferences.isConnected);
        // Configure components
        editFirstName = (EditText) findViewById(R.id.edit_firstname);
        editLastName = (EditText) findViewById(R.id.edit_lastname);
        editVehicleNo = (EditText) findViewById(R.id.edit_vehicle_num);
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
                intent.putExtra("log_index", log_index);
                intent.putExtra("dvir_index", dvir_index);
                startActivityForResult(intent, 1);
            }
        });

        signaturePad = (SignaturePad) findViewById(R.id.signature_pad);
        signatureMechanicPad = (SignaturePad) findViewById(R.id.signature_mechanic_pad);
 
        LinearLayout layoutMechanic = (LinearLayout) findViewById(R.id.layout_mechanic_sign);
        layoutMechanic.setVisibility(View.GONE);

        for (int i = 0; i < driverLog.dvirList.size(); i++) {
            if (Preferences.mVehicleList.get(vehicle_index).vehicle_no.equals(driverLog.dvirList.get(i).vehicle)) {
                dvir_index = i;
                break;
            }
        }

        if (dvir_index > -1) {
            mDvir = Preferences.mDriverLogs.get(log_index).dvirList.get(dvir_index);
            editFirstName.setText(mDvir.firstName);
            editLastName.setText(mDvir.lastName);
            editVehicleNo.setText(mDvir.vehicle);
            editLocation.setText(mDvir.location);
            if (mDvir.startOdometer > 0)
                editStartOdo.setText("" + mDvir.startOdometer);
//            if (mDvir.endOdometer > 0)
//                editEndOdo.setText("" + mDvir.endOdometer);

            String deviceId = Preferences.mVehicleList.get(vehicle_index).obdDeviceID;
            if (deviceId != null) {
               /* CalampConnect.fetchAvlEvent(deviceId, new CalampConnect.Gpxinterface () {
                    @Override
                    public void onFinished(AvlEvent event) {
                        if (event != null) {
                            editEndOdo.setText("" + event.vbOdometer);
                        }
                    }
                });*/

                GPSVOXRetrofitConnect connect = new GPSVOXRetrofitConnect(DriverFormActivity.this);
                connect.fetchAvlEvent(deviceId, new GPSVOXRetrofitConnect.Gpxinterface() {
                    @Override
                    public void onFinished(AvlEvent event) {
//                        if (event != null) {
//                            editStartOdo.setText("" + event.vbOdometer);
//                        }
                    }
                });

                if(DataManager.getInstance().isGpxBoolean()) {
                    DataManager.getInstance().setGpxBoolean(false);
                    AvlEvent event = new AvlEvent();
                    event = DataManager.getInstance().getEvent();

                    if (event != null) {
                        editStartOdo.setText("" + event.vbOdometer);
                    }
                }
            }
            editDate.setText(mDvir.logDate);
            if (mDvir.driverSign != null) {
                signaturePad.post(new Runnable() {
                    @Override
                    public void run() {
                        signaturePad.setSignatureBitmap(mDvir.driverSign);
                    }
                });
            }

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
            editFirstName.setText(Preferences.mDriver.firstname);
            editLastName.setText(Preferences.mDriver.lastname);
            editVehicleNo.setText(Preferences.getCurrentVehicle().vehicle_no);

            mDvir.carrierName = Preferences.mDriver.company.carrier_name;
            mDvir.carrierAddress = Preferences.mDriver.company.carrier_address;

            editDate.setText(driverLog.log_date);

            Preferences.mOutOfService = false;

        }
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
//        Intent intent = new Intent(mContext, VehicleListActivity.class);
//        intent.putExtra("log_index", log_index);
//    //    intent.putExtra("vehicle_index", vehicle_index);
//        startActivity(intent);
//        finish();
        finish();
    }

    @Override
    protected void onMenuItemRight() {
        saveForm();
    }

    public void clearSignature(View v) {
        signaturePad.clear();
    }

    public void clearMechanicSignature(View v) {
        signatureMechanicPad.clear();
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
        } catch(NumberFormatException nfe) {
            // Log exception.
            return 0;
        }
    }

    protected void saveForm() {
        if (!chkConfirm.isChecked())
        {
            showMessage("Please check the box or list defects found.");
            return;
        }
        if (signaturePad.isEmpty())
        {
            showMessage("Your signature is required.");
            return;
        }

        if (editStartOdo.getText().toString().isEmpty()) {
            showMessage("Start Odometer field is required.");
            return;
        }
        if (isPostTrip && editEndOdo.getText().toString().isEmpty()) {
            showMessage("Please input end odometer");
            return;
        }

        RequestParams params = new RequestParams();
        final int start_odometer = tryParseInt(editStartOdo.getText().toString());
        final int end_odometer = tryParseInt(editEndOdo.getText().toString());

        params.put("start_odometer", start_odometer);
        if (!editEndOdo.getText().toString().isEmpty()) {
            if (start_odometer > end_odometer) {
                showMessage("End odometer must be greater than start");
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
        params.put("log_date", editDate.getText().toString());

        Calendar calendar = Calendar.getInstance(Preferences.getDriverTimezone());
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a zzz");
        sdf.setTimeZone(Preferences.getDriverTimezone());
        final String dvir_time = sdf.format(calendar.getTime());

        params.put("dvir_time", dvir_time);

        if (!signaturePad.isEmpty()) {
            params.put("signature", ImageEncoder.encodeTobase64(signaturePad.getTransparentSignatureBitmap()));
        }

        if (!mListDefects.isEmpty()) {
//            if (isDefectAffected && signatureMechanicPad.isEmpty())
//            {
//                showMessage("Mechanic signature is required");
//                return;
//            }
//            if (!signatureMechanicPad.isEmpty()) {
//                params.put("signature_mechanic", ImageEncoder.encodeTobase64(signatureMechanicPad.getTransparentSignatureBitmap()));
//            }

            params.put("defect_affected", isDefectAffected ? 1: 0);
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
                        mDvir.logDate = editDate.getText().toString();
                        mDvir.logTime = dvir_time;
                        mDvir.driverSign = signaturePad.getTransparentSignatureBitmap();
                        if (!signatureMechanicPad.isEmpty())
                            mDvir.mechanicSign = signatureMechanicPad.getTransparentSignatureBitmap();
                        mDvir .isDefected = isDefectAffected;
                        // add defects
                        mDvir.defectList.clear();
                        for (Map<String, String> defect : mListDefects) {
                            mDvir.addNewDefect(defect.get("defect_id"), defect.get("defect_name"), defect.get("comment"));
                        }

                        if (dvir_index < 0) {
                            Preferences.mDriverLogs.get(log_index).dvirList.add(mDvir);
                        }

                        //DriverLog driverLog = Preferences
						if (log_index == 0)
							Preferences.mDvirId = mDvir.dvir_id;
						Preferences.mVehicles = response.getString("vehicle_nums");
						Preferences.mTotalMiles = response.getString("total_miles");

						Intent intent = new Intent(mContext, HomeActivity.class);
						intent.putExtra("log_index", log_index);
						startActivity(intent);
 
                        finish();
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
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses.size() > 0) {
                Address addr = addresses.get(0);
                String strAddress = addr.getAddressLine(0);
                for (int i = 1; i <= addr.getMaxAddressLineIndex(); i++) {
                    strAddress += ", " + addr.getAddressLine(i);
                }
                editLocation.setText(strAddress);
                Preferences.mCurrentLocation = strAddress;
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
