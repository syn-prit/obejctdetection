package com.tensorflow.objectscanner.fragments;

import static androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888;



import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.common.util.concurrent.ListenableFuture;
import com.tensorflow.objectscanner.R;
import com.tensorflow.objectscanner.databinding.FragmentCameraBinding;
import com.tensorflow.objectscanner.helperclasses.ObjectDetectorHelper;

import org.tensorflow.lite.task.gms.vision.detector.Detection;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CameraFragment extends Fragment implements ObjectDetectorHelper.DetectorListener {


    private static final String TAG = "CameraFragment";

    public static FragmentCameraBinding fragmentCameraBinding;

    public static FragmentCameraBinding fragmentCameraBinding() {
        return fragmentCameraBinding;
    }

    public ObjectDetectorHelper objectDetectorHelper;
    public static Bitmap bitmapBuffer = null;
    ImageProxy.PlaneProxy[] planes;
    public static Preview preview = null;
    public static ImageAnalysis imageAnalyzer = null;
    public static Camera camera = null;
    public static  ProcessCameraProvider cameraProvider;
    //Blocking camera operations are performed using this executor
    public static ExecutorService cameraExecutor;
    ByteBuffer buffer ;

    @Override
    public void onResume() {
        super.onResume();
        Log.e("CameraFragment", "CameraFragment--->onResume");
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            getLifecycle().addObserver(new LifecycleObserver() {
                @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
                public void onStartEvent() {
                    NavController navController;
                    navController = Navigation.findNavController(requireActivity(), R.id.fragment_container);
                    navController.navigate(R.id.action_camera_to_permissions);
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        Log.e("CameraFragment", "CameraFragment--->onDestroyView");
        super.onDestroyView();
        fragmentCameraBinding = null;
        // Shut down our background executor
        cameraExecutor.shutdown();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.e("CameraFragment", "CameraFragment--->onCreateView");
        fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false);
        return fragmentCameraBinding.getRoot();

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //objectDetectorHelper = new ObjectDetectorHelper(getContext(), getActivity());
//        objectDetectorHelper = new ObjectDetectorHelper();
        Log.e("CameraFragment", "CameraFragment--->onViewCreated");
        objectDetectorHelper = new ObjectDetectorHelper(requireContext(), this);


        // Attach listeners to UI control widgets
        initBottomSheetControls();

    }

    private void initBottomSheetControls() {
        // When clicked, lower detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.thresholdMinus.setOnClickListener(v -> {
            if (ObjectDetectorHelper.getThreshold() >= 0.1) {
                float i = ObjectDetectorHelper.getThreshold();
                i -= 0.1f;
                ObjectDetectorHelper.setThreshold(i);
                updateControlsUi();
            }
        });

        // When clicked, raise detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.thresholdPlus.setOnClickListener(v -> {
            if (ObjectDetectorHelper.getThreshold() <= 0.8) {
                float i = ObjectDetectorHelper.getThreshold();
                i += 0.1f;
                ObjectDetectorHelper.setThreshold(i);
                updateControlsUi();
            }
        });

        // When clicked, reduce the number of objects that can be detected at a time
        fragmentCameraBinding.bottomSheetLayout.maxResultsMinus.setOnClickListener(v -> {
            if (ObjectDetectorHelper.getMaxResults() > 1) {
                int i = ObjectDetectorHelper.getMaxResults();
                i--;
                ObjectDetectorHelper.setMaxResults(i);
                updateControlsUi();
            }
        });

        // When clicked, increase the number of objects that can be detected at a time
        fragmentCameraBinding.bottomSheetLayout.maxResultsPlus.setOnClickListener(v -> {
            if (ObjectDetectorHelper.getMaxResults() < 5) {
                int i = ObjectDetectorHelper.getMaxResults();
                i++;
                ObjectDetectorHelper.setMaxResults(i);
                updateControlsUi();
            }
        });

        // When clicked, decrease the number of threads used for detection
        fragmentCameraBinding.bottomSheetLayout.threadsMinus.setOnClickListener(v -> {
            if (ObjectDetectorHelper.getNumThreads() > 1) {
                int i = ObjectDetectorHelper.getNumThreads();
                i--;
                ObjectDetectorHelper.setNumThreads(i);
                updateControlsUi();
            }
        });

        // When clicked, increase the number of threads used for detection
        fragmentCameraBinding.bottomSheetLayout.threadsPlus.setOnClickListener(v -> {
            if (ObjectDetectorHelper.getNumThreads() < 4) {
                int i = ObjectDetectorHelper.getNumThreads();
                i++;
                ObjectDetectorHelper.setNumThreads(i);
                updateControlsUi();
            }
        });

        // When clicked, change the underlying hardware used for inference (CPU, GPU, NNAPI)
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(0, false);
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                ObjectDetectorHelper.setCurrentDelegate(position);
                updateControlsUi();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // No action
            }
        });

        // When clicked, change the underlying model used for object detection
        // GPU, and NNAPI
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.setSelection(0, false);
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                ObjectDetectorHelper.setCurrentModel(position);
                updateControlsUi();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // No action
            }
        });
    }



    private void updateControlsUi() {
        fragmentCameraBinding.bottomSheetLayout.maxResultsValue.setText(Integer.toString(ObjectDetectorHelper.getMaxResults()));
        fragmentCameraBinding.bottomSheetLayout.thresholdValue.setText(String.format("%.2f", ObjectDetectorHelper.getThreshold()));
        fragmentCameraBinding.bottomSheetLayout.threadsValue.setText(Integer.toString(ObjectDetectorHelper.getNumThreads()));

        // Needs to be cleared instead of reinitialized because the GPU
        // delegate needs to be initialized on the thread using it when applicable
        objectDetectorHelper.clearObjectDetector();
        fragmentCameraBinding.overlay.clear();
    }

    private void setUpCamera() {
        Log.e("CameraFragment", "CameraFragment--->setUpCamera");
        //  final ProcessCameraProvider[] cameraProvider = new ProcessCameraProvider[100];
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // CameraProvider
                    cameraProvider = cameraProviderFuture.get();
                    // Build and bind the camera use cases
                    bindCameraUseCases();
                } catch (ExecutionException | InterruptedException e) {
                    // Handle exceptions if necessary
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));

    }

    public void bindCameraUseCases() {
        Log.e("CameraFragment", "CameraFragment--->bindCameraUseCases");
        if (cameraProvider == null) {
            Log.e(TAG, "CameraFragment---->bindCameraUseCases   Camera initialization failed");
            throw new IllegalStateException("Camera initialization failed.");
        }

        // CameraSelector - assumes we're only using the back camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        // Only using the 4:3 ratio because this is the closest to our models
        preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.getDisplay().getRotation())
                .build();

        // ImageAnalysis configuration
        imageAnalyzer = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.getDisplay().getRotation())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();

        imageAnalyzer.setAnalyzer(cameraExecutor, image -> {
            // The image rotation and RGB image buffer are initialized only once
            // the analyzer has started running

                bitmapBuffer = Bitmap.createBitmap(
                        image.getWidth(),
                        image.getHeight(),
                        Bitmap.Config.ARGB_8888
                );

            detectObjects(image);



        });

        cameraProvider.unbindAll();

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);

            // Attach the viewfinder's surface provider to the preview use case
            if (preview != null) {
                preview.setSurfaceProvider(fragmentCameraBinding.viewFinder.getSurfaceProvider());
            }
        } catch (Exception exc) {
            Log.e(TAG, "Use case binding failed", exc);
        }
    }

    public void detectObjects(ImageProxy image) {
        Log.e("CameraFragment", "CameraFragment--->detectObjects");
        // Copy RGB bits to the shared bitmap buffer
        Log.e("CameraFragment", "detectObjects");

        planes = image.getPlanes();
        bitmapBuffer.copyPixelsFromBuffer(planes[0].getBuffer());

        int imageRotation = image.getImageInfo().getRotationDegrees();
        objectDetectorHelper.detect(bitmapBuffer, imageRotation);


    }


//        ByteBuffer planeBuffer = image.getPlanes()[0].getBuffer();
//        bitmapBuffer.copyPixelsFromBuffer(planeBuffer);
//
//        int imageRotation = image.getImageInfo().getRotationDegrees();
//
//        // Pass Bitmap and rotation to the object detector helper for processing and detection
//        objectDetectorHelper.detect(bitmapBuffer, imageRotation);






    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (imageAnalyzer != null) {
            imageAnalyzer.setTargetRotation(fragmentCameraBinding.viewFinder.getDisplay().getRotation());
        }
    }




    @Override
    public void onError(String error) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show());
        }

    }


    @Override
    public void onResults(List<Detection> w, long inferenceTime, int imageHeight, int imageWidth) {
        Log.e("CameraFragment", "onResults---------------->1");

        if(getActivity()!=null)
        {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("CameraFragment", "onResults---------------->run");
                    fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.setText(
                            String.format("%d ms", inferenceTime)
                    );

                    // Pass necessary information to OverlayView for drawing on the canvas

                    fragmentCameraBinding.overlay.setResults(
                            (w != null) ? w : new LinkedList<>(),
                            imageHeight,
                            imageWidth);
                    fragmentCameraBinding.overlay.invalidate();

                 //   fragmentCameraBinding().overlay.CL;();



                }
            });
        }

    }

    @Override
    public void onInitialized() {
        Log.e("CameraFragment", "CameraFragment--->onInitialized");
        ObjectDetectorHelper.setupObjectDetector();
// Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor();
        fragmentCameraBinding.viewFinder.post(new Runnable() {
            @Override
            public void run() {
                Log.e("CameraFragment", "CameraFragment--->setUpCamera");
                setUpCamera();
            }
        });
        fragmentCameraBinding.progressCircular.setVisibility(View.GONE);


    }
}