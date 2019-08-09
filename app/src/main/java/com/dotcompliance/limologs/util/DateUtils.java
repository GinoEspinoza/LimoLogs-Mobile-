package com.dotcompliance.limologs.util;

import android.util.Log;

import com.dotcompliance.limologs.data.Preferences;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateUtils {
    public static Date getDateWithoutTime(Date fecha) {
        Date res = fecha;
        Calendar calendar = Calendar.getInstance(Preferences.getDriverTimezone());

        calendar.setTime( fecha );
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        res = calendar.getTime();

        return res;
    }

    public static int getDifferenceInDays(Date date1, Date date2) {
        return (int) ((date1.getTime() - date2.getTime())/ (1000 * 60 * 60 * 24));
    }

    public static Date addDaysToDate(Date date, int days) {
        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
        cal.setTime(date);
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }

//    public static Calendar stringToCalendar(String timeString) {
//        return stringToCalendar(timeString, Preferences.getDriverTimezone());
//    }

    public static Calendar stringToTimeZoneCalendar(String timeString) {
        Calendar cal = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = sdf.parse(timeString);
            long old_miliSec = date.getTime();
            long diff_miliSec = TimeUnit.HOURS.toMillis(7);
            long new_miliSec = old_miliSec - diff_miliSec;

            cal.setTimeInMillis(new_miliSec);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return cal;
    }

    public static Calendar stringToCalendar(String timeString) {
        Calendar cal = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = sdf.parse(timeString);
            cal.setTime(date);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return cal;
    }


    public static Calendar stringToTimeZone7Calendar(String timeString) {
        Calendar cal = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = sdf.parse(timeString);
            long old_miliSec = date.getTime();
            long diff_miliSec = TimeUnit.HOURS.toMillis(7);
            long new_miliSec = old_miliSec + diff_miliSec;

            cal.setTimeInMillis(new_miliSec);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return cal;
    }
}
