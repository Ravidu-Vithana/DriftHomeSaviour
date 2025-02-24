package com.ryvk.drifthomesaviour;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.ryvk.drifthomesaviour.databinding.ActivityBaseBinding;
import com.ryvk.drifthomesaviour.ui.settings.SettingsFragment;

public class BaseActivity extends AppCompatActivity implements SettingsFragment.LogoutListener {
    private static final String TAG = "BaseActivity";

    private ActivityBaseBinding binding;
    private Saviour loggedSaviour;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loggedSaviour = Saviour.getSPSaviour(BaseActivity.this);

        binding = ActivityBaseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_profile, R.id.navigation_settings)
                .build();
        NavController navController = ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_base)).getNavController();
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: Base Activity");
        loggedSaviour.setOnline(false);
        loggedSaviour.updateSPSaviour(BaseActivity.this,loggedSaviour);
        super.onDestroy();
    }

    @Override
    public void onLogout() {
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i);
        finish();
    }
}