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

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int RC_EPSIGNIN = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button signInButton = findViewById(R.id.button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.hideKeyboard(MainActivity.this);
                EditText emailEditText = findViewById(R.id.editTextText2);
                EditText passwordEditText = findViewById(R.id.editTextText14);

                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent i = new Intent(MainActivity.this, EmailPasswordAuthentication.class);
                        i.putExtra("email",email);
                        i.putExtra("password",password);
                        i.putExtra("state",EmailPasswordAuthentication.SIGNINSTATE);
                        startActivityForResult(i,RC_EPSIGNIN);
                    }
                });
            }
        });

        Button goToSignUpButton = findViewById(R.id.button5);
        goToSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.hideKeyboard(MainActivity.this);
                        Intent i = new Intent(MainActivity.this, SignUpActivity.class);
                        startActivity(i);
                        finish();
                    }
                });
            }
        });

        ImageButton googleSigninButton = findViewById(R.id.imageButton3);
        googleSigninButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.hideKeyboard(MainActivity.this);
                        Intent i = new Intent(MainActivity.this,GoogleAuthentication.class);
                        startActivity(i);
                        finish();
                    }
                });
            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_EPSIGNIN){
            if (resultCode == RESULT_CANCELED && data != null) {
                String errorMessage = data.getStringExtra("AUTH_ERROR");
                if (errorMessage != null) {
                    runOnUiThread(()->AlertUtils.showAlert(this,"Login Error",errorMessage));
                }
            }
            if(resultCode == RESULT_OK){
                checkCurrentUser();
            }
        }
    }

    public void checkCurrentUser() {
        // [START check_current_user]
        FirebaseUser user = getFirebaseUser();
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

                                if(saviour.isBlocked()){
                                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                            .requestIdToken(getString(R.string.web_server_client_id))
                                            .requestEmail()
                                            .build();

                                    GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(MainActivity.this, gso);
                                    FirebaseAuth mAuth = FirebaseAuth.getInstance();
                                    mAuth.signOut();
                                    mGoogleSignInClient.signOut();
                                    Log.i(TAG, "onClick: Logout, user logged out------------------------");

                                    saviour.removeSPSaviour(MainActivity.this);

                                    AlertUtils.showAlert(MainActivity.this,"Blocked!","You have been blocked by the admin!");
                                }else {
                                    FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            String token = task.getResult();
                                            db.collection("saviour").document(user.getEmail())
                                                    .update("fcmToken", token)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void unused) {
                                                            SplashActivity.fcmToken = token;
                                                            Log.d(TAG, "onSuccess: fcm token updated");
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.d(TAG, "onFailure: fcm token update failed");
                                                        }
                                                    });
                                        }
                                    });

                                    saviour.updateSPSaviour(MainActivity.this,saviour);

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Intent i = new Intent(MainActivity.this, BaseActivity.class);
                                            startActivity(i);
                                            finish();
                                        }
                                    });
                                }


                            } else {
                                runOnUiThread(() -> AlertUtils.showAlert(getApplicationContext(),"Login Error","Data retrieval failed! Please restart the application."));
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i(TAG, "Error fetching saviour: " + e);
                            runOnUiThread(() -> AlertUtils.showAlert(getApplicationContext(),"Login Error","Data retrieval failed! Please restart the application."));
                        }
                    });

        }
    }

    public static FirebaseUser getFirebaseUser (){
        return FirebaseAuth.getInstance().getCurrentUser();
    }
}