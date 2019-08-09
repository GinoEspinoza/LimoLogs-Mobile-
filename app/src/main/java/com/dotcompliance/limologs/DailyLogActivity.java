package com.dotcompliance.limologs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dotcompliance.limologs.adapter.DutyListAdapter;
import com.dotcompliance.limologs.adapter.DutyListAdapter2;
import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.DutyStatus;
import com.dotcompliance.limologs.data.DvirLog;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.network.RestTask;
import com.dotcompliance.limologs.util.DataManager;
import com.dotcompliance.limologs.util.ViewUtils;
import com.dotcompliance.limologs.view.DvirPreview;
import com.dotcompliance.limologs.view.SelectableGridView;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class DailyLogActivity extends LimoBaseActivity {
    public SelectableGridView gridView;
    private ListView listviewDuties;
    private Button btnInsertDuty;
    private Button btnEditDuty;
    private Button btnEditForm;
    private TextView textHosAlert1;
    private TextView textHosAlert2;
    private TextView textHosAlert3;

    private int log_index = 0;
    private int selected_vehicle_index = -1;
    private DriverLog mLogData;

    public DutyListAdapter adapter;
    public DutyListAdapter2 adapter1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_daily_log);
        setConnectionStatus(Preferences.isConnected);
        //DataManager.getInstance().setGridselectable(true);
        try {


            if (DataManager.getInstance().isLog()) {
                DataManager.getInstance().setLog(false);
                log_index = DataManager.getInstance().getIndex();
                mLogData = Preferences.mDriverLogs.get(log_index);
            } else {
                Intent intent = getIntent();
                log_index = intent.getIntExtra("log_index", 0);
                mLogData = Preferences.mDriverLogs.get(log_index);
            }

            Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
            SimpleDateFormat sdfServerFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdfServerFormat.setTimeZone(Preferences.getDriverTimezone());
            cal.setTime(sdfServerFormat.parse(mLogData.log_date));// all done

            //cal.add(Calendar.DATE, -log_index);
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, LLL dd", Locale.US);
            sdf.setTimeZone(Preferences.getDriverTimezone());
            setTitle(sdf.format(cal.getTime()));

            initialize();
            checkHosLimits();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void initialize() {
        setLeftMenuItem("Back");
        //  setConnectionStatus(Preferences.isConnected);
        gridView = (SelectableGridView) findViewById(R.id.grid_daily_log);
        listviewDuties = (ListView) findViewById(R.id.listview_duties);
        btnInsertDuty = (Button) findViewById(R.id.button_insert_duty);
        btnEditDuty = (Button) findViewById(R.id.button_edit_duty);
        btnEditForm = (Button) findViewById(R.id.button_edit_form);
        textHosAlert1 = (TextView) findViewById(R.id.text_hos_alert1);
        textHosAlert2 = (TextView) findViewById(R.id.text_hos_alert2);
        textHosAlert3 = (TextView) findViewById(R.id.text_hos_alert3);

        gridView.setGridSelectable(true);
        gridView.setFocusableInTouchMode(false);
        gridView.setFocusable(false);
        if (mLogData != null) {
            gridView.setData(mLogData);
            gridView.setData(mLogData);
            adapter1 = new DutyListAdapter2(mContext, mLogData,mLogData.statusList);
            listviewDuties.setAdapter(adapter1);
       //     ViewUtils.setListViewHeightBasedOnItems(listviewDuties);
        } else {
            Toast.makeText(mContext, "No logs for the date", Toast.LENGTH_SHORT).show();
        }

        listviewDuties.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                gridView.selectDutyAtIndex(position - 1);
            }
        });
        listviewDuties.setSelection(0);


        gridView.setOnSelectDutyListener(new SelectableGridView.OnSelectDutyListener() {
            @Override
            public void onSelectDuty(int index) {
                final int pos = index + 1;
                listviewDuties.setSelection(pos);
                adapter1.notifyDataSetChanged();
              //  ViewUtils.setListViewHeightBasedOnItems(listviewDuties);
            }
        });

        btnEditForm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataManager.getInstance().setGridselectable(false);
                Intent intent = new Intent(mContext, CombinedSheetActivity.class);
                intent.putExtra("log_index", log_index);
                intent.putExtra("vehicle_index", selected_vehicle_index);
                startActivityForResult(intent, 1);
            }
        });

        btnInsertDuty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, EditDutyActivity.class);
                DataManager.getInstance().setInsertDuty(true);
                DataManager.getInstance().setGridselectable(false);
                intent.putExtra("log_index", log_index);
                intent.putExtra("duty_index", -1);
                startActivityForResult(intent, 2);
            }
        });

        btnEditDuty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataManager.getInstance().setGridselectable(false);
                Intent intent = new Intent(mContext, EditDutyActivity.class);
                intent.putExtra("log_index", log_index);
                intent.putExtra("duty_index", gridView.getSelectedIndex());
                startActivityForResult(intent, 2);
            }
        });

        if (mLogData != null) {
            if (mLogData.driverlog_id == 0) { // this part won't be called since user signs previous logs
                btnInsertDuty.setEnabled(false);
                btnEditDuty.setEnabled(false);
                new AlertDialog.Builder(mContext)
                        .setTitle("Warning")
                        .setMessage("Complete the form to continue.")
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(mContext, VehicleListActivity.class);
                                intent.putExtra("log_index", log_index);
                                startActivityForResult(intent, 1);
                            }
                        })
                        .show();
            }


            // load the latest DVIR for the day
            if (mLogData.lastDvir == null) {
                loadLastDvir();
            }
        } else {
            Toast.makeText(mContext, "No Log for the date", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onMenuItemLeft() {
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                btnEditDuty.setEnabled(true);
                btnInsertDuty.setEnabled(true);
            }
        } else if (requestCode == 2) {
            if (resultCode == RESULT_OK)
                adapter1.notifyDataSetChanged();

            // calculate hos hours
            checkHosLimits();
        } else if (mLogData.driverlog_id > 0) {
            btnEditDuty.setEnabled(true);
            btnInsertDuty.setEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.daily_log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_driver_form) {
            DataManager.getInstance().setGridselectable(false);
            Intent intent = new Intent(mContext, TripFormActivity.class);
            intent.putExtra("log_index", log_index);
            startActivityForResult(intent, 1);

            return true;
        } else if (id == R.id.action_add_vehicle) {
            DataManager.getInstance().setGridselectable(false);
            Intent intent = new Intent(mContext, VehicleListActivity.class);
            intent.putExtra("log_index", log_index);
            intent.putExtra("vehicle_index", selected_vehicle_index);
            startActivityForResult(intent, 3);

            return true;
        } else if (id == R.id.action_preview) {
            DataManager.getInstance().setGridselectable(false);
            Intent intent = new Intent(mContext, PreviewLogActivity.class);
            intent.putExtra("log_index", log_index);
            startActivityForResult(intent, 3);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void checkHosLimits() {
        int k = log_index;
        int off_time = 0, driving_time = 0, on_time = 0, off_rest_time = 0;
        boolean rest_break_required = false;
        boolean reset_flag = false;

        while (k < Preferences.mDriverLogs.size()) {
            ArrayList<DutyStatus> list = Preferences.mDriverLogs.get(k).statusList;
            for (int i = list.size() - 1; i >= 0; i--) {
                int duration;
                DutyStatus state = list.get(i);
                duration = state.end_time - state.start_time;

                if (state.status == DutyStatus.STATUS_OFF || state.status == DutyStatus.STATUS_SLEEPER) {
                    off_time += duration;
                    if (off_time >= 30) {
                        off_rest_time = 0;
                    }
                    if (off_time >= Preferences.OFFDUTY_HOUR) {
                        if (k > log_index) {
                            // no violations
                            reset_flag = true;
                            break;
                        } else {
                            driving_time = 0;
                            on_time = 0;
                        }
                    }
                } else if (state.status == DutyStatus.STATUS_DRIVING) {
                    off_time = 0;
                    driving_time += duration;
                    on_time += duration;
                    if (driving_time > 0 && !rest_break_required) off_rest_time += duration;
                } else if (state.status == DutyStatus.STATUS_ON) {
                    on_time += duration;
                    off_time = 0;
                    if (driving_time > 0 && !rest_break_required) off_rest_time += duration;
                }

                if (off_rest_time > 8 * 60)
                    rest_break_required = true;
            }

            if (reset_flag)
                break;
            if (on_time >= Preferences.ONDUTY_HOUR && driving_time >= Preferences.DRIVING_HOUR) {
                break;
            }
            k++;
        }

        int violations = 0;
        if (on_time >= Preferences.ONDUTY_HOUR) { // 15 hour on-duty limit
            textHosAlert1.setVisibility(View.VISIBLE);
            violations += DriverLog.VIOLATION_ONDUTY;
        } else
            textHosAlert1.setVisibility(View.GONE);

        if (driving_time >= Preferences.DRIVING_HOUR) { // 10 hour driving limit
            textHosAlert2.setVisibility(View.VISIBLE);
            violations += DriverLog.VIOLATION_DRIVING;
        } else {
            textHosAlert2.setVisibility(View.GONE);
        }

        if (Preferences.isREST_BREAK && rest_break_required) {
            textHosAlert3.setVisibility(View.VISIBLE);
            violations += DriverLog.VIOLATION_BREAK;
        } else
            textHosAlert3.setVisibility(View.GONE);

        if (violations != mLogData.violations) {
            RestTask.setViolations(mLogData.driverlog_id, violations);
            mLogData.violations = violations;
        }
    }

    private void loadLastDvir() {
        RequestParams params = new RequestParams();
        params.put("limited_date", Preferences.mDriverLogs.get(log_index).log_date);

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.post(Preferences.getUrlWithCredential("/log/last_dvir"), params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
                // startLoading();
                DataManager.getInstance().showProgressMessage(DailyLogActivity.this);
            }

            @Override
            public void onFinish() {
                DataManager.getInstance().hideProgressMessage();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Log.d("save dvir", response.toString(4));
                    int error = response.getInt("error");
                    if (error == 0) {
                        mLogData.lastDvir = new DvirLog(response.getJSONObject("dvir"));
                        for (int i = 0; i < Preferences.mVehicleList.size(); i++) {
                            if (mLogData.lastDvir.vehicle.equals(Preferences.mVehicleList.get(i).vehicle_no)) {
                                selected_vehicle_index = i;
                                break;
                            }
                        }
                        if (DataManager.getInstance().isInsertDuty()) {
                            DataManager.getInstance().setInsertDuty(false);
                            if (mLogData != null) {
                                gridView.setData(mLogData);
                                adapter1 = new DutyListAdapter2(mContext, mLogData,mLogData.statusList);
                                listviewDuties.setAdapter(adapter1);
                                ViewUtils.setListViewHeightBasedOnItems(listviewDuties);
                            } else {
                                Toast.makeText(mContext, "No logs for the date", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.d("last dvir: ", "unexpected response");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                if (throwable != null) {
                    Log.d("Network error", " " + throwable.getMessage());
                    showMessage("Failed to get last duty. " + throwable.getMessage());
                } else {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    showMessage("Network Error: " + statusCode);
                }
            }
        });
    }


}