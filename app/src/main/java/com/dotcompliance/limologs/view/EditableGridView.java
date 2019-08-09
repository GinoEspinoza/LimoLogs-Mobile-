package com.dotcompliance.limologs.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;

import com.dotcompliance.limologs.R;
import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.DutyStatus;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class EditableGridView extends BaseGridView{
    public ArrayList<DutyStatus> mChangedDuties = new ArrayList<>();

    private int selectedIndex = -1;
    private int knobPosY;

    private int duty_id = 0;
    private int duty_starttime = 0, duty_endtime = 0;
    private int duty_state = DutyStatus.STATUS_OFF;

    private TextView leftknob;
    private TextView rightknob;
    private TextView leftTimeTab;
    private TextView rightTimeTab;

    private Paint edgeLinePaint;
    private Paint newLinePaint;
    protected Paint selectedRectPaint;

    public EditableGridView(Context context) {
        super(context);
    }

    public EditableGridView(Context context, DriverLog log) {
        super(context, log);
        setData(log);
    }

    @Override
    public void setData(DriverLog log) {
        mData = log;
        for (int i = 0; i < log.statusList.size(); i ++) {
            mChangedDuties.add(new DutyStatus(log.statusList.get(i)));
        }

        for (int j = 0; j < mChangedDuties.size(); j++) {
            if (j < mChangedDuties.size() - 1) {
                duty_endtime = mChangedDuties.get(j + 1).start_time;
            } else {
                duty_endtime = 1440;
            }
            mChangedDuties.get(j).end_time = duty_endtime;
        }
    }

    public void setDutyState(int state) {
        duty_state = state;
        invalidate();
    }

    public void setIndex(int index) {
        // TODO Auto-generated method stub
        selectedIndex = index;
        if (selectedIndex > -1) {
            duty_id = mData.statusList.get(selectedIndex).Id;
        }
    }

    public void setTimeArea(int start_time, int end_time) {
        duty_starttime = start_time;
        duty_endtime = end_time;
        if (selectedIndex > 0 && selectedIndex < mData.statusList.size()) {
            mChangedDuties.get(selectedIndex - 1).end_time = duty_starttime;
        }
        if (selectedIndex > -1 && selectedIndex < mData.statusList.size() - 1) {
            mChangedDuties.get(selectedIndex + 1).start_time = duty_endtime;
        }
    //    rearrangeDuties();
        invalidate();
    }

    public int rearrangeDuties() {
        // we work with only duty_starttime, duty_endtime
        if (selectedIndex > -1)
            mChangedDuties.remove(selectedIndex);

        int i = 0, left_index = -1, right_index = -1;
        while(i < mChangedDuties.size()) {
            if (mChangedDuties.get(i).start_time >= mChangedDuties.get(i).end_time) {
                mChangedDuties.remove(i);
                continue;
            }
            if (mChangedDuties.get(i).start_time >= duty_starttime && mChangedDuties.get(i).end_time <= duty_endtime) {
                mChangedDuties.remove(i);
                continue;
            }
            if (mChangedDuties.get(i).start_time < duty_starttime) {
                left_index = i;
            }
            if (mChangedDuties.get(i).start_time <= duty_endtime && mChangedDuties.get(i).end_time > duty_endtime) {
                right_index = i;
            }
            i++;
        }
        Log.d("rearrange", "left: " + left_index + " right: " + right_index);

        if (left_index != -1 && left_index == right_index) { // should create another new duty
            DutyStatus duty = new DutyStatus();
            duty.Id = 0;
            duty.start_time = duty_endtime;
            duty.status = mChangedDuties.get(left_index).status;
            mChangedDuties.add(left_index + 1, duty);
        }
        else {
            if (right_index != -1)
                mChangedDuties.get(right_index).start_time = duty_endtime;
            for (i = left_index + 1; i < right_index; i++)
            {
                mChangedDuties.remove(left_index + 1);
            }
        }

        DutyStatus duty = new DutyStatus();
        duty.Id = duty_id;
        duty.start_time = duty_starttime;
        //duty.end_time = duty_endtime; end_time is not necessary to be stored
        duty.status = duty_state;
        mChangedDuties.add(left_index + 1, duty);

        selectedIndex = left_index + 1;
        if (selectedIndex < mChangedDuties.size() -1 && mChangedDuties.get(selectedIndex + 1).status == duty.status) { // merge current and the right
            mChangedDuties.remove(selectedIndex + 1);
        }
        if (selectedIndex > 0 && mChangedDuties.get(selectedIndex - 1).status == duty.status) {
            mChangedDuties.get(selectedIndex).start_time = mChangedDuties.get(selectedIndex - 1).start_time;
            mChangedDuties.remove(--selectedIndex);
        }

        return selectedIndex;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);

        for(int i=0;i < mChangedDuties.size();i++) {
            if (i == selectedIndex)
                continue;
            DutyStatus duty = mChangedDuties.get(i);
            int start_time = duty.start_time;
            int end_time = duty.end_time;
            if (start_time >= end_time) continue;
            if (start_time > duty_starttime && end_time < duty_endtime)
                continue;
            else if (start_time <= duty_starttime && end_time >= duty_endtime) {
                canvas.drawLine(getXPos(start_time) + 1, getYPos(duty.status), getXPos(duty_starttime) - 1, getYPos(duty.status), newLinePaint);
                canvas.drawLine(getXPos(duty_endtime) + 1, getYPos(duty.status), getXPos(end_time) - 1, getYPos(duty.status), newLinePaint);
                if( i > 0){
                    canvas.drawLine(getXPos(start_time), getYPos(mChangedDuties.get(i - 1).status),
                            getXPos(start_time) , getYPos(duty.status), vertTimelinePaint);
                }
                continue;
            }
            else if (start_time <= duty_starttime && end_time > duty_starttime)
                end_time = duty_starttime;
            else if (start_time < duty_endtime && end_time >= duty_endtime)
                start_time = duty_endtime;

            canvas.drawLine(getXPos(start_time) + 1, getYPos(duty.status),
                    getXPos(end_time) - 1, getYPos(duty.status), newLinePaint); // why we plus/min 1, because line overflows at the both ends
            if( i > 0){
                canvas.drawLine(getXPos(start_time), getYPos(mChangedDuties.get(i - 1).status),
                        getXPos(start_time) , getYPos(duty.status), vertTimelinePaint);
            }
        }
        canvas.drawLine(getXPos(duty_starttime), getYPos(duty_state), getXPos(duty_endtime), getYPos(duty_state), newLinePaint);
        drawKnob(canvas);
    }

    private void drawKnob(Canvas canvas) {
        // TODO Auto-generated method stub
        canvas.drawLine(getXPos(duty_starttime) - 4, marginVert, getXPos(duty_starttime) - 4, knobPosY - 20, edgeLinePaint);
        canvas.drawLine(getXPos(duty_endtime) + 4, marginVert, getXPos(duty_endtime) + 4, knobPosY - 20, edgeLinePaint);
        canvas.drawRect(getXPos(duty_starttime), marginVert, getXPos(duty_endtime), knobPosY - 28, selectedRectPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // TODO Auto-generated method stub
        super.onSizeChanged(w, h, oldw, oldh);
        knobPosY = getBottom() - (int)marginVert;
    }

    public void setKnobs(TextView left_knob, TextView right_knob, TextView left_time, TextView right_time) {
        // TODO Auto-generated method stub
        this.leftknob = left_knob;
        this.rightknob = right_knob;
        this.leftTimeTab = left_time;
        this.rightTimeTab = right_time;
    }

    @Override
    protected void init() {
        // TODO Auto-generated method stub
        super.init();

        horiTimelinePaint.setColor(ContextCompat.getColor(mContext, R.color.colorChartTransparent));
        vertTimelinePaint.setColor(ContextCompat.getColor(mContext, R.color.colorChartTransparent));

        newLinePaint = new Paint();
        newLinePaint.setColor(ContextCompat.getColor(mContext, R.color.colorChartPrimary));
        newLinePaint.setAntiAlias(true);
        newLinePaint.setStyle(Paint.Style.STROKE);
        newLinePaint.setStrokeJoin(Paint.Join.ROUND);
        newLinePaint.setStrokeCap(Paint.Cap.ROUND);
        newLinePaint.setStrokeWidth(5);

        edgeLinePaint = new Paint();
        edgeLinePaint.setColor(Color.DKGRAY);
        edgeLinePaint.setAntiAlias(true);
        edgeLinePaint.setStrokeWidth(8);

        selectedRectPaint = new Paint();
        selectedRectPaint.setColor(ContextCompat.getColor(mContext, R.color.colorSelectOverlay));
    }
}
