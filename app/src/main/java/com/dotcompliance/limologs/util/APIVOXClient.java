package com.dotcompliance.limologs.util;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class APIVOXClient {

    public static final String BASE_URL = "http://51.15.219.83/api/";

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