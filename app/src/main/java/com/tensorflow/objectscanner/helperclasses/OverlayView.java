package com.tensorflow.objectscanner.helperclasses;

import static java.lang.Math.max;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.tensorflow.objectscanner.R;

import org.tensorflow.lite.task.gms.vision.detector.Detection;

import java.util.LinkedList;
import java.util.List;

public class OverlayView extends View {

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.e("Camerafragment", " initPaints(); initPaints(); initPaints(); initPaints(); initPaints(); initPaints()" );
        initPaints();
    }

    private List<Detection> results = new LinkedList<Detection>();
    private static Paint boxPaint = new Paint();
    private static Paint textBackgroundPaint = new Paint();
    private static Paint textPaint = new Paint();

    private float scaleFactor = 1f;


    private Rect bounds = new Rect();
    private static final int BOUNDING_RECT_TEXT_PADDING = 8;


  public void clear() {
        textPaint.reset();
        textBackgroundPaint.reset();
        boxPaint.reset();
        invalidate();
        initPaints();
    }

    public void initPaints() {
        textBackgroundPaint.setColor(Color.BLACK);
        textBackgroundPaint.setStyle(Paint.Style.FILL);
        textBackgroundPaint.setTextSize(50f);

        textPaint.setColor(Color.WHITE);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(50f);

        boxPaint.setColor(ContextCompat.getColor(getContext(), R.color.bounding_box_color));
        boxPaint.setStrokeWidth(8F);
        boxPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.e("overlay", "onDraw");
        for (Detection result : results) {
            RectF boundingBox = result.getBoundingBox();

            float top = boundingBox.top * scaleFactor;
            float bottom = boundingBox.bottom * scaleFactor;
            float left = boundingBox.left * scaleFactor;
            float right = boundingBox.right * scaleFactor;

            // Draw bounding box around detected objects
            @SuppressLint("DrawAllocation") RectF drawableRect = new RectF(left, top, right, bottom);
            canvas.drawRect(drawableRect, boxPaint);

            // Create text to display alongside detected objects
            @SuppressLint("DefaultLocale") String drawableText = result.getCategories().get(0).getLabel() + " " + String.format("%.2f", result.getCategories().get(0).getScore());

            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length(), bounds);
            int textWidth = bounds.width();
            int textHeight = bounds.height();
            canvas.drawRect(left, top, left + textWidth + BOUNDING_RECT_TEXT_PADDING, top + textHeight + BOUNDING_RECT_TEXT_PADDING, textBackgroundPaint);

            // Draw text for detected object
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint);
        }
    }

    public void setResults(List<Detection> detectionResults, int imageHeight, int imageWidth) {
        Log.e("CameraFragment", "Overlay-----|> setResults");
        results = detectionResults;
        scaleFactor = max(getWidth() * 1f / imageWidth, getHeight() * 1f / imageHeight);

    }

}


