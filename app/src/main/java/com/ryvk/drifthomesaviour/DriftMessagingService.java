package com.ryvk.drifthomesaviour;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DriftMessagingService extends FirebaseMessagingService {
    private static final String TAG = "DriftMessagingService";
    private FusedLocationProviderClient fusedLocationClient;
    private RemoteMessage remoteMessage;
    private String API_KEY;
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        this.remoteMessage = remoteMessage;

        if (remoteMessage.getNotification() != null) {
            // Get the current location

            String title = remoteMessage.getNotification().getTitle();
            if(title.equals("rideRequest")){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        getCurrentLocation();
                    }
                }).start();
            }else if(title.equals("requestRideCancel")){
                requestRideCancel();
            }
        }
    }

    private void getCurrentLocation() {
        // Check if permission is granted (You should handle permissions)
        // Get the last known location

        Saviour loggedSaviour = Saviour.getSPSaviour(this);
        API_KEY = loggedSaviour.getApiKey(this);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                Location currentLocation = task.getResult();
                                if (currentLocation != null) {
                                    double latitude = currentLocation.getLatitude();
                                    double longitude = currentLocation.getLongitude();
                                    GeoPoint userLocation = new GeoPoint(latitude, longitude);

                                    String title = remoteMessage.getNotification().getTitle();
                                    if(title.equals("rideRequest")){
                                        newRideRequest(userLocation);
                                    }

                                }
                            } else {
                                Log.e(TAG, "Failed to get location.");
                            }
                        }
                    });
        }
    }

    private void newRideRequest(GeoPoint userLocation){
        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(GeoPoint.class, new GeoPointAdapter())
                .create();
        JsonObject rideData = gson.fromJson(body, JsonObject.class);
        GeoPoint pickupLocation = gson.fromJson(rideData.get("location"), GeoPoint.class);

        String origin = userLocation.getLatitude() + "," + userLocation.getLongitude();
        String destination = pickupLocation.getLatitude() + "," + pickupLocation.getLongitude();

        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination + "&key="+API_KEY;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error fetching directions: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        Gson gson = new Gson();
                        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                        JsonObject routes = jsonResponse.getAsJsonArray("routes").get(0).getAsJsonObject();
                        JsonObject legs = routes.getAsJsonArray("legs").get(0).getAsJsonObject();
                        JsonObject distance = legs.getAsJsonObject("distance");
                        double roadDistance = distance.get("value").getAsDouble();
                        int distanceThreshold =  getResources().getInteger(R.integer.pickup_distance_threshold_meters);
                        Log.d(TAG, "Road distance: " + roadDistance);

                        if(roadDistance < distanceThreshold){
                            Log.d(TAG, "Notification received: " + title + " : " + body);

                            Intent intent = new Intent(DriftMessagingService.this, RideRequestActivity.class);
                            intent.putExtra("title", title);
                            intent.putExtra("body", body);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing Directions API response: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "Failed to get directions: " + response.message());
                }
            }
        });
    }

    private void requestRideCancel(){
        String body = remoteMessage.getNotification().getBody();
        Log.d(TAG, "requestRideCancel: ride is requested to cancel -> "+body);

        Intent intent = new Intent("com.ryvk.drifthome.REQUEST_RIDE_CANCEL");
        intent.putExtra("rideData", body);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "New token generated: " + token);
        Saviour saviour = Saviour.getSPSaviour(this);
        FirebaseFirestore.getInstance().collection("saviour").document(saviour.getEmail())
                .update("fcmToken", token);
        SplashActivity.fcmToken = token;
    }
}