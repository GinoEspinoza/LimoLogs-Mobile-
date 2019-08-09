package com.dotcompliance.limologs.ELD;

/**
 * Created by kmw on 2017.09.11.
 */

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.dotcompliance.limologs.network.MyAsyncHttpClient;
import com.dotcompliance.limologs.util.DataManager;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import cz.msebera.android.httpclient.Header;

public class GPSVOXConnect {

    private static final String apiBase = "http://51.15.219.83/api";
    private static final String username = "admin@yourdomain.com";
    private static final String password = "pass6342word";
    private static final String serviceAppCode = "d8b98aad-cc6e-4899-a86d-e2848eaf03b4";
    private static String authToken = "";
    private static Date lastUsed;
    private static Context baseContext;
    private static String city = "";
    private static String state = "";
    static ArrayList<AvlEvent> eventList = new ArrayList<>();
    static ArrayList<AvlItemListModel> eventitemList = new ArrayList<>();


    public GPSVOXConnect(Context baseContext) {
        this.baseContext = baseContext;
    }


    public interface Gpxinterface {
        void onFinished(AvlEvent event);
    }

    /*
        get authToken from Calamp api
     */
    public static void authenticate() {
        final String url = apiBase + "/login";

        RequestParams params = new RequestParams();
        params.put("email", username);
        params.put("password", password);

        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
                Log.d("connect", url);
            }

            @Override
            public void onFinish() {
                Log.d("connect", "finished");
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    Log.d("TAG", responseBody.toString());
                    String str = new String(responseBody, "UTF-8");
                    Log.d("responseBody", str);

                    JSONObject response_Obj = new JSONObject(str);
                    String user_api_hash = response_Obj.optString("user_api_hash");
                    Log.e("user_api_hash", "onSuccess: " + user_api_hash);
                    authToken = user_api_hash;
//                    for (Header header : headers) {
//                        String name = header.getName();
//                        String value = header.getValue();
//
//                        if (name.equals("Set-Cookie")) {
//                            authToken = value.substring(value.indexOf("=") + 1, value.indexOf(";"));
//                            Log.d("authToken", authToken);
//
//                            lastUsed = Calendar.getInstance().getTime();
//                            Log.d("lastUsed", lastUsed.toString());
//                        }
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d("Error", "code=" + statusCode);
            }
        });
    }

    /*
        set  flag request per 60mins
     */
    public static Boolean isAvailable() {
        try {
            long diff = 0;
            Date current = Calendar.getInstance().getTime();
            Log.d("current", current.toString());
            diff = (current.getTime() - lastUsed.getTime()) / 1000;
            Log.d("difference", String.valueOf(diff));

            if (lastUsed != null && !authToken.isEmpty() && diff < 60) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    public static void fetchAvlEvent(String deviceID, final Gpxinterface gpxinterface) {
//    public static void test (String deviceID) {
  /*      if (!isAvailable()) {
        }*/
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        DateFormat timeformat = new SimpleDateFormat("HH:mm");
        Date date1 = new Date();
        Log.e("date", "saveLogsheet: " + timeformat.format(date1));
        Calendar now = Calendar.getInstance();
        now.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar now1 = Calendar.getInstance();
        now1.setTimeZone(TimeZone.getTimeZone("UTC"));
        now.add(Calendar.MINUTE, -1);
        Log.e("date", "saveLogsheet: " + now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE));

        String url = apiBase + "/get_history";
        RequestParams params = new RequestParams();
        params.put("user_api_hash", authToken);
        params.put("lang", "en");
        params.put("device_id", deviceID);
        params.put("from_date", /*"2017-11-25"*/ dateFormat.format(date));
        params.put("to_date",/* "2017-11-25"*/ dateFormat.format(date));
        int fromhour = now.get(Calendar.HOUR_OF_DAY);
        int fromminute = now.get(Calendar.MINUTE);
        int tohour = now1.get(Calendar.HOUR_OF_DAY);
        int tominute = now1.get(Calendar.MINUTE);
        String fromtime = String.format("%02d:%02d", fromhour, fromminute);
        String totime = String.format("%02d:%02d", tohour, tominute);


        Log.e("from", "fetchAvlEvent: " + fromtime);
        Log.e("to", "fetchAvlEvent: " + totime);
        params.put("from_time", fromtime)/*timeformat.format(date1)*/;
        params.put("to_time", totime/* now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE)*/);


        MyAsyncHttpClient client = new MyAsyncHttpClient();
        client.addHeader("Accept", "application/json");

        client.get(url, params, new JsonHttpResponseHandler() {
            public static final String TAG = "GPSVOX";

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {

                Log.e("GPSVOX", "onSuccess: " + responseBody.toString());
                try {
                    if (responseBody != null) {
                        eventitemList.clear();
                        eventList.clear();
                        DataManager.getInstance().ClearGPXboxList();
                        JSONArray response_Array = responseBody.optJSONArray("items");
                        if (responseBody != null) {
                            DataManager.getInstance().setGpxBoolean(true);
                        }
                        JSONObject deviceobj = responseBody.optJSONObject("device");
                        JSONObject traccarobk = deviceobj.optJSONObject("traccar");
                        if (response_Array == null) {
                            Log.e(TAG, "onSuccess: " + "Latitude :-----" + traccarobk.optInt("lastValidLatitude") +
                                    "Longitude--------------->>" + traccarobk.optInt("lastValidLongitude"));
                            DataManager.getInstance().setLatitude("" + traccarobk.optDouble("lastValidLatitude"));
                            DataManager.getInstance().setLongitude("" + traccarobk.optDouble("lastValidLongitude"));
                        }
                        if (response_Array != null) {
                            for (int i = 0; i < response_Array.length(); i++) {
                                AvlEvent event = new AvlEvent();
                                JSONObject main_ArrayObj = response_Array.optJSONObject(i);
                                int status = main_ArrayObj.optInt("status");

                                if (status == 1 || status == 2) {
                                    event.setStatus(status);
                                    if (main_ArrayObj.has("engine_work") && main_ArrayObj.has("engine_idle")) {
                                        int engine_work = main_ArrayObj.optInt("engine_work");
                                        int engine_idle = main_ArrayObj.optInt("engine_idle");
//                                    Log.e("engine_work", "onSuccess: " + engine_work);
                                        int EngineHours = (engine_work + engine_idle);
                                        JSONArray itemarray = main_ArrayObj.optJSONArray("items");


                                        for (int j = 0; j < itemarray.length(); j++) {
                                            int size = itemarray.length();
                                            float enginehours = EngineHours / size;
                                            event.setEngineHours(enginehours);
                                            AvlItemListModel itemListModel = new AvlItemListModel();
                                            JSONObject itemobj = itemarray.optJSONObject(j);
                                            JSONArray otherArray = itemobj.optJSONArray("other_arr");
                                            String str = otherArray.get(1).toString().replace("ignition: ", "");
                                            String strodometer = otherArray.get(3).toString().replace("odometer: ", "");
                                            itemListModel.setIgnition(Boolean.parseBoolean(str));
                                            itemListModel.setOdometer(Integer.parseInt(strodometer));
                                            //  }

                                            itemListModel.setServer_time(itemobj.optString("server_time"));
                                            itemListModel.setSpeed(itemobj.optInt("speed"));
                                            Log.e(TAG, "onSuccess: " + itemListModel.getSpeed());
                                            itemListModel.setLatitude(itemobj.optDouble("latitude"));
                                            itemListModel.setLongitude(itemobj.optDouble("longitude"));
                                          /*  getCityAndState(Double.parseDouble(Float.toString(event.getLatitude()))
                                                    , Double.parseDouble(Float.toString(event.getLongitude())));*/
                                            event.setCity("");
                                            event.setState("");
                                            Log.e("engine_work1", "onSuccess: " + itemobj.optInt("id"));
                                            eventitemList.add(itemListModel);


                                        }

                                        event.setItemList(eventitemList);
                                        eventList.add(event);

                                        DataManager.getInstance().setGPXboxEventList(eventList);
                                    }


                                }

                            }


                        }


                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable error, JSONObject responseBody) {
                Log.e(TAG, "onFailure: " + error);
            }
        });
    }

   /* public static void getCityAndState(double latitude, double longitude) {
        Geocoder geoCoder = new Geocoder(baseContext, Locale.getDefault());
        try {
            List<Address> addresses = geoCoder.getFromLocation(latitude, longitude, 1);

            String add = "";
            if (addresses.size() > 0) {
                for (int i = 0; i < addresses.get(0).getMaxAddressLineIndex(); i++)
                    add += addresses.get(0).getAddressLine(i) + "\n";
                state = addresses.get(0).getAdminArea();
                city = addresses.get(0).getLocality();

            }

            // showToastMessage(add);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }*/
}
