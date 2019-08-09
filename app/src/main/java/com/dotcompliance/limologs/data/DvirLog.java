package com.dotcompliance.limologs.data;


import android.graphics.Bitmap;

import com.dotcompliance.limologs.util.ImageEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class DvirLog {
    public int dvir_id = 0;
    public String firstName;
    public String lastName;
    public String carrierName;
    public String carrierAddress;
    public String vehicle;
    public String location;
    public int startOdometer;
    public int endOdometer;
    public String logDate;
    public String logTime;
    public Bitmap driverSign = null;
    public Bitmap mechanicSign = null;
    public Boolean isDefected = false;
    public int maintenanced;
    public String note;

    public ArrayList<DvirDefect> defectList = new ArrayList<DvirDefect> ();
    public String defects = "";

    public class DvirDefect {
        public String defect_id;
        public String defectName;
        public String comment;

        public DvirDefect(String defect_id, String defect_name, String comment) {
            this.defect_id = defect_id;
            this.defectName = defect_name;
            this.comment = comment;
        }
    }

    public DvirLog() {
        startOdometer = 0;
        endOdometer = 0;
    }

    public DvirLog(JSONObject json) {
        try {
            dvir_id = json.getInt("dvir_id");
            firstName = json.getString("firstname");
            lastName = json.getString("lastname");
            carrierName = json.getString("carrier_name");
            carrierAddress = json.getString("carrier_address");
            vehicle = json.getString("vehicle_no");
            location = json.getString("location");
            startOdometer = json.getInt("start_odometer");
            endOdometer = json.getInt("end_odometer");
            logDate = json.getString("log_date");
            logTime = json.getString("dvir_time");
            maintenanced = json.getInt("maintenanced");
            note = json.getString("note");

            if (maintenanced > 0) {
                defects = json.getString("defects");
            }

            if (json.has("signature"))
                driverSign = ImageEncoder.decodeBase64(json.getString("signature"));
            if (json.has("signature_mechanic"))
                mechanicSign = ImageEncoder.decodeBase64(json.getString("signature_mechanic"));
            isDefected = json.getInt("defect_affected") > 0 ? true: false;

            JSONArray defects = json.getJSONArray("dvirDefects");
            for (int i = 0; i < defects.length(); i ++) {
                String _defect_id = defects.getJSONObject(i).getString("defect_id");
                String _comment = defects.getJSONObject(i).getString("comment");
                String _defect = defects.getJSONObject(i).getJSONObject("defect").getString("defect_name");
                defectList.add(new DvirDefect(_defect_id, _defect, _comment));
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addNewDefect(String id, String d, String c) {
        this.defectList.add(new DvirDefect(id, d, c));
    }
}
