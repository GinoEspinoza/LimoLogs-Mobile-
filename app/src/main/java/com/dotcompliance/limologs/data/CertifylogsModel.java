package com.dotcompliance.limologs.data;

/**
 * Created by saurabh on 11/17/2017.
 */

public class CertifylogsModel {

    private String driverlog_id;
    private String log_date;
    private String id;
    private boolean checked = true;


    public String getDriverlog_id() {
        return driverlog_id;
    }

    public void setDriverlog_id(String driverlog_id) {
        this.driverlog_id = driverlog_id;
    }

    public String getLog_date() {
        return log_date;
    }

    public void setLog_date(String log_date) {
        this.log_date = log_date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
