package com.dotcompliance.limologs;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;

import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

public class DriversChecklistActivity extends LimoBaseActivity {

    ListView listViewItems;

    List<ItemData> checklist = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drivers_checklist);

        initialize();
    }

    protected void initialize() {
        setLeftMenuItem("Back");
        setRightMenuItem("Done");
        setConnectionStatus(Preferences.isConnected);
        listViewItems = (ListView) findViewById(R.id.listview_items);

        loadItems();
    }

    @Override
    protected void onMenuItemLeft() {
        finish();
    }

    @Override
    protected void onMenuItemRight() {
        // confirm checks
        RequestParams params = new RequestParams();

        params.put("vehicle_id", Preferences.getCurrentVehicle().vehicle_id);
        params.put("vehicle_clsid", Preferences.getCurrentVehicle().vehicle_clsid);

        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(Preferences.getDriverTimezone());
        params.put("date", sdf.format(cal.getTime()));

        List<Map<String, String>> list = new ArrayList<>();
        for (int i = 0; i < checklist.size(); i ++) {
            if (checklist.get(i).checked) {
                Map<String, String> item = new HashMap<>();
                item.put("item_id", checklist.get(i).itemId);
                list.add(item);
            }
        }
        params.put("list", list);

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.post(Preferences.getUrlWithCredential("/log/save_checklist"), params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
                startLoading();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                stopLoading();
                try {
                    int error = response.getInt("error");
                    if (error == 0) { // login success
                        finish();
                        showMessage("Done!");
                    } else {
                        Log.d("checklist failed: ", response.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.d("checklist download: ", "unexpected response");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                stopLoading();

                if (throwable != null) {
                    Log.d("Network error", " " +  throwable.getMessage());
                } else {
                    try {
                        Log.d("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    protected void loadItems() {

        String url = Preferences.getUrlWithCredential("/log/checklist");
        MyAsyncHttpClient client = new MyAsyncHttpClient();

        RequestParams params = new RequestParams();
        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(Preferences.getDriverTimezone());
        params.put("date", sdf.format(cal.getTime()));

        // params.put("vehicle_id", Preferences.getCurrentVehicle().vehicle_id); // Commenting this code to remove issue with vehicle no at DVIR/LOG screen MANGOIT ANDROID TEAM 28-09-2017
        params.put("vehicle_id",Preferences.mVehicleList.get(Preferences.mSelectedVehicleIndex).vehicle_id);

        client.get(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("http: ", this.getRequestURI().toString());
                startLoading();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                stopLoading();

                try {
                    int error = response.getInt("error");
                    if (error == 0) { // login success
                        Log.d("log/checklist", response.toString());
                        JSONArray array = response.getJSONArray("list");
                        for (int i = 0; i < array.length(); i++) {
                            ItemData item = new ItemData();
                            item.itemId = array.getJSONObject(i).getString("id");
                            item.title = array.getJSONObject(i).getString("title");
                            if (array.getJSONObject(i).isNull("checked"))
                                item.checked = false;
                            else
                                item.checked = array.getJSONObject(i).getInt("checked") == 1;
                            checklist.add(item);
                        }

                        MyListViewAdapter adapter = new MyListViewAdapter(mContext, R.id.listview_items, checklist);
                        listViewItems.setAdapter(adapter);
                    } else {
                        Log.d("checklist failed: ", response.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.d("checklist download: ", "unexpected response");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                stopLoading();

                if (throwable != null) {
                    Log.e("Network error", " " +  throwable.getMessage());
                } else {
                    try {
                        Log.e("network error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
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
                localView = LayoutInflater.from(context).inflate(R.layout.checklist_item, null);

                localViewHolder = new MyViewHolder();
                localViewHolder.chk = ((CheckBox) localView.findViewById(R.id.checkbox));
                localView.setTag(localViewHolder);
            }
            else if(localView.getTag() == null){
                localViewHolder = new MyViewHolder();
                localViewHolder.chk = ((CheckBox) localView.findViewById(R.id.checkbox));
                localView.setTag(localViewHolder);
            }

            localViewHolder = (MyViewHolder)localView.getTag();

            localViewHolder.chk.setText(item.title);
            localViewHolder.chk.setChecked(item.checked);

            localViewHolder.chk.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    boolean isChecked = !item.checked;
                    item.checked = isChecked;
                    notifyDataSetChanged();
                }
            });

            return localView;
        }

        class MyViewHolder
        {
            CheckBox chk;
        }
    }

    public class ItemData
    {
        public String itemId;
        public String title;
        public boolean checked;

        ItemData()
        {
        }
    }
}
