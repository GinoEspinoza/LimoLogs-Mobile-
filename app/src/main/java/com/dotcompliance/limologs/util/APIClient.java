package com.dotcompliance.limologs.util;

/**
 * Created by ADMIN on 26-10-2017.
 */

import com.dotcompliance.limologs.data.Preferences;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;



public class APIClient {


  //  public static final String BASE_URL ="http://api.limologs.com/v1/log/";
    public static final String BASE_URL = Preferences.API_BASE_PATH + "/";

    private static Retrofit retrofit = null;


    public static Retrofit getClient() {
        if (retrofit==null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}