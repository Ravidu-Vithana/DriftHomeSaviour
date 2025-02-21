package com.ryvk.drifthomesaviour.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.ryvk.drifthomesaviour.KycActivity;
import com.ryvk.drifthomesaviour.R;
import com.ryvk.drifthomesaviour.RideRequestActivity;
import com.ryvk.drifthomesaviour.Saviour;
import com.ryvk.drifthomesaviour.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {
    TextView kycTextView;
    Button kycButton;
    Switch onOfflineButton;
    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        kycTextView = binding.textView15;
        kycButton = binding.button9;
        onOfflineButton = binding.toggleButton;

        new Thread(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        }).start();

        return root;
    }

    private void updateUI(){

        Saviour loggedSaviour = Saviour.getSPSaviour(getContext());

        ((Activity) getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(loggedSaviour.getKyc() == Saviour.KYC_UNVERIYFIED){
                    kycTextView.setVisibility(View.VISIBLE);
                    kycButton.setVisibility(View.VISIBLE);
                    onOfflineButton.setVisibility(View.INVISIBLE);

                    kycButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent i = new Intent(getContext(), KycActivity.class);
                            startActivity(i);
                        }
                    });

                }else if(loggedSaviour.getKyc() == Saviour.KYC_PENDING){
                    kycTextView.setText(R.string.d_homeFragment_text4);
                    kycTextView.setVisibility(View.VISIBLE);
                    onOfflineButton.setVisibility(View.INVISIBLE);
                }else if(loggedSaviour.getKyc() == Saviour.KYC_VERIYFIED){
                    onOfflineButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}