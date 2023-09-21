package com.tensorflow.objectscanner.helperclasses;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tflite.client.TfLiteInitializationOptions;
import com.google.android.gms.tflite.gpu.support.TfLiteGpu;
import com.tensorflow.objectscanner.fragments.CameraFragment;

import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.gms.vision.TfLiteVision;
import org.tensorflow.lite.task.gms.vision.detector.Detection;
import org.tensorflow.lite.task.gms.vision.detector.ObjectDetector;

import java.util.List;

public class ObjectDetectorHelper {

    public static float threshold = 0.5f;
    public static int numThreads = 2;
    public static int maxResults = 3;
    public static int currentDelegate = 0;
    public static int currentModel = 0;
    public static Context context;
    public static DetectorListener objectDetectorListener;


    public final String TAG = "ObjectDetectionHelper";
    static ObjectDetector.ObjectDetectorOptions.Builder optionsBuilder ;
    public static boolean gpuSupported = false;
    public static ObjectDetector objectDetector = null; // For this example, this needs to be a var so it can be reset on changes. If the ObjectDetector will not change, a lazy val would be preferable.
    public static final String TAGs = "CameraFragment";

    public static final int DELEGATE_CPU = 0;
    public static final int DELEGATE_GPU = 1;
    public static final int DELEGATE_NNAPI = 2;
    public static final int MODEL_MOBILENETV1 = 0;
    public static final int MODEL_EFFICIENTDETV0 = 1;
    public static final int MODEL_EFFICIENTDETV1 = 2;
    public static final int MODEL_EFFICIENTDETV2 = 3;

    public ObjectDetectorHelper(Context requireContext, CameraFragment cameraFragment) {

        context = requireContext;
        objectDetectorListener = cameraFragment;
        test();
        //new ObjectDetectorHelper();


    }

    public static void setThreshold(float threshold) {
        ObjectDetectorHelper.threshold = threshold;
    }


    public static float getThreshold() {
        return threshold;
    }


    public static int getNumThreads() {
        return numThreads;
    }


    public static int getMaxResults() {
        return maxResults;
    }

    public static void setNumThreads(int numThreads) {
        ObjectDetectorHelper.numThreads = numThreads;
    }

    public static void setMaxResults(int maxResults) {
        ObjectDetectorHelper.maxResults = maxResults;
    }

    public static void setupObjectDetector() {
        Log.e(TAGs, "ObjectDetacter----->setupObjectDetector ");
        if (!TfLiteVision.isInitialized()) {
            Log.e("CameraFragment", "setupObjectDetector: TfLiteVision is not initialized yet");
        }
         optionsBuilder =
                ObjectDetector.ObjectDetectorOptions.builder().
                        setScoreThreshold(threshold).
                        setMaxResults(maxResults);
        BaseOptions.Builder baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads);


        switch (currentDelegate) {
            case DELEGATE_CPU:
                // Default
                break;
            case DELEGATE_GPU:
                if (gpuSupported) {
                    Log.e(TAGs, "ObjectDetacter----->gpuSupported ");
                    baseOptionsBuilder.useGpu();
                } else {
                    Log.e(TAGs, "ObjectDetacter----->GPU is not supported on this device ");
                    objectDetectorListener.onError("GPU is not supported on this device");
                }
                break;
            case DELEGATE_NNAPI:
                Log.e(TAGs, "ObjectDetacter----->DELEGATE_NNAPI ");
                baseOptionsBuilder.useNnapi();
                break;
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build());


        String modelName;
        switch (currentModel) {
            case MODEL_MOBILENETV1:
                modelName = "mobilenetv1.tflite";
                break;
            case MODEL_EFFICIENTDETV0:
                modelName = "efficientdet-lite0.tflite";
                break;
            case MODEL_EFFICIENTDETV1:
                modelName = "efficientdet-lite1.tflite";
                break;
            case MODEL_EFFICIENTDETV2:
                modelName = "efficientdet-lite2.tflite";
                break;
            default:
                modelName = "mobilenetv1.tflite";
                break;
        }
        try {
            Log.e(TAGs, "ObjectDetacter----->try ");
            objectDetector = ObjectDetector.createFromFileAndOptions(context, modelName, optionsBuilder.build());
        } catch (Exception e) {
            Log.e(TAGs, "ObjectDetacter----->Object detector failed to initialize. See error logs ");
            objectDetectorListener.onError("Object detector failed to initialize. See error logs for details");
            Log.e("CameraFragment", "TFLite failed to load model with error: " + e.getMessage());
        }

    }


    public static void setCurrentDelegate(int currentDelegate) {
        ObjectDetectorHelper.currentDelegate = currentDelegate;
    }


    public static void setCurrentModel(int currentModel) {
        ObjectDetectorHelper.currentModel = currentModel;
    }

    public static Context getContext() {
        return context;
    }


    public  void clearObjectDetector() {
        objectDetector = null;
    }

    public  void detect(Bitmap image, int imageRotation) {
        Log.e(TAGs, "ObjectDetacter----->detect");
    //    Log.e("TfLiteVision", "setupObjectDetector: TfLiteVision is not initialized yet");
//        Log.e(TAG, "detect: TfLiteVision is not initialized yet");
        if (!TfLiteVision.isInitialized()) {
//            Log.e("TfLiteVision", "detect: TfLiteVision is not initialized yet");
            Log.e(TAGs, "detect: TfLiteVision is not initialized yet");
            return;
        }
        if (objectDetector == null) {
            setupObjectDetector();
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        long inferenceTime = SystemClock.uptimeMillis();

        // Create preprocessor for the image.
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture

        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new Rot90Op(-imageRotation / 90))
                .build();

        // Preprocess the image and convert it into a TensorImage for detection.
        TensorImage tensorImage = imageProcessor.process(TensorImage.fromBitmap(image));


        List<Detection>  results = objectDetector.detect(tensorImage);
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime;
            objectDetectorListener.onResults(
                    results,
                    inferenceTime,
                    tensorImage.getHeight() ,
                    tensorImage.getWidth()
            );

        //results

    }

    private void test() {
        Log.e(TAGs, "ObjectDetacter----->init ");
        TfLiteGpu.isGpuDelegateAvailable(getContext()).onSuccessTask(new SuccessContinuation<Boolean, Void>() {
                    @NonNull
                    @Override
                    public Task<Void> then(Boolean gpuAvailable) throws Exception {
                        TfLiteInitializationOptions.Builder optionsBuilder =
                                TfLiteInitializationOptions.builder();
                        if (gpuAvailable != null && gpuAvailable) {
                            optionsBuilder.setEnableGpuDelegateSupport(true);
                        }
                        return TfLiteVision.initialize(getContext(), optionsBuilder.build());
                    }
                }).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        objectDetectorListener.onInitialized();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        objectDetectorListener.onError("TfLiteVision failed to initialize: " + e.getMessage());
                    }
                });
    }

    public interface DetectorListener {

        void onInitialized();

        void onError(String error);

        void onResults(List<Detection> results, long inferenceTime, int imageHeight, int imageWidth);


    }


}