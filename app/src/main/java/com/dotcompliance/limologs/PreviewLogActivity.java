package com.dotcompliance.limologs;

import android.content.Intent;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.dotcompliance.limologs.adapter.DutyListAdapter;
import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.util.ViewUtils;
import com.dotcompliance.limologs.view.BaseGridView;
import com.dotcompliance.limologs.view.DvirPreview;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class PreviewLogActivity extends LimoBaseActivity {
    private BaseGridView gridView;
    private ListView listDutyView;

    private TextView textDate;
    private TextView textDriver, textCoDriver;
    private TextView textCarrier, textCarrierAddress;
    private TextView textVehicles, textTrailer;
    private TextView textTrips, textTotalMiles;
    private TextView textHomeTerminal;
    private ImageView imgViewDriverSign;

    private DriverLog mData;

    String filePath = "/sdcard/limologs/log.pdf";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_preview_log);

        Intent intent = getIntent();
        mData = Preferences.mDriverLogs.get(intent.getIntExtra("log_index", 0));

        initialize();
    }

    protected void initialize() {
        setLeftMenuItem("Back");
        setRightMenuItem("Send");
        setConnectionStatus(Preferences.isConnected);
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

        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.lay_main_preview);
        for (int i = 0; i < mData.dvirList.size(); i ++) {
            DvirPreview dvirView = new DvirPreview(mContext);
            dvirView.setData(mData.dvirList.get(i));
            dvirView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            mainLayout.addView(dvirView);
        }
    }

    @Override
    protected void onMenuItemLeft() {
        finish();
    }

    @Override
    protected void onMenuItemRight() {
        PdfDocument document = new PdfDocument();

        View content = findViewById(R.id.lay_main_preview);

        int pageNumber = 1;
        Rect contentRect = new Rect(20, 20, 20 + content.getWidth(), 20 + content.getHeight());
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(content.getWidth() + 40,
                content.getHeight() + 20, pageNumber)
                .setContentRect(contentRect).create();

        PdfDocument.Page page = document.startPage(pageInfo);

        content.draw(page.getCanvas());

        document.finishPage(page);

        String pdfName = "log.pdf";

        File folder = new File(Preferences.getAppDirectory());
        if (!folder.exists()) {
            Log.i("Path:", "Creating");
            //folder.mkdir();
            folder.mkdirs();
        }


        File outputFile = new File(Preferences.getAppDirectory(), pdfName);

        Log.i("Path:", outputFile.getPath());
        try {
            outputFile.createNewFile();
            OutputStream out = new FileOutputStream(outputFile);
            document.writeTo(out);
            document.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(mContext, SendLogActivity.class);
        intent.putExtra("log_filename", Preferences.getAppDirectory() + pdfName);
        startActivity(intent);
    }
}
