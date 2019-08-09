package com.dotcompliance.limologs.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.dotcompliance.limologs.R;
import com.dotcompliance.limologs.data.DvirLog;
import com.dotcompliance.limologs.util.ViewUtils;

import java.util.ArrayList;

public class DvirPreview extends LinearLayout {
    private TextView textDriver;
    private TextView textVehicle;
    private TextView textCarrier, textCarrierAddress;
    private TextView textStartOdo, textEndOdo;
    private TextView textLocation;
    private TextView textDateTime;
    private TextView textNoDefect;
    private ListView listviewDefects;
    private TextView textDefectable;
    private TextView textMechanicNote;
    private ImageView imgViewMechanicSign, imgViewDriverSign;

    private  DvirLog mData;
    public DvirPreview(Context context) {
        super(context);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.layout_dvir_preview, this);
        textDateTime = (TextView) findViewById(R.id.text_date);
        textDriver = (TextView) findViewById(R.id.text_driver);
        textCarrier = (TextView) findViewById(R.id.text_carrier);
        textCarrierAddress = (TextView) findViewById(R.id.text_carrier_address);
        textVehicle = (TextView) findViewById(R.id.text_vehicle);
        textStartOdo = (TextView) findViewById(R.id.text_startodo);
        textEndOdo = (TextView) findViewById(R.id.text_endodo);
        textLocation = (TextView) findViewById(R.id.text_location);
        textNoDefect = (TextView) findViewById(R.id.text_no_defects);
        listviewDefects = (ListView) findViewById(R.id.list_defects);
        textDefectable = (TextView) findViewById(R.id.text_defectable);
        textMechanicNote = (TextView) findViewById(R.id.text_mechanic_note);

        imgViewDriverSign = (ImageView) findViewById(R.id.imgview_driver_sign);
        imgViewMechanicSign = (ImageView) findViewById(R.id.imgview_mechanic_sign);
    }

    public void setData(DvirLog data) {
        mData = data;
        textDriver.setText(data.firstName + " " + data.lastName);
        textVehicle.setText(data.vehicle);
        textCarrier.setText(data.carrierName);
        textCarrierAddress.setText(data.carrierAddress);
        if (data.startOdometer != 0)
            textStartOdo.setText("" + data.startOdometer);
        else
            textStartOdo.setText("");
        if (data.endOdometer != 0)
            textEndOdo.setText("" + data.endOdometer);
        else
            textEndOdo.setText("");
        textLocation.setText(data.location);
        textDateTime.setText(mData.logDate + "\n" + mData.logTime);
        // list defects
        if (data.defectList.size() > 0) {
            MyListViewAdapter adapter = new MyListViewAdapter(getContext(), mData.defectList);
            listviewDefects.setAdapter(adapter);
            ViewUtils.setListViewHeightBasedOnItems(listviewDefects);
            textNoDefect.setVisibility(GONE);
            listviewDefects.setVisibility(VISIBLE);
            textDefectable.setVisibility(VISIBLE);

            if (!data.isDefected) {
                textDefectable.setText("This defect DOES NOT affect the safe operation of the motor vehicle and IS NOT likely to result in its mechanical breakdown.");
            }
        }

        if (mData.driverSign != null)
            imgViewDriverSign.setImageBitmap(mData.driverSign);
        if (mData.mechanicSign != null) {
            imgViewMechanicSign.setImageBitmap(mData.mechanicSign);
            if (mData.note != "")
                textMechanicNote.setText("Mechanic's note - " + mData.note);
        }
        else {
            LinearLayout layout = (LinearLayout)findViewById(R.id.lay_mechanic_sign);
            layout.setVisibility(INVISIBLE);
        }
    }

    public class MyListViewAdapter extends ArrayAdapter<DvirLog.DvirDefect> {
        private Context context;

        public MyListViewAdapter(Context context, ArrayList<DvirLog.DvirDefect> list) {
            super(context, 0, list);
            this.context = context;
        }
        private class ViewHolder {
            TextView txtDefect;
            TextView txtComment;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            LayoutInflater inflater =  LayoutInflater.from(context);

            if (convertView == null)
            {
                convertView = inflater.inflate(R.layout.defectlist_row, null);
                holder = new ViewHolder();
                holder.txtDefect = (TextView) convertView.findViewById(R.id.col_defect_name);
                holder.txtComment = (TextView) convertView.findViewById(R.id.col_defect_comment);
                convertView.setTag(holder);
            }
            else
            {
                holder = (ViewHolder) convertView.getTag();
            }

            DvirLog.DvirDefect defect = getItem(position);
            holder.txtDefect.setText(defect.defectName);
            holder.txtComment.setText(defect.comment);

            return convertView;
        }
    }
}
