package com.example.music_zhoumingzheng.utils;

import com.example.music_zhoumingzheng.module.User;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("register")
    Call<ResponseBody> registerUser(@Body User user);

    @POST("login")
    Call<ResponseBody> loginUser(@Body User user);
}

