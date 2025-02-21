package com.ryvk.drifthomesaviour;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Saviour {
    private String email;
    private String name;
    private String mobile;
    private String dob;
    private String gender;
    private int tokens;
    private int trip_count;
    private String created_at;
    private String updated_at;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getTokens() {
        return tokens;
    }

    public void setTokens(int tokens) {
        this.tokens = tokens;
    }

    public int getTrip_count() {
        return trip_count;
    }

    public void setTrip_count(int trip_count) {
        this.trip_count = trip_count;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    public static Saviour getSPSaviour(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.ryvk.drifthome.data", Context.MODE_PRIVATE);
        String saviourJSON = sharedPreferences.getString("user",null);
        Gson gson = new Gson();
        return gson.fromJson(saviourJSON, Saviour.class);
    }

    public void updateSPSaviour (Context context,Saviour saviour){
        Gson gson = new Gson();
        String saviourJSON = gson.toJson(saviour);

        SharedPreferences sharedPreferences = context.getSharedPreferences("com.ryvk.drifthome.data",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user",saviourJSON);
        editor.apply();
    }

    public void removeSPSaviour(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.ryvk.drifthome.data",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("user");
        editor.apply();
    }

}
