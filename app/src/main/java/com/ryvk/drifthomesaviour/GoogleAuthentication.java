package com.ryvk.drifthomesaviour;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

import okhttp3.OkHttpClient;

public class GoogleAuthentication extends AppCompatActivity {

    private static final String TAG = "GoogleAuthentication";
    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;

    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_google_authentication);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if(InternetChecker.checkInternet(GoogleAuthentication.this)){
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.web_server_client_id))
                    .requestEmail()
                    .build();

            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            mAuth = FirebaseAuth.getInstance();
            signIn();
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                Log.d(TAG, "firebaseAuthWithGoogle Token----------:" + account.getIdToken());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
            }
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
            saviour.put("name", loggedUser.getDisplayName());
            saviour.put("email", loggedUser.getEmail());
            saviour.put("mobile", loggedUser.getPhoneNumber());
            saviour.put("token", 0);
            saviour.put("trip_count", 0);
            saviour.put("created_at", Validation.todayDateTime());
            saviour.put("updated_at", Validation.todayDateTime());

            db.collection("saviour")
                    .document(loggedUser.getEmail())
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

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            checkUserInDB(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            checkUserInDB(null);
                        }
                    }
                });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

}