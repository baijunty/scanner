package com.baijunty.scanner;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceManager;

import com.google.zxing.client.android.CaptureActivityHandler;
import com.google.zxing.client.android.ViewfinderView;
import com.google.zxing.client.android.camera.CameraManager;

public class ScannerView extends FrameLayout {
    private OnScanResultFound resultFoundListener=null;
    private ScannerManager manager;
    protected int sound;
    private Point theScreenResolution =new Point();
    public void setResultFoundListener(OnScanResultFound resultFoundListener) {
        this.resultFoundListener = resultFoundListener;
    }

    public ScannerManager getManager(){
        if (manager==null){
            manager= new ScannerManager(this);
        }
        return manager;
    }

    CameraManager cameraManager;

    public CameraManager getCameraManager() {
        if (cameraManager==null){
            cameraManager=new CameraManager(getContext().getApplicationContext());
        }
        return cameraManager;
    }

    public void setAspect(float aspect) {
        ViewfinderView viewfinderView=findViewById(R.id.viewfinder_view);
        viewfinderView.setAspect(aspect);
    }

    public ScannerView(@NonNull Context context) {
        super(context);
        layoutScannerView(null);
    }

    public ScannerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        layoutScannerView(attrs);
    }

    public ScannerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        layoutScannerView(attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ScannerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        layoutScannerView(attrs);
    }

    public void bindLifecycle(Lifecycle lifecycle){
        PreferenceManager.setDefaultValues(getContext(), R.xml.preferences, false);
        ViewfinderView viewfinderView=findViewById(R.id.viewfinder_view);
        viewfinderView.setCameraManager(getCameraManager());
        lifecycle.addObserver(getManager());
        getManager().addObserve(result -> {
            if (resultFoundListener!=null&&resultFoundListener.onFounded(result)){
                restartPreview();
            }
        });
        postInvalidateDelayed(200);
    }

    private void layoutScannerView(AttributeSet attrs) {
        if (getChildCount() <= 0) {
            View.inflate(getContext(), R.layout.capture, this);
            if (attrs!=null){
                TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ScannerView);
                float aspect=a.getFloat(R.styleable.ScannerView_aspect,1.0f);
                setAspect(aspect);
                sound=a.getResourceId(R.styleable.ScannerView_sound_source,R.raw.beep);
                a.recycle();
            }
        }
    }

    public void restartPreview(){
        CaptureActivityHandler handler=getManager().getHandler();
        if (handler!=null){
            handler.sendEmptyMessageDelayed(R.id.restart_preview, 2000);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        WindowManager manager = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
        for (int i = 0; i < getChildCount(); i++) {
            View v=getChildAt(i);
            if (v.getId()==R.id.preview_view&&manager!=null){
                Display display = manager.getDefaultDisplay();
                display.getSize(theScreenResolution);
                v.measure(theScreenResolution.x |  (2 << 30), theScreenResolution.y |  (2 << 30));
                break;
            }
        }
    }

    public void displayError(String msg) {
        if (msg==null){
            msg=getContext().getString(R.string.msg_camera_framework_bug);
        }
        TextView t=findViewById(R.id.status_view);
        t.setText(msg);
    }
}
