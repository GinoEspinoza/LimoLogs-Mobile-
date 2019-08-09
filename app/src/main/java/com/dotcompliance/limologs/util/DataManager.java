package com.dotcompliance.limologs.util;

import android.app.Activity;
import android.app.Dialog;
import android.text.Html;
import android.view.WindowManager;
import android.widget.TextView;

import com.dotcompliance.limologs.ELD.AvlEvent;
import com.dotcompliance.limologs.R;
import com.dotcompliance.limologs.data.UnassignedTime;

import java.util.ArrayList;

import retrofit2.http.PUT;

/**
 * Created by ADMIN on 24-10-2017.
 */

public class DataManager {
    private boolean isProgressDialogRunning = false;
    private Dialog mDialog;
    private Activity activity;
    public static final DataManager ourInstance = new DataManager();
    public String allEventResponse = "";
    public int index = 0;

    public int getVechileId() {
        return vechileId;
    }

    public void setVechileId(int vechileId) {
        this.vechileId = vechileId;
    }

    public int vechileId = 0;

    public static DataManager getInstance() {
        return ourInstance;
    }

    private String latitude = "";
    private String longitude = "";
    private String gpsodometer = "";
    private String vbodometer = "";
    private String timerecordingorigin = "2";
    private boolean log = false;
    private String classname = "";
    private boolean insertDuty = false;
    private boolean gridselectable = false;

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    private String time = "";

    public DataManager() {
    }

    private ArrayList<UnassignedTime> unassignedList = new ArrayList<UnassignedTime>();
    private ArrayList<AvlEvent> GPXboxEventList = new ArrayList<AvlEvent>();
    private AvlEvent event = new AvlEvent();
    private boolean gpxBoolean = false;


    public void showProgressMessage(Activity dialogActivity) {
        if (isProgressDialogRunning) {
            hideProgressMessage();
        }
        isProgressDialogRunning = true;
        mDialog = new Dialog(dialogActivity, android.R.style.Theme_Translucent_NoTitleBar);
        // mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(R.layout.dialog_loading);

        TextView textView = (TextView) mDialog.findViewById(R.id.textView);
        textView.setText(Html.fromHtml("Please Wait..."));

        WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
        lp.dimAmount = 0.8f;

        mDialog.getWindow().setAttributes(lp);
        mDialog.getWindow()
                .addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        mDialog.setCancelable(false);
        mDialog.setCanceledOnTouchOutside(false);
        try {
            mDialog.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        } catch (Error ex) {
            ex.printStackTrace();
        }

    }

    public void hideMessage() {
        if (mDialog != null)
            mDialog.dismiss();
    }


    public void hideProgressMessage() {
        isProgressDialogRunning = true;
        try {
            if (mDialog != null)
                mDialog.dismiss();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public ArrayList<UnassignedTime> getUnassignedList() {
        return unassignedList;
    }

    public void addbean(UnassignedTime bean) {
        unassignedList.add(bean);
        // return unassignedList;
    }

    public void setUnassignedList(ArrayList<UnassignedTime> unassignedList) {
        this.unassignedList = unassignedList;
    }

    public void ClearList() {
        unassignedList.clear();
    }

    public String getAllEventResponse() {
        return allEventResponse;
    }

    public void setAllEventResponse(String allEventResponse) {
        this.allEventResponse = allEventResponse;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getGpsodometer() {
        return gpsodometer;
    }

    public void setGpsodometer(String gpsodometer) {
        this.gpsodometer = gpsodometer;
    }

    public String getVbodometer() {
        return vbodometer;
    }

    public void setVbodometer(String vbodometer) {
        this.vbodometer = vbodometer;
    }

    public String getTimerecordingorigin() {
        return timerecordingorigin;
    }

    public void setTimerecordingorigin(String timerecordingorigin) {
        this.timerecordingorigin = timerecordingorigin;

    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isLog() {
        return log;
    }

    public void setLog(boolean log) {
        this.log = log;
    }

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public boolean isInsertDuty() {
        return insertDuty;
    }

    public void setInsertDuty(boolean insertDuty) {
        this.insertDuty = insertDuty;
    }

    public boolean isGridselectable() {
        return gridselectable;
    }

    public void setGridselectable(boolean gridselectable) {
        this.gridselectable = gridselectable;
    }

    public AvlEvent getEvent() {
        return event;
    }

    public void setEvent(AvlEvent event) {
        this.event = event;
    }

    public boolean isGpxBoolean() {
        return gpxBoolean;
    }

    public void setGpxBoolean(boolean gpxBoolean) {
        this.gpxBoolean = gpxBoolean;
    }


    public ArrayList<AvlEvent> getGPXboxEventList() {
        return GPXboxEventList;
    }

    public void addGPXBoxbean(AvlEvent bean) {
        GPXboxEventList.add(bean);
        // return unassignedList;
    }

    public void setGPXboxEventList(ArrayList<AvlEvent> GPXboxEventList) {
        this.GPXboxEventList = GPXboxEventList;
    }

    public void ClearGPXboxList() {
        GPXboxEventList.clear();
    }
}

