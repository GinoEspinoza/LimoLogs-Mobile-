package com.dotcompliance.limologs.view;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.dotcompliance.limologs.LimoBaseActivity;
import com.dotcompliance.limologs.R;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.data.VehicleInspect;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;

import cz.msebera.android.httpclient.Header;

public class LogsheetPhotoDetailActivity extends LimoBaseActivity {

    SubsamplingScaleImageView imageView;

    private String mImageFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logsheet_photo_detail);

        initialize();
    }

    @Override
    protected void onMenuItemLeft() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onMenuItemRight() {
        super.onMenuItemRight();

        savePhoto();
    }

    protected void initialize() {
        if (getIntent().getBooleanExtra("NEW_IMAGE", false)) {
            setRightMenuItem("Save");
            setLeftMenuItem("Cancel");
            setConnectionStatus(Preferences.isConnected);
        }


        else {
            setLeftMenuItem("Back");
        }

        imageView = (SubsamplingScaleImageView) findViewById(R.id.image_logsheet_view);

        if (getIntent().hasExtra("IMAGE_PATH")) {
            mImageFilePath = getIntent().getStringExtra("IMAGE_PATH");
            imageView.setImage(ImageSource.uri(mImageFilePath));
        }
    }

    private void savePhoto() {
        File file = new File(mImageFilePath);
        RequestParams params = new RequestParams();
        try {
            params.put("image", file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        String url = Preferences.getUrlWithCredential("/log/upload_logsheet_photo") + "&driverlog_id=" + Preferences.mDriverLogs.get(0).driverlog_id;

        client.post(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                startLoading();
                Log.d("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                stopLoading();
                try {
                    int error = response.getInt("error");
                    if (error == 0) { //
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Log.d("request failed: ", response.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.d("inspect list download: ", "unexpected response");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                stopLoading();
                if (throwable != null) {
                    Log.d("Network error", " " +  throwable.getMessage());
                    showMessage(throwable.getMessage());
                }
                else {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    showMessage("Request was failed");
                }
            }
        });
    }
}
