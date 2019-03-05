package org.genku.touchauth.Model;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;

import org.genku.touchauth.Activity.MainActivity;
import org.genku.touchauth.R;
import org.genku.touchauth.Service.FaceDataRecognitionService;


public class CameraWindow {
    private static final String TAG = "CameraWindow创建";

    private static WindowManager windowManager;

    private static Context applicationContext;

    private static SurfaceView dummyCameraView;

    private SurfaceView mSurfaceview;
    private SurfaceHolder mSurfaceHolder;
    private static Context context;

    /**
     * 显示全局窗口
     *
     * @param context
     */
    public static void show(Context context) {
        if (applicationContext == null) {
            //applicationContext = context.getApplicationContext();
            applicationContext = context.getApplicationContext();
            // linearLayout = (LinearLayout) LayoutInflater.from(context.getApplication()).inflate(R.layout.camera_activity_slient, null);
            //获得窗口管理对象
            WindowManager windowManager = (WindowManager) applicationContext
                    .getSystemService(Context.WINDOW_SERVICE);
            //定义了sufaceView实例
            dummyCameraView = new SurfaceView(applicationContext);
            //dummyCameraView = (SurfaceView) LayoutInflater.from(context.getApplicationContext()).inflate(R.layout.camera_activity_slient,null);
            //dummyCameraView = (SurfaceView) dummyCameraView.findViewById(R.id.camera_surfaceview);
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            layoutInflater.inflate(R.layout.camera_activity_slient, null);
            //获得Layoutparams对象，为后续操作准备
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.width = 140;
            params.height = 140;
            params.alpha = 0;
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            // 屏蔽点击事件
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

            //params.format=1;
            params.format = PixelFormat.TRANSPARENT;
            //显示图像
            windowManager.addView(dummyCameraView, params);
            Log.d(TAG, TAG + " showing");
            System.out.println("CameraWindow创建");
        }
    }

    /**
     * @return 获取窗口视图
     */
    public static SurfaceView getDummyCameraView() {
        //dummyCameraView = (SurfaceView) dummyCameraView.findViewById(R.id.camera_surfaceview);
        return dummyCameraView;
    }

    /**
     * 隐藏窗口
     */
    public static void dismiss() {
        try {
            if (windowManager != null && dummyCameraView != null) {
                windowManager.removeView(dummyCameraView);
                Log.d(TAG, TAG + " dismissed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
