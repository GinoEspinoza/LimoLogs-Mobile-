package com.dotcompliance.limologs;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.dotcompliance.limologs.adapter.DutyListAdapter;
import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.DutyStatus;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.data.UnassignedTime;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.network.RestTask;
import com.dotcompliance.limologs.util.DateUtils;
import com.dotcompliance.limologs.util.ViewUtils;
import com.dotcompliance.limologs.view.BaseGridView;
import com.dotcompliance.limologs.view.EditableGridView;
import com.dotcompliance.limologs.view.SelectableGridView;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import cz.msebera.android.httpclient.Header;

public class UnassignedTimeActivity extends LimoBaseActivity implements View.OnClickListener{

    private FrameLayout gridLayout;
    private EditableGridView gridView;

    private TextView leftKnob;
    private TextView rightKnob;
    private TextView leftTimeTab;
    private TextView rightTimeTab;

    private int driving_start;
    private int driving_end;

    private DriverLog mLogData;

    private int cur_index = 0;
    private int log_index;

    private ListView listviewDuties;
    DutyListAdapter adapter;

    private int vehicle_index;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unassigned_time);

        Button accept = (Button) findViewById(R.id.btn_accept);
        accept.setOnClickListener(this);

        Button decline = (Button) findViewById(R.id.btn_decline);
        decline.setOnClickListener(this);

        listviewDuties = (ListView) findViewById(R.id.unassigned_timelist);

        Intent intent = getIntent();
        vehicle_index = intent.getIntExtra("vehicle_index", 0);

        initialize();

    }

    private void initialize() {
        setLeftMenuItem("Back");
        setRightMenuItem("Next");
        setConnectionStatus(Preferences.isConnected);
        divideTimeRange(Preferences.unassignedTimes);
        if (Preferences.unassignedTimes.size() != 0) {
            populate(cur_index);
            drawGrid();
        }
//        else {
//            goVehiclelist();
//        }
    }

    @Override
    protected void onMenuItemLeft() {
        finish();
    }

    @Override
    protected void onMenuItemRight() {
        goVehiclelist();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_accept:
                replaceDutiesForLog();
//                cur_index++;
//                populate(cur_index);
//                drawGrid();
                gridView.rearrangeDuties();
                if (cur_index == Preferences.unassignedTimes.size()) {
                    goVehiclelist();
                    cur_index = 0;
                }
                break;
            case R.id.btn_decline:
                cur_index++;
                populate(cur_index);
                drawGrid();
                if (cur_index == Preferences.unassignedTimes.size()) {
                    goVehiclelist();
                    cur_index = 0;
                }
                break;
        }
    }

    private void divideTimeRange(ArrayList<UnassignedTime> model){
        ArrayList<UnassignedTime> temp = new ArrayList<>();
        for(int i = 0; i< model.size(); i++){
            Calendar start = DateUtils.stringToTimeZoneCalendar(model.get(i).un_starttime);
            Calendar end = DateUtils.stringToTimeZoneCalendar(model.get(i).un_endtime);
            if (start.get(start.DATE) < end.get(end.DATE)) {
                int diff = end.get(end.DATE) -  start.get(start.DATE) + 1;
                for(int j = 0; j < diff; j++) {
                    UnassignedTime time = new UnassignedTime();
                    if (j == 0) {
                        time.un_starttime = start.get(start.YEAR) + "-" + (start.get(start.MONTH) + 1) + "-" + start.get(start.DATE) + " " + start.get(start.HOUR_OF_DAY) + ":" + start.get(start.MINUTE) + ":" + start.get(start.SECOND);
                        time.un_endtime = start.get(start.YEAR) + "-" + (start.get(start.MONTH) + 1) + "-" + (start.get(start.DATE) + 1) + " " + "0:0:0";
                    } else if (j == diff - 1) {
                        time.un_starttime = end.get(end.YEAR) + "-" + (end.get(end.MONTH) + 1) + "-" + end.get(end.DATE) + " " + "0:0:0";
                        time.un_endtime = end.get(end.YEAR) + "-" + (end.get(end.MONTH) + 1) + "-" + end.get(end.DATE) + " " + end.get(end.HOUR_OF_DAY) + ":" + end.get(end.MINUTE) + ":" + end.get(end.SECOND);
                    } else {
                        start.add(start.DATE, j);
                        time.un_starttime = start.get(start.YEAR) + "-" + (start.get(start.MONTH) + 1) + "-" + start.get(start.DATE) + " " + "0:0:0";
                        time.un_endtime = start.get(start.YEAR) + "-" + (start.get(start.MONTH) + 1) + "-" + (start.get(start.DATE) + 1) + " " + "24:00:00";
                    }
                    /////
                    time.un_id = model.get(i).un_id;
                    time.un_duty = model.get(i).un_duty;
                    if (!time.un_starttime.equals(time.un_endtime)) {
                        temp.add(time);
                    }
                }
            }
            else{
                UnassignedTime time = new UnassignedTime();
                time.un_starttime = start.get(start.YEAR) + "-" + (start.get(start.MONTH) + 1) + "-" + start.get(start.DATE) + " " + start.get(start.HOUR_OF_DAY) + ":" + start.get(start.MINUTE) + ":" + start.get(start.SECOND);
                time.un_endtime = end.get(end.YEAR) + "-" + (end.get(end.MONTH) + 1) + "-" + end.get(end.DATE) + " " + end.get(end.HOUR_OF_DAY) + ":" + end.get(end.MINUTE) + ":" + end.get(end.SECOND);
                /////
                time.un_duty = model.get(i).un_duty;
                time.un_id = model.get(i).un_id;
                temp.add(time);
            }
        }
        Preferences.unassignedTimes.clear();
        Preferences.unassignedTimes.addAll(temp);
    }

    private void populate(int i) {
        if (Preferences.unassignedTimes.size() != 0 && i < Preferences.unassignedTimes.size()) {
            Calendar start = DateUtils.stringToCalendar(Preferences.unassignedTimes.get(i).un_starttime);
            Calendar end = DateUtils.stringToCalendar(Preferences.unassignedTimes.get(i).un_endtime);
            toMinute(start, end);
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
            String date = format1.format(start.getTime());
            for (int j =0; j < Preferences.mDriverLogs.size(); j++) {
                if (Preferences.mDriverLogs.get(j).log_date.equals(date)) {
                    log_index = j ;
                    break;
                }
            }
        }
    }

    private void toMinute(Calendar st, Calendar ed) {
        int st_PM = st.get(st.AM_PM);
        int ed_PM = ed.get(ed.AM_PM);
        int st_date = st.get(st.DATE);
        int ed_date = ed.get(ed.DATE);
        int st_hour = st.get(st.HOUR);
        int ed_hour = ed.get(ed.HOUR);
        int st_min = st.get(st.MINUTE);
        int ed_min = ed.get(ed.MINUTE);

        if (st_PM == 1) st_hour += 12;
        if (ed_PM == 1) ed_hour += 12;

        if (st_date == ed_date) {
            driving_start = st_hour * 60 + st_min;
            driving_end = ed_hour * 60 + ed_min;
        }else {
            if (st_hour == 0 && ed_hour == 0) {
                driving_start = 0;
                driving_end = 1440;
            }else {
                driving_start = st_hour * 60 + st_min;
                if (ed_hour == 0) {
                    driving_end = 1440;
                }else {
                    driving_end = ed_hour * 60 + ed_min;
                }
            }
        }

    }

    private void drawGrid() {

        leftKnob = new TextView(mContext);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(80, 40);
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

        mLogData = Preferences.mDriverLogs.get(log_index);

        String date=" ";
        SimpleDateFormat spf=new SimpleDateFormat("yyyy-MM-dd");
        Date newDate= null;
        try {
            newDate = spf.parse(mLogData.log_date);
            spf= new SimpleDateFormat("EEEE, LLL dd", Locale.US);
            date = spf.format(newDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        setTitle(date);

        gridView = new EditableGridView(mContext, mLogData);
        gridView.setKnobs(leftKnob, rightKnob, leftTimeTab, rightTimeTab);
        gridView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        gridLayout = (FrameLayout) findViewById(R.id.log_frame);
        gridLayout.removeAllViews();
        gridLayout.addView(gridView);
        gridLayout.addView(leftKnob);
        gridLayout.addView(rightKnob);
        gridLayout.addView(leftTimeTab);
        gridLayout.addView(rightTimeTab);

        gridView.setTimeArea(driving_start,driving_end);
        /////
        if (cur_index < Preferences.unassignedTimes.size()) {
            int duty = 2;
            if (Preferences.unassignedTimes.get(cur_index).un_duty == 1) {
                duty = 2;
            } else if (Preferences.unassignedTimes.get(cur_index).un_duty == 0) {
                duty = 3;
            }
            gridView.setDutyState(duty);
        }

        leftTimeTab.setText(String.format("%02d:%02d", driving_start / 60, driving_start % 60));
        rightTimeTab.setText(String.format("%02d:%02d", driving_end / 60, driving_end % 60));
        ViewTreeObserver vto = gridLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updateKnobPosition();
            }
        });

        adapter = new DutyListAdapter(mContext, mLogData);
        listviewDuties.setAdapter(adapter);
    }

    private void updateKnobPosition() {
        Rect rc = gridView.getGraphRect();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins((int) gridView.getXPos(driving_start) - leftKnob.getWidth(), rc.bottom, 0, 0);
        leftKnob.setLayoutParams(params);

        params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins((int)gridView.getXPos(driving_start) - leftTimeTab.getWidth(), rc.top - leftTimeTab.getHeight(), 0, 0);
        leftTimeTab.setLayoutParams(params);

        params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins((int) gridView.getXPos(driving_end), rc.bottom, 0, 0);
        rightKnob.setLayoutParams(params);

        params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins((int) gridView.getXPos(driving_end), rc.top - rightTimeTab.getHeight(), 0, 0);
        rightTimeTab.setLayoutParams(params);
    }

    private void goVehiclelist() {
        startLoading();
        RestTask.loadBodyInspection(Preferences.mVehicleList.get(vehicle_index), new RestTask.TaskCallbackInterface() {
            @Override
            public void onTaskCompleted(Boolean success, String message) {
                stopLoading();
                if (!success) {
                    Log.i("Vehicle", "Failed to get body inspection + " + message);
                }

                Intent intent = new Intent(mContext, LastDvirActivity.class);
                intent.putExtra("log_index", 0);
                intent.putExtra("vehicle_index", vehicle_index);
                startActivity(intent);
                //finish();
            }
        });
    }

    private void replaceDutiesForLog() {
        gridView.rearrangeDuties();

        List<Map<String, String>> duty_list = new ArrayList<>();
        for (int i = 0; i < gridView.mChangedDuties.size(); i++) {
            DutyStatus duty = gridView.mChangedDuties.get(i);
            Map<String, String> item = new HashMap<>();
            item.put("duty_status_id", "" + duty.Id);
            item.put("status", "" + duty.status);
            item.put("start_time", "" + duty.start_time);
            item.put("location", duty.location);
            item.put("remark", duty.remark);
            duty_list.add(item);
        }

        RequestParams params = new RequestParams();
        params.put("driverlog_id", mLogData.driverlog_id);
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
                        mLogData.statusList = gridView.mChangedDuties;
                        clearUnassignedTime();
                        cur_index++;
                        populate(cur_index);
                        drawGrid();
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

    private void clearUnassignedTime() {
        ArrayList<UnassignedTime> list = new ArrayList<>();
        for (int i = 0; i < Preferences.unassignedTimes.size(); i++) {
            UnassignedTime time = new UnassignedTime();
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar start = DateUtils.stringToTimeZone7Calendar(Preferences.unassignedTimes.get(i).un_starttime);
            time.un_starttime = format1.format(start.getTime());
            Calendar end = DateUtils.stringToTimeZone7Calendar(Preferences.unassignedTimes.get(i).un_endtime);
            time.un_endtime = format1.format(end.getTime());
            time.un_id = Preferences.unassignedTimes.get(i).un_id;
            time.un_duty = Preferences.unassignedTimes.get(i).un_duty;

            list.add(time);
        }
        RequestParams params = new RequestParams();
        params.put("vehicle_use_id", list.get(cur_index).un_id);
        params.put("start_time", list.get(cur_index).un_starttime);
        params.put("end_time", list.get(cur_index).un_endtime);

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        String url = Preferences.getUrlWithCredential("/limo/resolve_unassigned");
        client.post(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("UnassignedTime", "success");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }
        });
    }

}
