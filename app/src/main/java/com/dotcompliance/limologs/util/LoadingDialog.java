package com.dotcompliance.limologs.util;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import com.dotcompliance.limologs.R;

public class LoadingDialog extends Dialog
{
    public LoadingDialog(Context paramContext)
    {
        super(paramContext);
    }

    public LoadingDialog(Context paramContext, int paramInt)
    {
        super(paramContext, paramInt);
    }

    protected void onCreate(Bundle paramBundle)
    {
        super.onCreate(paramBundle);
        setContentView(R.layout.loading_progress);
    }
}
