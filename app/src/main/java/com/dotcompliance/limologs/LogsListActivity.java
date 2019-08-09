package com.dotcompliance.limologs;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.util.DataManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class LogsListActivity extends LimoBaseActivity {
    private ListView listviewLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_logs_list);
        initialize();
    }

    protected void initialize() {
        setLeftMenuItem("Back");
        setConnectionStatus(Preferences.isConnected);

        listviewLogs = (ListView)findViewById(R.id.listview_logs);

        final ArrayList<String> list = new ArrayList<>();
        final ArrayList<String> listDateServerFormat = new ArrayList<>();
        for (int i = 0; i <15; i ++) {
            Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
            cal.add(Calendar.DATE, -i);
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, LLL dd", Locale.US);
            SimpleDateFormat sdfServerFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setTimeZone(Preferences.getDriverTimezone());
            list.add(sdf.format(cal.getTime()));
            listDateServerFormat.add(sdfServerFormat.format(cal.getTime()));
        }

        if(DataManager.getInstance().isLog()){
            Intent intent = new Intent(mContext, DailyLogActivity.class);
            intent.putExtra("log_index", DataManager.getInstance().getIndex());
            startActivity(intent);
        }

        final ArrayAdapter<String> adpater = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, list);
        listviewLogs.setAdapter(adpater);
        listviewLogs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int log_index = fetchLogIndex(listDateServerFormat.get(position));
                if (log_index == -1) {
                    Toast.makeText(mContext, "No Log for the date", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(mContext, DailyLogActivity.class);
                intent.putExtra("log_index", log_index);
                DataManager.getInstance().setIndex(log_index);
                startActivity(intent);
            }
        });
    }

    private int fetchLogIndex(String date) {
        for (int i = 0; i < Preferences.mDriverLogs.size(); i++) {
            if (Preferences.mDriverLogs.get(i).log_date.contentEquals(date)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onMenuItemLeft() {
        if (DataManager.getInstance().getClassname().equalsIgnoreCase("home")){
            Intent intent = new Intent(LogsListActivity.this,HomeActivity.class);
            startActivity(intent);
            finish();
        }else if (DataManager.getInstance().getClassname().equalsIgnoreCase("vechile")){
            Intent intent = new Intent(LogsListActivity.this,VehicleListActivity.class);
            startActivity(intent);
            finish();
        }else if (DataManager.getInstance().getClassname().equalsIgnoreCase("monthlysign")){
            Intent intent = new Intent(LogsListActivity.this,MonthlySignActivity.class);
            startActivity(intent);
            finish();
        }else if (DataManager.getInstance().getClassname().equalsIgnoreCase("log")){
            Intent intent = new Intent(LogsListActivity.this,LogsActivity.class);
            startActivity(intent);
            finish();
        }else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.common, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_sign_out) {
            onSaveDuty();
            /*Preferences.clearSession(mContext);
            Preferences.mDriverLogs.clear();
            Preferences.mVehicleList.clear();
            startActivity(new Intent(mContext, LoginActivity.class));
            finish();*/
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
