package com.dotcompliance.limologs.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.dotcompliance.limologs.R;
import com.dotcompliance.limologs.data.Preferences;
import com.dotcompliance.limologs.data.VehicleInspect;

public class PinnedScaleImageView extends SubsamplingScaleImageView {
    Bitmap pin;

    public PinnedScaleImageView(Context context) {
        this(context, null);
    }

    public PinnedScaleImageView(Context context, AttributeSet attr) {
        super(context, attr);
        initialize();

        invalidate();
    }

    protected void initialize() {
        float density = getResources().getDisplayMetrics().densityDpi;
        pin = BitmapFactory.decodeResource(this.getResources(), R.drawable.x_mark);
        float w = (density/960f) * pin.getWidth();
        float h = (density/960f) * pin.getHeight();

        pin = Bitmap.createScaledBitmap(pin, (int)w, (int)h, true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Don't draw pin before image is ready so it doesn't move around during setup.
        if (!isReady()) {
            return;
        }

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        for (int i = 0; i < Preferences.mBodyInspectList.size(); i++) {
            VehicleInspect inspect = Preferences.mBodyInspectList.get(i);
            PointF vPin = sourceToViewCoord(inspect.xPos, inspect.yPos);
            float vX = vPin.x - (pin.getWidth()/2);
            float vY = vPin.y - pin.getHeight() / 2;
            canvas.drawBitmap(pin, vX, vY, paint);
        }
    }
}
