package com.ryvk.drifthomesaviour;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

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

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            // Handle the received notification
            Log.d(TAG, "Notification received: " + title + " : " + body);
            showNotification(title, body);
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