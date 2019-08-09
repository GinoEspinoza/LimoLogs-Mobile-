package com.dotcompliance.limologs;

import android.*;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.dotcompliance.limologs.ELD.AvlEvent;
import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.DutyStatus;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.data.Vehicle;
import com.dotcompliance.limologs.network.RestTask;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteLocationActivity extends LimoBaseActivity implements LocationListener {

    public static String DATA_LOCATION = "RESULT_LOCATION";
    public static String DATA_NOTE = "RESULT_NOTE";
    public static String DATA_STATUS = "RESULT_STATUS";
    public static String DATA_STATUS_FLAG = "RESULT_STATUS_FLAG";
    EditText editTextLocation;
    EditText editTextNote;
    Button buttonSave;
    private ImageButton btnGpsTracker;
    private ProgressBar progressTracking;
    private CheckBox statusCheckBox; // Either PC or YM
    private LocationManager locationManager;
    private String askDuty = "";
    private String statusPcOrYm, statusShort, whichBtnClicked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_location);
        editTextLocation = (EditText) findViewById(R.id.textfield_location);
        editTextNote = (EditText) findViewById(R.id.textfield_note);
        buttonSave = (Button) findViewById(R.id.button_save);
        btnGpsTracker = (ImageButton) findViewById(R.id.button_tracker);
        progressTracking = (ProgressBar) findViewById(R.id.progressGps);
        statusCheckBox = (CheckBox) findViewById(R.id.check_pc_or_ym);
        Intent intent = getIntent();
        statusPcOrYm = intent.getStringExtra("pc_or_ym"); // .getExtra("pc_or_ym", "");
        whichBtnClicked = intent.getStringExtra("which_btn_clicked");
        Log.e("181017", " Status coming : " + statusPcOrYm + " Btn Clicked : " + whichBtnClicked);

        if (statusPcOrYm == null || statusPcOrYm.matches("")) {
            statusCheckBox.setVisibility(View.INVISIBLE);
            if (whichBtnClicked != null && whichBtnClicked.matches("OFF"))
                statusShort = "PC";
            else if (whichBtnClicked != null && whichBtnClicked.matches("ON"))
                statusShort = "YM";
            else
                statusShort = null;
        } else if (statusPcOrYm != null && whichBtnClicked != null
                && (statusPcOrYm.matches("YM:  Yard Move")
                || statusPcOrYm.matches("PC:  Personal Conveyance"))) {

            if ((whichBtnClicked.matches("OFF")
                    && statusPcOrYm.matches("PC:  Personal Conveyance"))
                    || (whichBtnClicked.matches("ON") && statusPcOrYm.matches("YM:  Yard Move"))) {

                statusCheckBox.setVisibility(View.VISIBLE);
                statusCheckBox.setText(statusPcOrYm);

                if (statusPcOrYm.matches("YM:  Yard Move")) {
                    statusShort = "YM";
                } else if (statusPcOrYm.matches("PC:  Personal Conveyance")) {
                    statusShort = "PC";
                }

                Log.e("2010 : ", " Status set : " + statusShort);
            } else if (whichBtnClicked.matches("OFF")) {
                Log.e("20102017", " OFF clicked but no Check Box");
                statusCheckBox.setVisibility(View.INVISIBLE);
                statusShort = "PC";

            } else if (whichBtnClicked.matches("ON")) {
                statusCheckBox.setVisibility(View.INVISIBLE);
                Log.e("20102017", " ON clicked but no Check Box");
                statusShort = "YM";
            }
        }


        btnGpsTracker.performClick();

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int status_checked; //= (statusCheckBox.isChecked()) ? 1 : 0 ;

                if (statusCheckBox.isChecked()) {
                    status_checked = 1;
                    //statusShort = "";
                } else {
                    status_checked = 0;
                    //statusShort = "";
                }


                if (editTextLocation.getText().toString().isEmpty()) {
                    showMessage("Location cannot be empty.", true);
                    return;
                }


                if (editTextNote.getText().toString().length() > 0) {
                    if (editTextNote.getText().toString().trim().length() < 4) {
                        showMessage("Note must be more than 4 characters.", true);
                        return;
                    }
                }

                Intent intent = new Intent();
                intent.putExtra(DATA_LOCATION, "M-" + editTextLocation.getText().toString());
                intent.putExtra(DATA_NOTE, editTextNote.getText().toString());
                intent.putExtra(DATA_STATUS, statusShort);
                intent.putExtra(DATA_STATUS_FLAG, status_checked);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        setLeftMenuItem("Back");

        editTextLocation.setText(Preferences.mCurrentLocation);
    }

    @Override
    protected void onMenuItemLeft() {
        Intent resultIntent = new Intent();
        setResult(RESULT_CANCELED, resultIntent);
        finish();
    }

    public void trackGpsLocation(View v) {
        if (locationManager == null)
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
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
        Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
        try {
            Log.e("OnLocation", "onLocationChanged: " + location.getLatitude() + "   " + location.getLongitude());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses.size() > 0) {
                Address addr = addresses.get(0);
                String strAddress = addr.getLocality() + ", " + addr.getAdminArea();
                editTextLocation.setText(strAddress);
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


}
