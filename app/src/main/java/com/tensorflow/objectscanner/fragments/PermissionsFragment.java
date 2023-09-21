package com.tensorflow.objectscanner.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleCoroutineScope;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.tensorflow.objectscanner.R;


public class PermissionsFragment extends Fragment {

    NavController navController;

    private static final String[] PERMISSIONS_REQUIRED = {Manifest.permission.CAMERA};
    private LifecycleCoroutineScope lifecycleScope;

    public static boolean hasPermissions(Context context) {
        for (String permission : PERMISSIONS_REQUIRED) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            navigateToCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }


    private ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            Toast.makeText(getContext(), "Permission request granted", Toast.LENGTH_LONG).show();
            navigateToCamera();
        } else {
            Toast.makeText(getContext(), "Permission request denied", Toast.LENGTH_LONG).show();
        }
    });


    public void navigateToCamera() {

        getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            public void onStartEvent() {
                NavController navController;
                navController = Navigation.findNavController(requireActivity(), R.id.fragment_container);
                navController.navigate(R.id.action_permissions_to_camera);
            }
        });



    }
}







