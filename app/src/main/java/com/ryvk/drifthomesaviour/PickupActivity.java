package com.ryvk.drifthomesaviour;

import static com.ryvk.drifthomesaviour.Utils.decodePolyline;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PickupActivity extends AppCompatActivity  implements OnMapReadyCallback {
    private static final String TAG = "PickupActivity";
    private String intentBody;
    private Saviour loggedSaviour;
    private String rideId;
    private String drinkerName;
    private String drinkerMobile;
    private GeoPoint userLocation;
    private GeoPoint pickupLocation;
    private String pickupAddress;
    private String distanceInKm;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final OkHttpClient client = new OkHttpClient();
    private OnBackPressedCallback callback;
    private Thread markAsArrivedButtonThread;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pickup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(requestRideCancelReceiver,
                new IntentFilter("com.ryvk.drifthome.REQUEST_RIDE_CANCEL"));

        Intent intent = getIntent();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(GeoPoint.class, new GeoPointAdapter())
                .create();

        if (intent != null) {
            intentBody = intent.getStringExtra("body");
            JsonObject rideData = gson.fromJson(intentBody, JsonObject.class);

            rideId = rideData.get("rideId").getAsString();
            drinkerName = rideData.get("customerName").getAsString();
            distanceInKm = rideData.get("distanceInKm").getAsString();
            pickupLocation = gson.fromJson(rideData.get("location"), GeoPoint.class);

        }

        new Thread(()->{
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("trip")
                    .document(rideId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Trip trip = documentSnapshot.toObject(Trip.class);

                            db.collection("drinker")
                                    .document(trip.getDrinker_email())
                                    .get()
                                    .addOnSuccessListener(documentSnapshot2 -> {
                                        if (documentSnapshot2.exists()) {
                                            drinkerMobile = documentSnapshot2.get("mobile").toString();
                                        } else {
                                            Log.d(TAG, "onFailure: drinker data retrieval failed");
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.d(TAG, "onFailure: trip data retrieval failed");
                                    });

                        } else {

                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.d(TAG, "onFailure: trip data retrieval failed");
                    });
        }).start();

        loggedSaviour = Saviour.getSPSaviour(PickupActivity.this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(PickupActivity.this);
        checkLocationPermission();

        callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cancelRide(false);
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);

        ImageButton callButton = findViewById(R.id.imageButton);
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.showCallOptionsPopup(view,PickupActivity.this,drinkerMobile);
            }
        });

        ImageButton navigationButton = findViewById(R.id.imageButton2);
        navigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userLocation != null && pickupLocation != null) {
                    // Create the URI for Google Maps navigation
                    String uri = String.format("google.navigation:q=%f,%f&origin=%f,%f&mode=d",
                            pickupLocation.getLatitude(), pickupLocation.getLongitude(),
                            userLocation.getLatitude(), userLocation.getLongitude());

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

        Button reportButton = findViewById(R.id.button12);
        reportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.reportTrip(PickupActivity.this);
            }
        });

        Button markAsArrivedButton = findViewById(R.id.button13);
        markAsArrivedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                markAsArrived();
            }
        });
        markAsArrivedButtonThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for(int x = 20; x >= 0; x--){
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    int seconds = x;
                    runOnUiThread(()->markAsArrivedButton.setText("Mark in "+seconds+"s"));
                    try {
                        Thread.sleep(1000);
                    }catch (Exception e){
                        Log.e(TAG, "run: mark as arrived button enabling",e);
                    }
                }
                runOnUiThread(()->markAsArrivedButton.setText(R.string.d_pickup_btn3_text2));
                runOnUiThread(()->markAsArrivedButton.setEnabled(true));
            }
        });
        markAsArrivedButtonThread.start();

        Button cancelButton = findViewById(R.id.button14);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelRide(false);
            }
        });

        Button startTripButton = findViewById(R.id.button16);
        startTripButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTrip();
            }
        });

    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            loadMap();
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

    private void loadMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment3);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void getRoute() {
        String origin = userLocation.getLatitude() + "," + userLocation.getLongitude();
        String destination = pickupLocation.getLatitude() + "," + pickupLocation.getLongitude();

        String apiKey = loggedSaviour.getApiKey(PickupActivity.this);

        String directionsApiUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination + "&key=" + apiKey;
        String geocodingApiUrl = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + pickupLocation.getLatitude() + "," + pickupLocation.getLongitude() + "&key=" + apiKey;

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
                            pickupAddress = firstResult.get("formatted_address").getAsString();
                            Log.d(TAG, "Address: " + pickupAddress);
                            updateUI();
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

    private void drawRouteOnMap(List<LatLng> polylinePoints) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMap != null && !polylinePoints.isEmpty()) {
                    Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(polylinePoints).color(getResources().getColor(R.color.d_blue))); // Replace with your desired color
                    polyline.setWidth(10);
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
                        userLocation = new GeoPoint(location.getLatitude(),location.getLongitude());

                        Gson gson = new GsonBuilder()
                                .registerTypeAdapter(GeoPoint.class, new GeoPointAdapter())
                                .create();
                        JsonObject rideData = gson.fromJson(intentBody,JsonObject.class);
                        rideData.add("userLocation",gson.toJsonTree(userLocation).getAsJsonObject());
                        intentBody = gson.toJson(rideData);

                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
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
                        Toast.makeText(PickupActivity.this,"Location is null",Toast.LENGTH_LONG).show();
                    }
                }
            });

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    private void updateUI (){
        TextView nameTextView = findViewById(R.id.textView18);
        TextView addressTextView = findViewById(R.id.textView19);
        ProgressBar progressBar = findViewById(R.id.requestRideProgressBar);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                nameTextView.setText(drinkerName);
                addressTextView.setText(pickupAddress);

            }
        });
    }

    private BroadcastReceiver requestRideCancelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.ryvk.drifthome.REQUEST_RIDE_CANCEL".equals(intent.getAction())) {
                cancelRide(true);
            }
        }
    };

    private void startTrip(){

        AlertUtils.showConfirmDialog(PickupActivity.this, "Start Trip?", "Are you sure you want to start the trip?", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                JsonObject json = new JsonObject();
                try {
                    json.addProperty("rideId", rideId);
                    json.addProperty("fcmToken", RideRequestActivity.drinkerFcmToken);
                    Log.d(TAG, "onClick: mark as arrived -> fcmToken: "+RideRequestActivity.drinkerFcmToken);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                String BASE_URL = getResources().getString(R.string.base_url);
                RequestBody requestBody = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url(BASE_URL + "/start-trip")
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

                            Intent i = new Intent(getApplicationContext(), TripActivity.class);
                            i.putExtra("body", intentBody);
                            startActivity(i);
                            finish();

                        } else {
                            System.out.println("Error: " + response.code());
                        }
                    }
                });
            }
        });

    }

    private void markAsArrived(){

        Button markAsArrivedButton = findViewById(R.id.button13);
        Button iArrivedButton = findViewById(R.id.button15);
        Button startTripButton = findViewById(R.id.button16);

        runOnUiThread(()->{
            markAsArrivedButton.setVisibility(View.INVISIBLE);
            iArrivedButton.setVisibility(View.VISIBLE);
            startTripButton.setVisibility(View.VISIBLE);
        });

        JsonObject json = new JsonObject();
        try {
            json.addProperty("rideId", rideId);
            json.addProperty("fcmToken", RideRequestActivity.drinkerFcmToken);
            Log.d(TAG, "onClick: mark as arrived -> fcmToken: "+RideRequestActivity.drinkerFcmToken);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        String BASE_URL = getResources().getString(R.string.base_url);
        RequestBody requestBody = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(BASE_URL + "/mark-as-arrived")
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
    }

    private void cancelRide(boolean drinkerIsRequesting){

        String alertTitle = "Cancellation Request";
        String alertMessage = "Drinker is requesting to cancel.";

        if(!drinkerIsRequesting){
            alertTitle = "Cancel booking.";
            alertMessage = "Are you sure you want to cancel?";
        }

        AlertUtils.showConfirmDialog(PickupActivity.this, alertTitle, alertMessage, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                JsonObject json = new JsonObject();
                try {
                    json.addProperty("rideId", rideId);
                    json.addProperty("fcmToken", RideRequestActivity.drinkerFcmToken);
                    Log.d(TAG, "onClick: cancel booking confirmed -> fcmToken: "+RideRequestActivity.drinkerFcmToken);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                String BASE_URL = getResources().getString(R.string.base_url);
                RequestBody requestBody = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url(BASE_URL + "/send-ride-cancel")
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
                            finish();
                        } else {
                            System.out.println("Error: " + response.code());
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (markAsArrivedButtonThread != null && markAsArrivedButtonThread.isAlive()) {
            markAsArrivedButtonThread.interrupt();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(requestRideCancelReceiver);
        super.onDestroy();
    }
}