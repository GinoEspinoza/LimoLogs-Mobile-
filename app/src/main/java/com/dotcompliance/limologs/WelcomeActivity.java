package com.dotcompliance.limologs;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.dotcompliance.limologs.ELD.GPSVOXConnect;
import com.dotcompliance.limologs.ELD.GPSVOXRetrofitConnect;
import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.DutyStatus;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.data.Vehicle;
import com.dotcompliance.limologs.network.RestTask;
import com.dotcompliance.limologs.service.CheckVehicleCurrentStatusService;
import com.dotcompliance.limologs.util.DataManager;

import java.util.ArrayList;

public class WelcomeActivity extends LimoBaseActivity {

    TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
//        CalampConnect.authenticate();
        GPSVOXRetrofitConnect.authenticate();
        mTextView = (TextView) findViewById(R.id.textview_welcome);
        mTextView.setText("Welcome back, " + Preferences.mDriver.firstname + " " + Preferences.mDriver.lastname);

//        Intent intent = new Intent(WelcomeActivity.this, CheckVehicleCurrentStatusService.class);
//        startService(intent);

        setConnectionStatus(Preferences.isConnected);
    }

    public void onClickNext(View v) {
        DataManager.getInstance().showProgressMessage(this);
        RestTask.getAssignedItems(new RestTask.TaskCallbackInterface() {
            @Override
            public void onTaskCompleted(Boolean success, String message) {
                DataManager.getInstance().hideProgressMessage();
                if (success) {
                    Intent intent = new Intent(mContext, UnassignedDriverActivity.class);
                    intent.putExtra("vehicle_index", -1);
                    intent.putExtra("log_index", 0);
                    startActivity(intent);
                    finish();
                }
                else {
                    proceedToNext();
                }
            }
        });
    }

    private void proceedToNext() {
        // Get driver's last status
        ArrayList<DutyStatus> states =Preferences.mDriverLogs.get(0).statusList;
        int i = states.size() - 1;
        int status1 = 0;
        for (int j = i; j <= i && j >=0; j--) {

            if (states.get(j).status == 0 || states.get(j).status == 1 || states.get(j).status == 2 || states.get(j).status == 3
                    || states.get(j).status == 4 || states.get(j).status == 5) {
                status1 = states.get(j).status;
                break;
            }
        }

        if (status1 == DutyStatus.STATUS_OFF || status1 == DutyStatus.STATUS_SLEEPER) {
            startActivity(new Intent(WelcomeActivity.this, AskOnDutyActivity.class));
        }
        else {
            startActivity(new Intent(WelcomeActivity.this, VehicleListActivity.class));
        }
        finish();
    }
}
