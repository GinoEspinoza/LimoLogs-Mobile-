package com.dotcompliance.limologs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.dotcompliance.limologs.ELD.AvlEvent;
import com.dotcompliance.limologs.ELD.GPSVOXRetrofitConnect;
import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.DutyStatus;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.data.Vehicle;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.network.RestTask;
import com.dotcompliance.limologs.survey.AccidentSurveyController;
import com.dotcompliance.limologs.util.DataManager;
import com.dotcompliance.limologs.view.DutyStateButton;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;
import org.researchstack.backbone.result.TaskResult;
import org.researchstack.backbone.ui.ViewTaskActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import cz.msebera.android.httpclient.Header;

public class HomeActivity extends LimoBaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
    private static String TAG = "HOME";

    private static int REQUEST_FILL_FORM = 1;
    private static int REQUEST_NEW_DUTY = 2;
    private static int REQUEST_ACCIDENT_SURVEY = 3;

    DutyStateButton dutyOff;
    DutyStateButton dutySleeper;
    DutyStateButton dutyDriving;
    DutyStateButton dutyOn;

    TextView textViewOffTime;
    TextView textViewSbTime;
    TextView textViewDrivingTime;
    TextView textViewDrivingRmTime;
    TextView textViewOnTime;
    TextView textViewOnRmTime;
    TextView textViewCycleRmTime;
    String ignition_value = "";

    private DriverLog mDriverLog;
    public int mDutyState = 0;
    public int mLastStateTime = 0;
    int duty_status = 0;


    public int mDriveCount, mOnDutyCount, mCycleCount, mLastBreakCount;
    public int mOffTime, mSleeperTime, mDrivingTime, mOnTime;

    static Timer timer;
    TimerTask timerTask;

    Date obd_status_last_changed;
    private String deviceid = "";
    private TextView txtconnect;

    private boolean dialogShowing = false;
    private boolean posttripflag = false;

    private boolean alertClicked = false;

    private Handler handlerAlert = new Handler();
    private Runnable runnableAlert = null;

    public static boolean callService = true;

    public String ym_or_pc_status = "";

    public String statusPcOrYmLocal;
    public int status_checked;

    public String str = ""; // This variable will store the Check box Text either YM or PC
    private int dutystatusID;
    GPSVOXRetrofitConnect connect;
    long diff ;
    long diff1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        connect = new GPSVOXRetrofitConnect(HomeActivity.this);
        initialize();
    }

    private void initControls() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        dutyOff = (DutyStateButton) findViewById(R.id.duty_off_layout);
        dutySleeper = (DutyStateButton) findViewById(R.id.duty_sb_layout);
        dutyDriving = (DutyStateButton) findViewById(R.id.duty_driving_layout);
        dutyOn = (DutyStateButton) findViewById(R.id.duty_on_layout);

        textViewOffTime = (TextView) findViewById(R.id.text_off_time);
        textViewSbTime = (TextView) findViewById(R.id.text_sb_time);
        textViewDrivingTime = (TextView) findViewById(R.id.text_driving_time);
        textViewDrivingRmTime = (TextView) findViewById(R.id.text_driving_remain);
        textViewOnTime = (TextView) findViewById(R.id.text_on_time);
        textViewOnRmTime = (TextView) findViewById(R.id.text_on_remain);
        textViewCycleRmTime = (TextView) findViewById(R.id.text_cycle_remain);
        txtconnect = (TextView) findViewById(R.id.txtconnect);

        dutyOff.setOnClickListener(this);
        dutySleeper.setOnClickListener(this);
        dutyDriving.setOnClickListener(this);
        dutyOn.setOnClickListener(this);
//        DataManager.getInstance().showProgressMessage(HomeActivity.this);
//      //  GPSVOXConnect connect = new GPSVOXConnect(HomeActivity.this);
//        connect.fetchAvlEvent("", new Gpxinterface() {
//            @Override
//            public void onFinished(AvlEvent event) {
//                Toast.makeText(HomeActivity.this, "finsihed", Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    private void initialize() {

        initControls();
        setConnectionStatus(txtconnect ,Preferences.isConnected);
        obd_status_last_changed = Calendar.getInstance().getTime();
        int size = Preferences.mDriverLogs.size();
        mDriverLog = Preferences.mDriverLogs.get(0);

        populateLog();
        calculateHosLimits();
        startTimer();
        for (DutyStatus duty : mDriverLog.statusList) {
            if (duty.Id == 0) { // save first state
                Log.e("status", "initialize: " + duty.status);
                RestTask.saveStatus(mDriverLog.driverlog_id, duty, null);
            }
        }


    }

    @Override
    protected void onStart() {
        super.onStart();
        Preferences.isFullyActive = true;
        Preferences.logsActivity = this;

        startTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Preferences.isFullyActive = false;
        Preferences.logsActivity = null;

        stopTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Toast.makeText(mContext,"Hi",Toast.LENGTH_LONG).show();

        if (mDriverLog.driverlog_id == 0) {
            Log.e("181017", " ===> Home 199");
            Intent intent = new Intent(HomeActivity.this, CombinedSheetActivity.class);
            intent.putExtra("log_index", 0);
            startActivityForResult(intent, REQUEST_FILL_FORM);
            return;
        }

        if (Preferences.mOutOfService) {
            dutyDriving.setDisabled(true);
            dutyOn.setDisabled(true);
        } else {
            Vehicle vh = Preferences.getCurrentVehicle();
            if (vh != null) {
                dutyDriving.setDisabled(vh.rating < 9 && vh.gvwr <= 10000);
            }

            dutyOn.setDisabled(false);
        }

        if (Preferences.mVehicleIndex < 0) {
            dutyDriving.setDisabled(true);
        }

        calculateHosLimits();
        RestTask.submitHos(mDutyState, mOnDutyCount, mDriveCount, mCycleCount, mLastBreakCount);
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
        //getMenuInflater().inflate(R.menu.home, menu);
        //return true;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_add_trip) {
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
            Intent intent = new Intent(HomeActivity.this, VehicleListActivity.class);
            intent.putExtra("log_index", 0);
            //    intent.putExtra("dvir_index", -1); // create new one
            startActivity(intent);
        } else if (id == R.id.nav_form) {
            Intent intent = new Intent(HomeActivity.this, TripFormActivity.class);
            intent.putExtra("log_index", 0);
            startActivity(intent);
        } else if (id == R.id.nav_update_dvir) {
            Intent intent = new Intent(HomeActivity.this, DriverFormActivity.class);
            intent.putExtra("log_index", 0);
            startActivity(intent);
        } else if (id == R.id.nav_post_trip) {
            Intent intent = new Intent(HomeActivity.this, CombinedSheetActivity.class);
            intent.putExtra("post_trip", true);
            startActivity(intent);
        } else if (id == R.id.nav_view_graph) {
            startActivity(new Intent(mContext, GraphViewActivity.class));
        } else if (id == R.id.nav_recap) {
            startActivity(new Intent(mContext, RecapActivity.class));
        } else if (id == R.id.nav_editlogs) {
            DataManager.getInstance().setClassname("home");
            startActivity(new Intent(mContext, LogsListActivity.class));

        } else if (id == R.id.nav_inspection) {
            startActivity(new Intent(mContext, InspectionActivity.class));
            finish();
        } else if (id == R.id.nav_logsheet_photo) {
            Intent intent = new Intent(mContext, LogsheetPhotoActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_vehicle_docs) {
            if (Preferences.getCurrentVehicle() != null) {
                Intent intent = new Intent(mContext, VehicleDocListActivity.class);
                startActivity(intent);
            } else {
                showMessage("No vehicle is selected.", true);
            }
        } else if (id == R.id.nav_accident_report) {
            AccidentSurveyController controller = new AccidentSurveyController(mContext);
            startActivityForResult(controller.createSurveyIntent(), REQUEST_ACCIDENT_SURVEY);
        } else if (id == R.id.nav_signout) {
           /* Preferences.clearSession(mContext);
            Preferences.mDriverLogs.clear();
            Preferences.mVehicleList.clear();
            Preferences.isConnected = false;

            stopTimer();

            stopService(new Intent(mContext, LocationUpdateService.class));

            startActivity(new Intent(mContext, LoginActivity.class));
            finish();*/
            onSaveDuty();
            // checkCertification();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FILL_FORM) {
            // nothing to do, will do everything in onResume()
        } else if (requestCode == REQUEST_NEW_DUTY) {
            if (resultCode == RESULT_OK) {
                final String location = data.getStringExtra(NoteLocationActivity.DATA_LOCATION);
                final String note = data.getStringExtra(NoteLocationActivity.DATA_NOTE);

                statusPcOrYmLocal = data.getStringExtra(NoteLocationActivity.DATA_STATUS);
                status_checked = data.getIntExtra(NoteLocationActivity.DATA_STATUS_FLAG, 0); // data.getIntExtra(NoteLocationActivity.DATA_STATUS_FLAG);
                Log.e("191017", " checked box clicked or not : " + status_checked + " Status : " + statusPcOrYmLocal + "-");
                if (statusPcOrYmLocal != null && statusPcOrYmLocal.matches("PC")) {
                    if (status_checked == 1) {
                        duty_status = 4;
                    } else if (status_checked == 0) {
                        duty_status = 0;
                    }

                } else if (statusPcOrYmLocal != null && statusPcOrYmLocal.matches("YM")) {
                    if (status_checked == 1) {
                        duty_status = 5;
                    } else if (status_checked == 0) {
                        duty_status = 3;
                    }
                }

                Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
                int curr_time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
                if (curr_time > mLastStateTime) {
                    final int last_starttime = curr_time;

                    startLoading();
                    RestTask.saveNewStatus(0, duty_status, curr_time
                            , location, note, new RestTask.TaskCallbackInterface() {
                                @Override
                                public void onTaskCompleted(Boolean success, String message) {
                                    stopLoading();

                                    if (success) {
                                        mDutyState = duty_status;

                                        refreshDutyButtons();

                                        calculateHosLimits();
                                        RestTask.submitHos(mDutyState, mOnDutyCount, mDriveCount, mCycleCount, mLastBreakCount);

                                        mLastStateTime = last_starttime;
                                    } else {
                                        showMessage("Sorry, was unable to set new status: " + message);
                                        stopLoading();
                                    }
                                }
                            });
                } else {
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

                                    refreshDutyButtons();
                                    calculateHosLimits();
                                    RestTask.submitHos(mDutyState, mOnDutyCount, mDriveCount, mCycleCount, mLastBreakCount);
                                } else {
                                    showMessage("Sorry, was unable to set new status: " + message);
                                    stopLoading();
                                }
                            }
                        });
                    } else {
                        DutyStatus last_duty = duty_list.get(last_index);
                        last_duty.status = duty_status;
                        last_duty.location = location;
                        last_duty.remark = note;
                        RestTask.saveStatus(mDriverLog.driverlog_id, last_duty, new RestTask.TaskCallbackInterface() {
                            @Override
                            public void onTaskCompleted(Boolean success, String message) {
                                if (success) {
                                    mDutyState = duty_status;
                                    refreshDutyButtons();
                                    calculateHosLimits();
                                    RestTask.submitHos(mDutyState, mOnDutyCount, mDriveCount, mCycleCount, mLastBreakCount);
                                } else {
                                    showMessage("Sorry, was unable to set new status: " + message);
                                    stopLoading();
                                }
                            }
                        });
                    }
                }
            }
        } else if (requestCode == REQUEST_ACCIDENT_SURVEY) {
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

    protected void populateLog() {
        ArrayList<DutyStatus> states = mDriverLog.statusList;
        Log.e(TAG, "populateLog: " + states.toString());
        int i = states.size() - 1;
        dutystatusID = states.get(0).Id;
        boolean status = false;
        for (int j = i; j <= i && j >= 0; j--) {
            Log.e(TAG, " value I " + i + "  j  " + j);
            if (states.get(j).status == 0 || states.get(j).status == 1 || states.get(j).status == 2 || states.get(j).status == 3
                    || states.get(j).status == 4 || states.get(j).status == 5) {
                status = true;
                mDutyState = states.get(j).status;
                mLastStateTime = states.get(j).start_time;
                Log.e(TAG, ": " + mDutyState);
                // break;
            }
            if (status) {
                status = false;
                break;
            }

        }
        // mDutyState = states.get(states.size() - 1).status;
        //    mLastStateTime = states.get(states.size() - 1).start_time;

        refreshDutyButtons();

        Vehicle vh = Preferences.getCurrentVehicle();
        if (vh != null) {
            if (vh.rating < 9 && vh.gvwr < 10001)
                dutyDriving.setDisabled(true);
            if (!vh.sleeper_on)
                dutySleeper.setDisabled(true);
        }

    }

    public void refreshDutyButtons() {
        if (mDutyState == DutyStatus.STATUS_OFF || mDutyState == DutyStatus.STATUS_PC) {
            dutyOff.setSelected(true);
            dutyOff.getBackground().setAlpha(255);

            dutySleeper.setSelected(false);
            dutyDriving.setSelected(false);
            dutyOn.setSelected(false);

            dutySleeper.getBackground().setAlpha(150);
            dutyDriving.getBackground().setAlpha(150);
            dutyOn.getBackground().setAlpha(150);
        } else if (mDutyState == DutyStatus.STATUS_SLEEPER) {
            dutySleeper.setSelected(true);
            dutySleeper.getBackground().setAlpha(255);

            dutyOff.setSelected(false);
            dutyDriving.setSelected(false);
            dutyOn.setSelected(false);

            dutyOff.getBackground().setAlpha(150);
            dutyDriving.getBackground().setAlpha(150);
            dutyOn.getBackground().setAlpha(150);
        } else if (mDutyState == DutyStatus.STATUS_DRIVING) {
            dutyDriving.setSelected(true);
            dutyDriving.getBackground().setAlpha(255);

            dutyOff.setSelected(false);
            dutySleeper.setSelected(false);
            dutyOn.setSelected(false);

            dutyOff.getBackground().setAlpha(150);
            dutySleeper.getBackground().setAlpha(150);
            dutyOn.getBackground().setAlpha(150);
        } else if (mDutyState == DutyStatus.STATUS_ON || mDutyState == DutyStatus.STATUS_YM) {
            dutyOn.setSelected(true);
            dutyOn.getBackground().setAlpha(255);

            dutyOff.setSelected(false);
            dutySleeper.setSelected(false);
            dutyDriving.setSelected(false);

            dutySleeper.getBackground().setAlpha(150);
            dutyDriving.getBackground().setAlpha(150);
            dutyOff.getBackground().setAlpha(150);
        }
    }

    private void updateTimes() {
        textViewOffTime.setText(String.format(Locale.US, "TIME OFF TODAY: %02d:%02d", mOffTime / 60, mOffTime % 60));
        textViewSbTime.setText(String.format(Locale.US, "SB TIME TODAY: %02d:%02d", mSleeperTime / 60, mSleeperTime % 60));
        textViewDrivingTime.setText(String.format(Locale.US, "DRIVING TIME TODAY: %02d:%02d", mDrivingTime / 60, mDrivingTime % 60));
        textViewOnTime.setText(String.format(Locale.US, "ON DUTY TIME TODAY: %02d:%02d", mOnTime / 60, mOnTime % 60));

        textViewDrivingRmTime.setText(String.format(Locale.US, "DRIVING TIME REMAINING: %02d:%02d", mDriveCount / 60, mDriveCount % 60));
        textViewOnRmTime.setText(String.format(Locale.US, "ON DUTY TIME REMAINING: %02d:%02d", mOnDutyCount / 60, mOnDutyCount % 60));
        textViewCycleRmTime.setText(String.format(Locale.US, "CYCLE TIME REMAINING: %02d:%02d", mCycleCount / 60, mCycleCount % 60));

        if (mOnDutyCount == 0 || mCycleCount == 0) {
            dutyOn.setHosState(DutyStateButton.STATE_ALERT);
        } else if (mOnDutyCount <= 120 || mOnDutyCount <= 120) {
            dutyOn.setHosState(DutyStateButton.STATE_WARNING);
        } else {
            dutyOn.setHosState(DutyStateButton.STATE_NORMAL);
        }

        if (mDriveCount == 0) {
            dutyDriving.setHosState(DutyStateButton.STATE_ALERT);
        } else if (mDriveCount <= 120) {
            dutyDriving.setHosState(DutyStateButton.STATE_WARNING);
        } else {
            dutyDriving.setHosState(DutyStateButton.STATE_NORMAL);
        }
    }

    public void calculateHosLimits() {
        mCycleCount = Preferences.CYCLE_HOUR;
        mDriveCount = Preferences.DRIVING_HOUR;
        mOnDutyCount = Preferences.ONDUTY_HOUR;
        mLastBreakCount = 0;

        mOffTime = 0;
        mSleeperTime = 0;
        mDrivingTime = 0;
        mOnTime = 0;

        int off_time = 0;

        for (int i = Preferences.CYCLE_DAY - 1; i >= 0; i--) {

            if (i >= Preferences.mDriverLogs.size()) {
                //Toast.makeText(HomeActivity.this, "We are expecting atleast 8 drivers logs but found only " +Preferences.mDriverLogs.size(),  Toast.LENGTH_SHORT).show();
                continue;
            } else {

                ArrayList<DutyStatus> list = Preferences.mDriverLogs.get(i).statusList;
                for (int j = 0; j < list.size(); j++) {
                    int end_time;
                    DutyStatus state = list.get(j);
                    if (j == list.size() - 1) {
                        if (i == 0) {
                            Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
                            end_time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
                        } else
                            end_time = 1440;
                    } else
                        end_time = list.get(j + 1).start_time;

                    int duration = end_time - state.start_time;

                    if (i == 0) {
                        switch (state.getNormalizeStatus()) {
                            case DutyStatus.STATUS_SLEEPER:
                                mSleeperTime += duration;
                                break;
                            case DutyStatus.STATUS_OFF:
                                mOffTime += duration;
                                break;
                            case DutyStatus.STATUS_DRIVING:
                                mDrivingTime += duration;
                                break;
                            case DutyStatus.STATUS_ON:
                                mOnTime += duration;
                        }
                    }

                    switch (state.getNormalizeStatus()) {
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
            } // end Else
        } // Main loop
        if (mCycleCount < 0) mCycleCount = 0;
        if (mDriveCount < 0) mDriveCount = 0;
        if (mOnDutyCount < 0) mOnDutyCount = 0;

        if (Preferences.isREST_BREAK) {
            mLastBreakCount = 0;
        } else if (mLastBreakCount > 480) {
            mLastBreakCount = 480;
        }

        if (mCycleCount < Preferences.ONDUTY_HOUR && mCycleCount < mOnDutyCount)
            mOnDutyCount = mCycleCount;
        if (mOnDutyCount < Preferences.DRIVING_HOUR && mOnDutyCount < mDriveCount)
            mDriveCount = mOnDutyCount;

        updateTimes();
    }

    public void posttrip() {
        try {
            final Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
            int time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
            if (time == 1420) {
                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                //                            posttripflag =false;
                                break;
                        }
                    }
                };

                //            if (!posttripflag) {
                //                posttripflag = true;
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                AlertDialog dialog = builder.setMessage("Post trip inspestion")
                        .setPositiveButton("Yes", onClickListener)
                        .show();
                //            }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public void startTimer() {
//        timer = new Timer();
//        timerTask = new TimerTask() {
//            @Override
//            public void run() {
//                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.e(TAG, "run: timer Running ");
//                        Vehicle v = Preferences.getCurrentVehicle();
//
//                        //    Log.e("181017", " Device : " + v.obdDeviceID + "==>" + statusPcOrYmLocal + "<==" + "==>" + status_checked + "<==");
//
//                        try {
//                        //    if (v.obdDeviceID != null) {
//                                //deviceid = v.obdDeviceID;
//
//                                connect.fetchAvlEvent(deviceid, new Gpxinterface() {
//
//                                    @Override
//                                    public void onFinished(AvlEvent event){
//
//                                    }
////
////
////
////                                    /*{
////                                        Log.e(TAG, "onFinished: " );
////                                        if (event != null) { // line event not null STARTS
////
////                                            if (statusPcOrYmLocal != null && status_checked == 1) {
////                                                Log.e("181017", " Will call CalAmp for ignition status : " + statusPcOrYmLocal + " ==> " + event.ignitionStatus);
////
////                                                // if (event.ignitionStatus.matches("IGOFF")) {
////                                                if (event.igintionStatusGPX == false) {
////                                                    if (statusPcOrYmLocal.matches("PC")) {
////                                                        duty_status = 0;
////                                                        status_checked = 0;
////                                                        didNoteLocationNew(Preferences.mCurrentLocation, "", duty_status);
////                                                    } else if (statusPcOrYmLocal.matches("YM")) {
////                                                        duty_status = 3;
////                                                        status_checked = 0;
////                                                        didNoteLocationNew(Preferences.mCurrentLocation, "", duty_status);
////                                                    }
////                                                }
////
////
////                                            } else {
////
////                                                Log.e("181017", " Will call CalAmp for speed");
////
////                                                Date current = Calendar.getInstance().getTime();
////                                                long diff = (current.getTime() - obd_status_last_changed.getTime()) / 1000;  // This time is in Seconds
////                                                diff = (diff / 60); // This time is in Minutes
////
////                                                long dd = (current.getTime() - obd_status_last_changed.getTime());
////
//////                                                if ((mDutyState == DutyStatus.STATUS_ON && event.vbSpeed > 5)) {
////                                                if ((mDutyState == DutyStatus.STATUS_ON && event.status == 1)) {
////                                                    duty_status = DutyStatus.STATUS_DRIVING;
////                                                    mDutyState = duty_status;
////                                                    obd_status_last_changed = Calendar.getInstance().getTime();
////                                                    didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
////
////                                                    if (event.getEngineHours() > 0)
////                                                        updateDutyStatus("" + Preferences.VechileId,
////                                                                "" + event.engineHours, "" + dutystatusID, deviceid);
////
////
////                                                    // Other Buttons will be disabled
////                                                    dutyOff.setDisabled(true);
////                                                    dutySleeper.setDisabled(true);
////                                                    dutyOn.setDisabled(true);
////
////
//////                                                } else if (mDutyState == DutyStatus.STATUS_DRIVING && (event.vbSpeed > 0 && event.vbSpeed <= 5)) {
////                                                    // Other Buttons will be disabled
////                                                } else if (mDutyState == DutyStatus.STATUS_DRIVING && event.getStatus() == 1) {
////                                                    dutyOff.setDisabled(true);
////                                                    dutySleeper.setDisabled(true);
////                                                    dutyOn.setDisabled(true);
////                                                    if (event.getEngineHours() > 0)
////                                                        updateDutyStatus("" + Preferences.VechileId,
////                                                                "" + event.engineHours, "" + dutystatusID, deviceid);
////
////                                                } else if (mDutyState == DutyStatus.STATUS_DRIVING && event.vbSpeed == 0 && diff >= 5) {
////
////                                                    // Other Buttons will be enabled
////
////                                                    dutyOff.setEnabled(true);
////                                                    dutySleeper.setEnabled(true);
////                                                    dutyOn.setEnabled(true);
////                                                    dutyDriving.setEnabled(true);
////
////                                                    if (event.getEngineHours() > 0)
////                                                        updateDutyStatus("" + Preferences.VechileId,
////                                                                "" + event.engineHours, "" + dutystatusID, deviceid);
////
////                                                    //boolean alertClicked = false ;
////
////                                                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
////                                                        @Override
////                                                        public void onClick(DialogInterface dialog, int which) {
////                                                            switch (which) {
////                                                                case DialogInterface.BUTTON_POSITIVE:
////                                                                    dialogShowing = false;
////                                                                    alertClicked = true;
////                                                                    duty_status = DutyStatus.STATUS_DRIVING;
////                                                                    mDutyState = duty_status;
////                                                                    didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
////                                                                    if (runnableAlert != null) {
////                                                                        handlerAlert.removeCallbacks(runnableAlert);
////                                                                        runnableAlert = null;
////                                                                    }
////                                                                    break;
////
////                                                                case DialogInterface.BUTTON_NEGATIVE:
////                                                                    dialogShowing = false;
////                                                                    alertClicked = true;
////                                                                    duty_status = DutyStatus.STATUS_ON;
////                                                                    mDutyState = duty_status;
////                                                                    didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
////                                                                    if (runnableAlert != null) {
////                                                                        handlerAlert.removeCallbacks(runnableAlert);
////                                                                        runnableAlert = null;
////                                                                    }
////                                                                    break;
////                                                            }
////                                                        }
////                                                    };
////
////                                                    if (!dialogShowing) {
////                                                        dialogShowing = true;
////                                                        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
////                                                        final AlertDialog dialog = builder.setMessage("Are you currently driving?").setPositiveButton("Yes", dialogClickListener)
////                                                                .setNegativeButton("No", dialogClickListener).show();
////
////
////                                                        if (runnableAlert != null) {
////                                                            //+ Calendar.getInstance().getTime()
////                                                            handlerAlert.removeCallbacks(runnableAlert);
////                                                            runnableAlert = null;
////                                                        }
////
////                                                        if (!alertClicked) {
////                                                            runnableAlert = new Runnable() {
////                                                                @Override
////                                                                public void run() {
////
////                                                                    dialogShowing = false;
////                                                                    duty_status = DutyStatus.STATUS_ON;
////                                                                    mDutyState = duty_status;
////                                                                    didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
////                                                                    if (runnableAlert != null) {
////                                                                        handlerAlert.removeCallbacks(runnableAlert);
////                                                                        runnableAlert = null;
////                                                                    }
////                                                                    dialog.dismiss();
////                                                                }
////                                                            };
////                                                            handlerAlert.postDelayed(runnableAlert, 1000 * 60 * 1); // should be 1 minute
////                                                        }
////                                                    }
////                                                    obd_status_last_changed = Calendar.getInstance().getTime();
////                                                }
////                                            }
////                                        } // line event not null ENDS
////                                    }*/
//                                });
//
//
//                                if(DataManager.getInstance().isGpxBoolean()){
//                                    DataManager.getInstance().setGpxBoolean(false);
//                                    AvlEvent event = new AvlEvent();
//                                    event = DataManager.getInstance().getEvent();
//                                    {
//                                        Log.e(TAG, "onFinished: " );
//                                        if (event != null) { // line event not null STARTS
//
//                                            if (statusPcOrYmLocal != null && status_checked == 1) {
//                                                Log.e("181017", " Will call CalAmp for ignition status : " + statusPcOrYmLocal + " ==> " + event.ignitionStatus);
//
//                                                // if (event.ignitionStatus.matches("IGOFF")) {
//                                                if (event.igintionStatusGPX == false) {
//                                                    if (statusPcOrYmLocal.matches("PC")) {
//                                                        duty_status = 0;
//                                                        status_checked = 0;
//                                                        didNoteLocationNew(Preferences.mCurrentLocation, "", duty_status);
//                                                    } else if (statusPcOrYmLocal.matches("YM")) {
//                                                        duty_status = 3;
//                                                        status_checked = 0;
//                                                        didNoteLocationNew(Preferences.mCurrentLocation, "", duty_status);
//                                                    }
//                                                }
//
//
//                                            } else {
//
//                                                Log.e("181017", " Will call CalAmp for speed");
//
//                                                Date current = Calendar.getInstance().getTime();
//                                                long diff = (current.getTime() - obd_status_last_changed.getTime()) / 1000;  // This time is in Seconds
//                                                diff = (diff / 60); // This time is in Minutes
//
//                                                long dd = (current.getTime() - obd_status_last_changed.getTime());
//
////                                                if ((mDutyState == DutyStatus.STATUS_ON && event.vbSpeed > 5)) {
//                                                if ((mDutyState == DutyStatus.STATUS_ON && event.status == 1)) {
//                                                    duty_status = DutyStatus.STATUS_DRIVING;
//                                                    mDutyState = duty_status;
//                                                    obd_status_last_changed = Calendar.getInstance().getTime();
//                                                    didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
//
//                                                    if (event.getEngineHours() > 0)
//                                                        updateDutyStatus("" + Preferences.VechileId,
//                                                                "" + event.engineHours, "" + dutystatusID, deviceid);
//
//
//                                                    // Other Buttons will be disabled
//                                                    dutyOff.setDisabled(true);
//                                                    dutySleeper.setDisabled(true);
//                                                    dutyOn.setDisabled(true);
//
//
////                                                } else if (mDutyState == DutyStatus.STATUS_DRIVING && (event.vbSpeed > 0 && event.vbSpeed <= 5)) {
//                                                    // Other Buttons will be disabled
//                                                } else if (mDutyState == DutyStatus.STATUS_DRIVING && event.getStatus() == 1) {
//                                                    dutyOff.setDisabled(true);
//                                                    dutySleeper.setDisabled(true);
//                                                    dutyOn.setDisabled(true);
//                                                    if (event.getEngineHours() > 0)
//                                                        updateDutyStatus("" + Preferences.VechileId,
//                                                                "" + event.engineHours, "" + dutystatusID, deviceid);
//
//                                                } else if (mDutyState == DutyStatus.STATUS_DRIVING && event.vbSpeed == 0 && diff >= 5) {
//
//                                                    // Other Buttons will be enabled
//
//                                                    dutyOff.setEnabled(true);
//                                                    dutySleeper.setEnabled(true);
//                                                    dutyOn.setEnabled(true);
//                                                    dutyDriving.setEnabled(true);
//
//                                                    if (event.getEngineHours() > 0)
//                                                        updateDutyStatus("" + Preferences.VechileId,
//                                                                "" + event.engineHours, "" + dutystatusID, deviceid);
//
//                                                    //boolean alertClicked = false ;
//
//                                                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
//                                                        @Override
//                                                        public void onClick(DialogInterface dialog, int which) {
//                                                            switch (which) {
//                                                                case DialogInterface.BUTTON_POSITIVE:
//                                                                    dialogShowing = false;
//                                                                    alertClicked = true;
//                                                                    duty_status = DutyStatus.STATUS_DRIVING;
//                                                                    mDutyState = duty_status;
//                                                                    didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
//                                                                    if (runnableAlert != null) {
//                                                                        handlerAlert.removeCallbacks(runnableAlert);
//                                                                        runnableAlert = null;
//                                                                    }
//                                                                    break;
//
//                                                                case DialogInterface.BUTTON_NEGATIVE:
//                                                                    dialogShowing = false;
//                                                                    alertClicked = true;
//                                                                    duty_status = DutyStatus.STATUS_ON;
//                                                                    mDutyState = duty_status;
//                                                                    didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
//                                                                    if (runnableAlert != null) {
//                                                                        handlerAlert.removeCallbacks(runnableAlert);
//                                                                        runnableAlert = null;
//                                                                    }
//                                                                    break;
//                                                            }
//                                                        }
//                                                    };
//
//                                                    if (!dialogShowing) {
//                                                        dialogShowing = true;
//                                                        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
//                                                        final AlertDialog dialog = builder.setMessage("Are you currently driving?").setPositiveButton("Yes", dialogClickListener)
//                                                                .setNegativeButton("No", dialogClickListener).show();
//
//
//                                                        if (runnableAlert != null) {
//                                                            //+ Calendar.getInstance().getTime()
//                                                            handlerAlert.removeCallbacks(runnableAlert);
//                                                            runnableAlert = null;
//                                                        }
//
//                                                        if (!alertClicked) {
//                                                            runnableAlert = new Runnable() {
//                                                                @Override
//                                                                public void run() {
//
//                                                                    dialogShowing = false;
//                                                                    duty_status = DutyStatus.STATUS_ON;
//                                                                    mDutyState = duty_status;
//                                                                    didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
//                                                                    if (runnableAlert != null) {
//                                                                        handlerAlert.removeCallbacks(runnableAlert);
//                                                                        runnableAlert = null;
//                                                                    }
//                                                                    dialog.dismiss();
//                                                                }
//                                                            };
//                                                            handlerAlert.postDelayed(runnableAlert, 1000 * 60 * 1); // should be 1 minute
//                                                        }
//                                                    }
//                                                    obd_status_last_changed = Calendar.getInstance().getTime();
//                                                }else if (event.status == 2 ){
//                                                    if(mDutyState == DutyStatus.STATUS_DRIVING){
//
//                                                        duty_status = DutyStatus.STATUS_ON;
//                                                        mDutyState = duty_status;
//                                                        obd_status_last_changed = Calendar.getInstance().getTime();
//                                                        didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
//                                                        saveEngineHours(""+Preferences.VechileId,""+event.engineHours);
//                                                    }else {
//                                                        saveEngineHours("" + Preferences.VechileId
//                                                                , "" + event.engineHours);
//                                                    }
//                                                }
//                                            }
//                                        } // line event not null ENDS
//                                    }
//                                }
//                                if (v.obdDeviceID.equals(""))
//                                    v.obdDeviceID = "0";
//                                RestTask.trackVehicleOnSwitchOffAndNetworkLoss(mDriverLog.driver_id
//                                        , Integer.parseInt(v.obdDeviceID)
//                                        , mDriverLog.driverlog_id);
//                            //}
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//
//                        // get current time of day
//                        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
//
//                        if (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE) == 0) {
//                            // restart the app
//                            Intent i = getBaseContext().getPackageManager()
//                                    .getLaunchIntentForPackage(getBaseContext().getPackageName());
//                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                            startActivity(i);
//                        }
//
//                        switch (mDutyState) {
//                            case DutyStatus.STATUS_SLEEPER:
//                                mSleeperTime += 1;
//                                break;
//                            case DutyStatus.STATUS_OFF:
//                                mOffTime += 1;
//                                break;
//                            case DutyStatus.STATUS_DRIVING:
//                                mDrivingTime += 1;
//                                break;
//                            case DutyStatus.STATUS_ON:
//                                mOnTime += 1;
//                        }
//
//                        if (mOnDutyCount > 0 && mDutyState > DutyStatus.STATUS_SLEEPER) {
//                            mOnDutyCount--;
//                            if (mOnDutyCount < 600 && mOnDutyCount < mDriveCount)
//                                mDriveCount = mOnDutyCount;
//                            if (mOnDutyCount == 0) {
//                                mDriverLog.violations |= DriverLog.VIOLATION_ONDUTY;
//                            }
//                        }
//                        if (mDriveCount > 0 && mDutyState == DutyStatus.STATUS_DRIVING) {
//                            mDriveCount--;
//                            if (mDriveCount == 0) {
//                                mDriverLog.violations |= DriverLog.VIOLATION_DRIVING;
//                            }
//                        }
//                        if (mCycleCount > 0 && mDutyState > DutyStatus.STATUS_SLEEPER) {
//                            mCycleCount--;
//                            if (mCycleCount < 900 && mCycleCount < mOnDutyCount)
//                                mOnDutyCount = mCycleCount;
//                            if (mCycleCount == 0) {
//                                mDriverLog.violations |= DriverLog.VIOLATION_CYCLE;
//                            }
//                        }
//                        updateTimes();
//
//                        posttrip();
//
//                        // Following API will track the device in the case of Switch Off or Lose of Network connectivity
//
//
//                    }
//                }, 0);
//            }
//        };
//
//        timer.schedule(timerTask, 0, 1000 * 60); // should be 60 seconds
//    }


    public void startTimer() {
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "run: timer Running ");
                        Vehicle v = Preferences.getCurrentVehicle();

                        //    Log.e("181017", " Device : " + v.obdDeviceID + "==>" + statusPcOrYmLocal + "<==" + "==>" + status_checked + "<==");

                        try {
                            if (v.obdDeviceID != null) {
                                deviceid = v.obdDeviceID;
                                Log.e(TAG, "run: " + deviceid);
                                connect.fetchAvlEvent(deviceid, new GPSVOXRetrofitConnect.Gpxinterface() {

                                    @Override
                                    public void onFinished(AvlEvent event) {

                                    }
                                });


                                if (DataManager.getInstance().isGpxBoolean()) {
                                    DataManager.getInstance().setGpxBoolean(false);
                                    AvlEvent event = new AvlEvent();
                                    event = DataManager.getInstance().getEvent();
                                    {
                                        Log.e(TAG, "onFinished: " + DataManager.getInstance().getGPXboxEventList().size());
                                        if (DataManager.getInstance().getGPXboxEventList().size() != 0) { // line event not null STARTS

                                            for (int i = 0; i < DataManager.getInstance().getGPXboxEventList().size(); i++) {
                                                // if (DataManager.getInstance().getGPXboxEventList().get(i).getItemList().size() == 0) {
                                                for (int j = 0; j < DataManager.getInstance().getGPXboxEventList().get(i).getItemList().size(); j++) {

                                                    if (statusPcOrYmLocal != null && status_checked == 1) {
                                                        Log.e("181017", " Will call CalAmp for ignition status : " + statusPcOrYmLocal + " ==> " + event.ignitionStatus);

                                                        // if (event.ignitionStatus.matches("IGOFF")) {
                                                        if (DataManager.getInstance().getGPXboxEventList().get(i).getItemList().get(j).isIgnition() == false) {
                                                            if (statusPcOrYmLocal.matches("PC")) {
                                                                duty_status = 0;
                                                                status_checked = 0;
                                                                didNoteLocationNew(Preferences.mCurrentLocation, "", duty_status);
                                                            } else if (statusPcOrYmLocal.matches("YM")) {
                                                                duty_status = 3;
                                                                status_checked = 0;
                                                                didNoteLocationNew(Preferences.mCurrentLocation, "", duty_status);
                                                            }
                                                        }


                                                    } else {
                                                        Log.e("181017", " Will call CalAmp for speed");

                                                       /* diff = (current.getTime() - obd_status_last_changed.getTime()) / 1000;  // This time is in Seconds
                                                        diff = (diff / 60); // This time is in Minutes
                                                        Log.e(TAG, "diff  " + diff);*/

                                                        //   long dd = (current.getTime() - obd_status_last_changed.getTime());
                                                        int speed = DataManager.getInstance().getGPXboxEventList().get(i).getItemList().get(j).getSpeed();
                                                        if ((mDutyState == DutyStatus.STATUS_ON && speed > 5)) {
                                                            // if ((mDutyState == DutyStatus.STATUS_ON && event.status == 1)) {
                                                            duty_status = DutyStatus.STATUS_DRIVING;
                                                            mDutyState = duty_status;
                                                            obd_status_last_changed = Calendar.getInstance().getTime();
                                                            didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);

                                                            if (DataManager.getInstance().getGPXboxEventList().get(i).engineHours > 0)
                                                                updateDutyStatus("" + Preferences.VechileId,
                                                                        "" + DataManager.getInstance().getGPXboxEventList().get(i).engineHours, "" + dutystatusID);

                                                            //   dutyOff.getBackground().setAlpha(255);
                                                            // Other Buttons will be disabled
                                                            dutyOff.setDisabled(true);
                                                            dutySleeper.setDisabled(true);
                                                            dutyOn.setDisabled(true);


                                                        } else if (mDutyState == DutyStatus.STATUS_DRIVING && speed > 0) {
                                                            // Other Buttons will be disabled
//                                                } else if (mDutyState == DutyStatus.STATUS_DRIVING && event.getStatus() == 1) {
                                                            dutyOff.setDisabled(true);
                                                            //  dutyOff.getBackground().setAlpha(255);
                                                            dutySleeper.setDisabled(true);
                                                            dutyOn.setDisabled(true);
                                                            if (DataManager.getInstance().getGPXboxEventList().get(i).engineHours > 0)
                                                                updateDutyStatus("" + Preferences.VechileId,
                                                                        "" + DataManager.getInstance().getGPXboxEventList().get(i).engineHours, "" + dutystatusID);

                                                        } else if (mDutyState == DutyStatus.STATUS_DRIVING && speed == 0) {
                                                            Date current = Calendar.getInstance().getTime();
                                                            // Other Buttons will be enabled
                                                            diff = (current.getTime() - obd_status_last_changed.getTime()) / 1000;  // This time is in Seconds
                                                            diff = (diff / 60); // This time is in Minutes


                                                            if (DataManager.getInstance().getGPXboxEventList().get(i).engineHours > 0) {
                                                                if (DataManager.getInstance().getGPXboxEventList().get(i).engineHours > 0)
                                                                    updateDutyStatus("" + Preferences.VechileId,
                                                                            "" + DataManager.getInstance().getGPXboxEventList().get(i).engineHours,
                                                                            "" + dutystatusID);
                                                            }

//
//                                                            if (diff == 7) {
//                                                                diff = 0;
//                                                            }
                                                            Log.e(TAG, "diff  " + diff);
                                                            if (diff > 5) {
                                                                Log.e(TAG, "diff in IF  " + diff);
                                                                //    diff = 0;
                                                                if (diff > 6) {
                                                                    diff = 0;
                                                                }
                                                                dutyOff.getBackground().setAlpha(255);
                                                                dutyOff.setEnabled(true);
                                                                dutySleeper.setEnabled(true);
                                                                dutyOn.setEnabled(true);
                                                                dutyDriving.setEnabled(true);


                                                                //boolean alertClicked = false ;

                                                                final int finalI = i;
                                                                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialog, int which) {
                                                                        switch (which) {
                                                                            case DialogInterface.BUTTON_POSITIVE:
                                                                                try {
                                                                                    dialogShowing = false;
                                                                                    alertClicked = true;
                                                                                    diff = 0;
                                                                                    duty_status = DutyStatus.STATUS_DRIVING;
                                                                                    mDutyState = duty_status;
                                                                                    //        didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
                                                                                    if (runnableAlert != null) {
                                                                                        handlerAlert.removeCallbacks(runnableAlert);
                                                                                        runnableAlert = null;
                                                                                    }
//                                                                                    updateDutyStatus("" + Preferences.VechileId,
//                                                                                            "" + DataManager.getInstance().getGPXboxEventList().get(finalI).engineHours,
//                                                                                            "" + dutystatusID);
                                                                                } catch (Exception e) {
                                                                                    e.printStackTrace();
                                                                                }
                                                                                break;

                                                                            case DialogInterface.BUTTON_NEGATIVE:
                                                                                try {
                                                                                    dialogShowing = false;
                                                                                    alertClicked = true;
                                                                                    diff = 0;
                                                                                    duty_status = DutyStatus.STATUS_ON;

                                                                                    mDutyState = duty_status;
                                                                                    didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
                                                                                    if (runnableAlert != null) {
                                                                                        handlerAlert.removeCallbacks(runnableAlert);
                                                                                        runnableAlert = null;
                                                                                    }
                                                                                    saveEngineHours("" + Preferences.VechileId
                                                                                            , "" + DataManager.getInstance().getGPXboxEventList().get(finalI).engineHours);
                                                                                } catch (Exception e) {
                                                                                    e.printStackTrace();
                                                                                }
                                                                                break;
                                                                        }
                                                                    }
                                                                };

                                                                if (!dialogShowing) {
                                                                    dialogShowing = true;
                                                                    AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                                                                    final AlertDialog dialog = builder.setMessage("Are you currently driving?").setPositiveButton("Yes", dialogClickListener)
                                                                            .setNegativeButton("No", dialogClickListener).show();


                                                                    if (runnableAlert != null) {
                                                                        //+ Calendar.getInstance().getTime()
                                                                        handlerAlert.removeCallbacks(runnableAlert);
                                                                        runnableAlert = null;
                                                                    }

                                                                    if (!alertClicked) {
                                                                        runnableAlert = new Runnable() {
                                                                            @Override
                                                                            public void run() {
                                                                                diff = 0;
                                                                                dialogShowing = false;
                                                                                duty_status = DutyStatus.STATUS_ON;
                                                                                mDutyState = duty_status;
                                                                                didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
                                                                                if (runnableAlert != null) {
                                                                                    handlerAlert.removeCallbacks(runnableAlert);
                                                                                    runnableAlert = null;
                                                                                }

                                                                                if (DataManager.getInstance().getGPXboxEventList().size() != 0) {
                                                                                    saveEngineHours("" + Preferences.VechileId
                                                                                            , "" + DataManager.getInstance().getGPXboxEventList().get(finalI).engineHours);
                                                                                }

                                                                                dialog.dismiss();
                                                                            }
                                                                        };
                                                                        handlerAlert.postDelayed(runnableAlert, 1000 * 60 * 1); // should be 1 minute
                                                                    }
                                                                }
                                                                obd_status_last_changed = Calendar.getInstance().getTime();
                                                            }
                                                        } else if (DataManager.getInstance().getGPXboxEventList().get(i).status == 2) {
                                                            if (mDutyState == DutyStatus.STATUS_DRIVING) {
                                                                duty_status = DutyStatus.STATUS_ON;
                                                                mDutyState = duty_status;
                                                                obd_status_last_changed = Calendar.getInstance().getTime();
                                                                didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
                                                                saveEngineHours("" + Preferences.VechileId, ""
                                                                        + DataManager.getInstance().getGPXboxEventList().get(i).engineHours);


                                                            } else {
                                                                saveEngineHours("" + Preferences.VechileId
                                                                        , ""
                                                                                + DataManager.getInstance().getGPXboxEventList().get(i).engineHours);
                                                            }
                                                        }
                                                    }
                                                }

                                                // line event not null ENDS
                                                // }
                                            }
                                        } else {
                                            Date current = Calendar.getInstance().getTime();
//                                            long diff1 = (current.getTime() - obd_status_last_changed.getTime()) / 1000;  // This time is in Seconds
//                                            diff1 = (diff1 / 60); // This time is in Minutes
//                                            Log.e(TAG, "diff1  " + diff1);

                                            // This time is in Minutes
                                            if (mDutyState == DutyStatus.STATUS_DRIVING) {
                                                diff1 = (current.getTime() - obd_status_last_changed.getTime()) / 1000;  // This time is in Seconds
                                                diff1 = (diff1 / 60); // This time is in Minutes

                                                Log.e(TAG, "diff1" + diff1);

                                                if (diff1 > 5) {
                                                    Log.e(TAG, "diff1 in IF" + diff1);
                                                    if (diff1 > 6) {
                                                        diff1 = 0;
                                                    }
                                                    //   diff = 0;
                                                    dutyOff.setEnabled(true);
                                                    dutySleeper.setEnabled(true);
                                                    dutyOn.setEnabled(true);
                                                    dutyDriving.setEnabled(true);
                                                    //boolean alertClicked = false ;

                                                    // final int finalI = i;
                                                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            switch (which) {
                                                                case DialogInterface.BUTTON_POSITIVE:
                                                                    try {
                                                                        dialogShowing = false;
                                                                        alertClicked = true;
                                                                        duty_status = DutyStatus.STATUS_DRIVING;
                                                                        mDutyState = duty_status;
                                                                        diff1 = 0;
                                                                        //  didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
                                                                        if (runnableAlert != null) {
                                                                            handlerAlert.removeCallbacks(runnableAlert);
                                                                            runnableAlert = null;
                                                                        }
                                                                        // didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
                                                                     /*   updateDutyStatus("" + Preferences.VechileId,
                                                                                "" + DataManager.getInstance().getGPXboxEventList().get(finalI).engineHours,
                                                                                "" + dutystatusID);*/
                                                                    } catch (Exception e) {
                                                                        e.printStackTrace();
                                                                    }
                                                                    break;

                                                                case DialogInterface.BUTTON_NEGATIVE:
                                                                    try {
                                                                        dialogShowing = false;
                                                                        alertClicked = true;
                                                                        diff1 = 0;
                                                                        duty_status = DutyStatus.STATUS_ON;
                                                                        mDutyState = duty_status;
                                                                        didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
                                                                        if (runnableAlert != null) {
                                                                            handlerAlert.removeCallbacks(runnableAlert);
                                                                            runnableAlert = null;
                                                                        }
                                                                        //   didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
                                                                      /*  saveEngineHours("" + Preferences.VechileId
                                                                                , "" + DataManager.getInstance().getGPXboxEventList().get(finalI).engineHours);*/
                                                                    } catch (Exception e) {
                                                                        e.printStackTrace();
                                                                    }
                                                                    break;
                                                            }
                                                        }
                                                    };

                                                    if (!dialogShowing) {
                                                        dialogShowing = true;
                                                        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                                                        final AlertDialog dialog = builder.setMessage("Are you currently driving?").setPositiveButton("Yes", dialogClickListener)
                                                                .setNegativeButton("No", dialogClickListener).show();


                                                        if (runnableAlert != null) {
                                                            //+ Calendar.getInstance().getTime()
                                                            handlerAlert.removeCallbacks(runnableAlert);
                                                            runnableAlert = null;
                                                        }

                                                        if (!alertClicked) {
                                                            runnableAlert = new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    diff1 = 0;
                                                                    dialogShowing = false;
                                                                    duty_status = DutyStatus.STATUS_ON;
                                                                    mDutyState = duty_status;
                                                                    didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);
                                                                    if (runnableAlert != null) {
                                                                        handlerAlert.removeCallbacks(runnableAlert);
                                                                        runnableAlert = null;
                                                                    }
                                                                    dialog.dismiss();

                                                                      /*  saveEngineHours("" + Preferences.VechileId
                                                                                , "" + DataManager.getInstance().getGPXboxEventList().get(finalI).engineHours);*/
                                                                    // didNoteLocation(Preferences.mCurrentLocation, "Changed by ELD", duty_status);

                                                                }
                                                            };
                                                            handlerAlert.postDelayed(runnableAlert, 1000 * 60 * 1); // should be 1 minute
                                                        }
                                                    }
                                                    obd_status_last_changed = Calendar.getInstance().getTime();
                                                }
                                            } else if (mDutyState == DutyStatus.STATUS_YM || mDutyState == DutyStatus.STATUS_PC) {

                                                String ignitiion_value = getIgnitionValue(deviceid);
                                                if (ignitiion_value.equalsIgnoreCase("false")) {
                                                    if (statusPcOrYmLocal.matches("PC")) {
                                                        duty_status = 0;
                                                        status_checked = 0;
                                                        didNoteLocationNew(Preferences.mCurrentLocation, "", duty_status);
                                                    } else if (statusPcOrYmLocal.matches("YM")) {
                                                        duty_status = 3;
                                                        status_checked = 0;
                                                        didNoteLocationNew(Preferences.mCurrentLocation, "", duty_status);
                                                    }
                                                }

                                            }
                                        }
                                    }
                                }
                                if (v.obdDeviceID.equals(""))
                                    v.obdDeviceID = "0";
                                if (v.obdDeviceID.equals("0")) {

                                } else {
                                    RestTask.trackVehicleOnSwitchOffAndNetworkLoss(mDriverLog.driver_id
                                            , Integer.parseInt(v.obdDeviceID)
                                            , mDriverLog.driverlog_id);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // get current time of day
                        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());

                        if (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE) == 0) {
                            // restart the app
                            Intent i = getBaseContext().getPackageManager()
                                    .getLaunchIntentForPackage(getBaseContext().getPackageName());
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);
                        }

                        switch (mDutyState) {
                            case DutyStatus.STATUS_SLEEPER:
                                mSleeperTime += 1;
                                break;
                            case DutyStatus.STATUS_OFF:
                                mOffTime += 1;
                                break;
                            case DutyStatus.STATUS_DRIVING:
                                mDrivingTime += 1;
                                break;
                            case DutyStatus.STATUS_ON:
                                mOnTime += 1;
                        }

                        if (mOnDutyCount > 0 && mDutyState > DutyStatus.STATUS_SLEEPER) {
                            mOnDutyCount--;
                            if (mOnDutyCount < 600 && mOnDutyCount < mDriveCount)
                                mDriveCount = mOnDutyCount;
                            if (mOnDutyCount == 0) {
                                mDriverLog.violations |= DriverLog.VIOLATION_ONDUTY;
                            }
                        }
                        if (mDriveCount > 0 && mDutyState == DutyStatus.STATUS_DRIVING) {
                            mDriveCount--;
                            if (mDriveCount == 0) {
                                mDriverLog.violations |= DriverLog.VIOLATION_DRIVING;
                            }
                        }
                        if (mCycleCount > 0 && mDutyState > DutyStatus.STATUS_SLEEPER) {
                            mCycleCount--;
                            if (mCycleCount < 900 && mCycleCount < mOnDutyCount)
                                mOnDutyCount = mCycleCount;
                            if (mCycleCount == 0) {
                                mDriverLog.violations |= DriverLog.VIOLATION_CYCLE;
                            }
                        }
                        updateTimes();

                        posttrip();

                        // Following API will track the device in the case of Switch Off or Lose of Network connectivity


                    }

                }, 0);
            }
        };

        //timer.schedule(timerTask, 0, 1000 * 60); // should be 60 seconds TODO
        timer.schedule(timerTask, 0, 1000 * 5);
    }

    public void didNoteLocation(String location, String note, int duty_status_updated) {
        if (note.equals("Changed by ELD")) {
            DataManager.getInstance().setTimerecordingorigin("1");
        } else {
            DataManager.getInstance().setTimerecordingorigin("2");
        }
        duty_status = duty_status_updated;
        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
        int curr_time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        if (curr_time > mLastStateTime) {
            final int last_starttime = curr_time;

            //startLoading();
            RestTask.saveNewStatus(0, duty_status, curr_time, location, note, new RestTask.TaskCallbackInterface() {
                @Override
                public void onTaskCompleted(Boolean success, String message) {
                    //stopLoading();

                    if (success) {
                        mDutyState = duty_status;
                        refreshDutyButtons();

                        calculateHosLimits();
                        RestTask.submitHos(mDutyState, mOnDutyCount, mDriveCount, mCycleCount, mLastBreakCount);

                        mLastStateTime = last_starttime;
                    } else {
                        showMessage("Sorry, was unable to set new status: " + message);
                        DataManager.getInstance().hideProgressMessage();
                    }
                }
            });
        } else {
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

                            refreshDutyButtons();
                            calculateHosLimits();
                            RestTask.submitHos(mDutyState, mOnDutyCount, mDriveCount, mCycleCount, mLastBreakCount);
                        } else {
                            showMessage("Sorry, was unable to set new status: " + message);
                            DataManager.getInstance().hideProgressMessage();
                        }
                    }
                });
            } else {
                DutyStatus last_duty = duty_list.get(last_index);
                last_duty.status = duty_status;
                last_duty.location = location;
                last_duty.remark = note;
                RestTask.saveStatus(mDriverLog.driverlog_id, last_duty, new RestTask.TaskCallbackInterface() {
                    @Override
                    public void onTaskCompleted(Boolean success, String message) {
                        if (success) {
                            mDutyState = duty_status;
                            refreshDutyButtons();

                            calculateHosLimits();
                            RestTask.submitHos(mDutyState, mOnDutyCount, mDriveCount, mCycleCount, mLastBreakCount);
                        } else {
                            showMessage("Sorry, was unable to set new status: " + message);
                            DataManager.getInstance().hideProgressMessage();
                        }
                    }
                });
            }
        }
    }

    public void didNoteLocationNew(String location, String note, int duty_status_updated) {
        if (note.equals("Changed by ELD")) {
            DataManager.getInstance().setTimerecordingorigin("1");
        } else {
            DataManager.getInstance().setTimerecordingorigin("2");
        }
        duty_status = duty_status_updated;
        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
        int curr_time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        if (curr_time > mLastStateTime) {
            final int last_starttime = curr_time;

            //startLoading();
            RestTask.saveNewStatus(0, duty_status, curr_time, location, note, new RestTask.TaskCallbackInterface() {
                @Override
                public void onTaskCompleted(Boolean success, String message) {
                    //stopLoading();

                    if (success) {
                        mDutyState = duty_status;
                        refreshDutyButtons();

                        calculateHosLimits();
                        RestTask.submitHos(mDutyState, mOnDutyCount, mDriveCount, mCycleCount, mLastBreakCount);

                        mLastStateTime = last_starttime;
                    } else {
                        showMessage("Sorry, was unable to set new status: " + message);
                        DataManager.getInstance().hideProgressMessage();
                    }
                }
            });
        } else {
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

                            refreshDutyButtons();
                            calculateHosLimits();
                            RestTask.submitHos(mDutyState, mOnDutyCount, mDriveCount, mCycleCount, mLastBreakCount);
                        } else {
                            showMessage("Sorry, was unable to set new status: " + message);
                            DataManager.getInstance().hideProgressMessage();
                        }
                    }
                });
            } else {
                DutyStatus last_duty = duty_list.get(last_index);
                last_duty.status = duty_status;
                last_duty.location = location;
                last_duty.remark = note;
                RestTask.saveStatus(mDriverLog.driverlog_id, last_duty, new RestTask.TaskCallbackInterface() {
                    @Override
                    public void onTaskCompleted(Boolean success, String message) {
                        if (success) {
                            mDutyState = duty_status;
                            refreshDutyButtons();

                            calculateHosLimits();
                            RestTask.submitHos(mDutyState, mOnDutyCount, mDriveCount, mCycleCount, mLastBreakCount);
                        } else {
                            showMessage("Sorry, was unable to set new status: " + message);
                            DataManager.getInstance().hideProgressMessage();
                        }
                    }
                });
            }
        }

        stopTimer();
        startTimer();
    }

    @Override
    public Timer getTimer() {
        return timer;
    }

    @Override
    public void onClick(View v) {
        duty_status = mDutyState;
        String which_btn_clicked = "";

        if (v == dutyOff) {
            duty_status = DutyStatus.STATUS_OFF;
            which_btn_clicked = "OFF";
            Log.e("yash...", " OFF clicked  " + mDutyState);
            if (Preferences.mDriver.pc_status.matches("1")) {
                str = "PC:  Personal Conveyance";
            }
        } else if (v == dutySleeper && !dutySleeper.isDisabled()) {
            duty_status = DutyStatus.STATUS_SLEEPER;
        } else if (v == dutyDriving && !dutyDriving.isDisabled()) {
            duty_status = DutyStatus.STATUS_DRIVING;
        } else if (v == dutyOn && !dutyOn.isDisabled()) {
            duty_status = DutyStatus.STATUS_ON;
            which_btn_clicked = "ON";
            //Log.e("201017",  mDutyState + " ==> " + duty_status + );
            if (Preferences.mDriver.ym_status.matches("1")) {
                str = "YM:  Yard Move";
            }
        }

        if ((mDutyState != duty_status) || str.matches("YM:  Yard Move") || str.matches("PC:  Personal Conveyance")) { // && str.matches("")
            Intent intent = new Intent(HomeActivity.this, NoteLocationActivity.class);
            intent.putExtra("pc_or_ym", str);
            intent.putExtra("which_btn_clicked", which_btn_clicked);
            intent.putExtra("askOnLocation", "1");

            startActivityForResult(intent, REQUEST_NEW_DUTY);
        }
// else if (str.matches("ym")) {
//            Log.e("yash..." , " User want YM mode");
//        }  else if (str.matches("pc")) {
//            Log.e("yash..." , " User want PC mode");
//        }
    }

//    public String showAlert(String msg, final String status) {
//
//        String message = msg ;
//        boolean btn_clicked = false;
//
//        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                switch (which) {
//                    case DialogInterface.BUTTON_POSITIVE:
//                        Log.e("yash... " , "YES clicked");
//                        ym_or_pc_status = status;
//                        break;
//                    case DialogInterface.BUTTON_NEGATIVE:
//                        Log.e("yash... " , "NO clicked");
//                        ym_or_pc_status = "";
//                        break;
//                }
//            }
//        };
//
//
////            if (!posttripflag) {
////                posttripflag = true;
//        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
//        AlertDialog dialog = builder.setMessage(message)
//                .setPositiveButton("Yes", onClickListener)
//                .setNegativeButton("No", onClickListener)
//                .show();
//
//        return ym_or_pc_status ;
//    }

    //  }


    public void updateDutyStatus(final String vechileid, final String enginehours, String dutystatusid) {

        //    DataManager.getInstance().showProgressMessage(HomeActivity.this);


        Log.d("dutystatusid", dutystatusid + "!");

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.get(Preferences.updateDutyStatus("/log/update_duty_status", vechileid, enginehours,
                dutystatusid), new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                if (response != null) {
                    try {
                        Log.e(TAG, "updateDutyStatus: " + response.toString());
                        //DataManager.getInstance().hideProgressMessage();
                        //saveEngineHours(vechileid, enginehours, deviceid);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable
                    , org.json.JSONObject errorResponse) {
                try {
                    Log.e("Connection Response", "onFailure: " + errorResponse.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //  DataManager.getInstance().hideProgressMessage();


            }
        });


    }

    public void saveEngineHours(String vechileid, String enginehours) {

        //   DataManager.getInstance().showProgressMessage(HomeActivity.this);

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.get(Preferences.saveEngineHour("/log/save_engine_hours", vechileid, enginehours), new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                if (response != null) {
                    try {
                        Log.e(TAG, "saveEngineHours: " + response.toString());
                        //     DataManager.getInstance().hideProgressMessage();


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable
                    , org.json.JSONObject errorResponse) {
                try {
                    Log.e("Connection Response", "onFailure: " + errorResponse.toString());
                    DataManager.getInstance().hideProgressMessage();
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });


    }


    public String getIgnitionValue(String deviceID) {

        DataManager.getInstance().showProgressMessage(HomeActivity.this);
        //  DataManager.getInstance().showProgressMessage(CombinedSheetActivity.this);
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.get(Preferences.getOdoMeter("/log/get_ignition_value", deviceID), new JsonHttpResponseHandler() {
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
                            ignition_value = response.optString("ignition_value");
                            // editStartOdo.setText("" + odometer);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable
                    , org.json.JSONObject errorResponse) {
                try {
                    Log.e("Connection Response", "onFailure: " + errorResponse.toString());
                    DataManager.getInstance().hideProgressMessage();
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });

        return ignition_value;
    }
}
