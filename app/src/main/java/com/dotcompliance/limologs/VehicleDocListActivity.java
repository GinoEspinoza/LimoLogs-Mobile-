package com.dotcompliance.limologs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.data.VehicleDoc;

import java.util.List;

/**
 * An activity representing a list of VehicleDocs. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link VehicleDocumentDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class VehicleDocListActivity extends LimoBaseActivity {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicledoc_list);

        setTitle("Vehicle Documents");
        setLeftMenuItem("Back");
        setConnectionStatus(Preferences.isConnected);

        View recyclerView = findViewById(R.id.vehicledoc_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);

        if (findViewById(R.id.vehicledoc_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }

    @Override
    protected void onMenuItemLeft() {
        finish();
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(Preferences.getCurrentVehicle().docs));
    }

    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final List<VehicleDoc> mValues;

        public SimpleItemRecyclerViewAdapter(List<VehicleDoc> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.vehicledoc_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);
          //  holder.mIdView.setText(mValues.get(position).id);
            holder.mIdView.setVisibility(View.GONE);
            holder.mContentView.setText(mValues.get(position).title);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = Preferences.DOWNLOAD_LINK + "vehicledocs/" + Preferences.getCurrentVehicle().vehicle_id + "/" + holder.mItem.filename;

                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/viewer?url=" + url));
                    startActivity(browserIntent);

                    /*if (mTwoPane) {
                        Bundle arguments = new Bundle();

                        arguments.putString(VehicleDocDetailFragment.ARG_ITEM_URL, url);
                        VehicleDocDetailFragment fragment = new VehicleDocDetailFragment();
                        fragment.setArguments(arguments);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.vehicledoc_detail_container, fragment)
                                .commit();
                    } else {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, VehicleDocumentDetailActivity.class);
                        intent.putExtra(VehicleDocDetailFragment.ARG_ITEM_URL, url);

                        context.startActivity(intent);
                    }*/
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mIdView;
            public final TextView mContentView;
            public VehicleDoc mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mIdView = (TextView) view.findViewById(R.id.id);
                mContentView = (TextView) view.findViewById(R.id.content);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mContentView.getText() + "'";
            }
        }
    }
}
