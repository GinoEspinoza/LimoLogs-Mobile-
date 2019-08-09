package com.dotcompliance.limologs.network;

import com.loopj.android.http.AsyncHttpClient;

public class MyAsyncHttpClient extends AsyncHttpClient {
    public MyAsyncHttpClient() {
        super();
        addHeader("Accept", "*/*");
        setMaxRetriesAndTimeout(1, 30 * 1000);
    }

}
