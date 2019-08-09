package com.dotcompliance.limologs.data;

import java.util.ArrayList;

public class Vehicle {
    public int vehicle_id;
    public String vehicle_no;
    public int vehicle_clsid;
    public String vehicle_class;
    public String licenses;
    public int rating;
    public int gvwr;
    public boolean sleeper_on = false;

    public int inspect_count = 0;

    public ArrayList<VehicleDoc> docs = new ArrayList<>();

    public String obdDeviceID = "";
}
