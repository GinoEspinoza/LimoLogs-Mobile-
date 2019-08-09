package com.dotcompliance.limologs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.dotcompliance.limologs.adapter.CertifyLogItemRecyleAdapter;
import com.dotcompliance.limologs.adapter.UnAssignedDriverRecyleAdapter;
import com.dotcompliance.limologs.data.AssignDrivertoVechileModel;
import com.dotcompliance.limologs.data.DutyStatus;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.data.UnassignedTime;
import com.dotcompliance.limologs.network.RestTask;
import com.dotcompliance.limologs.util.APIClient;
import com.dotcompliance.limologs.util.APIInterface;
import com.dotcompliance.limologs.util.DataManager;
import com.dotcompliance.limologs.util.DateUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import static com.dotcompliance.limologs.data.Preferences.API_TOKEN;


public class UnassignedDriverActivity extends LimoBaseActivity implements View.OnClickListener {

    public static ListView unassignedTimelist;

    private ArrayList<UnassignedTime> mUnassignedDriverTime;
    private int driving_start;
    private int driving_end;
    private int cur_index = 0;
    UnassignedDriverListRowAdapter1 adapter;

    public static String comma_seperated_chk = "";
    private static String str = "";
    int log_index = 0;
    public static int vehicle_index = 0;
    public static Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unassigned_driver);
        mContext = UnassignedDriverActivity.this;
        DataManager.getInstance().ClearList();
        unassignedTimelist = (ListView) findViewById(R.id.unassigned_timelist);
        Button btnSave = (Button) findViewById(R.id.btn_save);

        Intent intent = getIntent();
        vehicle_index = intent.getIntExtra("vehicle_index", 0);

        initialize();
    }

    private void initialize() {
        setConnectionStatus(Preferences.isConnected);
        setLeftMenuItem("Back");
        setRightMenuItem("Next");

        mUnassignedDriverTime = Preferences.unassignedTimes;


        if (Preferences.unassignedTimes.size() != 0) {
            populate(cur_index);
            //drawGrid();
        }


        LayoutInflater inflater = getLayoutInflater();
        ViewGroup footer = (ViewGroup) inflater.inflate(R.layout.unassigned_driver_footer, unassignedTimelist, false);
        ViewGroup header = (ViewGroup) inflater.inflate(R.layout.unassigned_driver_list_row, unassignedTimelist, false);
        CheckBox check_unassigned_driver = (CheckBox) header.findViewById(R.id.check_unassigned_driver);
        TextView vehicle_name = (TextView) header.findViewById(R.id.vehicle_name);
        TextView col_start_time = (TextView) header.findViewById(R.id.col_start_time);
        TextView col_end_time = (TextView) header.findViewById(R.id.col_end_time);
        TextView txtselect = (TextView) header.findViewById(R.id.txtselect);
        txtselect.setVisibility(View.VISIBLE);
        vehicle_name.setTypeface(null, Typeface.BOLD);
        txtselect.setTypeface(null, Typeface.BOLD);
        col_start_time.setTypeface(null, Typeface.BOLD);
        col_end_time.setTypeface(null, Typeface.BOLD);
        check_unassigned_driver.setVisibility(View.GONE);

        Button button = (Button) footer.findViewById(R.id.btn_save);
        button.setOnClickListener(this);
        unassignedTimelist.addFooterView(footer, null, false);
        unassignedTimelist.addHeaderView(header, null, false);

      /*  UnAssignedDriverRecyleAdapter adapter = new UnAssignedDriverRecyleAdapter(mUnassignedDriverTime,UnassignedDriverActivity.this);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        unassignedTimelist.setLayoutManager(mLayoutManager);
        unassignedTimelist.setItemAnimator(new DefaultItemAnimator());
        unassignedTimelist.setAdapter(adapter);*/

        adapter = new UnassignedDriverListRowAdapter1(mContext, mUnassignedDriverTime);
        unassignedTimelist.setAdapter(adapter);
        adapter.notifyDataSetChanged();


    }

    private void populate(int i) {
        if (Preferences.unassignedTimes.size() != 0 && i < Preferences.unassignedTimes.size()) {
            Calendar start = DateUtils.stringToCalendar(Preferences.unassignedTimes.get(i).un_starttime);
            Calendar end = DateUtils.stringToCalendar(Preferences.unassignedTimes.get(i).un_endtime);
            toMinute(start, end);
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
            String date = format1.format(start.getTime());
            for (int j = 0; j < Preferences.mDriverLogs.size(); j++) {
                if (Preferences.mDriverLogs.get(j).log_date.equals(date)) {
                    log_index = j;
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
        } else {
            if (st_hour == 0 && ed_hour == 0) {
                driving_start = 0;
                driving_end = 1440;
            } else {
                driving_start = st_hour * 60 + st_min;
                if (ed_hour == 0) {
                    driving_end = 1440;
                } else {
                    driving_end = ed_hour * 60 + ed_min;
                }
            }
        }

    }

    @Override
    protected void onMenuItemLeft() {
        finish();
    }

    @Override
    protected void onMenuItemRight() {
        if (vehicle_index > -1) {
            goVehiclelist();
        }
        else {
            Intent intent = new Intent(mContext, VehicleListActivity.class);
            intent.putExtra("log_index", 0);
            startActivity(intent);
            finish();
        }
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

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_save:
                str = "";
                comma_seperated_chk = "";

                for (int i = 0; i < DataManager.getInstance().getUnassignedList().size(); i++) {
                    if (DataManager.getInstance().getUnassignedList().get(i).is_checked == true) {

                        try {
                            comma_seperated_chk = comma_seperated_chk + "," + DataManager.getInstance().getUnassignedList().get(i).un_id; // DataManager.getInstance().getUnassignedList().get(i).un_vehicle_id ;
                            str = comma_seperated_chk.substring(1/*,comma_seperated_chk.length()-1*/);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    //StringUtils.join(slist, ',');
                }
                if (str.isEmpty()) {
                    //Toast.makeText(mContext, "select atleast one check box" , Toast.LENGTH_SHORT).show();
                    Intent intent = getIntent();
                    intent = new Intent(UnassignedDriverActivity.this, LastDvirActivity.class);
                } else {

                    AssignDriverToVechile();

                }

                break;

            default:
                break;
        }
    }


    public static void AssignDriverToVechile() {
        try {
            DataManager.getInstance().showProgressMessage((Activity) mContext);
            final long startTime = System.currentTimeMillis();

            APIInterface apiService =
                    APIClient.getClient().create(APIInterface.class);


            Call<AssignDrivertoVechileModel> call = apiService.AssignDriverToVechile(API_TOKEN,
                    String.valueOf(Preferences.mDriver.driver_id)
                    , str);
            call.enqueue(new Callback<AssignDrivertoVechileModel>() {
                @Override
                public void onResponse(Call<AssignDrivertoVechileModel> call, Response<AssignDrivertoVechileModel> response) {
                    DataManager.getInstance().hideProgressMessage();

                    int statusCode = response.code();
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    System.out.println("Total elapsed http request/response time in milliseconds: " + elapsedTime);
                    Log.e("getPostMethods: ", "elapsedTime" + elapsedTime +"   "+response.toString());
                    if (response.body().getMessage().equalsIgnoreCase("success")){
                        DataManager.getInstance().ClearList();

                        if (vehicle_index > -1) {
                            Intent intent = new Intent(mContext, LastDvirActivity.class);
                            intent.putExtra("vehicle_index", vehicle_index);
                            mContext.startActivity(intent);
                        }
                        else {
                            ArrayList<DutyStatus> states =Preferences.mDriverLogs.get(0).statusList;
                            int i = states.size() - 1;
                            int status1 = 0;
                            for (int j = i; j <= i && j >=0; j--) {

                                if (states.get(j).status == 0 || states.get(j).status == 1 || states.get(j).status == 2 || states.get(j).status == 3
                                        || states.get(j).status == 4 || states.get(j).status == 5) {
                                    status1 = states.get(j).status;
                                    break;
                                }
                            }

                            if (status1 == DutyStatus.STATUS_OFF || status1 == DutyStatus.STATUS_SLEEPER) {
                                mContext.startActivity(new Intent(mContext, AskOnDutyActivity.class));
                            }
                            else {
                                mContext.startActivity(new Intent(mContext, VehicleListActivity.class));
                            }
                        }
                    }

                }

                @Override
                public void onFailure(Call<AssignDrivertoVechileModel> call, Throwable t) {
                    // Log error here since request failed
                    Log.e("Retrofit", t.toString());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
