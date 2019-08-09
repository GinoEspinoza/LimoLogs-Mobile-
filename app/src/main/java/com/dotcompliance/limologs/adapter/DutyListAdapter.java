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
import java.util.Calendar;
import java.util.Date;

public class DutyListAdapter extends BaseAdapter {
    private Context context;
    private DriverLog log;

    public DutyListAdapter(Context c, DriverLog log) {
        super();
        this.context = c;
        this.log = log;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return log.statusList.size() + 1;
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        if (position > 0)
            return log.statusList.get(position - 1);
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
            DutyStatus state = log.statusList.get(position - 1);
            holder.txtNo.setText("" + position);
            holder.txtStatus.setText(state.getStatusString());
            holder.txtLocation.setText(state.location);
            holder.txtRemark.setText(state.remark);

            int hr, min;
            int end_time = 0;
            if (position < log.statusList.size())
                end_time = log.statusList.get(position).start_time;

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

            min = state.start_time % 60;
            hr = state.start_time / 60;

            // (String(format: "%.2d:%.2d", duty.start_time / 60, duty.start_time % 60)
            holder.txtStart.setText(String.format("%02d:%02d", hr, min));
          /*  min = state.start_time % 60;
            hr = (state.start_time - min) / 60;
            holder.txtStart.setText(String.format("%02d:%02d", hr, min));

            min = (end_time - state.start_time) % 60;
            hr = (end_time - state.start_time - min) / 60;*/
            int duration = state.end_time - state.start_time;
            hr = duration / 60;
            min = duration % 60;
            String strDuration = "";
            if (hr > 0) strDuration = String.format("%d hr  ", hr);
            if (min > 0) strDuration += String.format("%d min", min);
            holder.txtDuration.setText(strDuration);
        }


       /* et duration = duty.end_time - duty.start_time
        let hr = duration / 60
        let min = duration % 60
        var str = ""
        if hr > 0 {
            str = String(format: "%d hr", hr)
        }
        if min > 0 {
            str = str.appendingFormat("  %d min", min)
        }*/

        return convertView;
    }
}
