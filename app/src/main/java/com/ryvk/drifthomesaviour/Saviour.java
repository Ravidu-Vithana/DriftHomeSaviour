package com.ryvk.drifthomesaviour;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;

public class Saviour {
    public static final int KYC_UNVERIYFIED = 0;
    public static final int KYC_PENDING = 1;
    public static final int KYC_VERIYFIED = 2;
    private String email;
    private String name;
    private String mobile;
    private String dob;
    private String gender;
    private int tokens;
    private int trip_count;
    private String vehicle;
    private int kyc;
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

    public String getVehicle() {
        return vehicle;
    }

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

    public int getKyc() {
        return kyc;
    }

    public void setKyc(int kyc) {
        this.kyc = kyc;
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

    public HashMap<String, Object> updateFields(String email, String name, String mobile, String gender, String dob) {
        if (email != null) {
            this.email = email;
        }
        if (name != null) {
            this.name = name;
        }
        if (mobile != null) {
            this.mobile = mobile;
        }
        if (gender != null) {
            this.gender = gender;
        }
        if (dob != null) {
            this.dob = dob;
        }

        HashMap<String, Object> drinker = new HashMap<>();
        drinker.put("name", this.getName());
        drinker.put("email", this.getEmail());
        drinker.put("mobile", this.getMobile());
        drinker.put("gender", this.getGender());
        drinker.put("dob", this.getDob());
        drinker.put("updated_at", Validation.todayDateTime());

        return drinker;

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
