package com.dotcompliance.limologs;

import android.os.Bundle;

import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.view.BaseGridView;

public class GraphViewActivity extends LimoBaseActivity {

    BaseGridView gridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph_view);

        initialize();
    }

    private void initControls() {
        gridView = (BaseGridView) findViewById(R.id.grid_log_graph);
    }

    private void initialize() {
        setTitle("View Graph");
        setLeftMenuItem("Back");

        initControls();
        setConnectionStatus(Preferences.isConnected);
        gridView.setData(Preferences.mDriverLogs.get(0));
    }

    @Override
    protected void onMenuItemLeft() {
        finish();
    }
}
