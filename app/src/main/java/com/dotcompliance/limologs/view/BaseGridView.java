package com.dotcompliance.limologs.view;

import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.view.Display;
import android.view.View;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.WindowManager;

import com.dotcompliance.limologs.R;
import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.DutyStatus;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.util.DateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BaseGridView extends View {
    protected DriverLog mData = null;

    public static float marginHori = 60;
    public static float marginVert = 35;

    protected Paint baselinePaint;
    protected Paint horiTimelinePaint, vertTimelinePaint;
    protected Paint timeTextPaint;
    protected Context mContext;
    protected float x_needle, y_needle, x, y;
    protected float xWidth, yHeight;
    protected int selectedDay = 0;
    public ArrayList<DutyStatus> mstatusList = new ArrayList<>();

    public BaseGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // TODO Auto-generated constructor stub
        init();
        mContext = context;
    }

    public BaseGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public BaseGridView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        mContext = context;
        init();
    }

    public BaseGridView(Context context, DriverLog log) {
        this(context);
        mData = log;
    }

    public void setData(DriverLog log) {
        mData = log;
        invalidate();
    }

    protected void init() {
        baselinePaint = new Paint();
        baselinePaint.setColor(ContextCompat.getColor(mContext, R.color.colorGridLine));
        baselinePaint.setAntiAlias(true);
        baselinePaint.setTextAlign(Paint.Align.LEFT);

        timeTextPaint = new Paint();
        timeTextPaint.setColor(Color.BLACK);
        timeTextPaint.setAntiAlias(true);
        timeTextPaint.setTextSize(20);

        //    timeTextPaint.setTextSize(getResources().getDimension(R.dimen.graph_time_font));
        horiTimelinePaint = new Paint();
        horiTimelinePaint.setColor(ContextCompat.getColor(mContext, R.color.colorChartPrimary));
        horiTimelinePaint.setAntiAlias(true);
        horiTimelinePaint.setStyle(Paint.Style.STROKE);
        horiTimelinePaint.setStrokeJoin(Paint.Join.ROUND);
        horiTimelinePaint.setStrokeCap(Paint.Cap.SQUARE);
        horiTimelinePaint.setStrokeWidth(5);

        vertTimelinePaint = new Paint();
        vertTimelinePaint.setColor(ContextCompat.getColor(mContext, R.color.colorChartPrimary));
        horiTimelinePaint.setAntiAlias(true);
        vertTimelinePaint.setStyle(Paint.Style.STROKE);
        vertTimelinePaint.setStrokeJoin(Paint.Join.ROUND);
        vertTimelinePaint.setStrokeCap(Paint.Cap.SQUARE);
        vertTimelinePaint.setStrokeWidth(3);

        setBackgroundColor(ContextCompat.getColor(mContext, R.color.colorGridBackground));

        // get dimension and determine textsize
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        baselinePaint.setTextSize(size.x / 64);
        timeTextPaint.setTextSize(size.x / 72);

        marginHori = size.x / 24;
        marginVert = size.y / 24;

    }

    public void cleargraph() {
        horiTimelinePaint.setColor(ContextCompat.getColor(mContext, R.color.colorChartTransparent));
        vertTimelinePaint.setColor(ContextCompat.getColor(mContext, R.color.colorChartTransparent));

    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        drawPaper(canvas);
        if (mData != null) {
            //cleargraph();
            drawGraph(canvas);
            drawScore(canvas);
        }
    }

    public void drawGraph(Canvas canvas) {
        mstatusList.clear();
        try {

            for (int i = 0; i < mData.statusList.size(); i++) {
                DutyStatus duty = mData.statusList.get(i);
                duty.status = duty.getNormalizeStatus();
                mstatusList.add(duty);
            }

            // START private home porno
            // if we have no graphics
            if (mstatusList.size() == 0) {

                int logIndex = fetchLogPosition(mData); 

                // find first duty status
                for (int i = logIndex; i < Preferences.mDriverLogs.size(); i++) {
                    DriverLog driverLog = Preferences.mDriverLogs.get(i);
                    DutyStatus dutyStatus = fetchLastTruestDutyStatus(driverLog);
                    if (dutyStatus != null) {
                        // fuck the system
                        DutyStatus tmpDutyStatus = DutyStatus.copy(dutyStatus);
                        tmpDutyStatus.start_time = 0;
                        mstatusList.add(tmpDutyStatus);
                        break;
                    }
                }
            }
            // END private home porno

            for (int i = 0; i < mstatusList.size(); i++) {
                DutyStatus duty = mstatusList.get(i);
                // if(duty.status ==0 || duty.status==1 || duty.status ==2 || duty.status ==3) {
                int start_time = duty.start_time;
                int end_time = 0;
                if (i < mstatusList.size() - 1)
                    end_time = mstatusList.get(i + 1).start_time;
                else { // last duty
                    Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
                    end_time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
                    end_time -= end_time % 15 - 15;

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    sdf.setTimeZone(Preferences.getDriverTimezone());
                    Date date = sdf.parse(mData.log_date);
                    if (DateUtils.getDateWithoutTime(date).compareTo(DateUtils.getDateWithoutTime(cal.getTime())) < 0) {
                        end_time = 1440;
                    }
                }

                duty.end_time = end_time;
                canvas.drawLine(getXPos(start_time) + 1, getYPos(duty.status),
                        getXPos(end_time) - 1, getYPos(duty.status), horiTimelinePaint); // why we plus/min 1, because line overflows at the both ends
                if (i > 0) {
                    canvas.drawLine(getXPos(start_time), getYPos(mstatusList.get(i - 1).status),
                            getXPos(start_time), getYPos(duty.status), vertTimelinePaint);
                }
                canvas.drawText(i + 1 + "", getXPos(start_time), getHeight(), baselinePaint);
                //  }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    private int fetchLogPosition(DriverLog driverLog) {
        for (int i = 0; i < Preferences.mDriverLogs.size(); i++) {
            if (Preferences.mDriverLogs.get(i).log_date.contentEquals(driverLog.log_date)) {
                return i;
            }
        }
        return -1;
    }

    private DutyStatus fetchLastTruestDutyStatus(DriverLog driverLog) {
        for (int i = driverLog.statusList.size() - 1; i >= 0; i--) {
            DutyStatus duty = driverLog.statusList.get(i);
            if (duty.status == 0 || duty.status == 1 || duty.status == 2 || duty.status == 3) {
                return duty;
            }
        }
        return null;
    }


    //    public void drawGraph(Canvas canvas){
//        try{
//
//            for (int i = 0; i <mData.statusList.size() ; i++) {
//                DutyStatus duty = mData.statusList.get(i);
//                if(duty.status ==0 || duty.status==1 || duty.status ==2 || duty.status ==3) {
//                    mstatusList.add(duty);
//                }
//            }
//            for(int i=0;i < mData.statusList.size();i++) {
//                DutyStatus duty = mData.statusList.get(i);
//                if(duty.status ==0 || duty.status==1 || duty.status ==2 || duty.status ==3) {
//                    int start_time = duty.start_time;
//                    int end_time = 0;
//                    if (i < mData.statusList.size() - 1)
//                        end_time = mData.statusList.get(i + 1).start_time;
//                    else { // last duty
//                        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
//                        end_time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
//                        end_time -= end_time % 15 - 15;
//
//                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//                        sdf.setTimeZone(Preferences.getDriverTimezone());
//                        Date date = sdf.parse(mData.log_date);
//                        if (DateUtils.getDateWithoutTime(date).compareTo(DateUtils.getDateWithoutTime(cal.getTime())) < 0) {
//                            end_time = 1440;
//                        }
//                    }
//
//                    duty.end_time = end_time;
//                    canvas.drawLine(getXPos(start_time) + 1, getYPos(duty.status),
//                            getXPos(end_time) - 1, getYPos(duty.status), horiTimelinePaint); // why we plus/min 1, because line overflows at the both ends
//                    if (i > 0) {
//                        canvas.drawLine(getXPos(start_time), getYPos(mData.statusList.get(i - 1).status),
//                                getXPos(start_time), getYPos(duty.status), vertTimelinePaint);
//                    }
//                    canvas.drawText(i + 1 + "", getXPos(start_time), getHeight(), baselinePaint);
//                }
//            }
//        }catch (Exception e) {
//            // TODO: handle exception
//        }
//    }
    protected void drawPaper(Canvas canvas) {
        for (int i = 0; i < 5; i++) {
            canvas.drawLine(marginHori, marginVert + i * (getHeight() - marginVert * 2) / 4, getWidth() - marginHori, marginVert + i * (getHeight() - marginVert * 2) / 4, baselinePaint);
        }
        for (int i = 0; i < 25; i++) {
            canvas.drawLine(marginHori + i * (getWidth() - marginHori * 2) / 24, marginVert, marginHori + i * (getWidth() - marginHori * 2) / 24, getHeight() - marginVert, baselinePaint);
            if (i == 0 || i == 24) {
                canvas.drawText("M", marginHori + i * (getWidth() - marginHori * 2) / 24, marginVert - 15, timeTextPaint);
            } else if (i == 12) {
                canvas.drawText("N", marginHori + i * (getWidth() - marginHori * 2) / 24, marginVert - 15, timeTextPaint);
            } else {
                canvas.drawText(i + "", marginHori + i * (getWidth() - marginHori * 2) / 24, marginVert - 15, timeTextPaint);
            }
        }
        for (int i = 1; i < 5; i++) {
            for (int j = 0; j < 24; j++) {
                canvas.drawLine(marginHori + j * x + x_needle,
                        i * y + marginVert,
                        marginHori + j * x + x_needle,
                        i * y + marginVert - y_needle, baselinePaint);
                canvas.drawLine(marginHori + j * x + x_needle * 2,
                        i * y + marginVert,
                        marginHori + j * x + x_needle * 2,
                        i * y + marginVert - y_needle * 2, baselinePaint);
                canvas.drawLine(marginHori + j * x + x_needle * 3,
                        i * y + marginVert,
                        marginHori + j * x + x_needle * 3,
                        i * y + marginVert - y_needle, baselinePaint);
            }
        }
        canvas.drawText("OFF", 10, marginVert + y * 0.6f, baselinePaint);
        canvas.drawText("SB", 10, marginVert + y * 1.6f, baselinePaint);
        canvas.drawText("DR", 10, marginVert + y * 2.6f, baselinePaint);
        canvas.drawText("ON", 10, marginVert + y * 3.6f, baselinePaint);
    }

    protected void drawScore(Canvas canvas) {
        int total[] = new int[5];
        try {
            for (int i = 0; i < mstatusList.size(); i++) {
                DutyStatus duty = mstatusList.get(i);
                if (duty.status == 0 || duty.status == 1 || duty.status == 2 || duty.status == 3) {
                    int start_time = duty.start_time;
                    int end_time = 0;
                    if (i < mstatusList.size() - 1)
                        end_time = mstatusList.get(i + 1).start_time;

                    if (end_time == 0) { // last duty
                        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
                        end_time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
                        end_time -= end_time % 15 - 15;

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        sdf.setTimeZone(Preferences.getDriverTimezone());
                        Date date = sdf.parse(mData.log_date);
                        if (DateUtils.getDateWithoutTime(date).compareTo(DateUtils.getDateWithoutTime(cal.getTime())) < 0) {
                            end_time = 1440;
                        }
                    }
                    total[duty.status] += end_time - start_time;
                }
            }
            int total_overall = 0;
            for (int i = 0; i < 4; i++) {
                total_overall += total[i];
                //canvas.drawText(String.format(Locale.US, "%.2f", total[i] / 60.0f), getWidth() - marginHori + 5, marginVert + y * (i + 0.6f), baselinePaint);
                canvas.drawText(String.format(Locale.US, "%1$2d:%2$02d", total[i]/100, total[i] % 60), getWidth() - marginHori + 5, marginVert + y * (i + 0.6f), baselinePaint);
            }

            //String str_total = "Total " + String.format(Locale.US, "%.2f", total_overall / 60.0f);
            String str_total = "Total " + String.format(Locale.US, "%1$2d:%2$02d", total_overall/100, total_overall % 60);
            canvas.drawText(str_total, getWidth() - baselinePaint.measureText(str_total), getHeight(), baselinePaint);
        } catch (Exception e) {
            // TODO: handle exception
        }
    }


//    protected void drawScore(Canvas canvas){
//        int total[] = new int[5];
//        try {
//            for (int i = 0; i < mData.statusList.size(); i++) {
//                DutyStatus duty = mData.statusList.get(i);
//                if(duty.status ==0 || duty.status==1 || duty.status ==2 || duty.status ==3) {
//                    int start_time = duty.start_time;
//                    int end_time = 0;
//                    if (i < mData.statusList.size() - 1)
//                        end_time = mData.statusList.get(i + 1).start_time;
//
//                    if (end_time == 0) { // last duty
//                        Calendar cal = Calendar.getInstance(Preferences.getDriverTimezone());
//                        end_time = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
//                        end_time -= end_time % 15 - 15;
//
//                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
//                        sdf.setTimeZone(Preferences.getDriverTimezone());
//                        Date date = sdf.parse(mData.log_date);
//                        if (DateUtils.getDateWithoutTime(date).compareTo(DateUtils.getDateWithoutTime(cal.getTime())) < 0) {
//                            end_time = 1440;
//                        }
//                    }
//                    total[duty.status] += end_time - start_time;
//                }
//            }
//            int total_overall = 0;
//            for (int i = 0; i < 4; i++) {
//                total_overall += total[i];
//                canvas.drawText(String.format(Locale.US, "%.2f", total[i] / 60.0f), getWidth() - marginHori + 5, marginVert + y * (i + 0.6f), baselinePaint);
//            }
//
//            String str_total = "Total " + String.format(Locale.US, "%.2f", total_overall / 60.0f);
//            canvas.drawText(str_total, getWidth() - baselinePaint.measureText(str_total), getHeight(), baselinePaint);
//        }
//        catch (Exception e) {
//            // TODO: handle exception
//        }
//    }


    protected float getYPos(int type) {
        return (float) (marginVert + (type + 0.5) * y);
    }

    public float getXPos(int time) {
        return marginHori + time / 1440.0f * xWidth;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // TODO Auto-generated method stub
        super.onSizeChanged(w, h, oldw, oldh);
        x_needle = (getWidth() - marginHori * 2) / 24 / 4;
        y_needle = (getHeight() - marginVert * 2) / 4 / 4;

        xWidth = getWidth() - marginHori * 2;
        yHeight = getHeight() - marginVert * 2;

        x = xWidth / 24;
        y = yHeight / 4;
    }

    public Rect getGraphRect() {
        Rect rc = new Rect();
        rc.left = (int) marginHori;
        rc.top = (int) marginVert;
        rc.right = getWidth() - (int) marginHori;
        rc.bottom = getHeight() - (int) marginVert;
        return rc;
    }


}
