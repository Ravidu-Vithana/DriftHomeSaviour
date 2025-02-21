package com.ryvk.drifthomesaviour;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class EmailPasswordAuthentication extends AppCompatActivity {

    private static final String TAG = "EmailPasswordAuthentication";
    private FirebaseAuth mAuth;

    private String name;
    private String email;
    private String mobile;
    private String password;
    int state;

    public static final int SIGNUPSTATE = 1;
    public static final int SIGNINSTATE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_email_password_authentication);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if(InternetChecker.checkInternet(EmailPasswordAuthentication.this)){
            mAuth = FirebaseAuth.getInstance();
            Intent i = getIntent();

            email = i.getStringExtra("email");
            password = i.getStringExtra("password");
            state = i.getIntExtra("state",SIGNINSTATE);

            if(state == SIGNUPSTATE){
                name = i.getStringExtra("name");
                mobile = i.getStringExtra("mobile");

                createAccount(email,password);
            }else{
                signIn(email,password);
            }
        }
    }

    private void createAccount(String email, String password) {
        // [START create_user_with_email]

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            checkUserInDB(user);
                        } else {
                            Exception exception = task.getException();
                            String errorMessage;

                            if (exception instanceof FirebaseAuthUserCollisionException) {
                                errorMessage = "The email address is already in use by another account.";
                            } else if (exception instanceof FirebaseAuthInvalidUserException) {
                                errorMessage = "No account found with this email.";
                            } else {
                                errorMessage = "Authentication failed. Please try again later.";
                            }

                            Log.w(TAG, "createAccountWithEmail: failure", exception);

                            Intent intent = new Intent();
                            intent.putExtra("AUTH_ERROR", errorMessage);
                            setResult(RESULT_CANCELED, intent);
                            finish();
                        }
                    }
                });

        // [END create_user_with_email]
    }

    private void signIn(String email, String password) {
        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Intent intent = new Intent();
                            setResult(RESULT_OK, intent);
                            finish();
                        } else {
                            Exception exception = task.getException();
                            String errorMessage;

                            if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                errorMessage = "Invalid email or password. Please try again.";
                            } else if (exception instanceof FirebaseAuthInvalidUserException) {
                                errorMessage = "No account found with this email.";
                            } else {
                                errorMessage = "Authentication failed. Please try again later.";
                            }

                            Log.w(TAG, "signInWithEmail: failure", exception);

                            // Send error message back to MainActivity
                            Intent intent = new Intent();
                            intent.putExtra("AUTH_ERROR", errorMessage);
                            setResult(RESULT_CANCELED, intent);
                            finish();

                        }
                    }
                });
        // [END sign_in_with_email]
    }

    private void sendEmailVerification() {
        // Send verification email
        final FirebaseUser user = mAuth.getCurrentUser();
        if(user != null){
            user.sendEmailVerification()
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            // Email sent
                        }
                    });
        }
    }

    private void checkUserInDB (FirebaseUser loggedUser){
        if(loggedUser != null){
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("saviour")
                    .document(loggedUser.getEmail())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                Log.i(TAG, "Saviour Data: " + documentSnapshot.getData());
                                Saviour saviour = documentSnapshot.toObject(Saviour.class);
                                Log.i(TAG, "Saviour Data Object: " + saviour.getEmail() + saviour.getName());

                                Intent intent = new Intent();
                                setResult(RESULT_OK, intent);
                                finish();
                            } else {
                                Log.i(TAG, "No saviour found.");
                                storeDetails(loggedUser);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i(TAG, "Error fetching saviour: " + e);
                            Intent intent = new Intent();
                            intent.putExtra("AUTH_ERROR", "Error fetching saviour!");
                            setResult(RESULT_CANCELED, intent);
                            finish();
                        }
                    });
        }else{
            Intent intent = new Intent();
            intent.putExtra("AUTH_ERROR", "Error fetching saviour!");
            setResult(RESULT_CANCELED, intent);
            finish();
        }
    }

    private void storeDetails(FirebaseUser loggedUser) {
        if(loggedUser != null){
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            HashMap<String, Object> saviour = new HashMap<>();
            saviour.put("name", name);
            saviour.put("email", email);
            saviour.put("mobile", mobile);
            saviour.put("token", 0);
            saviour.put("trip_count", 0);
            saviour.put("created_at", Validation.todayDateTime());
            saviour.put("updated_at", Validation.todayDateTime());

            db.collection("saviour")
                    .document(email)
                    .set(saviour)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.i(TAG, "store details: success");

                            Intent intent = new Intent();
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.i(TAG, "store details: failure");
                            Intent intent = new Intent();
                            intent.putExtra("AUTH_ERROR", "Data update failed!");
                            setResult(RESULT_CANCELED, intent);
                            finish();
                        }
                    });


        }
    }
}