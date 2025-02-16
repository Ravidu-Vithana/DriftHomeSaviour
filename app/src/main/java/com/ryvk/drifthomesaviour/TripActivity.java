package com.ryvk.drifthomesaviour;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TripActivity extends AppCompatActivity {

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

        Button reportButton = findViewById(R.id.button12);
        reportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.reportTrip(TripActivity.this);
            }
        });

        ImageButton navigationButton = findViewById(R.id.imageButton2);
        navigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Starting navigation...", Toast.LENGTH_SHORT).show();
            }
        });

        Button endButton = findViewById(R.id.button13);
        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView title = findViewById(R.id.textView17);
                        CardView fareCard = findViewById(R.id.cardView);
                        TextView feedbackSectionTitle = findViewById(R.id.textView21);
                        RatingBar feedbackRating = findViewById(R.id.ratingBar);
                        EditText feedbackDescription = findViewById(R.id.editTextTextMultiLine);
                        Button feedbackSubmitButton = findViewById(R.id.button17);
                        Button backToHomeButton = findViewById(R.id.button18);

                        title.setText(R.string.d_trip_text1_tripEnd);
                        endButton.setVisibility(View.INVISIBLE);
                        fareCard.setVisibility(View.VISIBLE);
                        feedbackSectionTitle.setVisibility(View.VISIBLE);
                        feedbackRating.setVisibility(View.VISIBLE);
                        feedbackDescription.setVisibility(View.VISIBLE);
                        feedbackSubmitButton.setVisibility(View.VISIBLE);
                        backToHomeButton.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }
}