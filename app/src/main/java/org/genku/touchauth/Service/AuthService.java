package org.genku.touchauth.Service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.genku.touchauth.Util.FileUtils;

public class AuthService extends Service {

    public static final int INTERVAL = 4;
    private Context context;
    private static final String authResultFilename = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Auth/AuthResult.txt";


    public AuthService() {
    }
    public AuthService(Context context) {
        this.context = context;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

       try {
            //stopService(new Intent(this, TouchDataCollectingService.class));
            //stopService(new Intent(this, SensorDataCollectingService.class));
            //stopService(new Intent(this, TouchPredictingService.class));
            //stopService(new Intent(this, SensorPredictingService.class));

  //      startService(new Intent(this, TouchPredictingService.class));
           //Intent intent3 = new Intent();
           //intent3.setClass(this, SensorPredictingService.class);
           //startService(intent3);
    //    startService(new Intent(this, SensorPredictingService.class));
       } catch (Exception e) {
           e.printStackTrace();
       }
       //把线程先去掉
       new Thread(new Runnable() {
            @Override
            public void run() {
                predict();
            }
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    private void predict() {
        Long start = System.currentTimeMillis();
        int flag = 1;
        try {
            //这个地方不应该是true,应该是综合的认证结果如果为true,则在循环一遍，先加上面部的
            while (flag == 1) {
                flag = 0;
                Long now = System.currentTimeMillis();
                if (now - start < 5 * 300) continue;

     //           double touchConfidence = TouchPredictingService.confidence;
                double sensorConfidence = SensorPredictingService.confidence;
      //         if (touchConfidence > 2 && sensorConfidence > 0) {
                //判别中超过半数则认为是正确的
                  if(sensorConfidence > 0.5){
                   // Toast.makeText(getApplicationContext(), "True", Toast.LENGTH_SHORT).show();
                    FileUtils.writeFile(authResultFilename, "True\r\n", true);
                    flag = 1;
                    Log.d("sensorauthtag","sensorauth结束-True");
                } else {
                   // Toast.makeText(getApplicationContext(), "False", Toast.LENGTH_SHORT).show();
                      //这里不进行错误写入，等面部错误时在写入
                    //FileUtils.writeFile(authResultFilename, "False\r\n", true);
                    Log.d("sensorauthtag","sensorauth结束-False");
                    //在这里当加速度计错误的时候，开始面部收集及认证
                      //如果面部认证正确，flag == 1；如果面部认证错误，flag == 0.
                      //并把结果写入文件中。
                      final Intent intent2 = new Intent(this,FaceDataRecognitionService.class);
                      new Thread(new Runnable() {
                          @Override
                          public void run() {
                              startService(intent2);
                          }
                          }).start();
                      if(FaceDataRecognitionService.score>0.6){
                          flag =1;
                          FileUtils.writeFile(authResultFilename, "True\r\n", true);
                          Log.d("faceauthtag","faceauth结束-True");
                      }
                      else{
                          FileUtils.writeFile(authResultFilename, "True\r\n", true);
                          Log.d("faceauthtag","faceauth结束-False");
                      }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }
}
