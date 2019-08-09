package com.dotcompliance.limologs.adapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.dotcompliance.limologs.R;
import com.dotcompliance.limologs.UnassignedDriverActivity;
import com.dotcompliance.limologs.data.UnassignedTime;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.util.DataManager;
import com.dotcompliance.limologs.util.DateUtils;

import static com.dotcompliance.limologs.UnassignedDriverActivity.unassignedTimelist;

public class UnassignedDriverListRowAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<UnassignedTime> log = new ArrayList<>();


    public UnassignedDriverListRowAdapter(Context c, ArrayList<UnassignedTime> log) {
        this.context = c;
        this.log = log;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return log.size(); // log + 1;
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
    /*    if (position > 0)
            return log.get(position - 1);
        else
            return null;*/
        return position;
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
        TextView txtVehicleNo;
        TextView txtStartTime;
        TextView txtEndTime;
        CheckBox chkUnassignedDriver;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub

        // TODO Auto-generated method stub
        //DutyListAdapter.ViewHolder holder;
        final UnassignedTime unassignedTime = (UnassignedTime) this.getItem(position);
        ViewHolder holder = null;
        LayoutInflater inflater = LayoutInflater.from(context);

        convertView = null;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.unassigned_driver_list_row, null);

            holder.txtVehicleNo = (TextView) convertView.findViewById(R.id.vehicle_name);
            holder.txtStartTime = (TextView) convertView.findViewById(R.id.col_start_time);
            holder.txtEndTime = (TextView) convertView.findViewById(R.id.col_end_time);
            holder.chkUnassignedDriver = (CheckBox) convertView.findViewById(R.id.check_unassigned_driver);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        //   holder.chkUnassignedDriver.setTag(position);

        if (position == 0) {
            holder.chkUnassignedDriver.setVisibility(View.INVISIBLE);
        }

        if (position > 0) {
            holder.txtVehicleNo.setText(unassignedTime.un_vehicle_id);
            holder.txtStartTime.setText(unassignedTime.un_starttime);
            holder.txtEndTime.setText(unassignedTime.un_endtime);
        }


        try {
            holder.chkUnassignedDriver.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    try {
//                        DataManager.getInstance().ClearList();
//                        DataManager.getInstance().addbean(unassignedTime);
                        if (position >= getCount()) {
                            System.out.println("Error: position '" + position + "' is out of bounds in 'arrayList'");
                            return;
                        }
                        if (isChecked) {

                            unassignedTime.setIs_checked(true);
                            log.add(position, unassignedTime);
                            DataManager.getInstance().setUnassignedList(log);
                          //  UnassignedDriverListRowAdapter adapter = new UnassignedDriverListRowAdapter(context, DataManager.getInstance().getUnassignedList());
                            //unassignedTimelist.setAdapter(adapter);
                            //    notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return convertView;
    }
}
