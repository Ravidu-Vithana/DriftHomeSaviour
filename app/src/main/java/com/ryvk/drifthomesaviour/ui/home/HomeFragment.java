package com.ryvk.drifthomesaviour.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.ryvk.drifthomesaviour.KycActivity;
import com.ryvk.drifthomesaviour.RideRequestActivity;
import com.ryvk.drifthomesaviour.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    Toast.makeText(getContext(), "Checked!", Toast.LENGTH_SHORT).show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            Intent i = new Intent(getContext(), RideRequestActivity.class);
                            startActivity(i);
                        }
                    }).start();

                }else{
                    Toast.makeText(getContext(), "Unchecked!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.button9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getContext(), KycActivity.class);
                startActivity(i);
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}