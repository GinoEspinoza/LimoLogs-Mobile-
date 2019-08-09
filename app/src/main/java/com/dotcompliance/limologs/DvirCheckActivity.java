package com.dotcompliance.limologs;

import android.*;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.dotcompliance.limologs.data.DvirLog;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.util.ImageEncoder;
import com.dotcompliance.limologs.util.ViewUtils;
import com.github.gcacace.signaturepad.views.SignaturePad;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import cz.msebera.android.httpclient.Header;

public class DvirCheckActivity extends LimoBaseActivity {
    private SignaturePad signaturePad;

    private int dvir_index;

    String note;
    Bitmap mBitmapReceipt = null;
    private String mCurrentPhotoPath = null;

    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_CAMERA_PERMISSION = 3;
    static final int REQUEST_STORAGE_PERMISSION = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_dvir_check);

        dvir_index = getIntent().getIntExtra("dvir_index", 0);

        initialize();
    }

    protected void initialize() {
        TextView textDateTime = (TextView) findViewById(R.id.text_date);
        TextView textDriver = (TextView) findViewById(R.id.text_driver);
        TextView textCarrier = (TextView) findViewById(R.id.text_carrier);
        TextView textCarrierAddress = (TextView) findViewById(R.id.text_carrier_address);
        TextView textVehicle = (TextView) findViewById(R.id.text_vehicle);
        TextView textStartOdo = (TextView) findViewById(R.id.text_startodo);
        TextView textEndOdo = (TextView) findViewById(R.id.text_endodo);
        TextView textLocation = (TextView) findViewById(R.id.text_location);
        TextView textNoDefect = (TextView) findViewById(R.id.text_no_defects);
        ListView listviewDefects = (ListView) findViewById(R.id.list_defects);
        signaturePad = (SignaturePad)findViewById(R.id.signature_pad);

        DvirLog data = Preferences.mDvirList.get(dvir_index);
        textDriver.setText(data.firstName + " " + data.lastName);
        textVehicle.setText(data.vehicle);
        textCarrier.setText(data.carrierName);
        textCarrierAddress.setText(data.carrierAddress);
        if (data.startOdometer != 0)
            textStartOdo.setText("" + data.startOdometer);
        else
            textStartOdo.setText("");
        if (data.endOdometer != 0)
            textEndOdo.setText("" + data.endOdometer);
        else
            textEndOdo.setText("");
        textLocation.setText(data.location);
        textDateTime.setText(data.logDate + "\n" + data.logTime);
        // list defects
        if (data.maintenanced > 0) {
            textNoDefect.setText(data.defects);
        }
        else if (data.defectList.size() > 0) {
            MyListViewAdapter adapter = new MyListViewAdapter(mContext, data.defectList);
            listviewDefects.setAdapter(adapter);
            ViewUtils.setListViewHeightBasedOnItems(listviewDefects);
            textNoDefect.setVisibility(View.GONE);
            listviewDefects.setVisibility(View.VISIBLE);
        }
        setConnectionStatus(Preferences.isConnected);
        setLeftMenuItem("Back");
        setRightMenuItem("Save");
    }

    public void clearSignature(View v) {
        signaturePad.clear();
    }

    @Override
    protected void onMenuItemLeft() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onMenuItemRight() {
        if (signaturePad.isEmpty()) {
            showMessage("Please make a signature");
            return;
        }

        final EditText editNote = new EditText(mContext);
        new AlertDialog.Builder(mContext)
                .setTitle("Write a note")
                .setView(editNote)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        note = editNote.getText().toString();

                        new AlertDialog.Builder(mContext)
                                .setTitle("Would you like to add a picture?")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        saveSignature();
                                    }
                                })
                                .create().show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            mBitmapReceipt = BitmapFactory.decodeFile(mCurrentPhotoPath);
            saveSignature();
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
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

    private void saveSignature() {
        final DvirLog data = Preferences.mDvirList.get(dvir_index);

        RequestParams params = new RequestParams();
        params.put("dvir_id", data.dvir_id);
        params.put("signature", ImageEncoder.encodeTobase64(signaturePad.getTransparentSignatureBitmap()));
        params.put("note", note);

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.post(Preferences.getUrlWithCredential("/mechanic/sign_dvir"), params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
                startLoading();
            }

            @Override
            public void onFinish() {
                stopLoading();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int error = response.getInt("error");
                    if (error == 0) {
                        setResult(RESULT_OK);
                        Preferences.mDvirList.remove(dvir_index);
                        finish();
                    } else {
                        Log.d("request failed: ", response.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.d("response error: ", e.getLocalizedMessage());
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                if(errorResponse != null) {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (JSONException e) {
                        showMessage("Network Error: " + statusCode);
                    }
                }
                else {
                    showMessage("Network Error: " + statusCode);
                }
            }
        });
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        mCurrentPhotoPath = image.getAbsolutePath();

        return image;
    }

    private void dispatchTakePictureIntent() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(DvirCheckActivity.this, android.Manifest.permission.CAMERA)) {
                    showMessage("You should allow camera permission.");

                } else {
                    ActivityCompat.requestPermissions(DvirCheckActivity.this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                }
                return;
            }
            dispatchTakePictureIntent();
        }
        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showMessage("Please allow Write Storage permission for this app.");
            }
            else {
                ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
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
                Uri photoURI = Uri.fromFile(photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    public class MyListViewAdapter extends ArrayAdapter<DvirLog.DvirDefect> {
        private Context context;

        public MyListViewAdapter(Context context, ArrayList<DvirLog.DvirDefect> list) {
            super(context, 0, list);
            this.context = context;
        }
        private class ViewHolder {
            TextView txtDefect;
            TextView txtComment;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            LayoutInflater inflater =  LayoutInflater.from(context);

            if (convertView == null)
            {
                convertView = inflater.inflate(R.layout.defectlist_row, null);
                holder = new ViewHolder();
                holder.txtDefect = (TextView) convertView.findViewById(R.id.col_defect_name);
                holder.txtComment = (TextView) convertView.findViewById(R.id.col_defect_comment);
                convertView.setTag(holder);
            }
            else
            {
                holder = (ViewHolder) convertView.getTag();
            }

            DvirLog.DvirDefect defect = getItem(position);
            holder.txtDefect.setText(defect.defectName);
            holder.txtComment.setText(defect.comment);

            return convertView;
        }
    }
}
