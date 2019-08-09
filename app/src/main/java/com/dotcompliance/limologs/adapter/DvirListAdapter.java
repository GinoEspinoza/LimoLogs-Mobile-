package com.dotcompliance.limologs.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.dotcompliance.limologs.R;
import com.dotcompliance.limologs.data.DvirLog;

import java.util.List;

public class DvirListAdapter extends ArrayAdapter<DvirLog> {

    public DvirListAdapter(Context context, List<DvirLog> objects) {
        super(context, 0, objects);
    }

    @Override
    public int getCount() {
        return super.getCount() + 1;
    }

    @Override
    public DvirLog getItem(int position) {
        // TODO Auto-generated method stub
        if (position > 0)
            return super.getItem(position - 1);
        else
            return null;
    }

    @Override
    public boolean isEnabled(int position) {
        return position > 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        DvirLog dvir = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.dvirlist_row, parent, false);
        }
        if (dvir != null) {
            TextView tvDate = (TextView) convertView.findViewById(R.id.col_dvir_date);
            TextView tvDriver = (TextView) convertView.findViewById(R.id.col_drivername);
            TextView tvVehicle = (TextView) convertView.findViewById(R.id.col_vehicle_num);
            TextView tvCarrier = (TextView) convertView.findViewById(R.id.col_carrier_name);
            TextView tvLocation = (TextView) convertView.findViewById(R.id.col_location);
            // Populate the data into the template view using the data object
            tvDate.setText(dvir.logDate);
            tvDriver.setText(dvir.firstName + " " + dvir.lastName);
            tvVehicle.setText(dvir.vehicle);
            tvCarrier.setText(dvir.carrierName);
            tvLocation.setText(dvir.location);
        }
        // Return the completed view to render on screen
        return convertView;
    }
}
