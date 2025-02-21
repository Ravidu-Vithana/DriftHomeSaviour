package com.ryvk.drifthomesaviour;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    private static final int RC_EPSIGNUP = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton googleSigninButton = findViewById(R.id.imageButton3);
        googleSigninButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent i = new Intent(SignUpActivity.this,GoogleAuthentication.class);
                        startActivityForResult(i,RC_EPSIGNUP);
                    }
                });
            }
        });

        Button signInButton = findViewById(R.id.button4);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                EditText nameEditText = findViewById(R.id.editTextText3);
                EditText emailEditText = findViewById(R.id.editTextText4);
                EditText mobileEditText = findViewById(R.id.editTextText5);
                EditText passwordEditText = findViewById(R.id.editTextText13);

                String name = nameEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();
                String mobile = mobileEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent i = new Intent(SignUpActivity.this, EmailPasswordAuthentication.class);
                        i.putExtra("name",name);
                        i.putExtra("email",email);
                        i.putExtra("mobile",mobile);
                        i.putExtra("password",password);
                        i.putExtra("state",EmailPasswordAuthentication.SIGNUPSTATE);
                        startActivityForResult(i,RC_EPSIGNUP);
                    }
                });
            }
        });

        Button goToSignInButton = findViewById(R.id.button6);
        goToSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent i = new Intent(SignUpActivity.this,MainActivity.class);
                        startActivity(i);
                    }
                });
            }
        });

    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_EPSIGNUP){
            if (resultCode == RESULT_CANCELED && data != null) {
                String errorMessage = data.getStringExtra("AUTH_ERROR");
                if (errorMessage != null) {
                    runOnUiThread(()->AlertUtils.showAlert(this,"Account Creation Failed!",errorMessage));
                }
            }
            if(resultCode == RESULT_OK){
                checkCurrentUser();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(InternetChecker.checkInternet(SignUpActivity.this)){
            checkCurrentUser();
        }
    }

    public void checkCurrentUser() {
        // [START check_current_user]
        FirebaseUser user = MainActivity.getFirebaseUser();
        if (user != null) {
            //already signed in, ready to intent Home

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("saviour")
                    .document(user.getEmail())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                Saviour saviour = documentSnapshot.toObject(Saviour.class);

                                //update shared preferences
                                saviour.updateSPSaviour(SignUpActivity.this,saviour);

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent i = new Intent(SignUpActivity.this, BaseActivity.class);
                                        startActivity(i);
                                    }
                                });

                            } else {
                                runOnUiThread(()->AlertUtils.showAlert(getApplicationContext(),"Login Error","Data retrieval failed! Please restart the application."));
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i(TAG, "Error fetching saviour: " + e);
                            runOnUiThread(()->AlertUtils.showAlert(getApplicationContext(),"Login Error","Data retrieval failed! Please restart the application."));
                        }
                    });

        }
    }

}