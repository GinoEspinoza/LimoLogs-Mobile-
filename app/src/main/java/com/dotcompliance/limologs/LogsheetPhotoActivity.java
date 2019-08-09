package com.dotcompliance.limologs;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;

import com.dotcompliance.limologs.adapter.ImageAdapter;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.util.ImageEncoder;
import com.dotcompliance.limologs.view.LogsheetPhotoDetailActivity;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import cz.msebera.android.httpclient.Header;

public class LogsheetPhotoActivity extends LimoBaseActivity {
    private GridView mGridView;

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_ADD_IMAGE = 2;
    static final int REQUEST_CAMERA_PERMISSION = 3;
    static final int REQUEST_STORAGE_PERMISSION = 4;

    static final String IMAGE_PATH = "image_path";

    ArrayList<Hashtable<String, String>> mPhotos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logsheet_photo);

        setTitle("LogSheet Photos");
        setLeftMenuItem("Back");
        setRightMenuItem("Take New");
        setConnectionStatus(Preferences.isConnected);
        initialize();

        loadPhotos();
    }

    @Override
    protected void onMenuItemLeft() {
        finish();
    }

    @Override
    protected void onMenuItemRight() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    showMessage("You should allow camera permission.");

                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                }
                return;
            }
            dispatchTakePictureIntent();
        }
        else {
            showMessage("Camera is not available.", true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mCurrentPhotoPath != null)
            outState.putString(IMAGE_PATH, mCurrentPhotoPath);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(IMAGE_PATH)) {
            mCurrentPhotoPath = savedInstanceState.getString(IMAGE_PATH);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    protected void initialize() {
        mGridView = (GridView) findViewById(R.id.grid_view);

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Bitmap bitmap = ((BitmapDrawable) ((ImageView)view).getDrawable()).getBitmap();
                String path = ImageEncoder.saveToInternalStorage(mContext, bitmap);

                Intent intent = new Intent(mContext, LogsheetPhotoDetailActivity.class);
                intent.putExtra("IMAGE_PATH", path);
                startActivity(intent);
            }
        });
    }

    private void loadPhotos() {
        RequestParams params = new RequestParams();
        params.put("driverlog_id", Preferences.mDriverLogs.get(0).driverlog_id);

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.get(Preferences.getUrlWithCredential("/log/logsheet_photos"), params, new JsonHttpResponseHandler() {
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
                    if (error == 0) {
                        mPhotos.clear();

                        JSONArray array = response.getJSONArray("list");
                        for (int i = 0; i < array.length(); i++) {
                            Hashtable<String, String> dict = new Hashtable<>();
                            dict.put("id", array.getJSONObject(i).getString("id"));
                            dict.put("filename", array.getJSONObject(i).getString("filename"));
                            mPhotos.add(dict);
                        }

                        ImageAdapter adapter = new ImageAdapter(mContext, mPhotos);
                        mGridView.setAdapter(adapter);
                    }
                    else {

                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                stopLoading();
                if (throwable != null) {
                    Log.e("Network error", " " +  throwable.getMessage());
                } else {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void dispatchTakePictureIntent() {
        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showMessage("Please allow Write Storage permission for this app.");
            }
            else {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            }
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                /*Uri photoURI = FileProvider.getUriForFile(this,
                        "com.dotcompliance.fileprovider",
                        photoFile);*/

                Uri photoURI = FileProvider.getUriForFile(mContext, "com.dotcompliance.limologs.fileprovider", photoFile);//Uri.fromFile(photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Intent intent = new Intent(mContext, LogsheetPhotoDetailActivity.class);
            intent.putExtra("IMAGE_PATH", mCurrentPhotoPath);
            intent.putExtra("NEW_IMAGE", true);
            startActivityForResult(intent, REQUEST_ADD_IMAGE);
        }
        else if (requestCode == REQUEST_ADD_IMAGE && resultCode == RESULT_OK) {
            loadPhotos();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION ) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
                return;
            }
        }
        else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        mCurrentPhotoPath = image.getAbsolutePath();

        return image;
    }
}
