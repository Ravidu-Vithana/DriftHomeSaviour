package com.ryvk.drifthomesaviour.ui.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.ryvk.drifthomesaviour.AlertUtils;
import com.ryvk.drifthomesaviour.KycActivity;
import com.ryvk.drifthomesaviour.MainActivity;
import com.ryvk.drifthomesaviour.R;
import com.ryvk.drifthomesaviour.RideRequestActivity;
import com.ryvk.drifthomesaviour.Saviour;
import com.ryvk.drifthomesaviour.SplashActivity;
import com.ryvk.drifthomesaviour.databinding.FragmentHomeBinding;
import com.ryvk.drifthomesaviour.ui.settings.SettingsFragment;

import java.util.Calendar;
import java.util.HashMap;

public class HomeFragment extends Fragment  implements OnMapReadyCallback {
    private static final String TAG = "HomeFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    TextView kycTextView;
    Button kycButton;
    Button tokenButton;
    Switch onOfflineButton;
    TextView greetingTextView;
    TextView nameTextView;
    private FragmentHomeBinding binding;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Saviour loggedSaviour;
    private SettingsFragment.LogoutListener logoutListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Load Google Map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        kycTextView = binding.textView15;
        greetingTextView = binding.textView13;
        nameTextView = binding.textView14;
        kycButton = binding.button9;
        tokenButton = binding.button8;
        onOfflineButton = binding.toggleButton;

        return root;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setAllGesturesEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.night_map_style));

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    private String getGreetingMessage() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) {
            return "Good Morning";
        } else if (hour >= 12 && hour < 18) {
            return "Good Afternoon";
        } else {
            return "Good Evening";
        }
    }
    private void enableUserLocation() {
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null &&
                (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                        !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
            // Prompt user to enable location services
            Toast.makeText(getActivity(), "Location services are disabled", Toast.LENGTH_LONG).show();
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);

        // default the location to Sri Lanka and move the camera
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(7.8731, 80.7718), 7));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    private void refreshData(){

        loggedSaviour = Saviour.getSPSaviour(getContext());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("saviour")
                .document(loggedSaviour.getEmail())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        loggedSaviour = documentSnapshot.toObject(Saviour.class);
                        loggedSaviour.updateSPSaviour(getContext(),loggedSaviour);

                        if(loggedSaviour.isBlocked()){
                            logoutUser();
                        }else{
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI();
                                }
                            }).start();
                        }

                    } else {
                        logoutUser();
                    }
                })
                .addOnFailureListener(e -> logoutUser());
    }

    private void logoutUser(){
        FirebaseUser loggedUser = MainActivity.getFirebaseUser();
        if(loggedUser != null){

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.web_server_client_id))
                    .requestEmail()
                    .build();
            loggedSaviour.setOnline(false);
            loggedSaviour.removeSPSaviour(getContext());

            GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getContext(), gso);
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            mAuth.signOut();
            mGoogleSignInClient.signOut();

            Log.i(TAG, "onClick: Logout, user logged out------------------------");
        }else{
            Log.i(TAG, "onClick: Logout . NO user ----------------------------------");
        }
        if (logoutListener != null) {
            logoutListener.onLogout();
        }
    }

    private void updateUI(){

        ((Activity) getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String firstName = loggedSaviour.getName().split(" ")[0];
                greetingTextView.setText(getGreetingMessage());
                nameTextView.setText(firstName);
                tokenButton.setText(String.valueOf(loggedSaviour.getTokens()));

                if(loggedSaviour.getKyc() == Saviour.KYC_UNVERIYFIED){
                    onOfflineButton.setVisibility(View.INVISIBLE);
                    kycTextView.setVisibility(View.VISIBLE);
                    kycButton.setVisibility(View.VISIBLE);

                    kycButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent i = new Intent(getContext(), KycActivity.class);
                            startActivity(i);
                        }
                    });

                }else if(loggedSaviour.getKyc() == Saviour.KYC_PENDING){
                    onOfflineButton.setVisibility(View.INVISIBLE);
                    kycTextView.setText(R.string.d_homeFragment_text4);
                    kycTextView.setVisibility(View.VISIBLE);
                }else if(loggedSaviour.getKyc() == Saviour.KYC_DECLINED){
                    onOfflineButton.setVisibility(View.INVISIBLE);
                    kycTextView.setText(R.string.d_homeFragment_text5);
                    kycTextView.setVisibility(View.VISIBLE);
                    kycButton.setText(R.string.d_homeFragment_btn2);
                    kycButton.setVisibility(View.VISIBLE);

                    kycButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent i = new Intent(getContext(), KycActivity.class);
                            startActivity(i);
                        }
                    });

                }else if(loggedSaviour.getKyc() == Saviour.KYC_VERIYFIED){
                    onOfflineButton.setChecked(loggedSaviour.isOnline());
                    onOfflineButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                            HashMap<String, Object> saviour;

                            if(isChecked){
                                loggedSaviour.setOnline(true);
                            }else{
                                loggedSaviour.setOnline(false);
                            }
                            loggedSaviour.updateSPSaviour(requireContext(),loggedSaviour);

                        }
                    });
                }
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof SettingsFragment.LogoutListener) {
            logoutListener = (SettingsFragment.LogoutListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement LogoutListener");
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}