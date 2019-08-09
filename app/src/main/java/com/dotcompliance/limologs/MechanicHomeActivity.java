package com.dotcompliance.limologs;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.dotcompliance.limologs.adapter.DvirListAdapter;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.RestTask;

public class MechanicHomeActivity extends LimoBaseActivity {
    private ListView listviewDvir;
    private DvirListAdapter lvAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_mechanic_home);

        initialize();
        RestTask.loadCompanyVehicleList(null);

        RestTask.downloadDvirList(new RestTask.TaskCallbackInterface() {
            @Override
            public void onTaskCompleted(Boolean success, String message) {
                if (success) {
                    lvAdapter = new DvirListAdapter(mContext, Preferences.mDvirList);
                    listviewDvir.setAdapter(lvAdapter);
                }
                else {
                    showMessage("Failed to load DVIRs: " + message);
                }
            }
        });
    }

    protected void initialize() {
        listviewDvir = (ListView)findViewById(R.id.list_mechanic_dvir);
        listviewDvir.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(mContext, DvirCheckActivity.class);
                intent.putExtra("dvir_index", position - 1);
                startActivityForResult(intent, 1);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mechanic_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_create_dvir) {
            startActivity(new Intent(mContext, MechanicDvirActivity.class));
        }
        else if (id == R.id.action_body_inspection) {
            startActivity(new Intent(mContext, MechanicInspectActivity.class));
        }
        else if (id == R.id.action_sign_out) {
            Preferences.clearSession(mContext);
            Preferences.mDvirList.clear();
            startActivity(new Intent(mContext, LoginActivity.class));
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK)
                lvAdapter.notifyDataSetChanged();
        }
    }
}
