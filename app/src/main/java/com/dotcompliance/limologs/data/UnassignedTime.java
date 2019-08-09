package com.dotcompliance.limologs.data;

import java.util.Date;

/**
 * Created by WebMobile on 9/19/2017.
 */

public class UnassignedTime {

    public int un_id;
    public String un_vehicle_id;
//    public int un_driver_id;
    public String un_starttime;
    //
    public int un_duty;
    public String un_endtime;

    public boolean is_checked = false;

    public boolean is_checked() {
        return is_checked;
    }

    public void setIs_checked(boolean is_checked) {
        this.is_checked = is_checked;
    }


}
