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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class KycActivity extends AppCompatActivity {
    private static final String TAG = "KycActivity";
    private static final int REQUEST_CODE_PICK_IMAGE = 100;
    private static final int REQUEST_CODE_CAPTURE_IMAGE = 200;

    private static final int PERMISSION_REQUEST_CODE = 300;

    private Map<String, Uri> imageUris = new HashMap<>();
    private String currentImageKey;

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

        Button uploadNicFrontButton = findViewById(R.id.button22);
        Button uploadNicBackButton = findViewById(R.id.button24);
        Button uploadDlFrontButton = findViewById(R.id.button25);
        Button uploadDlBackButton = findViewById(R.id.button21);
        Button uploadVrButton = findViewById(R.id.button26);
        Button uploadPccButton = findViewById(R.id.button27);
        CheckBox checkBox = findViewById(R.id.checkBox);
        Button uploadImagesButton = findViewById(R.id.button28);

        uploadNicFrontButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage("nicFront");
            }
        });

        uploadNicBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage("nicBack");
            }
        });

        uploadDlFrontButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage("dlFront");
            }
        });

        uploadDlBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage("dlBack");
            }
        });

        uploadVrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage("vehicleRegistration");
            }
        });

        uploadPccButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage("policeClearance");
            }
        });

        uploadImagesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkBox.isChecked()){
                    Log.i(TAG, "onClick: "+imageUris);
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
        }
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
                imageUris.put(currentImageKey, imageUri); // Store the image URI
                // Optionally, update the UI (e.g., show the image in an ImageView)
            }
        }
    }
    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "CapturedImage", null);
        return Uri.parse(path);
    }
    private void uploadImagesToFirebase() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("uploads");

        for (Map.Entry<String, Uri> entry : imageUris.entrySet()) {
            Uri fileUri = entry.getValue();
            String fileName = entry.getKey() + "_" + System.currentTimeMillis() + ".jpg";
            StorageReference fileRef = storageRef.child(fileName);

            fileRef.putFile(fileUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            Log.d("Firebase", "Uploaded: " + uri.toString());
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firebase", "Upload failed: " + e.getMessage());
                    });
        }
    }
}