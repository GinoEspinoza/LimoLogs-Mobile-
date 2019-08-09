package com.dotcompliance.limologs.network;


import android.util.Log;
import android.widget.Toast;

import com.dotcompliance.limologs.data.Defect;
import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.DutyStatus;
import com.dotcompliance.limologs.data.DvirLog;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.data.UnassignedTime;
import com.dotcompliance.limologs.data.Vehicle;
import com.dotcompliance.limologs.data.VehicleDoc;
import com.dotcompliance.limologs.data.VehicleInspect;
import com.dotcompliance.limologs.util.DataManager;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

import static com.dotcompliance.limologs.data.Preferences.API_TOKEN;

public class RestTask {

    public interface TaskCallbackInterface {
        void onTaskCompleted(Boolean success, String message);
    }

    public static void checkForUpdate(final TaskCallbackInterface callbackInterface) {
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("appid", Preferences.LIMO_APP_ID);
        client.get(Preferences.API_BASE_PATH + "/limo/check_update", params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int error = response.getInt("error");
                    if (error == 0) { // login success
                        int latest_version = response.getInt("version");
                        if (latest_version > Preferences.APP_VERSION) {
                            if (callbackInterface != null)
                                callbackInterface.onTaskCompleted(true, "New version " + latest_version + " is available! Please update the app.");
                        } else {
                            if (callbackInterface != null)
                                callbackInterface.onTaskCompleted(false, "No update available");
                        }
                    } else {
                        Log.d("request failed: ", response.getString("message"));
                        if (callbackInterface != null)
                            callbackInterface.onTaskCompleted(false, response.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.d("check_update", "unexpected response");
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, "Unexpected Response");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                try {
                    Log.d("check_update error", errorResponse.toString(4));
                } catch (Exception e) {
                    //e.printStackTrace();
                }
                if (callbackInterface != null)
                    callbackInterface.onTaskCompleted(false, "Error Code " + statusCode);
            }
        });
    }

    public static void downloadVehicleData() {
        downloadVehicleData(null);
    }

    public static void downloadVehicleData(final TaskCallbackInterface callback) {
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.get(Preferences.getUrlWithCredential("/limo/vehicle_list"), new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int error = response.getInt("error");
                    if (error == 0) { // login success
                        Preferences.mVehicleList.clear();

                        JSONArray array = response.getJSONArray("vehicles");
                        for (int i = 0; i < array.length(); i++) {
                            Vehicle vi = new Vehicle();
                            vi.vehicle_id = array.getJSONObject(i).getInt("vehicle_id");
                            vi.vehicle_no = array.getJSONObject(i).getString("vehicle_no");
                            vi.licenses = array.getJSONObject(i).getString("licenses");
                            vi.vehicle_clsid = array.getJSONObject(i).getJSONObject("vehicleClass").getInt("vehicle_clsid");
                            vi.vehicle_class = array.getJSONObject(i).getJSONObject("vehicleClass").getString("name");

                            try {
                                if (array.getJSONObject(i).has("obdDevice") && !array.getJSONObject(i).isNull("obdDevice")) {
                                    vi.obdDeviceID = array.getJSONObject(i).getJSONObject("obdDevice").getString("deviceId");
                                } else {
                                    vi.obdDeviceID = null;
                                }
                            } catch (Exception e) {
                                Log.d("obdDevice", response.toString());
                                e.printStackTrace();
                            }

                            try {
                                vi.rating = NumberFormat.getNumberInstance().parse(array.getJSONObject(i).getString("rating")).intValue();
                            } catch (Exception e) {
                                vi.rating = 1;
                            }
                            try {
                                vi.gvwr = NumberFormat.getNumberInstance().parse(array.getJSONObject(i).getString("gvwr")).intValue();
                            } catch (ParseException e) {
                                vi.gvwr = 1;
                            }
                            vi.sleeper_on = array.getJSONObject(i).getInt("sleeper_mode") != 0;

                            // vehicle docs
                            JSONArray arrDocs = array.getJSONObject(i).getJSONArray("vehicleDocs");
                            for (int j = 0; j < arrDocs.length(); j++) {
                                JSONObject obj = arrDocs.getJSONObject(j);
                                VehicleDoc doc = new VehicleDoc();
                                doc.id = obj.getString("id");
                                doc.title = obj.getString("title");
                                doc.filename = obj.getString("filename");
                                vi.docs.add(doc);
                            }

                            Preferences.mVehicleList.add(vi);
                        }
                        if (callback != null) callback.onTaskCompleted(true, "");
                    } else {
                        Log.d("request failed: ", response.getString("message"));
                        if (callback != null)
                            callback.onTaskCompleted(false, response.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.d("vehicle list download: ", "unexpected response");
                    e.printStackTrace();
                    if (callback != null) callback.onTaskCompleted(false, "Unexpected Response");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                if (throwable != null) {
                    Log.d("Network error", " " + throwable.getMessage());
                    if (callback != null) callback.onTaskCompleted(false, throwable.getMessage());
                } else {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (callback != null)
                        callback.onTaskCompleted(false, "Error Code " + statusCode);
                }
            }
        });
    }

    /* Body Inspection APIs */

    public static void loadBodyInspection(final Vehicle vi, final TaskCallbackInterface callbackInterface) {
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        String url = Preferences.API_BASE_PATH + "/limo/inspects?vehicle_id=" + vi.vehicle_id;
        client.get(url, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int error = response.getInt("error");
                    if (error == 0) { // login success
                        Preferences.mBodyInspectList.clear();
                        JSONArray array = response.getJSONArray("list");
                        for (int i = 0; i < array.length(); i++) {
                            VehicleInspect inspect = new VehicleInspect();
                            inspect.Id = array.getJSONObject(i).getInt("id");
                            inspect.xPos = array.getJSONObject(i).getInt("coord_x");
                            inspect.yPos = array.getJSONObject(i).getInt("coord_y");
                            inspect.note = array.getJSONObject(i).getString("note");

                            inspect.vehicle = vi;

                            Preferences.mBodyInspectList.add(inspect);
                        }
                        if (callbackInterface != null) callbackInterface.onTaskCompleted(true, "");
                    } else {
                        Log.d("request failed: ", response.getString("message"));
                        if (callbackInterface != null)
                            callbackInterface.onTaskCompleted(false, response.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.d("inspect list download: ", "unexpected response");
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, "Unexpected response");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                if (throwable != null) {
                    Log.d("Network error", "timeout");
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, throwable.getMessage());
                } else {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, "Error Code " + statusCode);
                }
            }
        });
    }

    public static void addNewInspect(int vehicleID, int cx, int cy, String note, String imagePath, final TaskCallbackInterface callbackInterface) {
        MyAsyncHttpClient client = new MyAsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("VehicleInspect[vehicle_id]", vehicleID);
        params.put("VehicleInspect[coord_x]", cx);
        params.put("VehicleInspect[coord_y]", cy);
        params.put("VehicleInspect[note]", note);

        if (imagePath != null) {
            File file = new File(imagePath);
            try {
                params.put("photofile", file);
            } catch (FileNotFoundException e) {
                Log.e("AddInspect", e.getMessage());
            }
        }

        client.post(Preferences.getUrlWithCredential("/limo/add_inspect"), params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int error = response.getInt("error");
                    if (error == 0) {
                        if (callbackInterface != null) callbackInterface.onTaskCompleted(true, "");
                    } else {
                        if (callbackInterface != null)
                            callbackInterface.onTaskCompleted(false, response.getString("message"));
                    }
                } catch (JSONException e) {
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, "Unexpected response from the server.");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                if (throwable != null) {
                    Log.d("Network error", "timeout");
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, throwable.getMessage());
                } else {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, "Error Code " + statusCode);
                }
            }
        });
    }

    public static void fixBodyInspect(int inspect_id, final TaskCallbackInterface callbackInterface) {
        MyAsyncHttpClient client = new MyAsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("inspect_id", inspect_id);

        client.get(Preferences.getUrlWithCredential("/limo/fix_inspect"), params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int error = response.getInt("error");
                    if (error == 0) {
                        if (callbackInterface != null) callbackInterface.onTaskCompleted(true, "");
                    } else {
                        if (callbackInterface != null)
                            callbackInterface.onTaskCompleted(false, response.getString("message"));
                    }
                } catch (JSONException e) {
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, "Unexpected response from the server.");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                if (throwable != null) {
                    Log.d("Network error", " " + throwable.getMessage());
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, throwable.getMessage());
                } else {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, "Error Code " + statusCode);
                }
            }
        });
    }

    public static void downloadDefects() {
        downloadDefects(null);
    }

    public static void downloadDefects(final TaskCallbackInterface callback) {
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.get(Preferences.API_BASE_PATH + "/log/defect_list", new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int error = response.getInt("error");
                    if (error == 0) { // success
                        Preferences.mDefectList.clear();
                        JSONArray array = response.getJSONArray("defects");
                        for (int i = 0; i < array.length(); i++) {
                            Defect defect = new Defect();
                            defect.defect_id = array.getJSONObject(i).getInt("defect_id");
                            defect.defect_name = array.getJSONObject(i).getString("defect_name");

                            Preferences.mDefectList.add(defect);
                        }
                        if (callback != null) callback.onTaskCompleted(true, "");
                    } else {
                        Log.d("request failed: ", response.getString("message"));
                        if (callback != null)
                            callback.onTaskCompleted(false, response.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.d("defect list download: ", "unexpected response");
                    if (callback != null) callback.onTaskCompleted(false, "Unexpected response");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                if (throwable != null) {
                    Log.d("Network error", " " + throwable.getMessage());
                    if (callback != null) callback.onTaskCompleted(false, throwable.getMessage());
                } else {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (callback != null)
                        callback.onTaskCompleted(false, "Error Code " + statusCode);
                }
            }
        });
    }

    public static void downloadLogs() {
        downloadLogs(null, "");
    }

    public static void downloadLogs(final TaskCallbackInterface callback, String status) {
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        RequestParams params = new RequestParams();
        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
        cal.add(Calendar.DATE, -14);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(Preferences.getDriverTimezone());
        params.put("limit_date", sdf.format(cal.getTime()));
        params.put("login", status);

        client.get(Preferences.getUrlWithCredential("/log/get_logs"), params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Log.e("Download Log", "onSuccess: " + response.toString());
                    int error = response.getInt("error");
                    if (error == 0) { // login success
                        Preferences.mDriverLogs.clear();

                        DvirLog lastDvir = null;
                        JSONArray array = response.getJSONArray("logs");
                        JSONArray arrayDvirs = response.getJSONArray("dvirs");
                        int j = 0, k = 0;
                        int totalRecordsForLogs = array.length();
                        for (int i = 0; i < 15; i++) { // Changed this on 16-10-2017,
                            //for (int i = 0; i <= totalRecordsForLogs; i++) {
                            //if()

                            DriverLog log = new DriverLog();
                            // calculate date i days ago
                            Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
                            cal.add(Calendar.DATE, i - 14); // Changed this on 16-10-2017,
                            //cal.add(Calendar.DATE, i - (totalRecordsForLogs - 1));

                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                            sdf.setTimeZone(Preferences.getDriverTimezone());

                            String log_date = sdf.format(cal.getTime());
                            log.log_date = log_date;

                            Log.e("datedriver", "onSuccess: " + log.log_date);


                            if (j < array.length()) {
                                DriverLog temp_log = new DriverLog(array.getJSONObject(j));
                                Log.e("dateapi", "onSuccess: " + temp_log.log_date);
                                if (temp_log.log_date.equals(log_date)) {
                                    j++;
                                    log = temp_log;
                                } else {
                                    continue;
                                }
                            }
                            if (log.statusList.isEmpty()) {
                                if (i == 0) {
                                    log.addNewStatus(0, 0, 0, 0, "", "");
//                                    log.addNewStatus(0, 2, 0, 0, "", "");
                                } else { // duty status from the previous day
                                    try {
//                                        DriverLog last_log = Preferences.mDriverLogs.get(0);
//                                        log.addNewStatus(0, last_log.statusList.get(last_log.statusList.size() - 1).status, 0, 0, "", "");

                                        DriverLog last_log = Preferences.mDriverLogs.get(0);

                                        ArrayList<DutyStatus> states = last_log.statusList;
                                        int count = states.size() - 1;
                                        boolean status = false;
                                        int status1 = 0;

                                        DutyStatus duty = null;
                                        for (int l = count; l <= count && l >= 0; l--) {

                                            if (states.get(l).status == 0 || states.get(l).status == 1 || states.get(l).status == 2 || states.get(l).status == 3
                                                    || states.get(l).status == 4 || states.get(l).status == 5) {
                                                status = true;
                                                duty = states.get(l);
                                                status1 = states.get(l).status;
               /* mLastStateTime = states.get(j).start_time;
                Log.e(TAG, ": " + mDutyState);*/
                                                //   Toast.makeText(mContext, ""+status1, Toast.LENGTH_SHORT).show();
                                                // break;
                                            }

                                            if (status) {
                                                status = false;
                                                break;
                                            }


                                        }
                                        log.addNewStatus(0, status1, 0, 0, "", "");


                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            while (k < arrayDvirs.length()) {
                                DvirLog dvir = new DvirLog(arrayDvirs.getJSONObject(k));
                                lastDvir = dvir;
                                if (dvir.logDate.equals(log_date)) {
                                    log.dvirList.add(dvir);
                                    k++;
                                } else
                                    break;
                            }

                            Preferences.mDriverLogs.add(0, log);
                        }

                        // get last vehicle used
                        if (lastDvir != null) {
                            Preferences.mDriverLogs.get(0).lastDvir = lastDvir;
                            for (int i = 0; i < Preferences.mVehicleList.size(); i++) {
                                if (Preferences.mVehicleList.get(i).vehicle_no.equals(lastDvir.vehicle)) {
                                    Preferences.mVehicleIndex = i;
                                    break;
                                }
                            }
                        }

                        if (callback != null) callback.onTaskCompleted(true, "");
                    } else {
                        Log.d("request failed: ", response.getString("message"));
                        if (callback != null)
                            callback.onTaskCompleted(false, response.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.d("defect list download: ", "unexpected response");
                    if (callback != null) callback.onTaskCompleted(false, "Unexpected response");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                try {
                    Log.d("network error: ", errorResponse.toString(4));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (callback != null)
                    callback.onTaskCompleted(false, "Error Code " + statusCode);
            }
        });
    }

/*    public static void saveNewStatus(final int log_index, int status, int start_time, String location, String remark) {
        saveNewStatus(log_index, status, start_time, location, remark, null);
    }*/

    public static void saveNewStatus(final int log_index, final int status, final int start_time,
                                     final String location, final String remark, final TaskCallbackInterface callbackInterface) {
        if (log_index >= Preferences.mDriverLogs.size()) {
            callbackInterface.onTaskCompleted(false, "Index out of bound");
            return;
        }
        RequestParams params = new RequestParams();
        Log.e("Status", "saveNewStatus:   ----" + DataManager.getInstance().getAllEventResponse());
        params.put("driverlog_id", Preferences.mDriverLogs.get(log_index).driverlog_id);
        params.put("status", status);
        params.put("start_time", start_time);
        params.put("location", location);
        params.put("remark", remark);
        params.put("vehicle_id", Preferences.VechileId);
        params.put("token", Preferences.API_TOKEN);
        params.put("latitude", DataManager.getInstance().getLatitude());
        params.put("longitude", DataManager.getInstance().getLongitude());
        params.put("gps_odometer", DataManager.getInstance().getGpsodometer());
        params.put("vb_odometer", DataManager.getInstance().getVbodometer());
        if (DataManager.getInstance().getTimerecordingorigin().equals(null)) {
            params.put("record_origin", "2");
        } else {
            params.put("record_origin", DataManager.getInstance().getTimerecordingorigin());
        }
        params.put("time", DataManager.getInstance().getTime());
        //
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.post(Preferences.getUrlWithCredential("/log/save_duty"), params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int error = response.getInt("error");
                    if (error == 0) { // login success
                        DataManager.getInstance().setLongitude("");
                        DataManager.getInstance().setLatitude("");
                        int _id = response.getInt("duty_status_id");
                        Preferences.mDriverLogs.get(log_index).addNewStatus(_id, status, start_time, 0, location, remark);
                        if (callbackInterface != null)
                            callbackInterface.onTaskCompleted(true, "");
                    } else {
                        Log.d("request failed: ", response.getString("message"));
                        if (callbackInterface != null)
                            callbackInterface.onTaskCompleted(false, response.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.d("save duty: ", "unexpected response");
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, "Unexpected response");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                if (throwable != null) {
                    Log.d("Network error", " " + throwable.getMessage());
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, throwable.getMessage());
                } else {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, "Error Code " + statusCode);
                }
            }
        });
    }

    public static void saveStatus(int driverlog_id, final DutyStatus state, final TaskCallbackInterface callbackInterface) {
        RequestParams params = new RequestParams();
        if (state.Id != 0)
            params.put("duty_status_id", state.Id);
        Log.e("Status", "saveNewStatus: ffff   ----" + DataManager.getInstance().getAllEventResponse());
        params.put("driverlog_id", driverlog_id);
        params.put("status", state.status);
        params.put("start_time", state.start_time);
        params.put("location", state.location);
        params.put("remark", state.remark);
        params.put("vehicle_id", Preferences.VechileId);
        params.put("token", Preferences.API_TOKEN);
        params.put("latitude", DataManager.getInstance().getLatitude());
        params.put("longitude", DataManager.getInstance().getLongitude());
        params.put("gps_odometer", DataManager.getInstance().getGpsodometer());
        params.put("vb_odometer", DataManager.getInstance().getVbodometer());
        if (DataManager.getInstance().getTimerecordingorigin().equals(null)) {
            params.put("record_origin", "2");
        } else {
            params.put("record_origin", DataManager.getInstance().getTimerecordingorigin());
        }
        params.put("time", DataManager.getInstance().getTime());

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.post(Preferences.getUrlWithCredential("/log/save_duty"), params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int error = response.getInt("error");
                    if (error == 0) { // login success
                        state.Id = response.getInt("duty_status_id");
                        if (callbackInterface != null)
                            callbackInterface.onTaskCompleted(true, "");
                    } else {
                        Log.d("request failed: ", response.getString("message"));
                        if (callbackInterface != null)
                            callbackInterface.onTaskCompleted(false, response.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.d("save duty: ", "unexpected response");
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, "Unexpected response");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                if (throwable != null) {
                    Log.d("Network error", " " + throwable.getMessage());
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, throwable.getMessage());
                } else {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, "Error Code " + statusCode);
                }
            }
        });
    }

    public static void removeDuty(int duty_status_id, final TaskCallbackInterface callbackInterface) {
        if (duty_status_id == 0) {
            if (callbackInterface != null) callbackInterface.onTaskCompleted(true, "");
            return;
        }
        RequestParams params = new RequestParams();
        params.put("duty_status_id", duty_status_id);
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.get(Preferences.getUrlWithCredential("/log/remove_duty"), params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int error = response.getInt("error");
                    if (error == 0) { // login success
                        if (callbackInterface != null)
                            callbackInterface.onTaskCompleted(true, "");
                    } else {
                        Log.d("request failed: ", response.getString("message"));
                        if (callbackInterface != null)
                            callbackInterface.onTaskCompleted(false, response.getString("message"));
                    }
                } catch (JSONException e) {
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, "Unexpected response");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                if (throwable != null) {
                    Log.d("Network error", " " + throwable.getMessage());
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, throwable.getMessage());
                } else {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (callbackInterface != null)
                        callbackInterface.onTaskCompleted(false, "Error Code " + statusCode);
                }
            }
        });
    }

    public static void notifyViolation(int driverlog_id, int violation_type) {
        if (driverlog_id < 1) {
            Log.w("RestTask", "Driverlog ID cannot be zero!");
            return;
        }
        RequestParams params = new RequestParams();
        params.put("driverlog_id", driverlog_id);
        params.put("violation_type", violation_type);
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.get(Preferences.getUrlWithCredential("/limo/notify_violation"), params, new TextHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.d("violation", responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                Log.d("violation", responseString);
            }
        });
    }

    public static void setViolations(int driverlog_id, int violations) {
        if (driverlog_id < 1) {
            Log.w("RestTask", "Driverlog ID cannot be zero!");
            return;
        }
        RequestParams params = new RequestParams();
        params.put("driverlog_id", driverlog_id);
        params.put("violations", violations);
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.get(Preferences.getUrlWithCredential("/log/set_violation"), params, new TextHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.d("violation", responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                Log.d("violation", responseString);
            }
        });
    }

    public static void submitHos(int state, int onduty, int driving, int cycle, int off_break) {
        RequestParams params = new RequestParams();
        params.put("state", state);
        params.put("onduty", onduty);
        params.put("driving", driving);
        params.put("cycle", cycle);
        params.put("off_break", off_break);

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.post(Preferences.getUrlWithCredential("/log/set_hos"), params, new TextHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {

            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {

            }
        });
    }

    /******************************/
    /**
     * Methods for mechanic app
     **/

    public static void downloadDvirList() {
        downloadDvirList(null);
    }

    public static void downloadDvirList(final TaskCallbackInterface callback) {
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.get(Preferences.getUrlWithCredential("/mechanic/dvir_list"), new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int error = response.getInt("error");
                    if (error == 0) {
                        JSONArray arrDvirs = response.getJSONArray("list");
                        if (Preferences.mDvirList == null)
                            Preferences.mDvirList = new ArrayList<>();
                        else
                            Preferences.mDvirList.clear();
                        for (int i = 0; i < arrDvirs.length(); i++) {
                            DvirLog dvir = new DvirLog(arrDvirs.getJSONObject(i));
                            Preferences.mDvirList.add(dvir);
                        }
                        if (callback != null)
                            callback.onTaskCompleted(true, "");
                    } else {
                        Log.d("request failed: ", response.getString("message"));
                        if (callback != null)
                            callback.onTaskCompleted(false, response.getString("message"));
                    }
                } catch (JSONException e) {
                    if (callback != null) callback.onTaskCompleted(false, "Unexpected response");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                if (throwable != null) {
                    Log.d("Network error", " " + throwable.getMessage());
                    if (callback != null) callback.onTaskCompleted(false, throwable.getMessage());
                } else {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (callback != null)
                        callback.onTaskCompleted(false, "Error Code " + statusCode);
                }
            }
        });
    }

    public static void loadCompanyVehicleList(final TaskCallbackInterface callback) {
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.get(Preferences.getUrlWithCredential("/mechanic/vehicle_list"), new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int error = response.getInt("error");
                    if (error == 0) { // login success
                        Preferences.mVehicleList.clear();
                        JSONArray array = response.getJSONArray("vehicles");
                        for (int i = 0; i < array.length(); i++) {
                            Vehicle vi = new Vehicle();
                            vi.vehicle_id = array.getJSONObject(i).getInt("vehicle_id");
                            vi.vehicle_no = array.getJSONObject(i).getString("vehicle_no");
                            vi.licenses = array.getJSONObject(i).getString("licenses");
                            vi.vehicle_clsid = array.getJSONObject(i).getJSONObject("vehicleClass").getInt("vehicle_clsid");
                            vi.vehicle_class = array.getJSONObject(i).getJSONObject("vehicleClass").getString("name");
                            vi.rating = array.getJSONObject(i).getInt("rating");
                            vi.sleeper_on = array.getJSONObject(i).getInt("sleeper_mode") != 0;

                            vi.inspect_count = array.getJSONObject(i).getJSONArray("vehicleInspects").length();

                            Preferences.mVehicleList.add(vi);
                        }
                        if (callback != null) callback.onTaskCompleted(true, "");
                    } else {
                        Log.d("request failed: ", response.getString("message"));
                        if (callback != null)
                            callback.onTaskCompleted(false, response.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.d("vehicle list download: ", "unexpected response");
                    if (callback != null) callback.onTaskCompleted(false, "Unexpected Response");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                if (throwable != null) {
                    Log.d("Network error", " " + throwable.getMessage());
                    if (callback != null) callback.onTaskCompleted(false, throwable.getMessage());
                } else {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (callback != null)
                        callback.onTaskCompleted(false, "Error Code " + statusCode);
                }
            }
        });
    }

    public static void commitLocation(double lat, double lng, String address) {
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("latitude", lat);
        params.put("longitude", lng);
        params.put("location", address);
        if (Preferences.getCurrentVehicle() != null)
            params.put("vehicle_num", Preferences.getCurrentVehicle().vehicle_no);
        else
            params.put("vehicle_num", "");

        client.post(Preferences.getUrlWithCredential("/log/update_location"), params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.i("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("location update", "success");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                if (throwable != null) {
                    Log.d("location error", " " + throwable.getMessage());
                } else {
                    try {
                        Log.d("location error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public static void getAssignedItems(final TaskCallbackInterface callbackInterface) {
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        String url = Preferences.getUrlWithCredential("/limo/assigned_items");

        client.get(url, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                super.onStart();
                Log.i("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Preferences.unassignedTimes.clear();

                    JSONArray array = response.getJSONArray("list");

                    for (int i = 0; i < array.length(); i++) {
                        UnassignedTime time = new UnassignedTime();
                        JSONObject object = array.getJSONObject(i);
                        /////
                        time.un_id = object.getInt("id");
                        time.un_duty = object.getInt("duty");
                        time.un_starttime = object.getString("started_at");
                        time.un_endtime = object.getString("ended_at");
                        time.un_vehicle_id = object.getString("vehicle_id");

                        Preferences.unassignedTimes.add(time);
                    }

                    if (Preferences.unassignedTimes.size() != 0)
                        callbackInterface.onTaskCompleted(true, "yes");
                    else callbackInterface.onTaskCompleted(false, "no");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Log.e("Unanssignedlist", "onFailure: ");
            }
        });
    }

    /*UnassignedTime */
    public static void getUnassignedVehicleTimes(final Vehicle vi, final TaskCallbackInterface callbackInterface) {
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        String url = Preferences.getUrlWithCredential("/limo/vehicle_unassigneds");

        RequestParams params = new RequestParams();
        params.put("vehicle_id", vi.vehicle_id);

        client.get(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                super.onStart();
                Log.i("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Preferences.unassignedTimes.clear();

                    JSONArray array = response.getJSONArray("list");

                    for (int i = 0; i < array.length(); i++) {
                        UnassignedTime time = new UnassignedTime();
                        JSONObject object = array.getJSONObject(i);
                        /////
                        time.un_id = object.getInt("id");
                        time.un_duty = object.getInt("duty");
                        time.un_starttime = object.getString("started_at");
                        time.un_endtime = object.getString("ended_at");
                        time.un_vehicle_id = object.getString("vehicle_id");

                        Preferences.unassignedTimes.add(time);
                    }

                    if (Preferences.unassignedTimes.size() != 0)
                        callbackInterface.onTaskCompleted(true, "yes");
                    else callbackInterface.onTaskCompleted(false, "no");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Log.e("Unanssignedlist", "onFailure: ");
            }
        });
    }

    public static void getUnassignedDriverTime(final int driver_id, String str, final TaskCallbackInterface callbackInterface) {
        MyAsyncHttpClient client = new MyAsyncHttpClient();
        String url = Preferences.API_BASE_PATH + "/log/assign_unassign_records"/*?driver_id=" + String.valueOf(driver_id) + "&token=" + API_TOKEN + "&unassign_records=" + str*/;
        RequestParams params = new RequestParams();
        params.put("driver_id", String.valueOf(driver_id));
        params.put("token", API_TOKEN);
        params.put("unassign_records", str);

        client.post(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Log.e("getUnassignedDriverTime", "onSuccess: " + statusCode + "  response  " + response.toString());
              /*  try {
                    Log.e("getUnassignedDriverTime", "onSuccess: "+statusCode +"  response  "+response.toString());
                    Preferences.unassignedTimes.clear();
                    for (int i = 0; i < response.length(); i++) {
                        UnassignedTime time = new UnassignedTime();
                        JSONObject object = response.getJSONObject(i);
                        /////
                        time.un_id = object.getInt("id");
                        time.un_duty = object.getInt("duty");
                        time.un_starttime = object.getString("started_at");
                        time.un_endtime = object.getString("ended_at");
                        time.un_vehicle_id = object.getString("vehicle_id");
                        Preferences.unassignedTimes.add(time);
                    }

                    if (Preferences.unassignedTimes.size() != 0)*/
                    if (Preferences.unassignedTimes.size() != 0)
                        callbackInterface.onTaskCompleted(true, "yes");
                    else callbackInterface.onTaskCompleted(false, "no");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {

            }
        });
    }
    //getUnassignedDriverTime


    public static void trackVehicleOnSwitchOffAndNetworkLoss(int driverId, int deviceId, int driverlogId) {
        Log.e("dave1505", " trackVehicleOnSwitchOffAndNetworkLoss called." + driverId + " -=> " + deviceId + " -=> " + driverlogId + "  ==> " + API_TOKEN);
        MyAsyncHttpClient client = new MyAsyncHttpClient();

        RequestParams params = new RequestParams();
        params.put("driver_id", driverId);
        params.put("device_id", deviceId);
        params.put("driverlog_id", driverlogId);
        params.put("token", API_TOKEN);

        String url = Preferences.API_BASE_PATH + "/log/track_vehicle";
        client.get(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.d("Success :", " track Vehicle On Switch Off And Netwok Loss ");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                try {
                    Log.d("error :", " track Vehicle On Switch Off And Netwok Loss " + errorResponse.toString(4));
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
        });
    }


    public void DataLog(JSONArray array) {
        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
