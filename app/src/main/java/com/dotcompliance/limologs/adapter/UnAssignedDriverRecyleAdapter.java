package com.dotcompliance.limologs.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.dotcompliance.limologs.LastDvirActivity;
import com.dotcompliance.limologs.R;
import com.dotcompliance.limologs.UnassignedDriverActivity;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.data.UnassignedTime;
import com.dotcompliance.limologs.util.DataManager;

import java.util.ArrayList;

import static com.dotcompliance.limologs.UnassignedDriverActivity.AssignDriverToVechile;
import static com.dotcompliance.limologs.UnassignedDriverActivity.comma_seperated_chk;

/**
 * Created by saurabh on 12/1/2017.
 */

public class UnAssignedDriverRecyleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<UnassignedTime> unassignedTimeArrayList = new ArrayList<UnassignedTime>();
    private Context context;
    private LayoutInflater layoutInflater;
    private static final int TYPE_FOOTER = 1;
    private static final int TYPE_ITEM = 2;
    private static final int TYPE_HEADER = 3;
    public String str;

    public UnAssignedDriverRecyleAdapter(ArrayList<UnassignedTime> unassignedTimeArrayList, Context context) {
        this.unassignedTimeArrayList = unassignedTimeArrayList;
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.unassigned_driver_list_row, parent, false);

            return new MyViewHolder(itemView);
        } else if (viewType == TYPE_FOOTER) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.unassigned_driver_footer, parent, false);
            return new FooterView(itemView);
        } else if (viewType == TYPE_HEADER) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.unassigned_driver_list_row, parent, false);
            return new HeaderView(itemView);
        } else return null;

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

        if (holder instanceof MyViewHolder) {
            final MyViewHolder itemViewHolder = (MyViewHolder) holder;
            String vehicle_name = "";
            //  if (pos > 0) {

            try {

                /*if (vehicle_name.equals("")) {
                    for (int i = 0; i < Preferences.mVehicleList.size(); i++) {
                        if (Preferences.mVehicleList.get(i).vehicle_id == Integer.parseInt(unassignedTimeArrayList.get(position).un_vehicle_id)) {
                           // vehicle_name = Preferences.mVehicleList.get(i).vehicle_no;
                            break;
                        }
                    }


                    if (vehicle_name.isEmpty()) {
                        vehicle_name = unassignedTimeArrayList.get(position).un_vehicle_id;
                    }
                }*/

                itemViewHolder.vehicleName.setText(vehicle_name);
                itemViewHolder.colStartTime.setText(unassignedTimeArrayList.get(position).un_starttime);
                itemViewHolder.colEndTime.setText(unassignedTimeArrayList.get(position).un_endtime);
                //}
                itemViewHolder.checkUnassignedDriver.setTag(position);
                itemViewHolder.checkUnassignedDriver.setOnCheckedChangeListener(null);
                itemViewHolder.checkUnassignedDriver.setChecked(unassignedTimeArrayList.get(position).is_checked());
                itemViewHolder.checkUnassignedDriver.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        unassignedTimeArrayList.get(itemViewHolder.getAdapterPosition()).setIs_checked(isChecked);
                        if (isChecked) {
//                        unassignedTimeArrayList.add(unassignedTimeArrayList.get(itemViewHolder.getAdapterPosition()).setIs_checked(true));
                            unassignedTimeArrayList.get(position).setIs_checked(true);
                            DataManager.getInstance().setUnassignedList(unassignedTimeArrayList);
                        } else {
                            unassignedTimeArrayList.get(position).setIs_checked(false);
                            DataManager.getInstance().setUnassignedList(unassignedTimeArrayList);
                        }

                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (holder instanceof FooterView) {
            final FooterView itemViewHolder = (FooterView) holder;
            itemViewHolder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    str = "";
                    comma_seperated_chk = "";

                    for (int i = 0; i < DataManager.getInstance().getUnassignedList().size(); i++) {
                        if (DataManager.getInstance().getUnassignedList().get(i).is_checked == true) {

                            try {
                                comma_seperated_chk = comma_seperated_chk + "," + DataManager.getInstance().getUnassignedList().get(i).un_id; // DataManager.getInstance().getUnassignedList().get(i).un_vehicle_id ;
                                str = comma_seperated_chk.substring(1/*,comma_seperated_chk.length()-1*/);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        //StringUtils.join(slist, ',');
                    }
                    if (str.isEmpty()) {
                        //Toast.makeText(mContext, "select atleast one check box" , Toast.LENGTH_SHORT).show();
                        Intent intent = ((Activity) context).getIntent();
                        intent = new Intent(context, LastDvirActivity.class);
                    } else {

                        AssignDriverToVechile();

                    }
                }
            });
        } else if (holder instanceof HeaderView) {
            final HeaderView itemViewHolder = (HeaderView) holder;
            itemViewHolder.checkUnassignedDriver.setVisibility(View.GONE);
            itemViewHolder.vehicleName.setTypeface(null, Typeface.BOLD);
            itemViewHolder.txtselect.setTypeface(null, Typeface.BOLD);
            itemViewHolder.colStartTime.setTypeface(null, Typeface.BOLD);
            itemViewHolder.colEndTime.setTypeface(null, Typeface.BOLD);
            itemViewHolder.txtselect.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public int getItemViewType(int position) {
        if (position == unassignedTimeArrayList.size() + 1) {
            return TYPE_FOOTER;
        } else if (position == 0) {
            return TYPE_HEADER;
        }
        return TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return unassignedTimeArrayList.size() + 1;
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {
        private CheckBox checkUnassignedDriver;
        private TextView vehicleName;
        private TextView colStartTime;
        private TextView colEndTime;

        public MyViewHolder(View view) {
            super(view);
            checkUnassignedDriver = (CheckBox) view.findViewById(R.id.check_unassigned_driver);
            vehicleName = (TextView) view.findViewById(R.id.vehicle_name);
            colStartTime = (TextView) view.findViewById(R.id.col_start_time);
            colEndTime = (TextView) view.findViewById(R.id.col_end_time);
        }
    }


    public class HeaderView extends RecyclerView.ViewHolder {
        private CheckBox checkUnassignedDriver;
        private TextView vehicleName;
        private TextView colStartTime;
        private TextView colEndTime;
        private TextView txtselect;

        public HeaderView(View view) {
            super(view);
            checkUnassignedDriver = (CheckBox) view.findViewById(R.id.check_unassigned_driver);
            vehicleName = (TextView) view.findViewById(R.id.vehicle_name);
            colStartTime = (TextView) view.findViewById(R.id.col_start_time);
            colEndTime = (TextView) view.findViewById(R.id.col_end_time);
            txtselect = (TextView) view.findViewById(R.id.txtselect);
        }
    }

    public class FooterView extends RecyclerView.ViewHolder {
        private Button button;

        public FooterView(View view) {
            super(view);
            button = (Button) view.findViewById(R.id.btn_save);
        }
    }
}
