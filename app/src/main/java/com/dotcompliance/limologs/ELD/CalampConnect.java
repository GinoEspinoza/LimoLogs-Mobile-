package com.dotcompliance.limologs.ELD;

/**
 * Created by kmw on 2017.09.11.
 */

import android.util.Log;

import com.dotcompliance.limologs.util.DataManager;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.Date;
import cz.msebera.android.httpclient.Header;

public class CalampConnect {

    private static final String apiBase = "https://connect.calamp.com/connect";
    private static final String username = "guinn37";
    private static final String password = "Development123";
    private static final String serviceAppCode = "d8b98aad-cc6e-4899-a86d-e2848eaf03b4";
    private static String authToken = "";
    private static Date lastUsed;

    public interface AvlEventInterface {
        void onFinished(AvlEvent event);
    }

    /*
        get authToken from Calamp api
     */
    public static void authenticate() {
        final String url = apiBase + "/services/login?useAuthToken=true";
        RequestParams params = new RequestParams();
        params.put("username", username);
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
                Log.d("TAG", headers.toString());

                for (Header header : headers) {
                    String name = header.getName();
                    String value = header.getValue();

                    if (name.equals("Set-Cookie")) {
                        authToken = value.substring(value.indexOf("=") + 1, value.indexOf(";"));
                        Log.d("authToken", authToken);

                        lastUsed = Calendar.getInstance().getTime();
                        Log.d("lastUsed", lastUsed.toString());
                    }
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

    public static void fetchAvlEvent(String deviceID, final AvlEventInterface avlEventInterface) {
//    public static void test (String deviceID) {
   /*     try {
            if (!isAvailable()) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        String url = apiBase + "/results/events/device/" + deviceID + "/avl/latest?v=2.0";
        RequestParams params = new RequestParams();
        params.put("numEvents", 1);
        params.put("pgsize", 1);

        AsyncHttpClient client = new AsyncHttpClient();

        client.addHeader("Accept", "application/json");
        client.addHeader("Calamp-Services-App", serviceAppCode);
        client.addHeader("Cookie", "authToken=" + authToken);

        client.get(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject responseBody) {
                lastUsed = Calendar.getInstance().getTime();
                try {
                    JSONObject response = responseBody.getJSONObject("response");
                    DataManager.getInstance().setAllEventResponse(response.toString());
                    JSONArray results = response.getJSONArray("results");
                    for (int i = 0; i < results.length(); i++) {
                        AvlEvent event = new AvlEvent();
                        JSONObject avlEvent = results.getJSONObject(i).getJSONObject("avlEvent");
                        JSONObject address = avlEvent.getJSONObject("address");

                        //Address
                        event.city = address.getString("city");
                        event.state = address.getString("state");
                        event.longitude = (float) address.getDouble("addressLongitude");
                        event.latitude = (float) address.getDouble("addressLatitude");

                        DataManager.getInstance().setLongitude("" + (float) address.getDouble("addressLongitude"));
                        DataManager.getInstance().setLatitude("" + (float) address.getDouble("addressLatitude"));

                        // first-level properties
                        event.eventCode = avlEvent.getInt("eventCode");
                        event.eventType = avlEvent.getString("eventType");
                        event.eventTime = avlEvent.getString("eventTime");
                        DataManager.getInstance().setTime( avlEvent.getString("eventTime"));
                        event.messageUuid = avlEvent.getString("messageUuid");
                        event.deviceMessageSequenceNumber = avlEvent.getInt("deviceMessageSequenceNumber");

//                         Ignition status
//                        print(avlJson["vbusIndicators"]["ignitionStatus"].stringValue)
//                        JSONObject ignitionStatusFromApi = avlEvent.getJSONObject("vbusIndicators");
//                        event.ignitionStatus = ignitionStatusFromApi.getBoolean("ignitionStatus");

                        event.ignitionStatus = avlEvent.getString("eventType");


                        // deviceDataConverted
                        JSONObject deviceDataConverted = avlEvent.getJSONObject("deviceDataConverted");
                        JSONObject gpsSpeed = deviceDataConverted.getJSONObject("gpsSpeed");
                        event.gpsOdometer = (float) gpsSpeed.getDouble("value");

                        JSONArray accumulators = deviceDataConverted.getJSONArray("accumulators");
                        for (int j = 0; j < accumulators.length(); j++) {
                            String label = accumulators.getJSONObject(j).getString("label");
                            if (label.equals("GPSOdometer")) {
                                event.gpsOdometer = (float) accumulators.getJSONObject(j).getDouble("value");
                                DataManager.getInstance().setGpsodometer("" + (float) accumulators.getJSONObject(j).getDouble("value"));
                            } else if (label.equals("VBSpeed"))
                                event.vbSpeed = (float) accumulators.getJSONObject(j).getDouble("value");
                            else if (label.equals("VBOdometer")) {
                                event.vbOdometer = (float) accumulators.getJSONObject(j).getDouble("value");
                                DataManager.getInstance().setVbodometer(""+(float) accumulators.getJSONObject(j).getDouble("value"));
                            }/*else if (label.equals("Time")){
                                DataManager.getInstance().setTime(""+(float) accumulators.getJSONObject(j).getDouble("value"));
                            }*/
                        }
                        avlEventInterface.onFinished(event);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable error, JSONObject responseBody) {

            }
        });
    }
}
