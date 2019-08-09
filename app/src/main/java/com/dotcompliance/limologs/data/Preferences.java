package com.dotcompliance.limologs.data;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.util.Log;

import com.dotcompliance.limologs.HomeActivity;
import com.dotcompliance.limologs.LogsActivity;
import com.google.firebase.crash.FirebaseCrash;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.TimeZone;


public class Preferences {
    // Static APP Info
    public static final String LIMO_APP_ID = "limologs";
    public static final int APP_VERSION = 2136; // X.X.XX

    //public static final String API_BASE_PATH = "http://api.limologs.com/v1";
    //public static final String API_BASE_PATH = "http://192.168.2.156/apilimo/index.php/v1";
    public static final String API_BASE_PATH = "http://dev.limologs.com/rest/web/index.php/v1";

    //public static final String DOWNLOAD_LINK = "http://files.limologs.com/";
    //public static final String DOWNLOAD_LINK = "http://192.168.2.156/limo/uploads/";
    public static final String DOWNLOAD_LINK = "http://dev.limologs.com/uploads/";

    public static int CYCLE_HOUR = 70 * 60;
    public static int CYCLE_DAY = 8;
    public static int OFFDUTY_HOUR = 8 * 60;
    public static int DRIVING_HOUR = 10 * 60;
    public static int ONDUTY_HOUR = 15 * 60;
    public static boolean isREST_BREAK = false;

    public static String API_TOKEN;

    public static DriverInfo mDriver = null;
    public static ArrayList<Defect> mDefectList = new ArrayList<>();
    public static ArrayList<Vehicle> mVehicleList = new ArrayList<>();
    public static ArrayList<VehicleInspect> mBodyInspectList = new ArrayList<>();

    public static ArrayList<UnassignedTime> unassignedTimes = new ArrayList<>();//Unassigned Time

    public static ArrayList<DriverLog> mDriverLogs = new ArrayList<>(20); // since we log for two weeks

    public static Date mLastLogDate = null;

    public static int mVehicleIndex = -1;
    public static int mDvirId = 0;
    public static String mVehicles, mTotalMiles;
    public static boolean mOutOfService = false;
    public static String mCurrentLocation = "";
    public static int VechileId = 0;
    public static boolean isConnected = false;
    public static boolean isCertify = false;

    public static int mSelectedVehicleIndex = 0; // This varible is created by MANGOIT ANDROID TEAM 28-09-2017 to remove bugs in 1st updation

    public static boolean isFullyActive = false;
    public static HomeActivity logsActivity = null;

    // Mechanic values
    public static ArrayList<DvirLog> mDvirList = null;

    public static String loginPath() {
        return API_BASE_PATH + "/limo/authenticate?appid=" + LIMO_APP_ID;
    }

    public static String getUrlWithCredential(String path) {
        if (Preferences.mDriver == null) {
            FirebaseCrash.log("Driver info is null at getUrlWithCredential");
            return "";
        }
        if (Preferences.mDriver.isDriver) {
            return API_BASE_PATH + path + "?driver_id=" + mDriver.driver_id + "&appid=" + LIMO_APP_ID + "&token=" + API_TOKEN;
        } else {
            return API_BASE_PATH + path + "?mechanic_id=" + mDriver.driver_id + "&appid=" + LIMO_APP_ID + "&token=" + API_TOKEN;
        }
    }

    public static String getConnection(String path, String vechileid) {
        if (Preferences.mDriver == null) {
            FirebaseCrash.log("Driver info is null at getUrlWithCredential");
            return "";
        }
        if (Preferences.mDriver.isDriver) {
            return API_BASE_PATH + path + "?vehicle_id=" + vechileid + "&driver_id=" + mDriver.driver_id + "&token=" + API_TOKEN;
        } else {
            return API_BASE_PATH + path + "?vehicle_id=" + vechileid + "&driver_id=" + mDriver.driver_id + "&token=" + API_TOKEN;
        }
    }

    public static String getCertificatioLog(String path) {
        if (Preferences.mDriver == null) {
            FirebaseCrash.log("Driver info is null at getUrlWithCredential");
            return "";
        }
        if (Preferences.mDriver.isDriver) {
            return API_BASE_PATH + path + "?driver_id=" + mDriver.driver_id + "&token=" + API_TOKEN;
        } else {
            return API_BASE_PATH + path + "?driver_id=" + mDriver.driver_id + "&token=" + API_TOKEN;
        }
    }


    public static String getOdoMeter(String path, String device_id) {
        if (Preferences.mDriver == null) {
            FirebaseCrash.log("Driver info is null at getUrlWithCredential");
            return "";
        }
        if (Preferences.mDriver.isDriver) {
            return API_BASE_PATH + path + "?driver_id=" + mDriver.driver_id + "&token=" + API_TOKEN + "&device_id=" + device_id;
        } else {
            return API_BASE_PATH + path + "?driver_id=" + mDriver.driver_id + "&token=" + API_TOKEN + "&device_id=" + device_id;
        }
    }


    public static String updateDutyStatus(String path, String vechileid, String enginehours,
                                          String duty_status_id) {
    /*    if (Preferences.mDriver == null) {
            FirebaseCrash.log("Driver info is null at getUrlWithCredential");
            return "";
        }*/

        Log.e("URL", "updateDutyStatus: " + API_BASE_PATH + path + "?vehicle_id=" + vechileid + "&total_engine_hours=" + enginehours
                + "&duty_status_id=" + duty_status_id + "&driver_id=" + mDriver.driver_id + "&token=" + API_TOKEN);

        return API_BASE_PATH + path + "?vehicle_id=" + vechileid + "&total_engine_hours=" + enginehours
                + "&duty_status_id=" + duty_status_id + "&driver_id=" + mDriver.driver_id + "&token=" + API_TOKEN;

    }

    public static String saveEngineHour(String path, String vechileid, String enginehours) {
    /*    if (Preferences.mDriver == null) {
            FirebaseCrash.log("Driver info is null at getUrlWithCredential");
            return "";
        }*/

        Log.e("URL", "saveEngineHour: " + API_BASE_PATH + path + "?vehicle_id=" + vechileid + "&total_engine_hours=" + enginehours
                + "&driver_id=" + mDriver.driver_id + "&token=" + API_TOKEN);

        return API_BASE_PATH + path + "?vehicle_id=" + vechileid + "&total_engine_hours=" + enginehours
                + "&driver_id=" + mDriver.driver_id + "&token=" + API_TOKEN;

    }


    public static String saveCertification(String path, String logs) {
        if (Preferences.mDriver == null) {
            FirebaseCrash.log("Driver info is null at getUrlWithCredential");
            return "";
        }
        if (Preferences.mDriver.isDriver) {
            return API_BASE_PATH + path + "?driver_id=" + mDriver.driver_id + "&token=" + API_TOKEN + "&certificates_logs=" + logs;
        } else {
            return API_BASE_PATH + path + "?driver_id=" + mDriver.driver_id + "&token=" + API_TOKEN + "&certificates_logs=" + logs;
        }
    }

    public static Vehicle getCurrentVehicle() {
        if (mVehicleIndex >= mVehicleList.size() || mVehicleIndex < 0)
            return null;
        return mVehicleList.get(mVehicleIndex);
    }

    public static void saveSession(String key, String value, Context context) {
        Editor editor = context.getSharedPreferences("PREFS", Activity.MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static TimeZone getDriverTimezone() {
        if (Preferences.mDriver == null) {
            FirebaseCrash.log("Driver info is null at getDriverTimezone");
            return new SimpleTimeZone(0, "GMT");
        }
        return TimeZone.getTimeZone(mDriver.company.timezone);
    }

    public static void saveSession(String key, Set<String> values, Context context) {
        Editor editor = context.getSharedPreferences("PREFS", Activity.MODE_PRIVATE).edit();
        editor.putStringSet(key, values);
        editor.apply();
    }
    public static void saveUnassignedTimeDate(Context context, long milliseconds) {
        Editor editor = context.getSharedPreferences("PREFS", Activity.MODE_PRIVATE).edit();
        editor.putLong("unassigned_time", milliseconds);
        editor.apply();
    }

    public static long getUnassignedTimeLong(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("PREFS", Activity.MODE_PRIVATE);
        return prefs.getLong("unassigned_time", 0);
    }

    public static String getSession(String key, Context context) {
        SharedPreferences prefs = context.getSharedPreferences("PREFS", Activity.MODE_PRIVATE);
        return prefs.getString(key, "");
    }

    public static void saveEmail(String email, Context context) {
        Editor editor = context.getSharedPreferences("PREFS", Activity.MODE_PRIVATE).edit();
        editor.putString("email", email);
        editor.apply();
    }

    public static String getEmail(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("PREFS", Activity.MODE_PRIVATE);
        return prefs.getString("email", "");
    }

    public static void clearSession(Context context) {
        Editor editor = context.getSharedPreferences("PREFS", Activity.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit();
    }

    public static String getAppDirectory() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/limologs";
    }
}
