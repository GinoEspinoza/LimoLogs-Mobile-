package com.dotcompliance.limologs.util;

import com.dotcompliance.limologs.data.AssignDrivertoVechileModel;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface APIVOXInterface {

    @FormUrlEncoded
    @POST("login")
    Call<Object> login(@Field("email") String email,
                                                           @Field("password") String password);

    @GET("get_history")
    Call<Object> getHistory(@Query("user_api_hash") String user_api_hash,
                                                @Query("lang") String lang,
                                                @Query("device_id") String device_id,
                                                @Query("from_date") String from_date,
                                                @Query("to_date") String to_date,
                                                @Query("from_time") String from_time,
                                                @Query("to_time") String to_time);
}
