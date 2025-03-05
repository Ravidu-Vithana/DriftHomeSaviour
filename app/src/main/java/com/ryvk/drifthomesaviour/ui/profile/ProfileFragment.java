package com.ryvk.drifthomesaviour.ui.profile;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.ryvk.drifthomesaviour.AlertUtils;
import com.ryvk.drifthomesaviour.R;
import com.ryvk.drifthomesaviour.Saviour;
import com.ryvk.drifthomesaviour.Utils;
import com.ryvk.drifthomesaviour.databinding.FragmentProfileBinding;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;
    private Uri profileImageUri;
    private ImageView profileImageView;
    private ActivityResultLauncher<Intent> cropImageLauncher;
    private static final int PICK_IMAGE_REQUEST = 10;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ProfileViewModel profileViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Saviour loggedSaviour = Saviour.getSPSaviour(getContext());

        profileImageView = binding.imageView9;
        ImageButton profileImagePickerButton = binding.imagePickerButton;
        EditText namefield = binding.editTextText8;
        EditText dobField = binding.editTextText9;
        EditText mobileField = binding.editTextText10;
        Spinner genderField = binding.spinner;
        EditText emailField = binding.editTextText12;
        EditText vehicleField = binding.editTextText;

        if (loggedSaviour.getProfile_pic() != null) Utils.loadImageUrlToView(getContext(),profileImageView,loggedSaviour.getProfile_pic());
        if (loggedSaviour.getName() != null) namefield.setText(loggedSaviour.getName());
        if (loggedSaviour.getDob() != null) dobField.setText(loggedSaviour.getDob());
        if (loggedSaviour.getMobile() != null) mobileField.setText(loggedSaviour.getMobile());
        if (loggedSaviour.getGender() != null){
            String[] genderArray = getResources().getStringArray(R.array.d_profileFragment_genderSpinner);
            int position = getGenderPosition(loggedSaviour.getGender(), genderArray);
            genderField.setSelection(position);
        }
        if (loggedSaviour.getEmail() != null) emailField.setText(loggedSaviour.getEmail());
        if (loggedSaviour.getVehicle() != null) vehicleField.setText(loggedSaviour.getVehicle());

        // Initialize crop image launcher
        cropImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        profileImageUri = UCrop.getOutput(result.getData());
                        if (profileImageUri != null) {
                            updateImageView(profileImageUri);
                            uploadToFirebaseStorage();
                        }
                    }
                });

        // Image picker button
        profileImagePickerButton.setOnClickListener(view -> {
            openGallery();
        });

        binding.button19.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.hideKeyboard((Activity) getContext());
                HashMap<String, Object> saviour = loggedSaviour.updateFields(
                        emailField.getText().toString().trim(),
                        namefield.getText().toString().trim(),
                        mobileField.getText().toString().trim(),
                        genderField.getSelectedItem().toString().trim(),
                        dobField.getText().toString().trim()
                );

                loggedSaviour.updateSPSaviour(getContext(), loggedSaviour);

                FirebaseFirestore db = FirebaseFirestore.getInstance();

                db.collection("saviour")
                        .document(loggedSaviour.getEmail())
                        .update(saviour)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.i(TAG, "update details: success");
                                AlertUtils.showAlert(getContext(),"Success!","Profile updated successfully!");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.i(TAG, "update details: failure");
                                AlertUtils.showAlert(getContext(),"Profile Update Failed!","Error: "+e);
                            }
                        });

            }
        });

        dobField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.hideKeyboard((Activity) getContext());
                showDatePickerDialog();
            }
        });

        return root;
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            Log.d(TAG, "onActivityResult: gallery image is selected: "+selectedImageUri);
            startCrop(selectedImageUri);
        } else {
            Log.d(TAG, "onActivityResult: some result received");
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void startCrop(Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(new File(requireContext().getCacheDir(), "cropped_image.jpg"));
        Intent cropIntent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .getIntent(requireContext());
        cropImageLauncher.launch(cropIntent);
    }

    private void updateImageView(Uri imageUri) {
        profileImageView.setImageBitmap(Utils.getRoundedImageBitmap(imageUri));
    }

    private void uploadToFirebaseStorage(){
        Saviour loggedSaviour = Saviour.getSPSaviour(getContext());

        if(profileImageUri != null){
            FirebaseStorage.getInstance().getReference().child("saviour_profile_pic").child(loggedSaviour.getEmail())
                    .putFile(profileImageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            taskSnapshot.getStorage().getDownloadUrl()
                                    .addOnSuccessListener(uri -> {
                                        String profileImageUrl = uri.toString();
                                        loggedSaviour.setProfile_pic(profileImageUrl);
                                        loggedSaviour.updateSPSaviour(getContext(), loggedSaviour);
                                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                                        db.collection("saviour")
                                                .document(loggedSaviour.getEmail())
                                                .update("profile_pic", profileImageUrl)
                                                .addOnSuccessListener(unused -> Toast.makeText(getContext(),"Profile Image Updated!",Toast.LENGTH_LONG).show())
                                                .addOnFailureListener(e -> AlertUtils.showAlert(getContext(), "Profile with image Update Failed!", "Error: " + e));
                                    })
                                    .addOnFailureListener(e -> Log.e("Storage", "Failed to get download URL", e))
                    )
                    .addOnFailureListener(e -> Log.e("Storage", "Image upload failed", e));
        }
    }
    private void showDatePickerDialog() {
        // Get current date
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                        // Format date as DD/MM/YYYY
                        String selectedDate = String.format("%02d/%02d/%04d", selectedDay, (selectedMonth + 1), selectedYear);
                        binding.editTextText9.setText(selectedDate);
                    }
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    private int getGenderPosition(String gender, String[] genderArray) {
        for (int i = 0; i < genderArray.length; i++) {
            if (genderArray[i].equalsIgnoreCase(gender)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}