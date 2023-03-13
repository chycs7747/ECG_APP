package com.example.mybluetooth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface EcgApi {

    @GET("ecg/")
    Call<List<Post>> getecg(
            //@Query(value = "user", encoded = true) String ecg_user
            @QueryMap Map<String, String> query
    );


    @FormUrlEncoded
    @POST("ecg/")
    Call<Post> postecg(@FieldMap HashMap<String, Object> param);
}