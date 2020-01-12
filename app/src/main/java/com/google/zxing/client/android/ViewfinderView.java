/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.FloatRange;

import com.baijunty.scanner.R;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public class ViewfinderView extends View {

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 100L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;
    private final Paint paint;
    private final int maskColor;
    private final int laserColor;
    private final int resultPointColor;
    protected int middle;
    protected CameraManager cameraManager;
    protected int scannerAlpha;
    protected float aspect = 1.0f;
    protected List<ResultPoint> possibleResultPoints;
    protected List<ResultPoint> lastPossibleResultPoints;
    protected Rect scanBoxRect;
    private int[] screen=new int[2];
    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);
        laserColor = resources.getColor(R.color.viewfinder_laser);
        resultPointColor = resources.getColor(R.color.possible_result_points);
        scannerAlpha = 0;
        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = null;
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int w = getMeasuredWidth() * 4 / 5;
        int h = getMeasuredHeight() * 4 / 5;
        if (w > 0 && h > 0 && cameraManager != null) {
            adjustOffset();
            cameraManager.setViewfinderView(this);
        }
    }

    public void setAspect(@FloatRange(from = 0.1,to = 1.0)  float aspect) {
        if (aspect<0.1){
            aspect=0.1f;
        } else if (aspect>1.0){
            aspect=1.0f;
        }
        this.aspect = aspect;
        requestLayout();
    }

    public Rect getScanBoxRect() {
        return scanBoxRect;
    }


    private void adjustOffset(){
        int w=getMeasuredWidth();
        int h=getMeasuredHeight();
        WindowManager manager = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
        if (manager != null) {
            Point size=new Point();
            Display display = manager.getDefaultDisplay();
            display.getSize(size);
            w=Math.min(w,size.x-screen[0]);
            h=Math.min(h,size.y-screen[1]);
        }
        int width=w*4/5;
        int height = Math.min((int) (width * aspect), h*4/5);
        int leftOffset = (w - width) / 2;
        int topOffset =Math.max((h - height) / 2,0);
        scanBoxRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
        Log.d("set frame rect", scanBoxRect.toString());
        TextView t=((View)getParent()).findViewById(R.id.status_view);
        float top=scanBoxRect.bottom+  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,  8, getContext().getResources().getDisplayMetrics());
        t.setY(top);
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        Rect frame = scanBoxRect;
        Rect previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }
        int last=screen[1];
        getLocationOnScreen(screen);
        if (last!=screen[1]){
            adjustOffset();
            cameraManager.refreshPreview();
        }
        drawRect(canvas,frame);
        drawPoint(canvas,frame,previewFrame);
        // Request another update at the animation interval, but only repaint the laser line,
        // not the entire viewfinder mask.
        postInvalidateDelayed(ANIMATION_DELAY,
                frame.left - POINT_SIZE,
                frame.top - POINT_SIZE,
                frame.right + POINT_SIZE,
                frame.bottom + POINT_SIZE);
    }

    protected void drawRect(Canvas canvas, Rect frame){
        int width = getWidth();
        int height = getHeight();
        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        // Draw a red "laser scanner" line through the middle to show decoding is active
        paint.setColor(laserColor);
        paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
        scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
        int length = Math.min(frame.width(), frame.height()) / 10;
        canvas.drawRect(frame.left, frame.top, frame.left + 8, frame.top + length, paint);
        canvas.drawRect(frame.right - 8, frame.top, frame.right, frame.top + length, paint);
        canvas.drawRect(frame.left, frame.bottom - length, frame.left + 8, frame.bottom, paint);
        canvas.drawRect(frame.right - 8, frame.bottom - length, frame.right, frame.bottom, paint);
        canvas.drawRect(frame.left, frame.top, frame.left + length, frame.top + 8, paint);
        canvas.drawRect(frame.right - length, frame.top, frame.right, frame.top + 8, paint);
        canvas.drawRect(frame.left, frame.bottom - 8, frame.left + length, frame.bottom, paint);
        canvas.drawRect(frame.right - length, frame.bottom - 8, frame.right, frame.bottom, paint);
        if (middle < frame.top || middle > frame.bottom) {
            middle = frame.top;
        }
        paint.setAlpha(255);
        canvas.drawOval(new RectF(frame.left + 2, middle - 2, frame.right - 1, middle + 3), paint);
        middle += height / 30;
    }

    protected void drawPoint(Canvas canvas,Rect frame,Rect previewFrame){
        float scaleX = frame.width() / (float) previewFrame.width();
        float scaleY = frame.height() / (float) previewFrame.height();
        List<ResultPoint> currentPossible = possibleResultPoints;
        List<ResultPoint> currentLast = lastPossibleResultPoints;
        int frameLeft = frame.left;
        int frameTop = frame.top;
        if (currentPossible.isEmpty()) {
            lastPossibleResultPoints = null;
        } else {
            possibleResultPoints = new ArrayList<>(5);
            lastPossibleResultPoints = currentPossible;
            paint.setAlpha(CURRENT_POINT_OPACITY);
            paint.setColor(resultPointColor);
            synchronized (currentPossible) {
                for (ResultPoint point : currentPossible) {
                    canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                            frameTop + (int) (point.getY() * scaleY),
                            POINT_SIZE, paint);
                }
            }
        }
        if (currentLast != null) {
            paint.setAlpha(CURRENT_POINT_OPACITY / 2);
            paint.setColor(resultPointColor);
            synchronized (currentLast) {
                float radius = POINT_SIZE / 2.0f;
                for (ResultPoint point : currentLast) {
                    canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                            frameTop + (int) (point.getY() * scaleY),
                            radius, paint);
                }
            }
        }
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

}
