package com.dotcompliance.limologs;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;

import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.DutyStatus;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.data.Vehicle;
import com.dotcompliance.limologs.network.RestTask;
import com.dotcompliance.limologs.service.LocationUpdateService;
import com.dotcompliance.limologs.survey.AccidentSurveyController;
import com.dotcompliance.limologs.util.DataManager;
import com.dotcompliance.limologs.view.BaseGridView;
import com.dotcompliance.limologs.view.ClockView;

import org.researchstack.backbone.result.TaskResult;
import org.researchstack.backbone.ui.ViewTaskActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class LogsActivity extends LimoBaseActivity implements NavigationView.OnNavigationItemSelectedListener, LocationListener {

    private BaseGridView logGridView;
    private RadioButton radioOff;
    private RadioButton radioSleeper;
    private RadioButton radioDriving;
    private RadioButton radioOnduty;
    private EditText editLocation;
    private EditText editRemark;
    private ClockView clockDrive;
    private ClockView clockOnDuty;
    private ClockView clockCycle;

    private ImageButton btnGpsTracker;
    private ProgressBar progressTracking;
    private LocationManager locationManager;

    private DriverLog mDriverLog;
    public int mDutyState = 0;
    public int mLastStateTime = 0;
    int duty_status = 0;

    public int mDriveCount, mOnDutyCount, mCycleCount, mLastBreakCount;

    Timer timer;
    TimerTask timerTask;

    @Override
    protected void onStart() {
        super.onStart();
        Preferences.isFullyActive = true;
        //Preferences.logsActivity = this;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Preferences.isFullyActive = false;
        Preferences.logsActivity = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_logs);

        mDriverLog = Preferences.mDriverLogs.get(0);

        initialize();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
        }
        else {
            if (!LocationUpdateService.IS_SERVICE_RUNNING) {
                Intent serviceIntent = new Intent(mContext, LocationUpdateService.class);
                startService(serviceIntent);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(mDriverLog.dvirList.isEmpty() || Preferences.getCurrentVehicle() == null) {
            //Intent intent = new Intent(LogsActivity.this, VehicleListActivity.class);
            //intent.putExtra("log_index", 0);
            //    intent.putExtra("dvir_index", -1); // create new one
            //startActivityForResult(intent, 1);
            //return;
        }
        else if(mDriverLog.driverlog_id == 0) {
            Intent intent = new Intent(LogsActivity.this, TripFormActivity.class);
            intent.putExtra("log_index", 0);
            startActivityForResult(intent, 2);
            return;
        }

        if (Preferences.mOutOfService) {
            radioDriving.setEnabled(false);
            radioOnduty.setEnabled(false);
        }
        else {
            Vehicle vh = Preferences.getCurrentVehicle();
            if (vh != null) {
                radioDriving.setEnabled(vh.rating >= 9 || vh.gvwr > 10000);
            }

            radioOnduty.setEnabled(true);
        }

        calculateHosLimits();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.common, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_accident_report) {
            final EditText editTrip = new EditText(mContext);
            AlertDialog dialog = new AlertDialog.Builder(mContext)
                    .setTitle("Add Trip")
                    .setView(editTrip)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String strNewTrip = editTrip.getText().toString();
                            if (strNewTrip.equals(""))
                                showMessage("Please input trip number");
                            else {
                                mDriverLog.trip += ", " + strNewTrip;
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).create();
            dialog.show();
        } else if (id == R.id.nav_addvehicle) {
            Intent intent = new Intent(LogsActivity.this, VehicleListActivity.class);
            intent.putExtra("log_index", 0);
            //    intent.putExtra("dvir_index", -1); // create new one
            startActivityForResult(intent, 1);
        } else if (id == R.id.nav_form) {
            Intent intent = new Intent(LogsActivity.this, TripFormActivity.class);
            intent.putExtra("log_index", 0);
            startActivityForResult(intent, 2);
        }
        else if (id == R.id.nav_update_dvir) {
            Intent intent = new Intent(LogsActivity.this, DriverFormActivity.class);
            intent.putExtra("log_index", 0);
            startActivityForResult(intent, 3);
        }
        else if (id == R.id.nav_post_trip) {
            Intent intent = new Intent(LogsActivity.this, DriverFormActivity.class);
            intent.putExtra("log_index", 0);
            intent.putExtra("post_trip", true);
            startActivityForResult(intent, 4);
        }
        else if (id == R.id.nav_recap) {
            startActivity(new Intent(mContext, RecapActivity.class));
        }
        else if (id == R.id.nav_editlogs) {
            DataManager.getInstance().setClassname("log");
            startActivity(new Intent(mContext, LogsListActivity.class));

        } else if (id == R.id.nav_inspection) {
            startActivity(new Intent(mContext, InspectionActivity.class));
            finish();
        }
        else if (id == R.id.nav_logsheet_photo) {
            Intent intent = new Intent(mContext, LogsheetPhotoActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.nav_vehicle_docs) {
            Intent intent = new Intent(mContext, VehicleDocListActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.nav_accident_report) {
            AccidentSurveyController controller = new AccidentSurveyController(mContext);
            startActivityForResult(controller.createSurveyIntent(), 5);
        }
        else if (id == R.id.nav_signout) {
            Preferences.clearSession(mContext);
            Preferences.mDriverLogs.clear();
            Preferences.mVehicleList.clear();

            stopLocalTimer();

            stopService(new Intent(mContext, LocationUpdateService.class));

            startActivity(new Intent(mContext, LoginActivity.class));
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    protected void initialize() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        logGridView = (BaseGridView) findViewById(R.id.grid_daily_log);
        editLocation = (EditText) findViewById(R.id.edit_location);
        editRemark = (EditText) findViewById(R.id.edit_remark);
        btnGpsTracker = (ImageButton) findViewById(R.id.button_tracker);
        progressTracking = (ProgressBar) findViewById(R.id.progressGps);

        radioOff = (RadioButton) findViewById(R.id.radio_duty_off);
        radioSleeper = (RadioButton) findViewById(R.id.radio_duty_sleep);
        radioDriving = (RadioButton) findViewById(R.id.radio_duty_drive);
        radioOnduty = (RadioButton) findViewById(R.id.radio_duty_on);

        clockOnDuty = (ClockView) findViewById(R.id.clockview_onduty);
        clockDrive = (ClockView) findViewById(R.id.clockview_drive);
        clockCycle = (ClockView) findViewById(R.id.clockview_cycle);

        clockDrive.setCircleType(ClockView.DRIVE_CIRCLE);
        clockOnDuty.setCircleType(ClockView.ONDUTY_CIRCLE);
        clockCycle.setCircleType(ClockView.CYCLE_CIRCLE);

        populateLog();
        calculateHosLimits();
        startTimer();

        DutyStatus firstStatus = mDriverLog.statusList.get(0);
        if (firstStatus.Id == 0) { // save first state
            RestTask.saveStatus(mDriverLog.driverlog_id, firstStatus, null);
        }
    }

    protected void populateLog() {
        logGridView.setData(mDriverLog);
        ArrayList<DutyStatus> states = mDriverLog.statusList;
        mDutyState = states.get(states.size()-1).status;
        mLastStateTime = states.get(states.size() - 1).start_time;
        if (mDutyState == DutyStatus.STATUS_OFF) radioOff.setChecked(true);
        if (mDutyState == DutyStatus.STATUS_SLEEPER) radioSleeper.setChecked(true);
        if (mDutyState == DutyStatus.STATUS_DRIVING) radioDriving.setChecked(true);
        if (mDutyState == DutyStatus.STATUS_ON) radioOnduty.setChecked(true);

        Vehicle vh = Preferences.getCurrentVehicle();
        if (vh != null) {
            if (vh.rating < 9 && vh.gvwr < 10001)
                radioDriving.setEnabled(false);
            if (!vh.sleeper_on)
                radioSleeper.setEnabled(false);
        }

    }

    public void calculateHosLimits() {
        mCycleCount = Preferences.CYCLE_HOUR;
        mDriveCount = Preferences.DRIVING_HOUR;
        mOnDutyCount = Preferences.ONDUTY_HOUR;
        mLastBreakCount = 0;

        int off_time = 0;

        for (int i = Preferences.CYCLE_DAY - 1; i >= 0; i --) {
            ArrayList<DutyStatus> list = Preferences.mDriverLogs.get(i).statusList;
            for (int j = 0; j < list.size(); j ++) {
                int end_time;
                DutyStatus state = list.get(j);
                if (j == list.size() -1) {
                    if (i == 0) {
                        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
                        end_time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
                        //end_time -= end_time % 15 - 15;
                    }
                    else
                        end_time = 1440;
                }
                else
                    end_time = list.get(j + 1).start_time;

                int duration = end_time - state.start_time;

                switch (state.status) {
                    case DutyStatus.STATUS_SLEEPER:
                    case DutyStatus.STATUS_OFF:
                        off_time += duration;
                        if (off_time >= Preferences.OFFDUTY_HOUR) {
                            // stop calculate drive & OnDuty time here
                            mDriveCount = Preferences.DRIVING_HOUR;
                            mOnDutyCount = Preferences.ONDUTY_HOUR;
                        }
                        if (off_time >= 30) {
                            mLastBreakCount = 0; // reset
                        }
                        break;
                    case DutyStatus.STATUS_DRIVING:
                        off_time = 0;
                        mLastBreakCount += duration;
                        if (mDriveCount > 0) {
                            mDriveCount -= duration;
                        }
                        if (mOnDutyCount > 0) {
                            mOnDutyCount -= duration;
                        }
                        if (mCycleCount > 0) {
                            mCycleCount -= duration;
                        }
                        break;
                    case DutyStatus.STATUS_ON:
                        off_time = 0;
                        mLastBreakCount += duration;
                        if (mOnDutyCount > 0) {
                            mOnDutyCount -= duration;
                        }
                        if (mCycleCount > 0) {
                            mCycleCount -= duration;
                        }
                        break;
                }
            }
        }
        if (mCycleCount < 0) mCycleCount = 0;
        if (mDriveCount < 0) mDriveCount = 0;
        if (mOnDutyCount < 0) mOnDutyCount = 0;

        if (Preferences.isREST_BREAK) {
            mLastBreakCount = 0;
        }
        else if (mLastBreakCount > 480) {
            mLastBreakCount = 480;
        }

        if (mCycleCount < Preferences.ONDUTY_HOUR && mCycleCount < mOnDutyCount)
            mOnDutyCount = mCycleCount;
        if (mOnDutyCount < Preferences.DRIVING_HOUR && mOnDutyCount < mDriveCount)
            mDriveCount = mOnDutyCount;

        clockOnDuty.setMinute(mOnDutyCount);
        clockDrive.setMinute(mDriveCount);
        clockCycle.setMinute(mCycleCount);
    }

    public void startTimer() {
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // get current time of day
                        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());

                        if (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE) == 0) {
                            // restart the app
                            Intent i = getBaseContext().getPackageManager()
                                    .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);
                        }

                        if (cal.get(Calendar.MINUTE) % 15 == 0) {
                            logGridView.invalidate();
                        }
                        if (mOnDutyCount > 0 && mDutyState > DutyStatus.STATUS_SLEEPER) {
                            mOnDutyCount--;
                            if (mOnDutyCount < 600 && mOnDutyCount < mDriveCount)
                                mDriveCount = mOnDutyCount;
                            if (mOnDutyCount == 0) {
                                //RestTask.notifyViolation(mDriverLog.driverlog_id, DriverLog.VIOLATION_ONDUTY);
                                mDriverLog.violations |= DriverLog.VIOLATION_ONDUTY;
                            }
                            /*if (mOnDutyCount == 120) {
                                RestTask.notifyOutOfTime(mDriverLog.driverlog_id, DriverLog.VIOLATION_ONDUTY);
                            }*/
                        }
                        if (mDriveCount > 0 && mDutyState == DutyStatus.STATUS_DRIVING) {
                            mDriveCount--;
                            if (mDriveCount == 0) {
                                //RestTask.notifyViolation(mDriverLog.driverlog_id, DriverLog.VIOLATION_DRIVING);
                                mDriverLog.violations |= DriverLog.VIOLATION_DRIVING;
                            }
                            /*if (mDriveCount == 120) {
                                RestTask.notifyOutOfTime(mDriverLog.driverlog_id, DriverLog.VIOLATION_DRIVING);
                            }*/
                        }
                        if (mCycleCount > 0 && mDutyState > DutyStatus.STATUS_SLEEPER) {
                            mCycleCount--;
                            if (mCycleCount < 900 && mCycleCount < mOnDutyCount)
                                mOnDutyCount = mCycleCount;
                            if (mCycleCount == 0) {
                                //RestTask.notifyViolation(mDriverLog.driverlog_id, DriverLog.VIOLATION_CYCLE);
                                mDriverLog.violations |= DriverLog.VIOLATION_CYCLE;
                            }
                            /*if (mCycleCount == 120) {
                                RestTask.notifyOutOfTime(mDriverLog.driverlog_id, DriverLog.VIOLATION_CYCLE);
                            }*/
                        }
                        clockOnDuty.setMinute(mOnDutyCount);
                        clockDrive.setMinute(mDriveCount);
                        clockCycle.setMinute(mCycleCount);
                    }
                }, 0);
            }
        };

        timer.schedule(timerTask, 0, 1000 * 60);
    }

    private void stopLocalTimer() {
        if (timer != null)
        {
            timer.cancel();
            timer = null;
        }
    }

    public void onSelectDuty(View v) {

        switch (v.getId()) {
            case R.id.radio_duty_off:
                duty_status = DutyStatus.STATUS_OFF;
                break;
            case R.id.radio_duty_sleep:
                duty_status = DutyStatus.STATUS_SLEEPER;
                break;
            case R.id.radio_duty_drive:
                duty_status = DutyStatus.STATUS_DRIVING;
                break;
            case R.id.radio_duty_on:
                duty_status = DutyStatus.STATUS_ON;
                break;
        }
        LinearLayout layout = (LinearLayout) findViewById(R.id.lay_save_state);
        if (duty_status != mDutyState) {
            layout.setVisibility(View.VISIBLE);
            if (Preferences.mCurrentLocation == "")
                this.trackGpsLocation(null);
            else
                editLocation.setText(Preferences.mCurrentLocation);
        }
        else
            layout.setVisibility(View.INVISIBLE);
    }

    public void onSaveDutyClick(View v) {
        /* DutyStatus firstStatus = mDriverLog.statusList.get(0);
        if (firstStatus.Id == 0) { // save first state
            RestTask.saveStatus(mDriverLog.driverlog_id, firstStatus, null);
        } */

        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
        int curr_time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        curr_time -= (curr_time % 15);
        if (curr_time > mLastStateTime) {
            final int last_starttime = curr_time;
            RestTask.saveNewStatus(0, duty_status, curr_time,
                    editLocation.getText().toString(),
                    editRemark.getText().toString(),
                    new RestTask.TaskCallbackInterface() {
                        @Override
                        public void onTaskCompleted(Boolean success, String message) {
                            if (success) {
                                mDutyState = duty_status;
                                LinearLayout layout = (LinearLayout) findViewById(R.id.lay_save_state);
                                layout.setVisibility(View.INVISIBLE);
                                editLocation.setText("");
                                editRemark.setText("");
                                logGridView.invalidate();

                                calculateHosLimits();
                                RestTask.submitHos(mDutyState, mOnDutyCount, mDriveCount, mCycleCount, mLastBreakCount);

                                mLastStateTime = last_starttime;
                            } else {
                                showMessage("Sorry, was unable to set new status: " + message);
                            }
                        }
                    });
        }
        else {
            // we should overwrite last duty status
            final ArrayList<DutyStatus> duty_list = mDriverLog.statusList;
            final int last_index = duty_list.size() - 1;
            if (last_index > 0 && duty_list.get(last_index - 1).status == duty_status) { // remove last status
                RestTask.removeDuty(duty_list.get(last_index).Id, new RestTask.TaskCallbackInterface() {
                    @Override
                    public void onTaskCompleted(Boolean success, String message) {
                        if (success) {
                            duty_list.remove(last_index);
                            mDutyState = duty_status;
                            mLastStateTime = duty_list.get(last_index - 1).start_time;
                            logGridView.invalidate();

                            calculateHosLimits();
                            RestTask.submitHos(mDutyState, mOnDutyCount, mDriveCount, mCycleCount, mLastBreakCount);
                        }
                        else {
                            showMessage("Sorry, was unable to set new status: " + message);
                        }
                    }
                });
            }
            else {
                DutyStatus last_duty = duty_list.get(last_index);
                last_duty.status = duty_status;
                last_duty.location = editLocation.getText().toString();
                last_duty.remark = editRemark.getText().toString();
                RestTask.saveStatus(mDriverLog.driverlog_id, last_duty, new RestTask.TaskCallbackInterface() {
                    @Override
                    public void onTaskCompleted(Boolean success, String message) {
                        if (success) {
                            mDutyState = duty_status;
                            LinearLayout layout = (LinearLayout) findViewById(R.id.lay_save_state);
                            layout.setVisibility(View.INVISIBLE);
                            editLocation.setText("");
                            editRemark.setText("");
                            logGridView.invalidate();

                            calculateHosLimits();
                            RestTask.submitHos(mDutyState, mOnDutyCount, mDriveCount, mCycleCount, mLastBreakCount);
                        }
                        else {
                            showMessage("Sorry, was unable to set new status: " + message);
                        }
                    }
                });
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 5) { // accident survey
            if (resultCode == RESULT_OK) {
                startLoading();
                AccidentSurveyController.processSurveyResult((TaskResult) data.getSerializableExtra(ViewTaskActivity.EXTRA_TASK_RESULT), new RestTask.TaskCallbackInterface() {
                    @Override
                    public void onTaskCompleted(Boolean success, String message) {
                        stopLoading();
                    }
                });
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    if (!LocationUpdateService.IS_SERVICE_RUNNING) {
                        Intent serviceIntent = new Intent(mContext, LocationUpdateService.class);
                        startService(serviceIntent);
                    }

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
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
