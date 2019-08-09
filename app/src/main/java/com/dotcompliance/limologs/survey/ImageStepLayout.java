package com.dotcompliance.limologs.survey;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dotcompliance.limologs.R;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.util.CameraPreview;

import org.researchstack.backbone.result.StepResult;
import org.researchstack.backbone.step.Step;
import org.researchstack.backbone.ui.callbacks.StepCallbacks;
import org.researchstack.backbone.ui.step.layout.StepLayout;
import org.researchstack.backbone.ui.views.FixedSubmitBarLayout;
import org.researchstack.backbone.ui.views.SubmitBar;
import org.researchstack.backbone.utils.TextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import rx.functions.Action1;

public class ImageStepLayout extends FixedSubmitBarLayout implements StepLayout {
    private StepCallbacks callbacks;
    private ImageCaptureStep step;
    private StepResult stepResult;

    private AppCompatButton mButton;
    private CameraPreview mPreview;

    private String mPhotoPath = null;

    private static String TAG = "img_step";

    public ImageStepLayout(Context context) {
        super(context);
    }

    public ImageStepLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public ImageStepLayout(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public int getContentResourceId() {
        return R.layout.layout_rsb_image_step;
    }

    @Override
    public void initialize(Step step, StepResult result) {
        this.step = (ImageCaptureStep) step;
        this.stepResult = result == null ? new StepResult<>(step) : result;
        initializeStep();
    }

    private void initializeStep() {
        if(step != null) {
            // Set Title
            if (!TextUtils.isEmpty(step.getTitle())) {
                TextView title = (TextView) findViewById(R.id.rsb_image_step_title);
                title.setVisibility(View.VISIBLE);
                title.setText(step.getTitle());
            }

            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(getContext());
            FrameLayout preview = (FrameLayout) findViewById(R.id.image_step_camera_preview);
            preview.addView(mPreview);

            // Set Camera Button
            mButton = (AppCompatButton) findViewById(R.id.rsb_image_step_button);
            if (mPhotoPath != null) {
                mButton.setText("Recapture");
            }
            else {
                mButton.setText("Take Picture");
            }

            mButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPhotoPath != null) {
                        mPreview.startPreview();
                        mButton.setText("Take Picture");
                        mPhotoPath = null;
                    }
                    else {
                        mPreview.takePicture(mPictureCallback);
                    }
                }
            });

            // Set Next / Skip
            SubmitBar submitBar = (SubmitBar) findViewById(org.researchstack.backbone.R.id.rsb_submit_bar);
            submitBar.setPositiveTitle(org.researchstack.backbone.R.string.rsb_next);
            submitBar.setPositiveAction(new Action1() {
                @Override
                public void call(Object o) {
                    if (mPhotoPath != null) {
                        mPreview.stop();
                        stepResult.setResult(mPhotoPath);
                        callbacks.onSaveStep(StepCallbacks.ACTION_NEXT, step, stepResult);
                    }
                    else {
                        Toast.makeText(getContext(), "Please take a picture to move next", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            if(step.isOptional())
            {
                submitBar.setNegativeTitle(org.researchstack.backbone.R.string.rsb_step_skip);
                submitBar.setNegativeAction(new Action1() {
                    @Override
                    public void call(Object o) {
                        mPreview.stop();
                        if(callbacks != null)
                        {
                            callbacks.onSaveStep(StepCallbacks.ACTION_NEXT, step, null);
                        }
                    }
                });
            }
            else
            {
                submitBar.getNegativeActionView().setVisibility(View.GONE);
            }
        }
    }

    @Override
    public View getLayout() {
        return this;
    }

    @Override
    public boolean isBackEventConsumed() {
        mPreview.stop();
        callbacks.onSaveStep(StepCallbacks.ACTION_PREV, step, null);
        return false;
    }

    @Override
    public void setCallbacks(StepCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // Request write storage permission
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    //
                }
                else {
                    ActivityCompat.requestPermissions((Activity)getContext(), new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
                return;
            }

            File pictureFile = getOutputMediaFile();
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            camera.stopPreview();
            mButton.setText("Recapture");
        }
    };

    /** Create a File for saving an image or video */
    private File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Preferences.getAppDirectory(), "Accident");

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        mPhotoPath = mediaStorageDir.getPath() + File.separator + "IMG_STEP_" + step.getIdentifier() + ".jpg";

        File mediaFile = new File(mPhotoPath);

        return mediaFile;
    }
}
