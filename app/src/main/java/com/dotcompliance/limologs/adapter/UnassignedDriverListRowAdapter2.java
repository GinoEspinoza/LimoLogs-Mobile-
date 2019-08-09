package com.dotcompliance.limologs.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.dotcompliance.limologs.R;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.data.UnassignedTime;
import com.dotcompliance.limologs.util.DataManager;

import java.util.ArrayList;

/**
 * Created by saurabh on 11/30/2017.
 */

public class UnassignedDriverListRowAdapter2 extends BaseAdapter {

    private ArrayList<UnassignedTime> unassignedTimeArrayList = new ArrayList<UnassignedTime>();
    private Context context;


    public UnassignedDriverListRowAdapter2(ArrayList<UnassignedTime> unassignedTimeArrayList, Context context) {
        this.unassignedTimeArrayList = unassignedTimeArrayList;
        this.context = context;
    }

    @Override
    public int getCount() {
        return unassignedTimeArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return unassignedTimeArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(context,
                    R.layout.unassigned_driver_list_row, null);
            new ViewHolder(convertView);
        }
        final ViewHolder holder = (ViewHolder) convertView.getTag();
        convertView.setTag(R.id.check_unassigned_driver, holder.checkUnassignedDriver);
        //Log.e("Inventory Stock Name", "Inventory Stock Name :----- " + salesOrderBean.getInventory_stock_name());

        String vehicle_name = "";
        //  if (pos > 0) {

        for (int i = 0; i < Preferences.mVehicleList.size(); i++) {
            if (Preferences.mVehicleList.get(i).vehicle_id == Integer.parseInt(unassignedTimeArrayList.get(position).un_vehicle_id)) {
                vehicle_name = Preferences.mVehicleList.get(i).vehicle_no;
                break;
            }
        }

        if (vehicle_name.isEmpty()) {
            vehicle_name = unassignedTimeArrayList.get(position).un_vehicle_id;
        }

 /*
//           if(unassignedTimeArrayList.contains(Preferences.mVehicleList.))
           for (int i = 0; i < Preferences.mVehicleList.size(); i++) {
               if(Integer.parseInt(unassignedTimeArrayList.get(pos).un_vehicle_id)==(Preferences.mVehicleList.get(i).vehicle_id)){

               }
           }*/

        holder.vehicleName.setText(vehicle_name);
        holder.colStartTime.setText(unassignedTimeArrayList.get(position).un_starttime);
        holder.colEndTime.setText(unassignedTimeArrayList.get(position).un_endtime);
        //}

       /* holder.checkUnassignedDriver.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //int getPosition = (Integer) buttonView.getTag();
                if (isChecked) {

                    unassignedTimeArrayList.get(position).setIs_checked(true);
                    //    unassignedTime.setIs_checked(true);
                    DataManager.getInstance().setUnassignedList(unassignedTimeArrayList);
                } else {
                    unassignedTimeArrayList.get(position).setIs_checked(false);
                    DataManager.getInstance().setUnassignedList(unassignedTimeArrayList);
                }
                Log.e("checkUnassignedDriver", "checkUnassignedDriver: "+DataManager.getInstance().getUnassignedList().toString());
            }
        });*/

       holder.checkUnassignedDriver.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               if (unassignedTimeArrayList.get(position).is_checked){
                   holder.checkUnassignedDriver.setBackgroundResource(R.drawable.ic_action_checked);
                   unassignedTimeArrayList.get(position).setIs_checked(true);
                   //    unassignedTime.setIs_checked(true);
                   DataManager.getInstance().setUnassignedList(unassignedTimeArrayList);
               }else {
                   holder.checkUnassignedDriver.setBackgroundResource(R.drawable.ic_action_unchecked);
                   unassignedTimeArrayList.get(position).setIs_checked(false);
                   DataManager.getInstance().setUnassignedList(unassignedTimeArrayList);
               }
               Log.e("checkUnassignedDriver", "checkUnassignedDriver: "+DataManager.getInstance().getUnassignedList().toString());
           }
       });




        return convertView;
    }

     class ViewHolder {
        private CheckBox checkUnassignedDriver;
        private TextView vehicleName;
        private TextView colStartTime;
        private TextView colEndTime;

        public ViewHolder(View view) {
            checkUnassignedDriver = (CheckBox) view.findViewById(R.id.check_unassigned_driver);
            vehicleName = (TextView) view.findViewById(R.id.vehicle_name);
            colStartTime = (TextView) view.findViewById(R.id.col_start_time);
            colEndTime = (TextView) view.findViewById(R.id.col_end_time);

            view.setTag(this);
        }
    }
}