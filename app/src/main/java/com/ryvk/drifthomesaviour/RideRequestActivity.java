package com.ryvk.drifthomesaviour;

import static com.ryvk.drifthomesaviour.Utils.decodePolyline;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RideRequestActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "RideRequestActivity";
    private String intentBody;
    private Saviour loggedSaviour;
    private String rideId;
    public static String drinkerFcmToken;
    private String drinkerName;
    private GeoPoint userLocation;
    private GeoPoint pickupLocation;
    private String distanceInKm;
    private GoogleMap mMap;
    private Thread standby;
    private FusedLocationProviderClient fusedLocationClient;
    private OnBackPressedCallback callback;
    private AlertDialog exitDialog;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;
    private static final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ride_request);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loggedSaviour = Saviour.getSPSaviour(RideRequestActivity.this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(RideRequestActivity.this);
        checkLocationPermission();

        callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                exitDialog = AlertUtils.showExitConfirmationDialog(RideRequestActivity.this);
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);

        Button acceptButton = findViewById(R.id.button10);
        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (standby != null && standby.isAlive()) {
                    standby.interrupt();
                }
                checkIfRequestExists();
                Intent i = new Intent(getApplicationContext(), PickupActivity.class);
                i.putExtra("body", intentBody);
                startActivity(i);
                finish();
            }
        });

        Button rejectButton = findViewById(R.id.button11);
        rejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                endRideRequest();
            }
        });

        Intent intent = getIntent();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(GeoPoint.class, new GeoPointAdapter())
                .create();

        if (intent != null) {
            intentBody = intent.getStringExtra("body");

            Log.d(TAG, "onCreate: get intent data "+intentBody);

            JsonObject rideData = gson.fromJson(intentBody, JsonObject.class);

            rideId = rideData.get("rideId").getAsString();
            drinkerName = rideData.get("customerName").getAsString();
            drinkerFcmToken = rideData.get("fcmToken").getAsString();
            pickupLocation = gson.fromJson(rideData.get("location"), GeoPoint.class);

        }
    }

    private void acceptRide(){
        Log.d(TAG, "acceptRide: fcm token :"+drinkerFcmToken);

        JsonObject json = new JsonObject();
        try {
            json.addProperty("rideId", rideId);
            json.addProperty("saviour_email", loggedSaviour.getEmail());
            json.addProperty("fcmToken", drinkerFcmToken);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        String BASE_URL = getResources().getString(R.string.base_url);
        RequestBody requestBody = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(BASE_URL + "/send-ride-accepted")
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
                } else {
                    System.out.println("Error: " + response.code());
                }
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {

                HashMap<String,Object> tripUpdateHash = new HashMap<>();
                tripUpdateHash.put("saviour_email",loggedSaviour.getEmail());
                tripUpdateHash.put("current_saviour_location",userLocation);

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("trip").document(rideId)
                        .update(tripUpdateHash)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d(TAG, "onSuccess: Saviour email added to trip : success");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, "onFailure: Saviour email adding to trip : failed");
                            }
                        });
            }
        }).start();

    }

    private void checkIfRequestExists(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("trip").document(rideId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        acceptRide();
                    } else {
                        Log.d("Firestore", "No such document exists");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching document", e);
                });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            loadMap();
        }
    }

    private void loadMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment2);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void getRoute() {
        String origin = userLocation.getLatitude() + "," + userLocation.getLongitude();
        String destination = pickupLocation.getLatitude() + "," + pickupLocation.getLongitude();

        String apiKey = loggedSaviour.getApiKey(RideRequestActivity.this);
        String directionsApiUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination + "&key=" + apiKey;

        Request directionsApiRequest = new Request.Builder().url(directionsApiUrl).build();

        client.newCall(directionsApiRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get directions", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        // Parse the Directions API response
                        JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);
                        JsonArray routes = jsonResponse.getAsJsonArray("routes");
                        if (routes.size() > 0) {
                            JsonObject route = routes.get(0).getAsJsonObject();
                            JsonArray legs = route.getAsJsonArray("legs");
                            if (legs.size() > 0) {
                                JsonObject leg = legs.get(0).getAsJsonObject();
                                JsonArray steps = leg.getAsJsonArray("steps");

                                List<LatLng> polylinePoints = new ArrayList<>();
                                for (JsonElement step : steps) {
                                    JsonObject stepObject = step.getAsJsonObject();
                                    String polyline = stepObject.getAsJsonObject("polyline").get("points").getAsString();
                                    polylinePoints.addAll(decodePolyline(polyline));
                                }
                                String distanceText = leg.getAsJsonObject("distance").get("text").getAsString();
                                double distanceValue = leg.getAsJsonObject("distance").get("value").getAsDouble(); // in meters
                                distanceInKm = String.valueOf(distanceValue / 1000);

                                Gson gson = new GsonBuilder()
                                        .registerTypeAdapter(GeoPoint.class, new GeoPointAdapter())
                                        .create();
                                JsonObject rideData = gson.fromJson(intentBody, JsonObject.class);
                                rideData.addProperty("distanceInKm",distanceInKm);
                                intentBody = gson.toJson(rideData);

                                Log.d(TAG, "Distance: " + distanceText + " (" + distanceInKm + " km)");
                                // Add the route polyline to the map
                                drawRouteOnMap(polylinePoints);
                                updateUI();

                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing directions response", e);
                    }
                }
            }
        });
    }

    private void drawRouteOnMap(List<LatLng> polylinePoints) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMap != null && !polylinePoints.isEmpty()) {
                    Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(polylinePoints).color(getResources().getColor(R.color.d_blue))); // Replace with your desired color
                    polyline.setWidth(10); // You can change the width of the polyline
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                        LatLng userLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
                        LatLng pickupLatLng = new LatLng(pickupLocation.getLatitude(), pickupLocation.getLongitude());

                        // Add marker for the user's location
                        mMap.addMarker(new MarkerOptions().position(userLatLng).title("Your Location"));

                        // Add marker for the pickup location
                        mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup Location"));
                        getRoute();

                        // Move camera to show both locations
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(userLatLng);
                        builder.include(pickupLatLng);
                        LatLngBounds bounds = builder.build();
                        int padding = 50; // padding around the map
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));

                    }else{
                        Toast.makeText(RideRequestActivity.this,"Location is null",Toast.LENGTH_LONG).show();
                    }
                }
            });

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, int deviceId) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    loadMap();
                }
            }
        }
    }

    private void updateUI (){
        TextView nameTextView = findViewById(R.id.textView16);
        TextView distanceTextView = findViewById(R.id.textView48);
        ProgressBar progressBar = findViewById(R.id.requestRideProgressBar);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                nameTextView.setText(drinkerName);
                distanceTextView.setText("("+distanceInKm+" KM Away)");

            }
        });
        standby = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int x = 0; x <= 10; x++) {
                        if (Thread.currentThread().isInterrupted()) {
                            return; // Stop the thread if interrupted
                        }
                        int progress = 10 * x;
                        runOnUiThread(() -> progressBar.setProgress(progress));
                        Thread.sleep(1000);
                    }
                    endRideRequest();
                } catch (InterruptedException e) {
                    Log.d(TAG, "Standby thread interrupted");
                }
            }
        });
        standby.start();
    }

    private void endRideRequest(){
        finish();
    }

    @Override
    public void finish() {
        if (standby != null && standby.isAlive()) {
            standby.interrupt();
        }
        super.finish();
    }

    @Override
    protected void onDestroy() {
        if(exitDialog != null && exitDialog.isShowing()){
            exitDialog.dismiss();
        }
        super.onDestroy();
    }
}