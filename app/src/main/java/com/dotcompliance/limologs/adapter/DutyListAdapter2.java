package com.dotcompliance.limologs.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.dotcompliance.limologs.R;
import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.DutyStatus;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.util.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class DutyListAdapter2 extends BaseAdapter {
    private Context context;
    private DriverLog log;
    private ArrayList<DutyStatus> list = new ArrayList<>();

    public DutyListAdapter2(Context context, DriverLog log, ArrayList<DutyStatus> list) {
        this.context = context;
        this.log = log;
        this.list = list;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return list.size() + 1;
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        if (position > 0)
            return list.get(position - 1);
        else
            return null;
    }

    @Override
    public boolean isEnabled(int position) {
        return position > 0;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return 0;
    }

    private class ViewHolder {
        TextView txtNo;
        TextView txtStatus;
        TextView txtStart;
        TextView txtDuration;
        TextView txtLocation;
        TextView txtRemark;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub

        // TODO Auto-generated method stub
        ViewHolder holder;
        LayoutInflater inflater = LayoutInflater.from(context);
        holder = new ViewHolder();
        convertView = null;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.dutylist_row, null);
//            holder = new ViewHolder();
            holder.txtNo = (TextView) convertView.findViewById(R.id.col_duty_no);
            holder.txtStatus = (TextView) convertView.findViewById(R.id.col_duty_status);
            holder.txtStart = (TextView) convertView.findViewById(R.id.col_duty_start);
            holder.txtDuration = (TextView) convertView.findViewById(R.id.col_duty_duration);
            holder.txtLocation = (TextView) convertView.findViewById(R.id.col_duty_loc);
            holder.txtRemark = (TextView) convertView.findViewById(R.id.col_duty_remark);
//            convertView.setTag(holder);
        }
//        else
//        {
//            holder = (ViewHolder) convertView.getTag();
//        }

        if (position > 0) {
           // DutyStatus state = log.statusList.get(position - 1);
            holder.txtNo.setText("" + position);
            holder.txtStatus.setText(list.get(position-1).getStatusString());
            holder.txtLocation.setText(list.get(position-1).location);
            holder.txtRemark.setText(list.get(position-1).remark);

            int hr, min;
            int end_time = 0;
            if (position < list.size())
                end_time = list.get(position-1).start_time;

            if (end_time == 0) { // last duty
                Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
                end_time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
                end_time -= end_time % 15 - 15;

                Date date = null;
                try {
                    date = new SimpleDateFormat("yyyy-MM-dd").parse(log.log_date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if (DateUtils.getDateWithoutTime(date).compareTo(DateUtils.getDateWithoutTime(cal.getTime())) < 0) {
                    end_time = 1440;
                }
            }

            min = list.get(position-1).start_time % 60;
            hr = list.get(position-1).start_time / 60;

            // (String(format: "%.2d:%.2d", duty.start_time / 60, duty.start_time % 60)
            holder.txtStart.setText(String.format("%02d:%02d", hr, min));
          /*  min = state.start_time % 60;
            hr = (state.start_time - min) / 60;
            holder.txtStart.setText(String.format("%02d:%02d", hr, min));

            min = (end_time - state.start_time) % 60;
            hr = (end_time - state.start_time - min) / 60;*/
            int duration = list.get(position-1).end_time - list.get(position-1).start_time;
            hr = duration / 60;
            min = duration % 60;
            String strDuration = "";
            if (hr > 0) strDuration = String.format("%d hr  ", hr);
            if (min > 0) strDuration += String.format("%d min", min);
            holder.txtDuration.setText(strDuration);
        }




        return convertView;
    }
}
