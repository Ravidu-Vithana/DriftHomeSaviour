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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
    private static final String TAG = "FCMService";
    private FusedLocationProviderClient fusedLocationClient;
    private RemoteMessage remoteMessage;
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        this.remoteMessage = remoteMessage;
        // Get the current location
        new Thread(new Runnable() {
            @Override
            public void run() {
                getCurrentLocation();
            }
        }).start();

    }

    private void getCurrentLocation() {
        // Check if permission is granted (You should handle permissions)
        // Get the last known location

        Saviour loggedSaviour = Saviour.getSPSaviour(this);
        String API_KEY = loggedSaviour.getApiKey(this);

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

                                    if (remoteMessage.getNotification() != null) {
                                        String title = remoteMessage.getNotification().getTitle();
                                        String body = remoteMessage.getNotification().getBody();

                                        Gson gson = new GsonBuilder()
                                                .registerTypeAdapter(GeoPoint.class, new GeoPointAdapter())
                                                .create();
                                        JsonObject rideData = gson.fromJson(body, JsonObject.class);

                                        String rideId = rideData.get("rideId").getAsString();
                                        String drinkerName = rideData.get("customerName").getAsString();
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
                                                    // Use Gson to parse the response
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
                                                            // Handle the received notification
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
                                }
                            } else {
                                Log.e(TAG, "Failed to get location.");
                            }
                        }
                    });
        }
    }

    private void showNotification(String title, String body) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "ride_requests_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Ride Requests", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        notificationManager.notify(0, notification);
    }

    @Override
    public void onNewToken(String token) {
        // This is called whenever a new token is generated
        Log.d(TAG, "New token: " + token);

        // Here, you can send the token to your server to save it for notifications
        sendTokenToServer(token);
    }

    private void sendTokenToServer(String token) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String json = "{\"token\":\"" + token + "\"}";
        RequestBody body = RequestBody.create(json, JSON);

        // Set up OkHttp client
        OkHttpClient client = new OkHttpClient();

        // Create the request
        Request request = new Request.Builder()
                .url("http://localhost:5000/send-ride-request") // Replace with your backend endpoint
                .post(body)
                .build();

        // Send the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Log failure or handle error
                Log.e(TAG, "Error sending token to server: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Handle successful response
                    Log.d(TAG, "Token sent successfully to server.");
                } else {
                    // Handle unsuccessful response
                    Log.e(TAG, "Failed to send token to server: " + response.message());
                }
            }
        });
    }
}