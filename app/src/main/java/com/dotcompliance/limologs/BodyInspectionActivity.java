package com.dotcompliance.limologs;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.data.VehicleInspect;
import com.dotcompliance.limologs.network.RestTask;
import com.dotcompliance.limologs.view.PinnedScaleImageView;

import java.io.InputStream;
import java.net.URL;

public class BodyInspectionActivity extends LimoBaseActivity {
    PinnedScaleImageView imageView;
    TextView textView;

    ProgressDialog pDialog;
    Bitmap bmpDiagram;

    int vehicleID;
    int vehicle_index;
    Boolean isDriverMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_body_inspection);

        vehicle_index = getIntent().getIntExtra("vehicle_index", Preferences.mVehicleIndex);
        requestDiagramForClass(Preferences.mVehicleList.get(vehicle_index).vehicle_clsid);

        isDriverMode = getIntent().getBooleanExtra("driver_mode", true);

        vehicleID = Preferences.mVehicleList.get(vehicle_index).vehicle_id;

        initialize();
    }

    protected void initialize() {
        setLeftMenuItem("Back");

        textView = (TextView) findViewById(R.id.text_hint);
        imageView = (PinnedScaleImageView) findViewById(R.id.image_diagram_view);

        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (imageView.isReady()) {
                    PointF sCoord = imageView.viewToSourceCoord(e.getX(), e.getY());
                    for (int i = 0; i < Preferences.mBodyInspectList.size(); i++) {
                        final VehicleInspect inspect = Preferences.mBodyInspectList.get(i);
                        RectF rc = new RectF(inspect.xPos - 30, inspect.yPos - 30, inspect.xPos + 30, inspect.yPos + 30);
                        if (rc.contains(sCoord.x, sCoord.y)) {
                            if (isDriverMode)
                                Toast.makeText(mContext, "Inspector's Note: " + inspect.note, Toast.LENGTH_SHORT).show();
                            else {
                                final int pos = i;
                                new AlertDialog.Builder(mContext)
                                    .setTitle("Inspect")
                                    .setMessage(inspect.note)
                                    .setPositiveButton("Fix it", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            RestTask.fixBodyInspect(inspect.Id, new RestTask.TaskCallbackInterface() {
                                                @Override
                                                public void onTaskCompleted(Boolean success, String message) {
                                                    if (success) {
                                                        inspect.vehicle.inspect_count--;

                                                        Preferences.mBodyInspectList.remove(pos);
                                                        imageView.invalidate();

                                                        // decrease damage count
                                                    }
                                                }
                                            });
                                        }
                                    }).setNegativeButton("Cancel", null).show();
                            }
                            break;
                        }
                    }
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (imageView.isReady()) {
                    final PointF sCoord = imageView.viewToSourceCoord(e.getX(), e.getY());

                    Intent intent = new Intent(mContext, AddDamageActivity.class);
                    intent.putExtra("vehicle_index", vehicle_index);
                    intent.putExtra("COORD_X", (int)sCoord.x);
                    intent.putExtra("COORD_Y", (int)sCoord.y);
                    startActivity(intent);
                }
            }
        });

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent);
            }
        });
    }

    @Override
    protected void onMenuItemLeft() {
        super.onMenuItemLeft();

        finish();
    }

    protected void requestDiagramForClass(int class_id) {
        new LoadImage().execute(Preferences.API_BASE_PATH + "/limo/vehicle_diagram?cls_id=" + class_id);
    }

    private class LoadImage extends AsyncTask<String, String, Bitmap> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(mContext);
            pDialog.setMessage("Loading Image ....");
            pDialog.show();

        }
        protected Bitmap doInBackground(String... args) {
            try {
                Log.i("Url", args[0]);
                bmpDiagram = BitmapFactory.decodeStream((InputStream)new URL(args[0]).getContent());

            } catch (Exception e) {
                e.printStackTrace();
            }
            return bmpDiagram;
        }

        protected void onPostExecute(Bitmap image) {

            if(image != null){
                imageView.setImage(ImageSource.bitmap(image));
                pDialog.dismiss();

            }else{

                pDialog.dismiss();
                Toast.makeText(mContext, "Image Does Not exist or Network Error", Toast.LENGTH_SHORT).show();

            }
        }
    }
}
