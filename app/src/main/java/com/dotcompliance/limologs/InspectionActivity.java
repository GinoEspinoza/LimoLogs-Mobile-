package com.dotcompliance.limologs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.service.LocationUpdateService;

public class InspectionActivity extends LimoBaseActivity {
    private Button btnView;
    private Button btnSendLog;
    private TextView textFmcsaLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inspection_layout);

        initialize();
    }

    protected void initialize() {
        setTitle("Inspection");
        setLeftMenuItem("Back");

        btnView = (Button) findViewById(R.id.button_inspection_view);
        btnView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, TotalPreviewActivity.class);
          //      intent.putExtra("log_index", 0);
                startActivity(intent);
            }
        });

        btnSendLog = (Button) findViewById(R.id.button_inspection_send);
        btnSendLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, SendLogActivity.class));
            }
        });

        WebView webView = (WebView) findViewById(R.id.web_view);
        webView.loadDataWithBaseURL("", "<p><strong>Questions:</strong><br/>" +
                "May a driver use a computer, tablet, or smartphone (that is not an Automatic On-Board Recording Device) to create, electronically sign, and store the record of duty status (RODS)?" +
                "</p>" +
                "<p>" +
                "<strong>Guidance:</strong><br/>" +
                "Yes. A driver may take manual duty-status entries to a computer, tablet, or smartphone program that is used to generate the graph grid and entries for the record of duty status (RODS) or log book, " +
                "provided the electronically-generated display (if any) and output includes the minimum information required by 395.8 and is formatted in accordance with that section. The driver must sign the RODS (manually or electronically) at the end of each 24-hour period to certify that all required entries are true and correct." +
                "</p>" +
                "<p>At the time of an inspection of records by an enforcement official, the driver may display the current and prior seven days RODS to the official on the device's screen.</p>",
                "text/html", "utf-8", null);

        textFmcsaLink = (TextView) findViewById(R.id.text_fmcsa_link);
        textFmcsaLink.setClickable(true);
        textFmcsaLink.setMovementMethod(LinkMovementMethod.getInstance());
        textFmcsaLink.setText(Html.fromHtml("<a href='https://www.gpo.gov/fdsys/pkg/FR-2014-07-10/html/2014-15951.htm'>FMCSA Guidance on Electronic Logs</a>"));
    }

    @Override
    protected void onMenuItemLeft() {
        Preferences.clearSession(mContext);
        Preferences.mDriverLogs.clear();
        Preferences.mVehicleList.clear();
        stopService(new Intent(mContext, LocationUpdateService.class));
        HomeActivity mHomeActivity = new HomeActivity();
        mHomeActivity.stopTimer();
        startActivity(new Intent(mContext, LoginActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        // disable back press
    }

    public void contactClicked(View v) {
        String[] TO = {"info@dotbuscompliance.com"};
        String[] CC = {""};
        Intent emailIntent = new Intent(Intent.ACTION_SEND);

        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_CC, CC);
        try {
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            Log.i("Finished sending email", "");
        }
        catch (android.content.ActivityNotFoundException ex) {
            showMessage("There is no email client installed.");
        }
    }
}
