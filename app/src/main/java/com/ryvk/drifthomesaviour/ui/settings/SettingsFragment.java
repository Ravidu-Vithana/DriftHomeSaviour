package com.ryvk.drifthomesaviour.ui.settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ryvk.drifthomesaviour.AlertUtils;
import com.ryvk.drifthomesaviour.MainActivity;
import com.ryvk.drifthomesaviour.R;
import com.ryvk.drifthomesaviour.Saviour;
import com.ryvk.drifthomesaviour.WithdrawMoneyActivity;
import com.ryvk.drifthomesaviour.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";
    private FragmentSettingsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SettingsViewModel settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Saviour loggedSaviour = Saviour.getSPSaviour(getContext());

        binding.button14.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getContext(), WithdrawMoneyActivity.class);
                startActivity(i);
            }
        });

        binding.button15.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertUtils.showConfirmDialog(getContext(), "Confirm Action", "Are you sure you want to logout?",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                FirebaseUser loggedUser = MainActivity.getFirebaseUser();
                                if(loggedUser != null){

                                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                            .requestIdToken(getString(R.string.web_server_client_id))
                                            .requestEmail()
                                            .build();

                                    loggedSaviour.removeSPSaviour(getContext());

                                    GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getContext(), gso);
                                    FirebaseAuth mAuth = FirebaseAuth.getInstance();
                                    mAuth.signOut();
                                    mGoogleSignInClient.signOut();

                                    Log.i(TAG, "onClick: Logout, user logged out------------------------");
                                }else{
                                    Log.i(TAG, "onClick: Logout . NO user ----------------------------------");
                                }
                                Intent i = new Intent(getContext(), MainActivity.class);
                                startActivity(i);
                            }
                        }
                );
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