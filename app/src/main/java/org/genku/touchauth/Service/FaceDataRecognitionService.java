package org.genku.touchauth.Service;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothClass;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.SyncStateContract;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import com.arcsoft.facedetection.*;
import com.arcsoft.facerecognition.*;
import com.arcsoft.facetracking.*;
import com.guo.android_extend.image.ImageConverter;

import java.io.File;
import android.net.Uri;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import org.genku.touchauth.Activity.CameraActivity;
import org.genku.touchauth.Model.Application;
import org.genku.touchauth.Model.Application.*;
import org.genku.touchauth.Model.CameraPreview;
import org.genku.touchauth.Model.CameraWindow;
import org.genku.touchauth.Model.FaceDB;
import org.genku.touchauth.R;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.Provider;
import java.util.*;
import java.util.logging.Logger;

import static android.app.Activity.RESULT_OK;
//import static org.genku.touchauth.Activity.CameraActivity.cActivity;


public class FaceDataRecognitionService extends Service implements SurfaceTexture.OnFrameAvailableListener {
    private  static final String TAG = "FDRS";
    public static Context context;
    private AppCompatActivity mActivity;
    private Camera mCamera;
    private String commandId;
    private boolean isRunning;
    public static float score;
    private final SurfaceHolder mHolder = null;
    private  SurfaceTexture surfaceTexture;
    //public MyHandler handler = new MyHandler(this);
    public FaceDataRecognitionService() {

    }
    public FaceDataRecognitionService(Context context) {
            this.context = context;
            mActivity=(AppCompatActivity)context;
    }
   //变量定义
    public final String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Auth/Face/";
    public final String enrollFilename = dir + "face.jpeg";
    private static final int REQUEST_CODE_IMAGE_CAMERA = 1;
    private static final int REQUEST_CODE_IMAGE_OP = 2;
    private static final int REQUEST_CODE_OP = 3;
    private Bitmap mBitmap;
    private Bitmap rBitmap;
    private Rect src = new Rect();
    private Rect dst = new Rect();
    public MyHandler handler = new MyHandler(this);
    private static class MyHandler extends Handler {
        WeakReference<FaceDataRecognitionService> mContext;

        MyHandler(FaceDataRecognitionService mContext) {
           this.mContext = new WeakReference<FaceDataRecognitionService>(mContext);
        }

        @Override
        public void handleMessage(Message msg) {
        }
    }
    //create
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }
    //service主代码
    //得在执行完毕后自动停止
   public int onStartCommand(final Intent intent, int flags, final int startId) {
       //如果存在注册过的特征，则直接进行比对
       //如果没有存在注册过的特征，则注册并提取特征
       if (!new File(enrollFilename).exists()) {
           Log.d(TAG, "Don't enroll!");
           //开始注册
                    /*
                    //Intent可以传递对象
                    Intent getImageByCamera = new Intent("android.media.action.IMAGE_CAPTURE");
                   //ContentValues存储机制，可以存String,int的数据
                    ContentValues values = new ContentValues(2);
                    ContentResolver mResolver = getBaseContext().getContentResolver();
                    values.put(MediaStore.Images.Media.DATA, enrollFilename);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    //图像原始路径
                    //debug时mpath为空
                    Uri mPath = mResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    //Camera截图后存到对应文件中
                    Application.setCaptureImage(mPath);
                    getImageByCamera.putExtra(MediaStore.EXTRA_OUTPUT, mPath);
                    */
                    /*
                    final Intent getImageByCamera = new Intent();
                    getImageByCamera.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getImageByCamera.setClass(getApplicationContext(),CameraActivity.class);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                getApplicationContext().startActivity(getImageByCamera);
                            } catch (ActivityNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    //解编码图像
                    */
           new Thread(new Runnable() {
               @Override
               public void run() {
                   try {
                       startTakePic(intent);
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
               }
           }).start();
           new Thread(new Runnable() {
               @Override
               public void run() {
                   mBitmap = Application.decodeImage(enrollFilename);
                   //转换图像为NV21格式
                   byte[] data = new byte[mBitmap.getWidth() * mBitmap.getHeight() * 3 / 2];
                   ImageConverter convert = new ImageConverter();
                   convert.initial(mBitmap.getWidth(), mBitmap.getHeight(), ImageConverter.CP_PAF_NV21);
                   if (convert.convert(mBitmap, data)) {
                       Log.d(TAG, "convert ok!");
                   }
                   convert.destroy();
                   //得到了图像数据，提取信息
                   //首先，初始化人脸识别引擎
                   AFR_FSDKEngine rengine = new AFR_FSDKEngine();
                   AFR_FSDKFace face = new AFR_FSDKFace();
                   AFR_FSDKError rerror = rengine.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
                   //List<AFR_FSDKFace> rresult = new ArrayList<AFR_FSDKFace>();
                   AFR_FSDKFace rresult = new AFR_FSDKFace();
                   AFD_FSDKEngine dengine = new AFD_FSDKEngine();
                   AFD_FSDKError derror = dengine.AFD_FSDK_InitialFaceEngine(FaceDB.appid, FaceDB.fd_key, AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT, 16, 5);
                   List<AFD_FSDKFace> dresult = new ArrayList<AFD_FSDKFace>();
                   Log.d("com.arcsoft", "AFR_FSDK_InitialEngine = " + rerror.getCode());
                   Log.d("com.arcsoft", "AFD_FSDK_InitialFaceEngine = " + derror.getCode());
                   //提取特征注册用户
                   derror = dengine.AFD_FSDK_StillImageFaceDetection(data, mBitmap.getWidth(), mBitmap.getHeight(), AFD_FSDKEngine.CP_PAF_NV21, dresult);
                   rerror = rengine.AFR_FSDK_ExtractFRFeature(data, mBitmap.getWidth(), mBitmap.getHeight(), AFR_FSDKEngine.CP_PAF_NV21, new Rect(dresult.get(0).getRect()), AFR_FSDKEngine.AFR_FOC_0, face);
                   Log.d("com.arcsoft", "Face=" + face.getFeatureData()[0] + "," + face.getFeatureData()[1] + "," + face.getFeatureData()[2] + "," + rerror.getCode());
                   //将人脸注册到人脸库中
                   FaceDB.addFace("genuine", face);
                   derror = dengine.AFD_FSDK_UninitialFaceEngine();
                   Log.d(TAG, "AFD_FSDK_UninitialFaceEngine =" + derror.getCode());
                   rerror = rengine.AFR_FSDK_UninitialEngine();
                   Log.d(TAG, "AFR_FSDK_UninitialFaceEngine =" + rerror.getCode());
               }
           }).start();
       } else {
           //进行认证
           //转换图像
           byte[] rdata = new byte[rBitmap.getWidth() * mBitmap.getHeight() * 3 / 2];
           ImageConverter convert = new ImageConverter();
           convert.initial(mBitmap.getWidth(), mBitmap.getHeight(), ImageConverter.CP_PAF_NV21);
           if (convert.convert(mBitmap, rdata)) {
               Log.d(TAG, "convert ok!");
           }
           convert.destroy();
           //真实用户
           byte[] data = new byte[mBitmap.getWidth() * mBitmap.getHeight() * 3 / 2];
           ImageConverter convert1 = new ImageConverter();
           convert1.initial(mBitmap.getWidth(), mBitmap.getHeight(), ImageConverter.CP_PAF_NV21);
           if (convert1.convert(mBitmap, data)) {
               Log.d(TAG, "convert ok!");
           }
           convert1.destroy();
           //认证
           //首先，初始化人脸识别引擎
           AFR_FSDKEngine rengine = new AFR_FSDKEngine();
           AFR_FSDKFace face1 = new AFR_FSDKFace();
           AFR_FSDKFace face2 = new AFR_FSDKFace();
           AFR_FSDKError rerror = rengine.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
           //List<AFR_FSDKFace> rresult = new ArrayList<AFR_FSDKFace>();
           AFR_FSDKFace rresult = new AFR_FSDKFace();
           AFD_FSDKEngine dengine = new AFD_FSDKEngine();
           AFD_FSDKError derror = dengine.AFD_FSDK_InitialFaceEngine(FaceDB.appid, FaceDB.fd_key, AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT, 16, 5);
           List<AFD_FSDKFace> dresult = new ArrayList<AFD_FSDKFace>();
           AFR_FSDKMatching rscore = new AFR_FSDKMatching();
           //真实用户
           derror = dengine.AFD_FSDK_StillImageFaceDetection(data, mBitmap.getWidth(), mBitmap.getHeight(), AFD_FSDKEngine.CP_PAF_NV21, dresult);
           rerror = rengine.AFR_FSDK_ExtractFRFeature(data, mBitmap.getWidth(), mBitmap.getHeight(), AFR_FSDKEngine.CP_PAF_NV21, new Rect(dresult.get(0).getRect()), AFR_FSDKEngine.AFR_FOC_0, face1);
           Log.d("com.arcsoft", "Face1=" + face1.getFeatureData()[0] + "," + face1.getFeatureData()[1] + "," + face1.getFeatureData()[2] + "," + rerror.getCode());
           //检测用户
           derror = dengine.AFD_FSDK_StillImageFaceDetection(rdata, rBitmap.getWidth(), rBitmap.getHeight(), AFD_FSDKEngine.CP_PAF_NV21, dresult);
           rerror = rengine.AFR_FSDK_ExtractFRFeature(rdata, rBitmap.getWidth(), rBitmap.getHeight(), AFR_FSDKEngine.CP_PAF_NV21, new Rect(dresult.get(0).getRect()), AFR_FSDKEngine.AFR_FOC_0, face2);
           Log.d("com.arcsoft", "Face2=" + face2.getFeatureData()[0] + "," + face2.getFeatureData()[1] + "," + face2.getFeatureData()[2] + "," + rerror.getCode());
           rerror = rengine.AFR_FSDK_FacePairMatching(face1, face2, rscore);
           score = rscore.getScore();
           Log.d("com.arcsoft", "Score:" + rscore.getScore());
           derror = dengine.AFD_FSDK_UninitialFaceEngine();
           Log.d(TAG, "AFD_FSDK_UninitialFaceEngine =" + derror.getCode());
           rerror = rengine.AFR_FSDK_UninitialEngine();
           Log.d(TAG, "AFR_FSDK_UninitialFaceEngine =" + rerror.getCode());

       }
       return super.onStartCommand(intent, flags, startId);
   }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private void startTakePic(Intent intent) throws IOException {
        mCamera = getFacingFrontCamera();
        //CameraPreview camPreview = new CameraPreview(getBaseContext(),mCamera);
        //camPreview.setSurfaceTextureListener(camPreview);
        //FrameLayout preview = (FrameLayout) getBaseContext().findViewById(R.id.camera_surfaceview);
        //preview.addView(camPreview);

        surfaceTexture = new SurfaceTexture(10);
        surfaceTexture.setOnFrameAvailableListener(this);
        //mCamera.setDisplayOrientation(180);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Camera.Parameters params = mCamera.getParameters();
                params.setPreviewFormat(ImageFormat.NV21);
                params.setZoom(0);//设置焦距为0
                //Camera.Size previewSize = params.getPreviewSizes();
                params.setPreviewSize(1280,720);
                params.setPictureSize(1280,720);
                mCamera.setParameters(params);
                if (mCamera == null) {
                    // Seeing this on Nexus 7 2012 -- I guess it wants a rear-facing camera, but
                    // there isn't one.  TODO: fix
                    //throw new RuntimeException("Default camera not available");
                    Log.e(TAG, "openCamere, mCamera == null!");
                }

                //这一步是最关键的，使用surfaceTexture来承载相机的预览，而不需要设置一个可见的view
                try {
                    mCamera.setPreviewTexture(surfaceTexture);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mCamera.startPreview();
                takePicture();
            }
            }).start();
        }

    private Camera getFacingFrontCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    return Camera.open(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    private void takePicture(){
        mCamera.takePicture(mShutterCallback, null, mPictureCallback);
    }
    private Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            //playContinuousSound();
        }
    };
    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            Log.i(TAG, "data time = " + System.currentTimeMillis());
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (!new File(enrollFilename).exists()) {
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) // 判断是否可以对SDcard进行操作
                {      // 获取SDCard指定目录下
                    String sdCardDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Auth/Face/";
                    File dirFile = new File(sdCardDir);  //目录转化成文件夹
                    if (!dirFile.exists()) {                //如果不存在，那就建立这个文件夹
                        dirFile.mkdirs();
                    }                            //文件夹有啦，就可以保存图片啦
                    File file = new File(sdCardDir, "face.jpeg");// 在SDcard的目录下创建图片文,以当前时间为其命名
                    FileOutputStream out = null;
                    try {
                        out = new FileOutputStream(file);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    //out.flush();
                    //out.close();
                    camera.startPreview();
                }
            } else {
                //camera.startPreview();
                //返回捕捉的认证图片
                rBitmap = bitmap;
                camera.startPreview();
            }
            releaseCamera();
        }
    };
    private void releaseCamera() {
        if (mCamera != null) {
            Log.d(TAG, "releaseCamera...");
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {

    }

/*
    private void startTakePic(Intent intent) {
        if (!isRunning) {
            //getStringExtra是为了获得intent中的什么数据
            //commandId = intent.getStringExtra("commandId");
            CameraWindow.show(getBaseContext());
            SurfaceView preview = CameraWindow.getDummyCameraView();
            if (preview != null) {
                //preview = (SurfaceView)findViewById(R.id.camera_surfaceview);
                autoTakePic(preview);
            } else {
                stopSelf();
            }
        }
    }
    private void autoTakePic(SurfaceView preview) {
        Log.d(TAG, "autoTakePic...");
        isRunning = true;
        mCamera = getFacingFrontCamera();
        if (mCamera == null) {
            Log.w(TAG, "getFacingFrontCamera return null");
            stopSelf();
            return;
        }
        try {
            mCamera.setPreviewDisplay(preview.getHolder());
            mCamera.startPreview();// 开始预览
            takePicture();
        } catch (Exception e) {
            e.printStackTrace();
            releaseCamera();
            stopSelf();
        }
    }
    private void takePicture() throws Exception {
        Log.d(TAG, "takePicture...");
        try {
            mCamera.takePicture(null, null, this);
            releaseCamera();
        } catch (Exception e) {
            Log.d(TAG, "takePicture failed!");
            e.printStackTrace();
            throw e;
        }
    }

    private Camera getFacingFrontCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    releaseCamera();
                    return Camera.open(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    //拍完照片回调
//private Camera.PictureCallback myPicture = new Camera.PictureCallback() {
    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.d(TAG, "onPictureTaken...");
        try {
            camera.startPreview();
            // 大于500K，压缩预防内存溢出
            BitmapFactory.Options opts = null;
            if (data.length > 500 * 1024) {
                opts = new BitmapFactory.Options();
                opts.inSampleSize = 2;
            }
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length,
                    opts);
            if (!new File(enrollFilename).exists()) {
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) // 判断是否可以对SDcard进行操作
                {      // 获取SDCard指定目录下
                    String sdCardDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Auth/Face/";
                    File dirFile = new File(sdCardDir);  //目录转化成文件夹
                    if (!dirFile.exists()) {                //如果不存在，那就建立这个文件夹
                        dirFile.mkdirs();
                    }                            //文件夹有啦，就可以保存图片啦
                    File file = new File(sdCardDir, "face.jpeg");// 在SDcard的目录下创建图片文,以当前时间为其命名
                    FileOutputStream out = null;
                    try {
                        out = new FileOutputStream(file);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    out.flush();
                    out.close();
                    camera.startPreview();
                }
            } else {
                camera.startPreview();
                //返回捕捉的认证图片
                rBitmap = bitmap;
                camera.startPreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }
    }
//};

    private void releaseCamera() {
        if (mCamera != null) {
            Log.d(TAG, "releaseCamera...");
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy...");
        commandId = null;
        isRunning = false;
        releaseCamera();
    }
    */
/*
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
    */
}
