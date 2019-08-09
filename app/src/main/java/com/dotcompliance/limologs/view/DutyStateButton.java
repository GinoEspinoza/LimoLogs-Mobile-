package com.dotcompliance.limologs.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.dotcompliance.limologs.R;


public class DutyStateButton extends LinearLayout {

    public static int STATE_NORMAL = 0;
    public static int STATE_WARNING = 1;
    public static int STATE_ALERT = 2;

    private static final int[] STATE_SELECTED_MODE = {R.attr.state_selected};
    private static final int[] STATE_ENABLED_MODE = {R.attr.state_disabled};
    private static final int[] STATE_HOS_ALERT = {R.attr.hos_alert};
    private static final int[] STATE_HOS_WARNING = {R.attr.hos_warning};

    protected boolean disabled;
    protected boolean selected;
    protected int hosState = 0;

    public DutyStateButton(Context context) {
        this(context, null);
    }

    public DutyStateButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DutyStateButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DutyStateButton, defStyleAttr, 0);

        try {
            this.disabled = a.getBoolean(R.styleable.DutyStateButton_state_disabled, false);
            this.selected = a.getBoolean(R.styleable.DutyStateButton_state_selected, false);

            if (a.getBoolean(R.styleable.DutyStateButton_hos_warning, false))
                this.hosState = STATE_WARNING;
            else if (a.getBoolean(R.styleable.DutyStateButton_hos_alert, false))
                this.hosState = STATE_ALERT;
            else
                this.hosState = STATE_NORMAL;
        }
        finally {
            a.recycle();
        }

        construct();
    }

    private void construct() {

    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 3);

        if (selected) {
            mergeDrawableStates(drawableState, STATE_SELECTED_MODE);
        }

        if (disabled) {
            mergeDrawableStates(drawableState, STATE_ENABLED_MODE);
        }

        if (hosState == STATE_WARNING) {
            mergeDrawableStates(drawableState, STATE_HOS_WARNING);
            //return drawableState;
        }
        else if (hosState == STATE_ALERT) {
            mergeDrawableStates(drawableState, STATE_HOS_ALERT);
            //return drawableState;
        }

        return drawableState;
        //return super.onCreateDrawableState(extraSpace);
    }

    public void setDisabled(boolean disabled) {
        if (this.disabled != disabled) {
            this.disabled = disabled;
            refreshDrawableState();
        }
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setSelected(boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;
            refreshDrawableState();
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public void setHosState(int state) {
        if (this.hosState != state) {
            this.hosState = state;
            refreshDrawableState();
        }
    }

    public int getHosState() {
        return hosState;
    }
}
