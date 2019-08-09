package com.dotcompliance.limologs.view;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.dotcompliance.limologs.R;
import com.dotcompliance.limologs.adapter.DutyListAdapter;
import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.util.ViewUtils;

public class DailyLogPreview extends LinearLayout {
    private BaseGridView gridView;
    private ListView listDutyView;
    private TextView textDate;
    private TextView textDriver, textCoDriver;
    private TextView textCarrier, textCarrierAddress;
    private TextView textVehicles, textTrailer;
    private TextView textTrips, textTotalMiles;
    private TextView textHomeTerminal;
    private ImageView imgViewDriverSign;
    private Context mContext;
    private DriverLog mData;
    private Boolean isDvirShown = false;

    public DailyLogPreview(Context context, DriverLog logData, Boolean dvirShown) {
        super(context);
        mContext = context;
        mData = logData;
        Log.e("yash...",  " DailyLogPreview.java");
        isDvirShown = dvirShown;
        init();
    }

    protected void init() {
        inflate(mContext, R.layout.layout_preview_log, this);

        textDate = (TextView) findViewById(R.id.text_date);
        textDriver = (TextView) findViewById(R.id.text_driver);
        textCoDriver = (TextView) findViewById(R.id.text_codriver);
        textCarrier = (TextView) findViewById(R.id.text_carrier);
        textCarrierAddress = (TextView) findViewById(R.id.text_carrier_address);
        textVehicles = (TextView) findViewById(R.id.text_vehicles);
        textTrailer = (TextView) findViewById(R.id.text_trailer);
        textTrips = (TextView) findViewById(R.id.text_trips);
        textTotalMiles = (TextView) findViewById(R.id.text_total_miles);
        textHomeTerminal = (TextView) findViewById(R.id.text_home_terminal);

        gridView = (BaseGridView) findViewById(R.id.grid_daily_log);
        listDutyView = (ListView) findViewById(R.id.list_duties);
        imgViewDriverSign = (ImageView) findViewById(R.id.imgview_driver_sign);

        textDate.setText(mData.log_date);
        textDriver.setText(mData.firstname + " " + mData.lastname);
        textCoDriver.setText(mData.co_driver);
        textCarrier.setText(mData.carrier_name);
        textCarrierAddress.setText(mData.carrier_address);
        textVehicles.setText(mData.vehicle_nums);
        textTrailer.setText(mData.trailer);
        textTrips.setText(mData.trip);
        textTotalMiles.setText(mData.total_miles);
        textHomeTerminal.setText(mData.home_terminal);


        gridView.setData(mData);

        if (mData.signature != null)
            imgViewDriverSign.setImageBitmap(mData.signature);

        DutyListAdapter adapter = new DutyListAdapter(mContext, mData);
        listDutyView.setAdapter(adapter);
        ViewUtils.setListViewHeightBasedOnItems(listDutyView);

        if (isDvirShown && mData.lastDvir != null) {
            LinearLayout mainLayout = (LinearLayout) findViewById(R.id.lay_main_preview);

            DvirPreview dvirView = new DvirPreview(mContext);
            dvirView.setData(mData.lastDvir);
            dvirView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            mainLayout.addView(dvirView);
        }
    }
}
