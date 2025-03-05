package com.ryvk.drifthomesaviour;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TripActivity extends AppCompatActivity {
    private static final String TAG = "TripActivity";
    private String intentBody;
    private Trip tripData;
    private String drinkerMobile;
    private String rideId;
    private int totalFare;
    private static final OkHttpClient client = new OkHttpClient();
    private OnBackPressedCallback callback;
    private Thread endButtonThread;
    private TextView nameText;
    private TextView pickupLocationText;
    private TextView dropLocationText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trip);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        nameText = findViewById(R.id.textView18);
        pickupLocationText = findViewById(R.id.textView19);
        dropLocationText = findViewById(R.id.textView20);

        Intent intent = getIntent();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(GeoPoint.class, new GeoPointAdapter())
                .create();

        if (intent != null) {
            intentBody = intent.getStringExtra("body");
            JsonObject rideData = gson.fromJson(intentBody, JsonObject.class);

            rideId = rideData.get("rideId").getAsString();
            loadTripData();

        }

        callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(TripActivity.this,"Not Allowed!",Toast.LENGTH_SHORT).show();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);

        Button reportButton = findViewById(R.id.button12);
        reportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.reportTrip(TripActivity.this);
            }
        });

        ImageButton callButton = findViewById(R.id.imageButton);
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.showCallOptionsPopup(view,TripActivity.this,drinkerMobile);
            }
        });

        ImageButton navigationButton = findViewById(R.id.imageButton2);
        navigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tripData.getPickup() != null && tripData.getDrop() != null) {
                    // Create the URI for Google Maps navigation
                    String uri = String.format("google.navigation:q=%f,%f&origin=%f,%f&mode=d",
                            tripData.getDrop().getLatitude(), tripData.getDrop().getLongitude(),
                            tripData.getPickup().getLatitude(), tripData.getPickup().getLongitude());

                    // Create an Intent to open Google Maps
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    intent.setPackage("com.google.android.apps.maps");

                    // Check if Google Maps is available
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        Toast.makeText(getApplicationContext(), "Google Maps is not installed", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Location data is missing", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button endButton = findViewById(R.id.button13);
        Button feedbackSubmitButton = findViewById(R.id.button17);
        Button backToHomeButton = findViewById(R.id.button18);
        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertUtils.showConfirmDialog(TripActivity.this, "Trip End?", "Are you sure you want to end the trip?", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        endTrip();
                    }
                });
            }
        });
        endButtonThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for(int x = 20; x >= 0; x--){
                    int seconds = x;
                    runOnUiThread(()->endButton.setText("End in "+seconds+"s"));
                    try {
                        Thread.sleep(1000);
                    }catch (Exception e){
                        Log.e(TAG, "run: end trip button",e);
                    }
                }
                runOnUiThread(()->endButton.setText(R.string.d_trip_btn2_end));
                runOnUiThread(()->endButton.setEnabled(true));
            }
        });
        endButtonThread.start();

        RatingBar ratingBar = findViewById(R.id.ratingBar);
        EditText feedbackInput = findViewById(R.id.editTextTextMultiLine);

        feedbackSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float rating = ratingBar.getRating();
                String feedback = feedbackInput.getText().toString().trim();

                if (rating < 1) {
                    rating = 1;
                }

                if (feedback.isEmpty()) {
                    Toast.makeText(TripActivity.this, "Please enter your feedback!", Toast.LENGTH_SHORT).show();
                } else {
                    Map<String, Object> feedbackData = new HashMap<>();
                    feedbackData.put("rating", rating);
                    feedbackData.put("feedback", feedback);
                    feedbackData.put("timestamp", System.currentTimeMillis());

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("trip").document(rideId)
                            .update("feedback_of_saviour", feedbackData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(TripActivity.this, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(TripActivity.this, "Failed to submit feedback: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            }
        });
        backToHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void loadTripLocations(){

        Saviour loggedSaviour = Saviour.getSPSaviour(TripActivity.this);
        String apiKey = loggedSaviour.getApiKey(TripActivity.this);

        String geocodingApiUrl = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + tripData.getPickup().getLatitude() + "," + tripData.getPickup().getLongitude() + "&key=" + apiKey;
        Request geocodeApiRequest = new Request.Builder().url(geocodingApiUrl).build();
        client.newCall(geocodeApiRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get geocode data", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        // Parse the Geocoding API response
                        Gson gson = new Gson();
                        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                        JsonArray results = jsonResponse.getAsJsonArray("results");

                        if (results != null && results.size() > 0) {
                            JsonObject firstResult = results.get(0).getAsJsonObject();
                            String pickupAddress = firstResult.get("formatted_address").getAsString();
                            Log.d(TAG, "Address: " + pickupAddress);
                            pickupLocationText.setText(pickupAddress);
                        } else {
                            Log.d(TAG, "No address found");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing geocode response", e);
                    }
                }
            }
        });

        String geocodingApiUrl2 = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + tripData.getDrop().getLatitude() + "," + tripData.getDrop().getLongitude() + "&key=" + apiKey;
        Request geocodeApiRequest2 = new Request.Builder().url(geocodingApiUrl2).build();
        client.newCall(geocodeApiRequest2).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get geocode data", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        // Parse the Geocoding API response
                        Gson gson = new Gson();
                        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                        JsonArray results = jsonResponse.getAsJsonArray("results");

                        if (results != null && results.size() > 0) {
                            JsonObject firstResult = results.get(0).getAsJsonObject();
                            String dropAddress = firstResult.get("formatted_address").getAsString();
                            Log.d(TAG, "Address: " + dropAddress);
                            dropLocationText.setText(dropAddress);
                        } else {
                            Log.d(TAG, "No address found");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing geocode response", e);
                    }
                }
            }
        });
    }

    private void loadTripData(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("trip").document(rideId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tripData = documentSnapshot.toObject(Trip.class);
                        loadTripLocations();

                        db.collection("drinker")
                                .document(tripData.getDrinker_email())
                                .get()
                                .addOnSuccessListener(documentSnapshot2 -> {
                                    if (documentSnapshot2.exists()) {
                                        nameText.setText(documentSnapshot2.get("name").toString());
                                        drinkerMobile = documentSnapshot2.get("mobile").toString();
                                    } else {
                                        Log.d(TAG, "onFailure: drinker data retrieval failed");
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.d(TAG, "onFailure: trip data retrieval failed");
                                });
                    } else {
                        Log.d("Firestore", "No such document exists");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching document", e);
                });
    }

    private void endTrip(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Saviour loggedSaviour = Saviour.getSPSaviour(TripActivity.this);
                String api_key = loggedSaviour.getApiKey(TripActivity.this);

                int tripDistance = Utils.getRoadDistance(api_key,tripData.getPickup(),tripData.getDrop());

                if(tripDistance != -1){
                    tripDistance = tripDistance / 1000;  //convert to KM

                    int feePerKM = getResources().getInteger(R.integer.fee_per_km);
                    totalFare = tripDistance * feePerKM;
                    loggedSaviour.setTokens(loggedSaviour.getTokens()+totalFare);
                    loggedSaviour.updateSPSaviour(TripActivity.this,loggedSaviour);

                    JsonObject json = new JsonObject();
                    try {
                        json.addProperty("rideId", rideId);
                        json.addProperty("fcmToken", RideRequestActivity.drinkerFcmToken);
                        Log.d(TAG, "onClick: mark as arrived -> fcmToken: "+RideRequestActivity.drinkerFcmToken);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }

                    HashMap<String,Object> saviour = new HashMap<>();
                    saviour.put("tokens",loggedSaviour.getTokens());

                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    db.collection("saviour")
                            .document(loggedSaviour.getEmail())
                            .update(saviour)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Log.i(TAG, "update details: success");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.i(TAG, "update details: failure");
                                    runOnUiThread(()->AlertUtils.showAlert(TripActivity.this,"Tokens Update Failed!","Error: "+e));
                                }
                            });

                    String BASE_URL = getResources().getString(R.string.base_url);
                    RequestBody requestBody = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url(BASE_URL + "/end-trip")
                            .post(requestBody)
                            .build();
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            e.printStackTrace();
                            System.out.println("Request Failed: " + e.getMessage());
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                System.out.println("Notification Sent: " + response.body().string());
                                updateUI();
                            } else {
                                System.out.println("Error: " + response.code());
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private void updateUI(){
        TextView title = findViewById(R.id.textView17);
        CardView fareCard = findViewById(R.id.cardView);
        TextView totalFareText = findViewById(R.id.textView23);
        TextView feedbackSectionTitle = findViewById(R.id.textView21);
        RatingBar feedbackRating = findViewById(R.id.ratingBar);
        EditText feedbackDescription = findViewById(R.id.editTextTextMultiLine);
        Button endButton = findViewById(R.id.button13);
        Button feedbackSubmitButton = findViewById(R.id.button17);
        Button backToHomeButton = findViewById(R.id.button18);

        runOnUiThread(()->{
            title.setText(R.string.d_trip_text1_tripEnd);
            totalFareText.setText(String.valueOf(totalFare));
            endButton.setVisibility(View.INVISIBLE);
            fareCard.setVisibility(View.VISIBLE);
            feedbackSectionTitle.setVisibility(View.VISIBLE);
            feedbackRating.setVisibility(View.VISIBLE);
            feedbackDescription.setVisibility(View.VISIBLE);
            feedbackSubmitButton.setVisibility(View.VISIBLE);
            backToHomeButton.setVisibility(View.VISIBLE);
        });
    }

    @Override
    protected void onDestroy() {
        if (endButtonThread != null && endButtonThread.isAlive()) {
            endButtonThread.interrupt();
        }
        super.onDestroy();
    }
}