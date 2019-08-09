package com.dotcompliance.limologs;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.data.VehicleInspect;
import com.dotcompliance.limologs.network.RestTask;
import com.dotcompliance.limologs.util.DataManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AddDamageActivity extends LimoBaseActivity {

    private TextView textViewChecker;
    private EditText editTextComment;
    private Button buttonTakePhoto;
    private ImageView imageView;

    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_CAMERA_PERMISSION = 3;
    static final int REQUEST_STORAGE_PERMISSION = 4;

    static final String IMAGE_PATH = "image_path";

    private String mCurrentPhotoPath = null;
    private int mCoordX, mCoordY;
    private int mVehicleIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_damage);

        if (savedInstanceState != null && savedInstanceState.containsKey(IMAGE_PATH)) {
            mCurrentPhotoPath = savedInstanceState.getString(IMAGE_PATH);
        }

        initialize();

        mCoordX = getIntent().getIntExtra("COORD_X", 0);
        mCoordY = getIntent().getIntExtra("COORD_Y", 0);

        mVehicleIndex = getIntent().getIntExtra("vehicle_index", Preferences.mVehicleIndex);
    }

    @Override
    protected void onMenuItemLeft() {
        finish();
    }

    @Override
    protected void onMenuItemRight() {
        saveDamage();
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
        setTitle("Add Body Damage");
        setLeftMenuItem("Cancel");
        setRightMenuItem("Save");
        setConnectionStatus(Preferences.isConnected);
        textViewChecker = (TextView) findViewById(R.id.textview_checker);
        editTextComment = (EditText) findViewById(R.id.edittext_comment);
        buttonTakePhoto = (Button) findViewById(R.id.button_take_photo);
        imageView = (ImageView) findViewById(R.id.image_view);

        textViewChecker.setText("Checker Name: " + Preferences.mDriver.firstname + " " + Preferences.mDriver.lastname);

        buttonTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                    if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(AddDamageActivity.this, android.Manifest.permission.CAMERA)) {
                            showMessage("You should allow camera permission.");

                        } else {
                            ActivityCompat.requestPermissions(AddDamageActivity.this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                        }
                        return;
                    }
                    dispatchTakePictureIntent();
                }
            }
        });

        if (mCurrentPhotoPath != null) {
            imageView.setImageBitmap(BitmapFactory.decodeFile(mCurrentPhotoPath));
        }

        setupUI(this.getWindow().getDecorView().findViewById(android.R.id.content));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            imageView.setImageBitmap(BitmapFactory.decodeFile(mCurrentPhotoPath));
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

    private void saveDamage() {
        if (mVehicleIndex < 0 || mVehicleIndex >= Preferences.mVehicleList.size()) {
            Toast.makeText(mContext, "Vehicle is not selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        DataManager.getInstance().showProgressMessage(AddDamageActivity.this);
        RestTask.addNewInspect(Preferences.mVehicleList.get(mVehicleIndex).vehicle_id, mCoordX, mCoordY, editTextComment.getText().toString(), mCurrentPhotoPath, new RestTask.TaskCallbackInterface() {
            @Override
            public void onTaskCompleted(Boolean success, String message) {
                DataManager.getInstance().hideProgressMessage();

                if (success) {
                    VehicleInspect vi = new VehicleInspect();
                    vi.note = editTextComment.getText().toString();
                    vi.xPos = mCoordX;
                    vi.yPos = mCoordY;
                    vi.vehicle = Preferences.getCurrentVehicle();
                    vi.vehicle.inspect_count++;

                    Preferences.mBodyInspectList.add(vi);

                    finish();
                }
                else {
                    Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

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
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }
}
