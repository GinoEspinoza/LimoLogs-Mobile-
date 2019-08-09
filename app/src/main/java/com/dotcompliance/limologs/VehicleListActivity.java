package com.dotcompliance.limologs;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.network.RestTask;
import com.dotcompliance.limologs.service.LocationUpdateService;
import com.dotcompliance.limologs.util.DataManager;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

import cz.msebera.android.httpclient.Header;

public class VehicleListActivity extends LimoBaseActivity {
    private AppCompatSpinner spnVehicle;

    private ArrayList<String> mArrVehicle = new ArrayList<>();
    private int log_index;
     boolean connectionStatus = false;
    private int is_certify;
    boolean vechileStatus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vehicle_list_layout);
        Intent intent = getIntent();
        log_index = intent.getIntExtra("log_index", 0);
        initialize();
    }

    protected void initialize() {
        setRightMenuItem("Next");

        spnVehicle = (AppCompatSpinner) findViewById(R.id.spin_vehicle);

        if (Preferences.mVehicleList.isEmpty()) {
            RestTask.downloadVehicleData(new RestTask.TaskCallbackInterface() {
                @Override
                public void onTaskCompleted(Boolean success, String message) {
                    if (success)
                        configureSpinner();
                    else
                        showMessage("Load vehicle data failed: " + message);
                }
            });
        } else {
            configureSpinner();
        }

        setConnectionStatus(Preferences.isConnected);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.common_extra, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_recap) {
            startActivity(new Intent(mContext, RecapActivity.class));
        } else if (id == R.id.nav_editlogs) {
            DataManager.getInstance().setClassname("vechile");
            startActivity(new Intent(mContext, LogsListActivity.class));
        } else if (id == R.id.nav_inspection) {
            startActivity(new Intent(mContext, InspectionActivity.class));
            finish();
        } else if (id == R.id.action_sign_out) {
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

    protected void configureSpinner() {
        mArrVehicle.clear();
        mArrVehicle.add("No Vehicle");

        for (int i = 0; i < Preferences.mVehicleList.size(); i++) {
            mArrVehicle.add(Preferences.mVehicleList.get(i).vehicle_no + " / " + Preferences.mVehicleList.get(i).vehicle_class);
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_dropdown_item, mArrVehicle);
        adapter.setDropDownViewResource(R.layout.simple_spinner_item);
        spnVehicle.setAdapter(adapter);
        spnVehicle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {

                   if(mArrVehicle.get(position).toString().equalsIgnoreCase("No Vehicle")){
                      // Toast.makeText(mContext, "", Toast.LENGTH_SHORT).show();
                       vechileStatus = true;
                   }else {
                       vechileStatus = false;
                       Preferences.mSelectedVehicleIndex = position - 1;
                       Preferences.VechileId = Preferences.mVehicleList.get(position - 1).vehicle_id;
                       checkELDConnection();

                   }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        if (Preferences.mVehicleIndex > -1)
            spnVehicle.setSelection(Preferences.mVehicleIndex + 1);
    }

    @Override
    protected void onMenuItemLeft() {
        if (Preferences.isFullyActive) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onMenuItemRight() {
//        if(vechileStatus){
//            setConnectionStatus(false);
//            Toast.makeText(mContext, "Select Vehicle", Toast.LENGTH_SHORT).show();
//        }else {
            vechileStatus = false;
            DataManager.getInstance().showProgressMessage(VehicleListActivity.this);
            int dvir_index = -1;
            final int vehicle_index = spnVehicle.getSelectedItemPosition() - 1;

            Calendar today = Calendar.getInstance();

            long diff = today.getTimeInMillis() - Preferences.getUnassignedTimeLong(VehicleListActivity.this);
            long diffDays = diff / (24 * 60 * 60 * 1000);

            if (vehicle_index < 0) {
//                stopLoading();
                DataManager.getInstance().hideProgressMessage();
                // no vehicle selected, skip
                Intent intent = new Intent(mContext, CombinedSheetActivity.class);
                intent.putExtra("log_index", log_index);
                intent.putExtra("vehicle_index", -1);
                if (log_index == 0) {
                    Preferences.mVehicleIndex = -1;
                }
                startActivity(intent);
                return;
            } else if (vehicle_index == Preferences.mVehicleIndex && diffDays < 1) {
             //   stopLoading();
                DataManager.getInstance().hideProgressMessage();
                if (log_index == 0 && Preferences.mDriverLogs.get(0).driverlog_id != 0) {
                    Intent intent = new Intent(mContext, HomeActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                } else if (log_index == 0 && Preferences.mDriverLogs.get(0).driverlog_id == 0) {
                    RestTask.getUnassignedVehicleTimes(Preferences.mVehicleList.get(vehicle_index), new RestTask.TaskCallbackInterface() {
                        @Override
                        public void onTaskCompleted(Boolean success, String message) {
//                            stopLoading();
                            DataManager.getInstance().hideProgressMessage();
                            if (message.equals("yes")) {
                                Intent intent = new Intent(VehicleListActivity.this, UnassignedDriverActivity.class);
                                intent.putExtra("vehicle_index", vehicle_index);
                                intent.putExtra("log_index", log_index);
                                startActivity(intent);
                            } else if (message.equals("no")) {
                                showLastDvir(vehicle_index);
                            }
                        }
                    });
                }
            } else {
                RestTask.getUnassignedVehicleTimes(Preferences.mVehicleList.get(vehicle_index), new RestTask.TaskCallbackInterface() {
                    @Override
                    public void onTaskCompleted(Boolean success, String message) {
//                        stopLoading();/**/
                        Preferences.saveUnassignedTimeDate(VehicleListActivity.this, Calendar.getInstance().getTimeInMillis());

                        DataManager.getInstance().hideProgressMessage();

                        if (message.equals("yes")) {
                            Intent intent = new Intent(VehicleListActivity.this, UnassignedDriverActivity.class);
                            intent.putExtra("vehicle_index", vehicle_index);
                            startActivity(intent);
                        } else if (message.equals("no")) {
                            showLastDvir(vehicle_index);
                        }

                    }
                });
            }
     //   }

    }

    private void showLastDvir(final int vehicleIndex) {
        startLoading();
        RestTask.loadBodyInspection(Preferences.mVehicleList.get(vehicleIndex), new RestTask.TaskCallbackInterface() {
            @Override
            public void onTaskCompleted(Boolean success, String message) {
                stopLoading();
                if (!success) {
                    Log.i("Vehicle", "Failed to get body inspection + " + message);
                }

                Intent intent = new Intent(mContext, LastDvirActivity.class);
                intent.putExtra("log_index", 0);
                intent.putExtra("vehicle_index", vehicleIndex);
                startActivity(intent);
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        // configureSpinner();
        if (Preferences.mVehicleList.isEmpty()) {
            RestTask.downloadVehicleData(new RestTask.TaskCallbackInterface() {
                @Override
                public void onTaskCompleted(Boolean success, String message) {
                    if (success)
                        configureSpinner();
                    else
                        showMessage("Load vehicle data failed: " + message);
                }
            });
        } else {
            configureSpinner();
        }
    }


    public boolean checkELDConnection() {

        DataManager.getInstance().showProgressMessage(VehicleListActivity.this);
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.get(Preferences.getConnection("/log/check_vehicle_connection",
                ""+Preferences.VechileId),  new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                connectionStatus = true;
                Log.e("Connection Response", "onSuccess: "+response.toString() );
                try{
                    int isConnected = response.optInt("is_connected");
                    if(isConnected ==0)
                        Preferences.isConnected = false;
                    else
                        Preferences.isConnected = true;

                    setConnectionStatus(Preferences.isConnected);
                    DataManager.getInstance().hideProgressMessage();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
//                Log.e("Connection Response", "onFailure: "+errorResponse.toString() );
                connectionStatus = false;
                DataManager.getInstance().hideProgressMessage();
            }
        });

        return connectionStatus;

    }


    public void onSaveDuty() {
//         DutyStatus firstStatus = mDriverLog.statusList.get(0);
//        if (firstStatus.Id == 0) { // save first state
//            RestTask.saveStatus(mDriverLog.driverlog_id, firstStatus, null);
//        }
        DataManager.getInstance().showProgressMessage(VehicleListActivity.this);
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
                            DataManager.getInstance().hideProgressMessage();
                        }
                    }
                });


    }

    public int checkCertification() {

        DataManager.getInstance().showProgressMessage(VehicleListActivity.this);

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
                                startActivity(new Intent(VehicleListActivity.this, CertifyLogsActivity.class));
                                VehicleListActivity.this.finish();
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

}
