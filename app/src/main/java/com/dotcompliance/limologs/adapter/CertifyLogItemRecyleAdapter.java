package com.dotcompliance.limologs.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.dotcompliance.limologs.R;
import com.dotcompliance.limologs.data.CertifylogsModel;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by saurabh on 12/1/2017.
 */

public class CertifyLogItemRecyleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<CertifylogsModel> certifyLogList;
    private ArrayList<String> checkedList = new ArrayList<String>();
    public static String strselected = "";
    private static final int TYPE_FOOTER = 1;
    private static final int TYPE_ITEM = 2;


    public CertifyLogItemRecyleAdapter(List<CertifylogsModel> certifyLogList) {
        this.certifyLogList = certifyLogList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.certifylog_item, parent, false);

            return new MyViewHolder(itemView);
        } else if (viewType == TYPE_FOOTER) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.certify_header, parent, false);
            return new FooterViewHolder(itemView);
        } else return null;

    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof MyViewHolder) {
            final MyViewHolder itemViewHolder = (MyViewHolder) holder;
            //  final CertifylogsModel certifylogsModel = certifyLogList.get(position);
            itemViewHolder.txtLogDate.setText(certifyLogList.get(position).getLog_date());

            itemViewHolder.cbLogsData.setTag(position);
            itemViewHolder.cbLogsData.setOnCheckedChangeListener(null);
            itemViewHolder.cbLogsData.setChecked(certifyLogList.get(position).isChecked());

            if (certifyLogList.get(position).isChecked()){
                checkedList.add(certifyLogList.get(itemViewHolder.getAdapterPosition()).getDriverlog_id());
            }else {
                certifyLogList.get(itemViewHolder.getAdapterPosition()).setChecked(false);
                for (int i = 0; i < checkedList.size(); i++) {
                    if (checkedList.get(i).toString().contains(certifyLogList.get(itemViewHolder.getAdapterPosition()).getDriverlog_id())) {
                        checkedList.remove(i);
                    }


                }
            }

            StringUtils.join(checkedList, ',');
            Log.e("Checked value", "onClick: " + checkedList.toString());
            strselected = checkedList.toString().replace("[", "");
            strselected = strselected.replace("]", "");
            Log.e("Checked value", "onClick: " + strselected);



            itemViewHolder.cbLogsData.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    certifyLogList.get(itemViewHolder.getAdapterPosition()).setChecked(isChecked);
                    if (isChecked){
                        checkedList.add(certifyLogList.get(itemViewHolder.getAdapterPosition()).getDriverlog_id());
                    }else {
                        certifyLogList.get(itemViewHolder.getAdapterPosition()).setChecked(false);
                        for (int i = 0; i < checkedList.size(); i++) {
                            if (checkedList.get(i).toString().contains(certifyLogList.get(itemViewHolder.getAdapterPosition()).getDriverlog_id())) {
                                checkedList.remove(i);
                            }


                        }
                    }

                    StringUtils.join(checkedList, ',');
                    Log.e("Checked value", "onClick: " + checkedList.toString());
                    strselected = checkedList.toString().replace("[", "");
                    strselected = strselected.replace("]", "");
                    Log.e("Checked value", "onClick: " + strselected);

                }
            });

        } else if (holder instanceof FooterViewHolder) {

        }
    }


    private class FooterViewHolder extends RecyclerView.ViewHolder {
        TextView footerText;


        public FooterViewHolder(View view) {
            super(view);
            // footerText = (TextView) view.findViewById(R.id.footer_text);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == certifyLogList.size()) {
            return TYPE_FOOTER;
        }
        return TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return certifyLogList.size() + 1;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private CheckBox cbLogsData;
        private TextView txtLogDate;

        public MyViewHolder(View view) {
            super(view);
            cbLogsData = (CheckBox) view.findViewById(R.id.cb_logsData);
            txtLogDate = (TextView) view.findViewById(R.id.txt_logDate);
        }
    }
}
