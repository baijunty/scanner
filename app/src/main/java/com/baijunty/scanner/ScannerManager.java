package com.baijunty.scanner;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.zxing.Result;
import com.google.zxing.client.android.AmbientLightManager;
import com.google.zxing.client.android.BeepManager;
import com.google.zxing.client.android.CaptureActivityHandler;
import com.google.zxing.client.android.ViewfinderView;
import com.google.zxing.client.android.camera.CameraManager;

import java.io.IOException;

public class ScannerManager implements LifecycleObserver, SurfaceHolder.Callback{
    private String TAG = ScannerView.class.getSimpleName();
    private final ScannerView scannerView;
    private final AmbientLightManager ambientLightManager;
    private final BeepManager beepManager ;
    ScannerManager(ScannerView scannerView) {
        this.scannerView = scannerView;
        ambientLightManager =new  AmbientLightManager(scannerView.getContext());
        beepManager  =new  BeepManager(scannerView);
    }

    private Boolean hasSurface  = false;
    private MutableLiveData<Result> result =new MutableLiveData<>();

    public Context getContext(){
        return scannerView.getContext();
    }
    public CameraManager getCameraManager(){
        return  scannerView.getCameraManager();
    }
    private CaptureActivityHandler handler=null;

    public CaptureActivityHandler getHandler() {
        return handler;
    }

    public ViewfinderView getViewfinderView(){
        return scannerView.findViewById(R.id.viewfinder_view);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause(){
        ambientLightManager.stop();
        beepManager.close();
        if (handler!=null){
            handler.quitSynchronously();
        }
        getCameraManager().closeDriver();
        handler=null;
        if (!hasSurface) {
            SurfaceView surfaceView = scannerView.findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume(){
        beepManager.updatePrefs();
        ambientLightManager.start(getCameraManager());
        SurfaceView surfaceView = scannerView.findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
        }
    }


    private void initCamera(SurfaceHolder surfaceHolder) {
        if (getCameraManager().isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            getCameraManager().openDriver(surfaceHolder);
            handler=handler==null?new CaptureActivityHandler(this, null, null, null, getCameraManager()):handler;
            handler.sendEmptyMessageDelayed(R.id.restart_preview, 2000);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            scannerView.displayError(null);
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            scannerView.displayError(null);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    public void onFoundResult(Result result) {
        this.result.setValue(result);
        beepManager.playBeepSoundAndVibrate();
    }

    void addObserve(Observer<Result> observer ){
        result.observeForever(observer);
    }
}
