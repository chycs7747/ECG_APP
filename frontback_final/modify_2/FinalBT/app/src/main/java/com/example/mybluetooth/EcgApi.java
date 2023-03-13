package com.example.mybluetooth;

import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface EcgApi {

    @GET("ecg")
    Call<List<Post>> getecg(
            @Query(value = "search", encoded = true) String ecg_user
    );


    @FormUrlEncoded
    @POST("ecg/?format=json")
    Call<Post> postecg(@FieldMap HashMap<String, Object> param);
}