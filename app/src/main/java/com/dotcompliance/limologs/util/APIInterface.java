package com.dotcompliance.limologs.util;

import com.dotcompliance.limologs.data.AssignDrivertoVechileModel;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Created by ADMIN on 26-10-2017.
 */

public interface APIInterface {

    @FormUrlEncoded
    @POST("log/assign_unassign_records")
    Call<AssignDrivertoVechileModel> AssignDriverToVechile(@Field("token") String token,
                                                           @Field("driver_id") String driver_id,
                                                           @Field("unassign_records") String unassign_records);



}
