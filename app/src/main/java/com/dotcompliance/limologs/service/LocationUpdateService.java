package com.dotcompliance.limologs.service;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.network.RestTask;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class LocationUpdateService extends Service {
    public static boolean IS_SERVICE_RUNNING = false;

    private static final String TAG = "LocationService3";

    private static final int GPS_TIME_INTERVAL = 12000;

    private LocationManager mLocationManager;
    private LimoLocationListener mLocationListener;

    String strApiBasePath;
    String strToken = "";
    String driver_id = "";

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Service is created");

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Starting service");

        strApiBasePath = Preferences.API_BASE_PATH;
        //strApiBasePath = "http://192.168.2.119/apilimo/index.php/v1";
        strToken = Preferences.getSession("TOKEN", getApplicationContext());
        driver_id = Preferences.getSession("DRIVER_ID", getApplicationContext());

        Log.i(TAG, strToken);

        if (driver_id.equals("") || strToken.equals(""))
            return START_NOT_STICKY;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return START_STICKY;
        }
        mLocationListener = new LimoLocationListener();
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_TIME_INTERVAL, 50, mLocationListener);

        IS_SERVICE_RUNNING = true;

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Destroy service");
        IS_SERVICE_RUNNING = false;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager.removeUpdates(mLocationListener);
    }

    private class LimoLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            Log.i(TAG, "Location changed lat/lng: " + location.getLatitude() + " / " + location.getLongitude());
            String address = "";

            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                if (addresses.size() > 0) {
                    for (int i = 0; i < addresses.get(0).getMaxAddressLineIndex(); i ++) {
                        address += addresses.get(0).getAddressLine(i) + ", ";
                    }
                    if (address.length() > 2)
                        address = address.substring(0, address.length()-2);
                    Log.i(TAG, address);
                }
            }
            catch (IOException e) {
                Log.d("Geocoder: ", e.getLocalizedMessage());
            }

            commitLocation(location.getLatitude(), location.getLongitude(), address);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    private void commitLocation(double lat, double lng, String address) {
        String strVehicleNum = Preferences.getSession("VEHICLE_NUM", getApplicationContext());

        MyAsyncHttpClient client = new MyAsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("latitude", lat);
        params.put("longitude", lng);
        params.put("location", address);
        params.put("vehicle_num", strVehicleNum);
        Log.i(TAG, "vehicle " + strVehicleNum);

        String url = strApiBasePath + "/log/update_location" + "?driver_id=" + driver_id + "&token=" + strToken;

        client.post(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.i("http: ", this.getRequestURI().toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("location update", "success");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, java.lang.Throwable throwable, org.json.JSONObject errorResponse) {
                if (throwable != null) {
                    Log.e("Network error", " " +  throwable.getMessage());
                } else {
                    try {
                        Log.d("location error: ", errorResponse.toString(4));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
