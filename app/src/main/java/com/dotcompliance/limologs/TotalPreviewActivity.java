package com.dotcompliance.limologs;

import android.content.Intent;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.view.DailyLogPreview;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class TotalPreviewActivity extends LimoBaseActivity {
    LinearLayout layoutInspection;

    ArrayList<DailyLogPreview> listViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_total_preview);

        initialize();
    }

    protected void initialize() {
        setLeftMenuItem("Back");
        setRightMenuItem("Send");
        setConnectionStatus(Preferences.isConnected);
        Log.e("yash...",  " TotalPreviewActivity.java");

        layoutInspection = (LinearLayout)findViewById(R.id.lay_inspection);
        for (int i = 0; i < 8; i++) {
            Log.e("Value", "initialize: "+Preferences.mDriverLogs.toString() );
            if( i >= Preferences.mDriverLogs.size() ) {
                break;
            }
                DailyLogPreview preview = new DailyLogPreview(mContext, Preferences.mDriverLogs.get(i), i == 0);
                preview.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT
                        , LinearLayout.LayoutParams.WRAP_CONTENT));
                layoutInspection.addView(preview);
                listViews.add(preview);

        }
    }

    @Override
    protected void onMenuItemLeft() {
        finish();
    }

    @Override
    protected void onMenuItemRight() {
        /*PdfDocument document = new PdfDocument();

        for (int i = 0; i < 8; i++) {
            int pageNumber = i;
            View content = listViews.get(i);
            Rect contentRect = new Rect(20, 20, 20 + content.getWidth(), 20 + content.getHeight());
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(content.getWidth() + 40,
                    content.getHeight() + 40, pageNumber)
                    .setContentRect(contentRect).create();

            PdfDocument.Page page = document.startPage(pageInfo);

            content.draw(page.getCanvas());

            document.finishPage(page);
        }

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
        }*/

        Intent intent = new Intent(mContext, SendLogActivity.class);
        //intent.putExtra("log_filename", Preferences.getAppDirectory() + pdfName);
        startActivity(intent);
    }
}
