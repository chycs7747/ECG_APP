package com.example.mybluetooth;

import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface LoginApi {

    @GET("user")
    Call<List<Post>> getdata(
            @Query(value = "email", encoded = true) String email
    );


    @FormUrlEncoded
    @POST("user/")
    Call<Post> postdata(@FieldMap HashMap<String, Object> param);
}
