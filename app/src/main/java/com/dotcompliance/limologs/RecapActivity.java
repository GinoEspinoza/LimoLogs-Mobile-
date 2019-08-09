package com.dotcompliance.limologs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.dotcompliance.limologs.data.DutyStatus;
import com.dotcompliance.limologs.data.Preferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RecapActivity extends LimoBaseActivity {
    private ListView lvRecap;

    private ArrayList<RowData> listRecaps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_recap);

        initialize();
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
            Preferences.clearSession(mContext);
            Preferences.mDriverLogs.clear();
            Preferences.mVehicleList.clear();
            startActivity(new Intent(mContext, LoginActivity.class));
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onMenuItemLeft() {
        finish();
    }

    protected void initialize() {
        setLeftMenuItem("Back");
        setConnectionStatus(Preferences.isConnected);

        lvRecap = (ListView) findViewById(R.id.listview_recap);

        try {
            listRecaps = new ArrayList<>();
            int total_hours = 0;
            // Calculate HOS hours
            for (int i = Preferences.CYCLE_DAY - 1; i >= 0; i--) {
                RowData item = new RowData();
                if (i > 0) {
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DATE, -i);
                    SimpleDateFormat sdf = new SimpleDateFormat("EEEE  LLL dd", Locale.US);
                    sdf.setTimeZone(Preferences.getDriverTimezone());

                    item.title = sdf.format(cal.getTime());
                } else {
                    item.title = "Hours Worked Today";
                }

                int hours_worked = 0;
                if (Preferences.mDriverLogs.size() > i) {

                    ArrayList<DutyStatus> list = Preferences.mDriverLogs.get(i).statusList;
                    for (int j = 0; j < list.size(); j++) {
                        int end_time;
                        DutyStatus state = list.get(j);

                        if (state.getNormalizeStatus() > DutyStatus.STATUS_SLEEPER) {

                            if (j == list.size() - 1) {
                                if (i == 0) {
                                    Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
                                    end_time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
                                    end_time -= end_time % 15 - 15;
                                } else
                                    end_time = 1440;
                            } else
                                end_time = list.get(j + 1).start_time;

                            hours_worked += end_time - state.start_time;
                        }
                    }
                }
                total_hours += hours_worked;

                item.hours = hours_worked;

                listRecaps.add(item);
            }

            RowData item = new RowData();
            item.title = "Total Hours Worked Last " + (Preferences.CYCLE_DAY - 1) + " Days";
            item.hours = total_hours - listRecaps.get(Preferences.CYCLE_DAY - 1).hours;
            listRecaps.add(item);

            item = new RowData();
            item.title = "Hours Available Tomorrow";
            item.hours = Preferences.CYCLE_HOUR - total_hours + listRecaps.get(0).hours;
            if (item.hours < 0) item.hours = 0;
            listRecaps.add(item);

            lvRecap.setAdapter(new MyListViewAdapter(mContext, listRecaps));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class RowData {
        public String title;
        public int hours;
    }

    private class MyListViewAdapter extends BaseAdapter {
        private Context context;
        private List<RowData> list;

        public MyListViewAdapter(Context context, List<RowData> list) {
            super();
            this.context = context;
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public RowData getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final RowData item = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.recap_row, parent, false);
            }
            TextView textTitle = (TextView) convertView.findViewById(R.id.col_recap_title);
            TextView textHours = (TextView) convertView.findViewById(R.id.col_recap_hours);

            textTitle.setText(item.title);

            //textHours.setText(String.format(Locale.US, "%.2f", item.hours / 60.0f));
            textHours.setText(String.format(Locale.US, "%1$2d:%2$02d", item.hours/60, item.hours % 60));

            return convertView;
        }
    }
}
