package com.ryvk.drifthomesaviour;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import java.util.HashMap;

public class Saviour {
    public static final int KYC_UNVERIYFIED = 0;
    public static final int KYC_PENDING = 1;
    public static final int KYC_VERIYFIED = 2;
    public static final int KYC_DECLINED = 3;
    private String email;
    private String name;
    private String mobile;
    private String dob;
    private String gender;
    private String profile_pic;
    private int tokens;
    private int trip_count;
    private String vehicle;
    private int kyc;
    private boolean blocked;
    private boolean online;
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

    public String getProfile_pic() {
        return profile_pic;
    }

    public void setProfile_pic(String profile_pic) {
        this.profile_pic = profile_pic;
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

    public boolean isBlocked() {return blocked;}

    public void setBlocked(boolean blocked) {this.blocked = blocked;}

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;

        HashMap<String, Object> saviour = new HashMap<>();
        saviour.put("online", this.isOnline());

        if(this.isOnline()){
            FirebaseMessaging.getInstance().subscribeToTopic("saviours")
                    .addOnCompleteListener(task -> {
                        String msg = task.isSuccessful() ? "Subscribed to saviours topic!" : "Subscription failed.";
                        Log.d("FCM", msg);
                        Log.d("Saviour", "Saviour is added to the topic");
                    });
        }else{
            FirebaseMessaging.getInstance().unsubscribeFromTopic("saviours")
                    .addOnCompleteListener(task -> {
                        String msg = task.isSuccessful() ? "Unsubscribed from saviours topic!" : "Unsubscription failed.";
                        Log.d("FCM", msg);
                        Log.d("Saviour", "Saviour is removed from the topic");
                    });
        }

        if(this.email != null){
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("saviour")
                    .document(this.email)
                    .update(saviour)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.i("Saviour", "update online: success");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i("Saviour", "update online: failure");
                        }
                    });
        }

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

        HashMap<String, Object> saviour = new HashMap<>();
        saviour.put("name", this.getName());
        saviour.put("email", this.getEmail());
        saviour.put("mobile", this.getMobile());
        saviour.put("gender", this.getGender());
        saviour.put("dob", this.getDob());
        saviour.put("updated_at", Validation.todayDateTime());

        return saviour;

    }

    public String getApiKey(Context context) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return ai.metaData.getString("com.google.android.geo.API_KEY");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Saviour getSPSaviour(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.ryvk.drifthomesaviour.data", Context.MODE_PRIVATE);
        String saviourJSON = sharedPreferences.getString("user",null);
        Gson gson = new Gson();
        return gson.fromJson(saviourJSON, Saviour.class);
    }

    public void updateSPSaviour (Context context,Saviour saviour){
        Gson gson = new Gson();
        String saviourJSON = gson.toJson(saviour);

        SharedPreferences sharedPreferences = context.getSharedPreferences("com.ryvk.drifthomesaviour.data",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user",saviourJSON);
        editor.apply();
        editor.commit();
    }

    public void removeSPSaviour(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.ryvk.drifthomesaviour.data",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("user");
        editor.apply();
    }

}
