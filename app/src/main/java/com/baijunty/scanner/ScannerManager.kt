package com.baijunty.scanner

import android.content.Context
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.lifecycle.*
import com.google.zxing.Result
import com.google.zxing.client.android.*
import com.google.zxing.client.android.camera.CameraManager
import java.io.IOException

class ScannerManager(private val scannerView: ScannerView) : LifecycleObserver ,SurfaceHolder.Callback{
    private val TAG = ScannerView::class.java.simpleName

    private  val ambientLightManager: AmbientLightManager = AmbientLightManager(scannerView.context)
    private var beepManager: BeepManager = BeepManager(scannerView)
    private var hasSurface: Boolean = false
    private val result:MutableLiveData<Result> = MutableLiveData()

    val context: Context get() = scannerView.context
    val cameraManager: CameraManager
        get() = scannerView.cameraManager

    var handler: CaptureActivityHandler?=null
    val viewfinderView: ViewfinderView
        get() = scannerView.findViewById(R.id.viewfinder_view)

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause(){
        ambientLightManager.stop()
        beepManager.close()
        handler?.quitSynchronously()
        cameraManager.closeDriver()
        handler=null
        if (!hasSurface) {
            val surfaceView = scannerView.findViewById<SurfaceView>(R.id.preview_view)
            val surfaceHolder = surfaceView.holder
            surfaceHolder.removeCallback(this)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume(){
        beepManager.updatePrefs()
        ambientLightManager.start(cameraManager)
        val surfaceView = scannerView.findViewById<SurfaceView>(R.id.preview_view)
        val surfaceHolder = surfaceView.holder
        if (hasSurface) {
            initCamera(surfaceHolder)
        } else {
            surfaceHolder.addCallback(this)
        }
    }


    private fun initCamera(surfaceHolder: SurfaceHolder) {
        if (cameraManager.isOpen) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?")
            return
        }
        try {
            cameraManager.openDriver(surfaceHolder)
            handler=handler?:CaptureActivityHandler(this, null, null, null, cameraManager)
            handler?.sendEmptyMessageDelayed(R.id.restart_preview, 2000)
        } catch (ioe: IOException) {
            Log.w(TAG, ioe)
            scannerView.displayError()
        } catch (e: RuntimeException) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e)
            scannerView.displayError()
        }

    }



    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (!hasSurface) {
            hasSurface = true
            initCamera(holder)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        hasSurface = false
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
    }

    fun onFoundResult(result: Result) {
        this.result.value=result
        beepManager.playBeepSoundAndVibrate()
    }

    fun addObserve(observer: Observer<Result>){
        result.observeForever(observer)
    }
}