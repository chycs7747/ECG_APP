package com.example.mybluetooth;

import com.google.gson.annotations.SerializedName;

public class Post {
    @SerializedName("id")
    private int id;
    @SerializedName("email")
    private String email;
    @SerializedName("password")
    private String password;
    @SerializedName("ecg")
    private String ecg;
    @SerializedName("ecg_user")
    private String ecg_user;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail() { this.email = email; }

    public String getPassword() { return password; }

    public void setPassword(){
        this.password = password;
    }

    public String getEcg() {
        return ecg;
    }

    public void setEcg() { this.ecg = ecg; }

    public String getEcg_user() {
        return ecg_user;
    }

    public void setEcg_user() { this.ecg_user = ecg_user; }

}