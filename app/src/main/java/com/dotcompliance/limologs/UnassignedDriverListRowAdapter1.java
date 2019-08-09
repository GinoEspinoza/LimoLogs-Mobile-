package com.dotcompliance.limologs;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.data.UnassignedTime;
import com.dotcompliance.limologs.util.DataManager;
import java.util.ArrayList;

public class UnassignedDriverListRowAdapter1 extends BaseAdapter {

    private ArrayList<UnassignedTime> unassignedTimeArrayList = new ArrayList<UnassignedTime>();
    private Context context;
    private LayoutInflater layoutInflater;

    public UnassignedDriverListRowAdapter1(Context context, ArrayList<UnassignedTime> unassignedTimeArrayList) {
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
        this.unassignedTimeArrayList = unassignedTimeArrayList;
    }

    @Override
    public int getCount() {
        return unassignedTimeArrayList.size();
    }

    @Override
    public UnassignedTime getItem(int position) {
        return unassignedTimeArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.unassigned_driver_list_row, null);
            convertView.setTag(new ViewHolder(convertView));
        }
        initializeViews((UnassignedTime) getItem(position), (ViewHolder) convertView.getTag(), position);

        return convertView;
    }

    private void initializeViews(final UnassignedTime unassignedTime, final ViewHolder holder, final int pos) {
        //TODO implement

        String vehicle_name = "";
      //  if (pos > 0) {

            for (int i = 0; i < Preferences.mVehicleList.size(); i++) {
                if (Preferences.mVehicleList.get(i).vehicle_id == Integer.parseInt(unassignedTime.un_vehicle_id)) {
                    vehicle_name = Preferences.mVehicleList.get(i).vehicle_no;
                    break;
                }
            }

            if (vehicle_name.isEmpty()) {
                vehicle_name = unassignedTime.un_vehicle_id;
            }

 /*
//           if(unassignedTimeArrayList.contains(Preferences.mVehicleList.))
           for (int i = 0; i < Preferences.mVehicleList.size(); i++) {
               if(Integer.parseInt(unassignedTimeArrayList.get(pos).un_vehicle_id)==(Preferences.mVehicleList.get(i).vehicle_id)){

               }
           }*/

            holder.vehicleName.setText(vehicle_name);
            holder.colStartTime.setText(unassignedTime.un_starttime);
            holder.colEndTime.setText(unassignedTime.un_endtime);
        //}

        holder.checkUnassignedDriver.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                   // unassignedTimeArrayList.set(pos).setIs_checked(true);
                    unassignedTime.setIs_checked(true);
                    DataManager.getInstance().setUnassignedList(unassignedTimeArrayList);
                } else {
                    unassignedTime.setIs_checked(false);
                    DataManager.getInstance().setUnassignedList(unassignedTimeArrayList);
                }
            }
        });

       /* holder.checkUnassignedDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (holder.checkUnassignedDriver.isChecked()) {
                    unassignedTime.setIs_checked(true);
                    DataManager.getInstance().setUnassignedList(unassignedTimeArrayList);
                } else {
                    unassignedTime.setIs_checked(false);
                    DataManager.getInstance().setUnassignedList(unassignedTimeArrayList);
                }
                Log.e("checkUnassignedDriver", "checkUnassignedDriver: "+DataManager.getInstance().getUnassignedList().toString());
            }
        });*/

    }

    protected class ViewHolder {
        private CheckBox checkUnassignedDriver;
        private TextView vehicleName;
        private TextView colStartTime;
        private TextView colEndTime;

        public ViewHolder(View view) {
            checkUnassignedDriver = (CheckBox) view.findViewById(R.id.check_unassigned_driver);
            vehicleName = (TextView) view.findViewById(R.id.vehicle_name);
            colStartTime = (TextView) view.findViewById(R.id.col_start_time);
            colEndTime = (TextView) view.findViewById(R.id.col_end_time);
        }
    }
}
