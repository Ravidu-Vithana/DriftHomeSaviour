package com.ryvk.drifthomesaviour;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PickupActivity extends AppCompatActivity {

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

        ImageButton callButton = findViewById(R.id.imageButton);
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCallOptionsPopup(view);
            }
        });

        ImageButton navigationButton = findViewById(R.id.imageButton2);
        navigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Starting navigation...", Toast.LENGTH_SHORT).show();
            }
        });

        Button reportButton = findViewById(R.id.button12);
        reportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Reporting incident", Toast.LENGTH_SHORT).show();
            }
        });

        Button markAsArrivedButton = findViewById(R.id.button13);
        Button iArrivedButton = findViewById(R.id.button15);
        Button startTripButton = findViewById(R.id.button16);
        markAsArrivedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        markAsArrivedButton.setVisibility(View.INVISIBLE);
                        iArrivedButton.setVisibility(View.VISIBLE);
                        startTripButton.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        Button cancelButton = findViewById(R.id.button14);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), BaseActivity.class);
                startActivity(i);
            }
        });

        startTripButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), TripActivity.class);
                startActivity(i);
            }
        });

    }

    private void showCallOptionsPopup(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("Call the Drinker");
        popup.getMenu().add("Call 1990");
        popup.getMenu().add("Call 119");

        popup.setOnMenuItemClickListener(item -> {
            Toast.makeText(this, "Clicked: " + item.getTitle(), Toast.LENGTH_SHORT).show();
            return true;
        });

        popup.show();
    }
}