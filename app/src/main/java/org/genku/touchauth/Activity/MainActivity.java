package org.genku.touchauth.Activity;

/**
 * Created by genku on 4/1/2017.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.genku.touchauth.Model.NGramModel;
import org.genku.touchauth.Model.SVM;
import org.genku.touchauth.R;
import org.genku.touchauth.Service.AuthService;
import org.genku.touchauth.Service.SensorDataCollectingService;
import org.genku.touchauth.Service.SensorPredictingService;
import org.genku.touchauth.Util.FileUtils;

public class MainActivity extends AppCompatActivity {
//创建了一下sensor模型变量
      public static SVM sensorModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //初始化初始界面
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //存储路径初始化
        //获取当前路径
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath();
        //构建我们捕捉数据需要存储数据的文件夹
        FileUtils.makeRootDirectory(dir + "/Auth/");
        FileUtils.makeRootDirectory(dir + "/Auth/Sensor/");

        //获取权限
        isGrantExternalRW(this);
    }
    //版本权限
    public static boolean isGrantExternalRW(Activity activity) {
        //Build.VERSION.SDK_INT是用来判断Android SDK版本，手机的版本
        //Build.VERSION_CODES.M是Android的版本
        //checkselfpermission--这里用来检查写入sd卡的权限是否已经授权了
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //这里是请求权限
            activity.requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);

            return false;
        }
            return true;
        //return false;
    }
    //问：try catch是什么？
    //try catch是异常处理机制，catch是把错误信息存储在exception
    public void onCollectStartButtonClick(View view) {
        //服务一定要在andeoidmanifest中声明

        Toast.makeText(this, "Collecting Start...", Toast.LENGTH_SHORT).show();

        Intent intent1 = new Intent();
        intent1.setClass(this, SensorDataCollectingService.class);
        startService(intent1);
    }

    public void onTrainStartButtonClick(View view) {
        Log.d("Traintag","训练已经开始运行了");
       // Toast.makeText(this, "Training Start...", Toast.LENGTH_SHORT).show();
        //准备数据
        Log.d("Traintag","Toast数据已经过了");
        String dir  = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Auth/";
        //存储模型
        String trainFvFilename = dir + "Sensor/FeatureVectors.txt";
        String modelFilename = dir + "Sensor/Model.txt";
        //String centroidsFilename = dir + "Sensor/Centroids.txt";
        double[][] fv = FileUtils.readSvmFileToMatrix(trainFvFilename);
        //System.out.println(fv);
        //NGramModel model = new NGramModel(120, 2);

        double[] sensorLabel = new double[fv.length];
        //0是false,1是true
        for (int i = 0; i < sensorLabel.length; ++i) {
            sensorLabel[i] = 1;
        }
        sensorModel = new SVM();
        sensorModel.train(fv,sensorLabel);
        sensorModel.save(sensorModel,modelFilename);

        Toast.makeText(this, "Training End!", Toast.LENGTH_SHORT).show();
    }

    public void onTestStartButtonClick(View view) {

        Toast.makeText(this, " Start...", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent();
        intent.setClass(this, AuthService.class);
        startService(intent);
    }

    public void onStopButtonClick(View view) {
/*
        // Kill the previous getevent process
        //停止按钮是用来杀死我创造的所有进程
        try {
            String[] cmd = {
                    "/system/bin/sh",
                    "-c",
                    "ps | grep getevent | awk \'{print $2}\' | xargs su am kill"
            };
            Process p = Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
*/
       try {
            stopService(new Intent(this, SensorDataCollectingService.class));
            stopService(new Intent(this, SensorPredictingService.class));
            stopService(new Intent(this, AuthService.class));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
