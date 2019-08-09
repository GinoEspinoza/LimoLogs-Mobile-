package com.dotcompliance.limologs;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.FileCallBack;

import java.io.File;

import okhttp3.Call;

public class VehicleDocumentDetailActivity extends LimoBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_document_detail);

        setLeftMenuItem("Back");

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/viewer?url=" + getIntent().getStringExtra(VehicleDocDetailFragment.ARG_ITEM_URL)));
        startActivity(browserIntent);

        /*startLoading();

        OkHttpUtils.get().url(getIntent().getStringExtra(VehicleDocDetailFragment.ARG_ITEM_URL))
                .build()
                .execute(new FileCallBack(Environment.getExternalStorageDirectory().getAbsolutePath() + "/limologs", "temp.pdf") {
            @Override
            public void onError(Call call, Exception e, int id) {
                stopLoading();
            }

            @Override
            public void onResponse(File response, int id) {
                stopLoading();

                initialize();
            }
        });*/
    }

    @Override
    protected void onMenuItemLeft() {
        finish();
    }

    protected void initialize() {
        VehicleDocDetailFragment fragment = new VehicleDocDetailFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.vehicledoc_detail_container, fragment)
                .commit();
    }
}
