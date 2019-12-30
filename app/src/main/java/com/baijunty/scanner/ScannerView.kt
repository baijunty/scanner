package com.baijunty.scanner

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import com.google.zxing.client.android.ViewfinderView
import com.google.zxing.client.android.camera.CameraManager

class ScannerView : FrameLayout{
    var resultFoundListener:OnScanResultFound?=null
    private var aspect=1.0f
    private val manager:ScannerManager by lazy {
        ScannerManager(this)
    }
    val cameraManager: CameraManager by lazy {
        CameraManager(context.applicationContext)
    }

    constructor(context: Context) : super(context) {
        layoutScannerView(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        layoutScannerView(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        layoutScannerView(attrs)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        layoutScannerView(attrs)
    }

    fun bindLifecycle(lifecycle: Lifecycle){
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false)
        val viewfinderView:ViewfinderView=findViewById(R.id.viewfinder_view)
        viewfinderView.setCameraManager(cameraManager)
        viewfinderView.setAspect(aspect)
        lifecycle.addObserver(manager)
        manager.addObserve(Observer{
            if (resultFoundListener?.onFounded(it)==true){
                restartPreview()
            }
        })
        postInvalidateDelayed(200)
    }

    private fun layoutScannerView(attrs: AttributeSet?) {
        if (childCount <= 0) {
            View.inflate(context, R.layout.capture, this)
            attrs?.let {
                val a = context.obtainStyledAttributes(attrs, R.styleable.ScannerView)
                aspect=a.getFloat(R.styleable.ScannerView_aspect,1.0f)
                a.recycle()
            }
        }
    }

    fun restartPreview(){
        manager.handler?.sendEmptyMessageDelayed(R.id.restart_preview, 2000)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w=measuredWidth
        val h=measuredHeight
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = manager.defaultDisplay
        val theScreenResolution = Point()
        display.getSize(theScreenResolution)
        for (index in 0 until childCount){
            val v=getChildAt(index)
            if (v.id==R.id.preview_view){
                v.measure(theScreenResolution.x or  (2 shl 30), theScreenResolution.y or  (2 shl 30))
            }
        }
        setMeasuredDimension(w,h)
    }

    fun displayError(msg:String=context.getString(R.string.msg_camera_framework_bug)) {
        val t=findViewById<TextView>(R.id.status_view)
        t.text=msg
    }


}