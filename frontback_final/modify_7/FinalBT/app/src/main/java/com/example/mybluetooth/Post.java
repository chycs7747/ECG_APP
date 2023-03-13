package com.example.mybluetooth;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;


public class Post implements Serializable {
    @SerializedName("id")
    private int id;

    //ecg field
    @SerializedName("ecg")
    private String ecg;
    @SerializedName("ecg_user")
    private String ecg_user;
    @SerializedName("weather")
    private String weather;
    @SerializedName("temperature")
    private String temperature;
    @SerializedName("micro_dust")
    private String micro_dust;
    @SerializedName("tmicro_dust")
    private String tmicro_dust;
    @SerializedName("uv_ray")
    private String uv_ray;
    @SerializedName("created_date")
    private String created_date;
    @SerializedName("location")
    private String location;
    //user field
    @SerializedName("email")
    private String email;
    @SerializedName("password")
    private String password;



    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    //ecg
    public String getEcg() {
        return ecg;
    }

    public void setEcg() { this.ecg = ecg; }

    public String getEcg_user() {
        return ecg_user;
    }

    public void setEcg_user() { this.ecg_user = ecg_user; }

    public String getWeather() {
        return weather;
    }

    public void setWeather() { this.weather = weather; }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature() { this.temperature = temperature; }

    public String getMicro_dust() {
        return micro_dust;
    }

    public void setMicro_dust() { this.micro_dust = micro_dust; }

    public String getTmicro_dust() {
        return tmicro_dust;
    }

    public void setTmicro_dust() { this.tmicro_dust = tmicro_dust; }

    public String getUv_ray() {
        return uv_ray;
    }

    public void setUv_ray() { this.uv_ray = uv_ray; }

    public String getCreated_date() {
        return created_date;
    }

    public void setCreated_date() { this.created_date = created_date; }

    public String getLocation() {
        return location;
    }

    public void setLocatione() { this.location = location; }


    //user
    public String getEmail() {
        return email;
    }

    public void setEmail() { this.email = email; }

    public String getPassword() { return password; }

    public void setPassword(){
        this.password = password;
    }
}