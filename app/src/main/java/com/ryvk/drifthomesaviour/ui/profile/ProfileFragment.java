package com.ryvk.drifthomesaviour.ui.profile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ryvk.drifthomesaviour.AlertUtils;
import com.ryvk.drifthomesaviour.R;
import com.ryvk.drifthomesaviour.Saviour;
import com.ryvk.drifthomesaviour.databinding.FragmentProfileBinding;

import java.util.Calendar;
import java.util.HashMap;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ProfileViewModel profileViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Saviour loggedSaviour = Saviour.getSPSaviour(getContext());

        EditText namefield = binding.editTextText8;
        EditText dobField = binding.editTextText9;
        EditText mobileField = binding.editTextText10;
        Spinner genderField = binding.spinner;
        EditText emailField = binding.editTextText12;
        EditText vehicleField = binding.editTextText;

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

        binding.button19.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                showDatePickerDialog();
            }
        });

        return root;
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