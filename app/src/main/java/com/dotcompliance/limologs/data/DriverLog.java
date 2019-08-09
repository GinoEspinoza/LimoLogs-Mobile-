package com.dotcompliance.limologs.data;

import android.graphics.Bitmap;
import android.util.Log;

import com.dotcompliance.limologs.util.ImageEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class DriverLog {

    public static final int VIOLATION_ONDUTY = 0x01;
    public static final int VIOLATION_DRIVING = 0x02;
    public static final int VIOLATION_CYCLE = 0x04;
    public static final int VIOLATION_BREAK = 0x08;

    public int driverlog_id;
    public int driver_id;
    public String firstname, lastname;
    public String carrier_name, carrier_address;
    public String co_driver = "N/A";
    public String home_terminal = "N/A";
    public String vehicle_nums;
    public String trip;
    public String trailer = "N/A";
    public String total_miles;
    public String log_date;
    public Bitmap signature = null;
    public int violations = 0;

    public ArrayList<DutyStatus> statusList = new ArrayList<>();
    public ArrayList<DvirLog> dvirList = new ArrayList<>();

    public DvirLog lastDvir = null;

    public DriverLog() {
        carrier_name = Preferences.mDriver.company.carrier_name;
        carrier_address = Preferences.mDriver.company.carrier_address;
    }

    public DriverLog(JSONObject json) {
        Log.d("DriverLog",json.toString());
        try {
            driverlog_id = json.getInt("driverlog_id");
            driver_id = json.getInt("driver_id");
            firstname = json.getString("firstname");
            lastname = json.getString("lastname");
            carrier_name = json.getString("carrier_name");
            carrier_address = json.getString("carrier_address");
            co_driver = json.getString("co_driver");
            home_terminal = json.getString("home_terminal");
            vehicle_nums = json.getString("vehicle");
            trip = json.getString("trip");
            trailer = json.getString("trailer");
            total_miles = json.getString("total_miles");
            log_date = json.getString("log_date");
            violations = json.getInt("violations");
            if (json.has("signature"))
                signature = ImageEncoder.decodeBase64(json.getString("signature"));

            JSONArray states = json.getJSONArray("states");
            for (int i = 0; i < states.length(); i ++) {
                DutyStatus duty = new DutyStatus();
                JSONObject obj = states.getJSONObject(i);
                duty.Id = obj.getInt("duty_status_id");
                duty.status = obj.getInt("status");
                duty.start_time = obj.getInt("start_time");
                //duty.end_time = obj.getInt("end_time");
                duty.location = obj.isNull("location") ? "" : obj.optString("location");
                duty.remark = obj.isNull("remark") ? "" : obj.optString("remark");
                statusList.add(duty);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void addNewStatus(int _id, int status, int start_time, int end_time, String location, String remark) {
        statusList.add(new DutyStatus(_id, status, start_time, end_time, location, remark));
    }

    public DutyStatus lastStatus() {
        if (statusList.size() > 0) {
            return statusList.get(statusList.size() - 1);
        }
        return null;
    }
}
