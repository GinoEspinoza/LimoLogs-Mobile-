package com.dotcompliance.limologs;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.data.Vehicle;
import com.dotcompliance.limologs.network.RestTask;

import java.util.ArrayList;

public class MechanicInspectActivity extends LimoBaseActivity {
    ListView listViewVehicles;
    ArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_mechanic_inspect);

        initialize();
    }

    protected void initialize() {
        setLeftMenuItem("Back");

        listViewVehicles = (ListView) findViewById(R.id.listview_vehicles);

        final ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < Preferences.mVehicleList.size(); i ++ ) {
            Vehicle v = Preferences.mVehicleList.get(i);
            list.add(v.vehicle_no + " / " + v.vehicle_class + " / " + v.inspect_count + " scratches or dents");
        }

        adapter = new ArrayAdapter(mContext, android.R.layout.simple_list_item_1, list);
        listViewVehicles.setAdapter(adapter);
        listViewVehicles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final int pos = position;
                startLoading();
                RestTask.loadBodyInspection(Preferences.mVehicleList.get(pos), new RestTask.TaskCallbackInterface() {
                    @Override
                    public void onTaskCompleted(Boolean success, String message) {
                        stopLoading();

                        Intent intent = new Intent(mContext, BodyInspectionActivity.class);
                        intent.putExtra("vehicle_index", pos);
                        intent.putExtra("driver_mode", false);
                        startActivity(intent);
                    }
                });
            }
        });
    }

    @Override
    protected void onMenuItemLeft() {
        super.onMenuItemLeft();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        adapter.notifyDataSetChanged();
    }
}
