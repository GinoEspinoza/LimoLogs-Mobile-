package com.dotcompliance.limologs.service;

        import android.app.Service;
        import android.content.Intent;
        import android.os.Handler;
        import android.os.IBinder;
        import android.os.Looper;
        import android.util.Log;
        import android.widget.Toast;

        //import com.dotcompliance.limologs.AutomaticVehicleStatusChangeActivity;
        import com.dotcompliance.limologs.ELD.AvlEvent;
        import com.dotcompliance.limologs.ELD.GPSVOXConnect;
        import com.dotcompliance.limologs.ELD.GPSVOXRetrofitConnect;
        import com.dotcompliance.limologs.data.Preferences;
        import com.dotcompliance.limologs.data.Vehicle;
        import com.dotcompliance.limologs.util.DataManager;

        import java.util.Calendar;
        import java.util.Date;
        import java.util.Timer;
        import java.util.TimerTask;

public class CheckVehicleCurrentStatusService extends Service {

    public static Timer mTimer;
    public String mTimeStr;

    Timer timer;
    TimerTask timerTask;
    public int mDutyState = 0;
    Date obd_status_last_changed;
    int duty_status = 0;

    public CheckVehicleCurrentStatusService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                //startTimer();
                TimerMethod();
                //Toast.makeText(getApplicationContext(),"Service Called", Toast.LENGTH_SHORT).show();
            }
        }, 0, 1000 * 1);
        return super.onStartCommand(intent, flags, startId);
    }

    private void TimerMethodOLD() {

    /*        Time time = new Time();
       mTimeStr = time.toString();*/

        mTimeStr = Calendar.getInstance().getTime().toString();
        Log.d("15052016 1", mTimeStr);

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        Vehicle v = Preferences.getCurrentVehicle();

                        Log.d("15052016 2", v.obdDeviceID);

                        if (v.obdDeviceID != null) {

                          /*  CalampConnect.fetchAvlEvent(v.obdDeviceID, new CalampConnect.Gpxinterface() {
                                @Override
                                public void onFinished(AvlEvent event) {
                                    if (event != null) {
                                        Log.e("prabhat ", event.vbSpeed + " => ");
                                    }
                                }
                            });*/

                            GPSVOXRetrofitConnect connect = new GPSVOXRetrofitConnect(CheckVehicleCurrentStatusService.this);
                            connect.fetchAvlEvent(v.obdDeviceID, new GPSVOXRetrofitConnect.Gpxinterface() {
                                @Override
                                public void onFinished(AvlEvent event) {

                                }
                            });

                            if(DataManager.getInstance().isGpxBoolean()) {
                                DataManager.getInstance().setGpxBoolean(false);
                                AvlEvent event = new AvlEvent();
                                event = DataManager.getInstance().getEvent();

                                if (event != null) {
                                    Log.e("prabhat ", event.vbSpeed + " => ");
                                }
                            }

                        }
                    }
                },0);
            }
        };
        timer.schedule(timerTask, 0, 15000 * 60);
    }

    private void TimerMethod() {

        //Toast.makeText(mContext, "Success", Toast.LENGTH_SHORT).show();

        Log.e("yash..." , " service called...");

        Vehicle v = Preferences.getCurrentVehicle();

        //String deviceId = Preferences.mVehicleList.get(vehicle_index).obdDeviceID;
        final String deviceId =  v.obdDeviceID ; // "110952";
        obd_status_last_changed = Calendar.getInstance().getTime();
        //ArrayList<DutyStatus> states = mDriverLog.statusList;
        //mDutyState = states.get(states.size()-1).status;
        if (deviceId != null) {

            Handler mainHandler = new Handler(Looper.getMainLooper());
            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    //Code that uses AsyncHttpClient in your case ConsultaCaract()

                    // ********************* Code Should be Uncommented STARTS ********************* //

//                    CalampConnect.fetchAvlEvent(deviceId, new CalampConnect.Gpxinterface () {
//                        @Override
//                        public void onFinished(AvlEvent event) {
//                            if (event != null) {
//                                Date current = Calendar.getInstance().getTime();
//                                long diff = (current.getTime() - obd_status_last_changed.getTime()) / 1000;
//                                //editEndOdo.setText("" + event.vbOdometer);
//                                Log.e("Prabhat : " , " ==> ");
//                                mDutyState = 2;
//                                if(mDutyState == DutyStatus.STATUS_ON && event.vbSpeed > 5) {
//                                    duty_status = DutyStatus.STATUS_DRIVING;
//                                    Intent intent = new Intent(getApplicationContext(), AutomaticVehicleStatusChangeActivity.class);
//                                    intent.putExtra("TEMP","0");
//                                    intent.putExtra("duty_status",duty_status);
//                                    startActivity(intent);
//
//                                } else if (mDutyState == DutyStatus.STATUS_DRIVING && event.vbSpeed <= 5 && diff >= 1) {
//                                    duty_status = DutyStatus.STATUS_ON;
//                                    Intent intent = new Intent(getApplicationContext(), AutomaticVehicleStatusChangeActivity.class);
//                                    intent.putExtra("TEMP","1");
//                                    intent.putExtra("duty_status",duty_status);
//                                    startActivity(intent);
//                                }
//
//
//                            }
//                        }
//                    });
                }

                    // ********************* Code Should be Uncommented ENDS ********************* //
            };
            mainHandler.post(myRunnable);



        }
    }


//    private void runOnUiThread(Runnable timer_tick) {
//        Toast.makeText(getApplicationContext(), mTimeStr, Toast.LENGTH_SHORT).show();
//    }

    private Runnable Timer_Tick = new Runnable() {
        public void run() {
            //This method runs in the same thread as the UI.
            Toast.makeText(CheckVehicleCurrentStatusService.this, mTimeStr, Toast.LENGTH_SHORT).show();
            //Do something to the UI thread here
        }
    };


    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }
}
