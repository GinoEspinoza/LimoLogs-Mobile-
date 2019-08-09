package com.dotcompliance.limologs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;

import com.dotcompliance.limologs.data.Defect;
import com.dotcompliance.limologs.data.DvirLog;
import com.dotcompliance.limologs.data.Preferences;

import java.util.ArrayList;
import java.util.List;

public class DefectActivity extends LimoBaseActivity {
    RadioButton radioDefect;
    RadioButton radioUndefect;

    ListView listview;
    List<ItemData> data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_defect);
        setLeftMenuItem("Cancel");
        setRightMenuItem("Save");
        setConnectionStatus(Preferences.isConnected);
        radioDefect = (RadioButton) findViewById(R.id.radio_defect_affect);
        radioUndefect = (RadioButton) findViewById(R.id.radio_defect_unaffect);
        listview = (ListView)findViewById(R.id.listview);

        data = new ArrayList<>();
        for (int i = 0; i < Preferences.mDefectList.size(); i ++) {
            ItemData item = new ItemData(false, "");
            Defect defect = Preferences.mDefectList.get(i);
            item.defect_id = defect.defect_id;
            item.defect_name = defect.defect_name;

            data.add(item);
        }

        int log_index = getIntent().getIntExtra("log_index", 0);
        int dvir_index = getIntent().getIntExtra("dvir_index", -1);
        if (dvir_index > -1) {
            DvirLog dvirLog = Preferences.mDriverLogs.get(log_index).dvirList.get(dvir_index);
            for (int i = 0; i < dvirLog.defectList.size(); i ++) {
                for (int j = 0; j < data.size(); j ++) {
                    if (dvirLog.defectList.get(i).defectName.equals(data.get(j).defect_name)) {
                        data.get(j).checked = true;
                        data.get(j).comment = dvirLog.defectList.get(i).comment;
                        break;
                    }
                }
            }
            radioDefect.setChecked(dvirLog.isDefected);
        }

        MyListViewAdapter adapter = new MyListViewAdapter(this, R.layout.list_item_selected, data);

        listview.setAdapter(adapter);

    }

    @Override
    protected void onMenuItemLeft() {
        Intent resultIntent = new Intent();
        setResult(RESULT_CANCELED, resultIntent);
        finish();
    }

    @Override
    protected void onMenuItemRight() {
        ArrayList<Integer> listCheckedIDs = new ArrayList<>();
        ArrayList<String> listComments = new ArrayList<>();
        ArrayList<String> listDefects = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            ItemData defect = data.get(i);
            if (defect.checked) {
                listCheckedIDs.add(defect.defect_id);
                listComments.add(defect.comment);
                listDefects.add(defect.defect_name);
            }
        }
        Intent resultIntent = new Intent();
        resultIntent.putExtra("defect_affectable", radioDefect.isChecked());
        resultIntent.putIntegerArrayListExtra("checked_ids", listCheckedIDs);
        resultIntent.putStringArrayListExtra("comments", listComments);
        resultIntent.putStringArrayListExtra("defects", listDefects);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    public class MyListViewAdapter  extends ArrayAdapter<ItemData>
    {
        private Context context;

        public MyListViewAdapter(Context c, int resource, List<ItemData> items) {
            super(c, resource, items);
            context = c;
        }


        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View localView = convertView;
            MyViewHolder localViewHolder;

            final ItemData item = getItem(position);

            if(localView == null) {
                localView = LayoutInflater.from(context).inflate(R.layout.list_item_selected, null);

                localViewHolder = new MyViewHolder();
                localViewHolder.chk = ((CheckBox) localView.findViewById(R.id.chkSelect));
                localViewHolder.et = ((EditText) localView.findViewById(R.id.etComment));
                localView.setTag(localViewHolder);
            }else if(localView.getTag() == null){
                localViewHolder = new MyViewHolder();
                localViewHolder.chk = ((CheckBox) localView.findViewById(R.id.chkSelect));
                localViewHolder.et = ((EditText) localView.findViewById(R.id.etComment));
                localView.setTag(localViewHolder);
            }

            localViewHolder = (MyViewHolder)localView.getTag();

            localViewHolder.chk.setText(item.defect_name);
            if(item.checked) {
                localViewHolder.chk.setChecked(true);
                localViewHolder.et.setText(item.comment);
                localViewHolder.et.setVisibility(View.VISIBLE);
            }else{
                localViewHolder.chk.setChecked(false);
                localViewHolder.et.setVisibility(View.GONE);
            }

            localViewHolder.chk.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    boolean isChecked = !item.checked;
                    item.checked = isChecked;
                    if(!isChecked){
                        item.comment = "";
                    }
                    notifyDataSetChanged();
                }
            });

            localViewHolder.et.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if(item.checked)
                        item.comment = s.toString();
                }
            });

            return localView;
        }

        class MyViewHolder
        {
            CheckBox chk;
            EditText et;
        }
    }

    public class ItemData extends Defect
    {
        public boolean checked;
        public  String comment;

        ItemData(boolean c, String m)
        {
            checked = c;
            comment = m;
        }
    }
}
