package com.dotcompliance.limologs.view;

import android.graphics.Point;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.WindowManager;

import com.dotcompliance.limologs.R;

public class ClockView extends View {

    static public final int BREAK_CIRCLE = 0;
    static public final int DRIVE_CIRCLE = 1;
    static public final int ONDUTY_CIRCLE = 2;
    static public final int CYCLE_CIRCLE = 3;

    private int raidus;
    private int centerX;
    private int centerY;
    private Paint borderForePaint;
    private Paint clockPaint;
    private Paint textPaint;
    private int circle_type;
    private RectF rectForCircle, rectForBorder;
    private float endAngle;
    private int minutes;

    private Context mContext;

    public ClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        // TODO Auto-generated constructor stub
        clockPaint = new Paint();
        clockPaint.setStyle(Paint.Style.FILL);
        clockPaint.setAntiAlias(true);

        borderForePaint = new Paint();
        borderForePaint.setStyle(Paint.Style.STROKE);
    //    borderForePaint.setStrokeWidth(getResources().getDimension(R.dimen.stroke));
        borderForePaint.setStrokeWidth(4);
        borderForePaint.setAntiAlias(true);


        int color = ContextCompat.getColor(mContext, R.color.colorClockDefault);
        clockPaint.setColor(color);
        borderForePaint.setColor(lightenColor(color));

//        borderBackPaint = new Paint();
//        borderBackPaint.setColor(ContextCompat.getColor(context, R.color.colorClockDefault));
//        borderBackPaint.setStyle(Paint.Style.STROKE);
//        borderBackPaint.setStrokeWidth(5);
//    //    borderBackPaint.setStrokeWidth(getResources().getDimension(R.dimen.stroke));
//        borderBackPaint.setAntiAlias(true);

        textPaint = new Paint();
        //textPaint.setTextSize(context.getResources().getDimension(R.dimen.font_40));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        endAngle = 360;

        // get dimension and determine textsize
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        textPaint.setTextSize(size.y / 24);
    }

    public void setCircleType(int circle_type) {
        this.circle_type = circle_type;
        init();
    }

    public void setMinute(int m) {
        if (m == 0) {
            int color = ContextCompat.getColor(mContext, R.color.colorClockAlert);
            clockPaint.setColor(color);
        //  borderForePaint.setColor(darkenColor(color));
        }
        else if (m <= 120) {
            int color = ContextCompat.getColor(mContext, R.color.colorClockWarning);
            clockPaint.setColor(color);
            borderForePaint.setColor(darkenColor(color));
        }
        else if (m > 120) {
            int color = ContextCompat.getColor(mContext, R.color.colorClockDefault);
            clockPaint.setColor(color);
            borderForePaint.setColor(lightenColor(color));
        }
        minutes = m;
        endAngle = getEndAngle();
        invalidate();
    }

    private void init(){
        switch (circle_type) {
            case BREAK_CIRCLE:
                borderForePaint.setColor(Color.YELLOW);
                break;
            case DRIVE_CIRCLE:
             //   borderForePaint.setColor(Color.argb(255, 0, 200, 0));
                minutes = 600;
                break;
            case ONDUTY_CIRCLE:
             //   borderForePaint.setColor(Color.argb(255, 0, 200, 100));
                minutes = 900;
                break;
            case CYCLE_CIRCLE:
             //   borderForePaint.setColor(Color.argb(255, 50, 200, 0));
                minutes = 4200;
                break;
        }
        endAngle = 360;
        invalidate();
    }

    public int lightenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] += 0.4f * (1 - hsv[2]);
        return Color.HSVToColor(hsv);
    }

    public int darkenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }

    private int getEndAngle() {
        // TODO Auto-generated method stub
        switch (circle_type) {
            case BREAK_CIRCLE:
                return 0;
            case DRIVE_CIRCLE:
                return 360 * minutes / 600;
            case ONDUTY_CIRCLE:
                return 360 * minutes / 900;
            case CYCLE_CIRCLE:
                return  360 * minutes / 4200;
        }
        return 0;
    }
    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        int hour, min_of_hour;
        min_of_hour = minutes % 60;
        hour = (minutes - min_of_hour) / 60;

        super.onDraw(canvas);
        canvas.drawOval(rectForCircle, clockPaint);
    //    canvas.drawArc(rectForCircle, 0, 360, false, borderBackPaint);
        canvas.drawArc(rectForBorder, 270 - endAngle, endAngle, false, borderForePaint);

        canvas.drawText(String.format("%02d : %02d", hour, min_of_hour), centerX, centerY, textPaint);
        switch (circle_type) {
            case BREAK_CIRCLE:
                canvas.drawText("Break", centerX, centerY+textPaint.getTextSize(), textPaint);
                break;
            case DRIVE_CIRCLE:
                canvas.drawText("Drive", centerX, centerY+textPaint.getTextSize(), textPaint);
                break;
            case ONDUTY_CIRCLE:
                canvas.drawText("On-Duty", centerX, centerY+textPaint.getTextSize(), textPaint);
                break;
            case CYCLE_CIRCLE:
                canvas.drawText("Cycle", centerX, centerY+textPaint.getTextSize(), textPaint);
                break;
        }

    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // TODO Auto-generated method stub
        super.onSizeChanged(w, h, oldw, oldh);
        raidus = Math.min(w, h) / 2 - 10;
        centerX = w / 2;
        centerY = h / 2;
        rectForCircle = new RectF(centerX-raidus, centerY-raidus, centerX+raidus, centerY+raidus);
        rectForBorder = new RectF(rectForCircle.left + 5, rectForCircle.top + 5, rectForCircle.right - 5, rectForCircle.bottom - 5);
    }
}
