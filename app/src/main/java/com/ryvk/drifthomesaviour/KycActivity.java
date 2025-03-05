package com.ryvk.drifthomesaviour;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.Manifest;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KycActivity extends AppCompatActivity {
    private static final String TAG = "KycActivity";
    private static final int REQUEST_CODE_PICK_IMAGE = 100;
    private static final int REQUEST_CODE_CAPTURE_IMAGE = 200;

    private static final int PERMISSION_REQUEST_CODE = 300;

    private Map<String, Uri> imageUris = new HashMap<>();
    private Map<String, Object> allImageData = new HashMap<>();
    private String currentImageKey;
    private static final String nicFront = "nicFront";
    private static final String nicBack = "nicBack";
    private static final String dlFront = "dlFront";
    private static final String dlBack = "dlBack";
    private static final String vehicleRegistration = "vehicleRegistration";
    private static final String policeClearance = "policeClearance";
    private boolean uploadStarted;
    private OnBackPressedCallback callback;
    private Button uploadImagesButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_kyc);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(uploadStarted){
                    AlertUtils.showAlert(KycActivity.this,"Uploading files..","Please wait until the upload is finished");
                }else{
                    finish();
                }
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);

        Button uploadNicFrontButton = findViewById(R.id.button22);
        Button uploadNicBackButton = findViewById(R.id.button24);
        Button uploadDlFrontButton = findViewById(R.id.button25);
        Button uploadDlBackButton = findViewById(R.id.button21);
        Button uploadVrButton = findViewById(R.id.button26);
        Button uploadPccButton = findViewById(R.id.button27);
        CheckBox checkBox = findViewById(R.id.checkBox);
        uploadImagesButton = findViewById(R.id.button28);
        progressBar = findViewById(R.id.progressBar);

        uploadNicFrontButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage(nicFront);
            }
        });

        uploadNicBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage(nicBack);
            }
        });

        uploadDlFrontButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage(dlFront);
            }
        });

        uploadDlBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage(dlBack);
            }
        });

        uploadVrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage(vehicleRegistration);
            }
        });

        uploadPccButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage(policeClearance);
            }
        });

        uploadImagesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkBox.isChecked()){
                    Log.i(TAG, "onClick: "+imageUris);
                    uploadImageFiles();
                }else{
                    AlertUtils.showAlert(KycActivity.this,"Error","Please check the box to confirm.");
                }
            }
        });

    }
    private void selectImage(String imageKey) {
        currentImageKey = imageKey;

        CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select an option");

        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Take Photo")) {
                openCamera();
            } else if (options[item].equals("Choose from Gallery")) {
                openGallery();
            } else {
                dialog.dismiss();
            }
        });
        runOnUiThread(builder::show);
    }

    private void openCamera() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, REQUEST_CODE_CAPTURE_IMAGE);
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && currentImageKey != null) {
            Uri imageUri = null;

            if (requestCode == REQUEST_CODE_PICK_IMAGE && data != null) {
                imageUri = data.getData(); // Image from gallery
            } else if (requestCode == REQUEST_CODE_CAPTURE_IMAGE && data != null && data.getExtras() != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                imageUri = getImageUriFromBitmap(photo); // Convert bitmap to Uri
            }

            if (imageUri != null) {
                imageUris.put(currentImageKey, imageUri);
                updateUI(currentImageKey);
                Log.d(TAG, "onActivityResult: image key: "+currentImageKey+" image uri: "+imageUri);
            }
        }
    }
    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "CapturedImage", null);
        return Uri.parse(path);
    }

    private void updateUI(String imageKey){
        Button updatingButton;
        if(imageKey.equals(nicFront)){
            updatingButton = findViewById(R.id.button22);
            updatingButton.setText(R.string.d_kyc_btn1_retake);
        }else if(imageKey.equals(nicBack)){
            updatingButton = findViewById(R.id.button24);
            updatingButton.setText(R.string.d_kyc_btn2_retake);
        }else if(imageKey.equals(dlFront)){
            updatingButton = findViewById(R.id.button25);
            updatingButton.setText(R.string.d_kyc_btn3_retake);
        }else if(imageKey.equals(dlBack)){
            updatingButton = findViewById(R.id.button21);
            updatingButton.setText(R.string.d_kyc_btn4_retake);
        }else if(imageKey.equals(vehicleRegistration)){
            updatingButton = findViewById(R.id.button26);
            updatingButton.setText(R.string.d_kyc_btn5_retake);
        } else if (imageKey.equals(policeClearance)) {
            updatingButton = findViewById(R.id.button27);
            updatingButton.setText(R.string.d_kyc_btn6_retake);
        }
    }

    private void uploadImageFiles() {

        Log.d(TAG, "uploadImageFiles: upload started");

        Saviour loggedSaviour = Saviour.getSPSaviour(KycActivity.this);

        FirebaseStorage storage = FirebaseStorage.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid(); // Current user ID

        if(imageUris.get(nicFront) != null && imageUris.get(nicBack) != null && imageUris.get(dlFront) != null && imageUris.get(dlBack) != null && imageUris.get(vehicleRegistration) != null && imageUris.get(policeClearance) != null){
            uploadStarted = true;
            progressBar.setVisibility(View.VISIBLE);
            uploadImagesButton.setEnabled(false);

            List<Uri> imageUrisArray = Arrays.asList(imageUris.get(nicFront), imageUris.get(nicBack), imageUris.get(dlFront), imageUris.get(dlBack), imageUris.get(vehicleRegistration), imageUris.get(policeClearance));

            for (int i = 0; i < imageUrisArray.size(); i++) {
                Uri fileUri = imageUrisArray.get(i);
                if (fileUri != null) {
                    String fileName = "image_" + (i+1);
                    StorageReference storageRef = storage.getReference().child("saviour_kyc").child(loggedSaviour.getEmail()).child(fileName);

                    try {
                        InputStream stream = getContentResolver().openInputStream(fileUri);
                        if (stream != null) {
                            UploadTask uploadTask = storageRef.putStream(stream);
                            int finalI = i;
                            uploadTask.addOnSuccessListener(taskSnapshot ->
                                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {

                                        Map<String, Object> imageData = new HashMap<>();
                                        imageData.put("downloadUrl", uri.toString());
                                        imageData.put("timestamp", Validation.todayDateTime());
                                        allImageData.put(fileName,imageData);

                                        if(finalI == imageUrisArray.size() - 1){
                                            saveImageMetadata(loggedSaviour.getEmail());
                                        }

                                    })
                            ).addOnFailureListener(e -> {
                                Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "File error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }else{
            AlertUtils.showAlert(KycActivity.this,"Error","Please upload all the documents");
        }
    }

    private void saveImageMetadata(String email) {
        Log.d(TAG, "saveImageMetadata: save to firestore started");
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if(allImageData.size() == 6){
            db.collection("kyc")
                    .document(email)
                    .set(allImageData)
                    .addOnSuccessListener(documentReference -> {
                        Log.d("Firestore", "Image metadata saved");
                        AlertUtils.showAlert(KycActivity.this,"Success","Images uploaded successfully. Please await verification.");
                        uploadStarted = false;
                        progressBar.setVisibility(View.INVISIBLE);
                        uploadImagesButton.setEnabled(true);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error saving image metadata", e);
                        uploadStarted = false;
                        progressBar.setVisibility(View.INVISIBLE);
                        uploadImagesButton.setEnabled(true);
                    });

            db.collection("saviour")
                    .document(email)
                    .update("kyc",Saviour.KYC_PENDING)
                    .addOnSuccessListener(unused -> Toast.makeText(KycActivity.this,"Kyc status Updated!",Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e -> {
                        AlertUtils.showAlert(KycActivity.this, "Kyc status Update Failed!", "Error: " + e);
                        uploadStarted = false;
                        progressBar.setVisibility(View.INVISIBLE);
                        uploadImagesButton.setEnabled(true);
                    });
        }else{
            Log.d(TAG, "size: "+allImageData.size());
            uploadStarted = false;
            progressBar.setVisibility(View.INVISIBLE);
        }
    }
}