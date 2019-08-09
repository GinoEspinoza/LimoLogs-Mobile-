package com.dotcompliance.limologs;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TimePicker;

import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.DutyStatus;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.network.RestTask;
import com.dotcompliance.limologs.util.DataManager;
import com.dotcompliance.limologs.view.CustomScrollView;
import com.dotcompliance.limologs.view.DiscreteTimePickerDialog;
import com.dotcompliance.limologs.view.EditableGridView;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import co.touchlab.squeaky.stmt.query.In;
import cz.msebera.android.httpclient.Header;

public class EditDutyActivity extends LimoBaseActivity implements View.OnTouchListener, LocationListener {
    private CustomScrollView scrollView;
    private FrameLayout gridLayout;
    private EditableGridView gridView;
    private EditText editLocation;
    private EditText editRemark;
    private EditText editStartTime;
    private EditText editEndTime;

    private RadioButton radioOff;
    private RadioButton radioSleeper;
    private RadioButton radioDriving;
    private RadioButton radioOnduty;

    private TextView leftKnob;
    private TextView rightKnob;
    private TextView leftTimeTab;
    private TextView rightTimeTab;

    private ImageButton btnGpsTracker;
    private ProgressBar progressTracking;
    private LocationManager locationManager;

    private DriverLog mLogData;
    private int mDutyIndex = -1;
    private ArrayList<DutyStatus> dutyList;

    int log_index;

    private boolean left_flag = false;
    private boolean right_flag = false;
    private int duty_starttime;
    private int duty_endtime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_edit_duty);

        Intent intent =getIntent();
        log_index = intent.getIntExtra("log_index", 0);
        mDutyIndex = intent.getIntExtra("duty_index", -1);
        mLogData = Preferences.mDriverLogs.get(log_index);
        dutyList = mLogData.statusList;

        initialize();
        DataManager.getInstance().setInsertDuty(DataManager.getInstance().isInsertDuty());
    }

    protected void initialize() {
        setLeftMenuItem("Cancel");
        setRightMenuItem("Save");
        setConnectionStatus(Preferences.isConnected);
        scrollView = (CustomScrollView) findViewById(R.id.scrollview_container);

        gridLayout = (FrameLayout) findViewById(R.id.lay_grid_frame);
        editLocation = (EditText)findViewById(R.id.edit_location);
        editRemark = (EditText)findViewById(R.id.edit_remark);

        editStartTime = (EditText) findViewById(R.id.edit_starttime);
        editEndTime = (EditText) findViewById(R.id.edit_endtime);

        btnGpsTracker = (ImageButton) findViewById(R.id.button_tracker);
        progressTracking = (ProgressBar) findViewById(R.id.progressGps);

        radioOff = (RadioButton) findViewById(R.id.radio_duty_off);
        radioSleeper = (RadioButton) findViewById(R.id.radio_duty_sleep);
        radioDriving = (RadioButton) findViewById(R.id.radio_duty_drive);
        radioOnduty = (RadioButton) findViewById(R.id.radio_duty_on);

        gridView = new EditableGridView(mContext, mLogData);

        editStartTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = duty_starttime / 60;
                int minute = duty_starttime % 60;
                DiscreteTimePickerDialog mTimePicker;
                mTimePicker = new DiscreteTimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        if (selectedHour == 24) { // this will not happen
                            selectedHour = 0;
                        }
                        duty_starttime = 60 * selectedHour + selectedMinute;
                        if (duty_starttime >= duty_endtime) {
                            duty_endtime = duty_starttime + 15;
                            updateEndTimeField();
                        }
                        gridView.setTimeArea(duty_starttime, duty_endtime);
                        leftTimeTab.setText(String.format("%02d:%02d", duty_starttime / 60, duty_starttime % 60));

                        String noonIndicator = "AM";
                        if (selectedHour >= 12)
                            noonIndicator = "PM";
                        if (selectedHour > 12)
                            selectedHour -= 12;
                        editStartTime.setText(String.format("%02d:%02d %s", selectedHour, selectedMinute, noonIndicator));
                    }
                }, hour, minute, false);//Yes 24 hour time
                mTimePicker.setTitle("Start Time");
                mTimePicker.show();
            }
        });

        editEndTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = duty_endtime / 60;
                int minute = duty_endtime % 60;
                DiscreteTimePickerDialog mTimePicker;
                mTimePicker = new DiscreteTimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        if (selectedHour == 0 && selectedMinute == 0) {
                            selectedHour = 24;
                        }
                        duty_endtime = 60 * selectedHour + selectedMinute;
                        if (duty_starttime >= duty_endtime) {
                            duty_starttime = duty_endtime - 15;
                            updateStartTimeField();
                        }
                        gridView.setTimeArea(duty_starttime, duty_endtime);
                        rightTimeTab.setText(String.format("%02d:%02d", duty_endtime / 60, duty_endtime % 60));

                        String noonIndicator = "AM";
                        if (selectedHour >= 12 && selectedHour != 24)
                            noonIndicator = "PM";
                        if (selectedHour > 12)
                            selectedHour -= 12;
                        editEndTime.setText(String.format("%02d:%02d %s", selectedHour, selectedMinute, noonIndicator));
                    }
                }, hour, minute, false);//Yes 24 hour time
                mTimePicker.setTitle("End Time");
                mTimePicker.show();
            }
        });

//        if (Preferences.getCurrentVehicle().rating < 9)
//            radioDriving.setEnabled(false);
//        if (Preferences.getCurrentVehicle().sleeper_on == false)
//            radioSleeper.setEnabled(false);

        leftKnob = new TextView(mContext);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(40, 80);
        leftKnob.setLayoutParams(lp);
        leftKnob.setBackgroundResource(R.drawable.left_knob);
        rightKnob = new TextView(mContext);
        rightKnob.setLayoutParams(lp);
        rightKnob.setBackgroundResource(R.drawable.right_knob);

        leftTimeTab = new TextView(mContext);
        leftTimeTab.setLayoutParams(lp);
        leftTimeTab.setPadding(5, 2, 5, 2);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            leftTimeTab.setTextAppearance(android.R.style.TextAppearance_Material_Medium);
        }
        else {
            leftTimeTab.setTextAppearance(mContext, android.R.style.TextAppearance_Material_Medium);
        }
        leftTimeTab.setBackgroundColor(Color.DKGRAY);
        leftTimeTab.setTextColor(Color.WHITE);

        rightTimeTab = new TextView(mContext);
        rightTimeTab.setLayoutParams(lp);
        rightTimeTab.setPadding(5, 2, 5, 2);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rightTimeTab.setTextAppearance(android.R.style.TextAppearance_Medium);
        }
        else {
            rightTimeTab.setTextAppearance(mContext, android.R.style.TextAppearance_Material_Medium);
        }
        rightTimeTab.setBackgroundColor(Color.DKGRAY);
        rightTimeTab.setTextColor(Color.WHITE);

        gridView.setKnobs(leftKnob, rightKnob, leftTimeTab, rightTimeTab);

        FrameLayout.LayoutParams lp2 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp2.setMargins(0, 30, 0, 40);

        gridView.setLayoutParams(lp2);
        gridLayout.addView(gridView);
        gridLayout.addView(leftKnob);
        gridLayout.addView(rightKnob);
        gridLayout.addView(leftTimeTab);
        gridLayout.addView(rightTimeTab);

        leftKnob.setOnTouchListener(this);
        rightKnob.setOnTouchListener(this);

        if (mDutyIndex == 0) { // first duty
            leftKnob.setVisibility(View.INVISIBLE);
            leftTimeTab.setVisibility(View.INVISIBLE);
            editStartTime.setEnabled(false);
        }
        if (mDutyIndex == mLogData.statusList.size() - 1) { // last duty
            rightKnob.setVisibility(View.INVISIBLE);
            rightTimeTab.setVisibility(View.INVISIBLE);
            editEndTime.setEnabled(false);
        }

        gridView.setIndex(mDutyIndex);
        if (mDutyIndex > -1) {
            editLocation.setText(dutyList.get(mDutyIndex).location);
            editRemark.setText(dutyList.get(mDutyIndex).remark);
            duty_starttime = dutyList.get(mDutyIndex).start_time;
            duty_endtime = dutyList.get(mDutyIndex).end_time;

            int status = dutyList.get(mDutyIndex).status;
            gridView.setDutyState(status);

            if (status == DutyStatus.STATUS_OFF) radioOff.setChecked(true);
            if (status == DutyStatus.STATUS_SLEEPER) radioSleeper.setChecked(true);
            if (status == DutyStatus.STATUS_DRIVING) radioDriving.setChecked(true);
            if (status == DutyStatus.STATUS_ON) radioOnduty.setChecked(true);
        }
        else {
            duty_starttime =  timeToQuarter(mLogData.statusList.get(mLogData.statusList.size() - 1).end_time / 3);
            duty_endtime =  timeToQuarter(mLogData.statusList.get(mLogData.statusList.size() - 1).end_time / 3 * 2);
            if (duty_endtime == duty_starttime)
                duty_endtime = duty_starttime + 15;
            gridView.setDutyState(DutyStatus.STATUS_OFF);
        }
        gridView.setTimeArea(duty_starttime, duty_endtime);
        updateStartTimeField();
        updateEndTimeField();

        ViewTreeObserver vto = gridLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updateKnobPosition();
            }
        });
    }

    @Override
    protected void onMenuItemLeft() {
        Intent resultIntent = new Intent();
        setResult(RESULT_CANCELED, resultIntent);
        finish();
    }

    @Override
    protected void onMenuItemRight() {
        // save
        final int index = gridView.rearrangeDuties();
        gridView.mChangedDuties.get(index).location = editLocation.getText().toString();
        gridView.mChangedDuties.get(index).remark = editRemark.getText().toString();

        RequestParams params = new RequestParams();
        params.put("driverlog_id", mLogData.driverlog_id);

        // put duty list
        List<Map<String, String>> duty_list = new ArrayList<>();
        for (int i = 0; i < gridView.mChangedDuties.size(); i++) {
            DutyStatus duty = gridView.mChangedDuties.get(i);
            Map<String, String> item = new HashMap<>();
            item.put("duty_status_id", "" + duty.Id);
            item.put("status", "" + duty.status);
            item.put("start_time", "" + duty.start_time);
            //item.put("end_time", "" + duty.end_time);
            item.put("location", duty.location);
            item.put("remark", duty.remark);
            duty_list.add(item);
        }
        params.put("duty_list", duty_list);

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.post(Preferences.getUrlWithCredential("/log/update_duty_log"), params, new JsonHttpResponseHandler() {
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
                    Log.d("save duty list", response.toString(4));
                    int error = response.getInt("error");
                    if (error == 0) {
                       // mLogData.statusList.clear();
                        mLogData.statusList = gridView.mChangedDuties;

                        if (Preferences.logsActivity != null) {
                            if (log_index == 0) {
                                Preferences.logsActivity.mDutyState = gridView.mChangedDuties.get(gridView.mChangedDuties.size() - 1).status;
                                Preferences.logsActivity.mLastStateTime = gridView.mChangedDuties.get(gridView.mChangedDuties.size() - 1).start_time;
                            }
                            Preferences.logsActivity.calculateHosLimits();
                            RestTask.submitHos(Preferences.logsActivity.mDutyState,
                                    Preferences.logsActivity.mOnDutyCount,
                                    Preferences.logsActivity.mDriveCount,
                                    Preferences.logsActivity.mCycleCount,
                                    Preferences.logsActivity.mLastBreakCount);
                        }

                        /*Intent resultIntent = new Intent();
                        setResult(RESULT_OK, resultIntent);
                        finish();*/
                        Intent intent = new Intent(EditDutyActivity.this,LogsListActivity.class);
                        DataManager.getInstance().setLog(true);
                        startActivity(intent);
                        finish();
                    }
                } catch (JSONException e) {
                    showMessage("Sorry, failed to save duty.");
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

    public void onSelectDuty(View v) {
        switch (v.getId()) {
            case R.id.radio_duty_off:
                gridView.setDutyState(DutyStatus.STATUS_OFF);
                break;
            case R.id.radio_duty_sleep:
                gridView.setDutyState(DutyStatus.STATUS_SLEEPER);
                break;
            case R.id.radio_duty_drive:
                gridView.setDutyState(DutyStatus.STATUS_DRIVING);
                break;
            case R.id.radio_duty_on:
                gridView.setDutyState(DutyStatus.STATUS_ON);
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(v == leftKnob){
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                left_flag = true;
                scrollView.setScrollingEnabled(false);
            }
            else if(event.getAction() == MotionEvent.ACTION_CANCEL){
                left_flag = false;
                scrollView.setScrollingEnabled(true);
            }
        }else if(v == rightKnob){
            scrollView.requestDisallowInterceptTouchEvent(true);
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                right_flag = true;
                scrollView.setScrollingEnabled(false);
            }
            else if(event.getAction() == MotionEvent.ACTION_CANCEL){
                right_flag = false;
                scrollView.setScrollingEnabled(true);
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        float x;

        if(left_flag){
            if(event.getAction() == MotionEvent.ACTION_MOVE) {
                Rect rc = gridView.getGraphRect();
                x = event.getX() - gridLayout.getLeft() - scrollView.getLeft() - rc.left;
                if (x < 0){
                    x = 0;
                }
                if (x > rc.width()){
                    x = rc.width();
                }
                int timePos = timeToQuarter((int) (1440 * x / rc.width()));
                if((timePos + 15 > duty_endtime)){
                    return false;
                }
                duty_starttime = timePos;
                updateStartTimeField();

            }else if(event.getAction() == MotionEvent.ACTION_UP){
                scrollView.setScrollingEnabled(true);
                left_flag = false;
            }
        }else if(right_flag){
            if(event.getAction() == MotionEvent.ACTION_MOVE){
                Rect rc = gridView.getGraphRect();
                x = event.getX() - gridLayout.getLeft() -scrollView.getLeft() - rc.left;
                if (x < 0){
                    x = 0;
                }
                if (x > rc.width()){
                    x = rc.width();
                }

                int timePos = timeToQuarter((int) (1440 * x / rc.width()));
                if((timePos - 15 < duty_starttime) || (timePos > gridView.mChangedDuties.get(gridView.mChangedDuties.size() - 1).end_time)) {
                    return false;
                }
                duty_endtime = timeToQuarter((int)(x * 1440 / rc.width()));
                updateEndTimeField();
            }
            else if(event.getAction() == MotionEvent.ACTION_UP){
                scrollView.setScrollingEnabled(true);
                right_flag = false;
            }
        }
        gridView.setTimeArea(duty_starttime, duty_endtime);
        if(event.getAction() == MotionEvent.ACTION_DOWN){

        }
        return super.onTouchEvent(event);
    }

    public int timeToQuarter(int time){
        return time - (time % 15);
    }

    private void updateStartTimeField() {
        int hour = duty_starttime / 60;
        int minute = duty_starttime % 60;
        String noonIndicator = "AM";
        if (hour >= 12)
            noonIndicator = "PM";
        if (hour > 12)
            hour -= 12;
        editStartTime.setText(String.format("%02d:%02d %s", hour, minute, noonIndicator));
        leftTimeTab.setText(String.format("%02d:%02d", duty_starttime / 60, duty_starttime % 60));
    }

    private void updateEndTimeField() {
        int hour = duty_endtime / 60;
        int minute = duty_endtime % 60;
        String noonIndicator = "AM";
        if (hour >= 12 && hour != 24)
            noonIndicator = "PM";
        if (hour > 12)
            hour -= 12;
        editEndTime.setText(String.format("%02d:%02d %s", hour, minute, noonIndicator));
        rightTimeTab.setText(String.format("%02d:%02d", duty_endtime / 60, duty_endtime % 60));
    }

    private void updateKnobPosition() {
        Rect rc = gridView.getGraphRect();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins((int) gridView.getXPos(duty_starttime) - leftKnob.getWidth(), rc.bottom + 40, 0, 0);
        leftKnob.setLayoutParams(params);

        params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins((int)gridView.getXPos(duty_starttime) - leftTimeTab.getWidth(), rc.top - leftTimeTab.getHeight() + 30, 0, 0);
        leftTimeTab.setLayoutParams(params);

        params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins((int) gridView.getXPos(duty_endtime), rc.bottom + 40, 0, 0);
        rightKnob.setLayoutParams(params);

        params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins((int) gridView.getXPos(duty_endtime), rc.top - rightTimeTab.getHeight() + 30, 0, 0);
        rightTimeTab.setLayoutParams(params);
    }

    public void trackGpsLocation(View v) {
        if (locationManager == null)
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
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
