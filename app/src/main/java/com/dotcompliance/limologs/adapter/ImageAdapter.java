package com.dotcompliance.limologs.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.dotcompliance.limologs.R;
import com.dotcompliance.limologs.data.Preferences;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Hashtable;


public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<Hashtable<String, String>> items;

    public ImageAdapter(Context c, ArrayList<Hashtable<String, String>> list) {
        mContext = c;
        items = list;
    }

    public int getCount() {
        return items.size();
    }

    public Hashtable<String, String> getItem(int position) {
        return items.get(position);
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(180, 150));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        String url = Preferences.DOWNLOAD_LINK + "logsheets/" + Preferences.mDriverLogs.get(0).driverlog_id + "/" + getItem(position).get("filename");

        Picasso.with(mContext).load(url).into(imageView);

        return imageView;
    }
}
