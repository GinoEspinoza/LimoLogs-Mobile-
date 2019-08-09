package com.dotcompliance.limologs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.dotcompliance.limologs.ELD.AvlEvent;
import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.DutyStatus;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.data.Vehicle;
import com.dotcompliance.limologs.network.RestTask;
import com.dotcompliance.limologs.util.DateUtils;

import java.util.Calendar;
import java.util.Date;

public class AskOnDutyActivity extends LimoBaseActivity {

    Button buttonYes;
    Button buttonNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ask_on_duty);

        buttonYes = (Button) findViewById(R.id.button_yes);
        buttonNo = (Button) findViewById(R.id.button_no);

        buttonYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AskOnDutyActivity.this, NoteLocationActivity.class);
              //  intent.putExtra("askOnLocation","1");
                startActivityForResult(intent, 1);
            }
        });

        buttonNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AskOnDutyActivity.this, VehicleListActivity.class));
                finish();
            }
        });

        setConnectionStatus(Preferences.isConnected);

        if (Preferences.mLastLogDate != null) {
            Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
            if (DateUtils.getDifferenceInDays(cal.getTime(), Preferences.mLastLogDate) > 1) {
                Intent intent1 = new Intent(mContext, MonthlySignActivity.class);
                startActivity(intent1);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            DriverLog log = Preferences.mDriverLogs.get(0);
            DutyStatus lastState = log.statusList.get(log.statusList.size() - 1);

            Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
            int curr_time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);

            log.addNewStatus(0, DutyStatus.STATUS_ON, curr_time, 0, data.getStringExtra(NoteLocationActivity.DATA_LOCATION), data.getStringExtra(NoteLocationActivity.DATA_NOTE));

            startActivity(new Intent(AskOnDutyActivity.this, VehicleListActivity.class));
            finish();
        }
    }



}
