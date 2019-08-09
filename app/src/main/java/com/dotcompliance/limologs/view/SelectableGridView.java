package com.dotcompliance.limologs.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.dotcompliance.limologs.R;
import com.dotcompliance.limologs.data.DriverLog;
import com.dotcompliance.limologs.data.DutyStatus;
import com.dotcompliance.limologs.util.DataManager;

public class SelectableGridView extends BaseGridView {
    public interface OnSelectDutyListener {
        void onSelectDuty(int index);
    }

    protected OnSelectDutyListener onSelectDutyListener;

    protected Paint selectedRectPaint;
    protected int mSelectedIndex = -1;

    private boolean isGridSelectable = false;

    public void setGridSelectable(boolean gridSelectable) {
        isGridSelectable = gridSelectable;
    }

    public SelectableGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SelectableGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectableGridView(Context context) {
        super(context);
    }

    public SelectableGridView(Context context, DriverLog log) {
        super(context, log);
    }

    @Override
    protected void init() {
        super.init();

        selectedRectPaint = new Paint();
        selectedRectPaint.setColor(ContextCompat.getColor(mContext, R.color.colorSelectOverlay));
        selectedRectPaint.setAntiAlias(true);
    }

    public void selectDutyAtIndex(int index) {
        mSelectedIndex = index;
        invalidate();
    }

    public int getSelectedIndex() {
        return mSelectedIndex;
    }

    public void setOnSelectDutyListener(OnSelectDutyListener listener) {
        this.onSelectDutyListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mSelectedIndex > -1) {
            // draw selection
            DutyStatus duty = mData.statusList.get(mSelectedIndex);
            canvas.drawRect(getXPos(duty.start_time), marginVert, getXPos(duty.end_time), getHeight() - marginVert, selectedRectPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        float x, y;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x = event.getX();
                y = event.getY();
                if (isGridSelectable) {
                    mSelectedIndex = getIndexIntouch(x, y);
                    if (mSelectedIndex > -1) {
                        invalidate();
                        if (onSelectDutyListener != null)
                            onSelectDutyListener.onSelectDuty(mSelectedIndex);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return true;
    }

    private int getIndexIntouch(float x, float y) {
        int timex = (int) (1440 * (x - marginHori) / xWidth);
        int i = 0;
        while (i < mData.statusList.size()) {
            if (timex <= mData.statusList.get(i).end_time) {
                return i;
            }
            i++;
        }
        return -1;
    }
}
