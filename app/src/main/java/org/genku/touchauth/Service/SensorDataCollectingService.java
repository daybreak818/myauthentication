package org.genku.touchauth.Service;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;

import org.genku.touchauth.Model.SensorFeatureExtraction;
import org.genku.touchauth.Util.DataUtils;
import org.genku.touchauth.Util.FileUtils;

import java.util.ArrayList;
import java.util.List;

public class SensorDataCollectingService extends Service implements SensorEventListener {
    public SensorDataCollectingService() {

    }

    public static double INTERVAL = 10;
    public static double WINDOW_INTERVAL = 2;

    public final String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Auth/Sensor/";
    public final String accDir = dir + "Acc/";
    public final String magDir = dir + "Mag/";
    public final String gyrDir = dir + "Gyr/";
    public final String featureVectorsFilename = dir + "FeatureVectors.txt";
    public double[][] featureVectors={};
    private SensorManager sensorManager;
    private double[] gravity = {0, 0, 9.81};

    private List<List<Double>> accRawData = new ArrayList<>();
    private List<List<Double>> magRawData = new ArrayList<>();
    private List<List<Double>> gyrRawData = new ArrayList<>();

    private List<List<Double>> accTempData = new ArrayList<>();
    private List<List<Double>> magTempData = new ArrayList<>();
    private List<List<Double>> gyrTempData = new ArrayList<>();

    private int groupCount = 0;
    //先修改成2组
    private static int MAX_GROUP_COUNT = 2;



    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {

        FileUtils.makeRootDirectory(accDir);
        FileUtils.makeRootDirectory(magDir);
        FileUtils.makeRootDirectory(gyrDir);
        //实例化传感器
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //注册用户
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);

        new Thread(new Runnable() {
            @Override
            public void run() {
                //毫秒为单位
                Long startTime = System.currentTimeMillis();
                groupCount = 0;
                //try {
                    while (true) {
                        Long currentTime = System.currentTimeMillis();
                        //目前这里INTERVAL是10，所以这里是10s
                        if (currentTime - startTime > INTERVAL * 1000) {   //间隔是10，它还乘了1000，单位应该是ms
                            startTime = currentTime;
                            //原始数据
                            List<List<Double>> accData = accRawData;
                            accRawData = new ArrayList<>();
                            List<List<Double>> magData = magRawData;
                            magRawData = new ArrayList<>();
                            List<List<Double>> gyrData = gyrRawData;
                            gyrRawData = new ArrayList<>();

                            accTempData.addAll(accData);
                            magTempData.addAll(magData);
                            gyrTempData.addAll(gyrData);
                            ++groupCount;
                            try {
                                if (groupCount == MAX_GROUP_COUNT) {

                                    groupCount = 0;
                                    //System.out.print("acctemp数据"+ accTempData);
                                    //System.out.print("magtemp数据"+ magTempData);
                                    //System.out.print("gyrtemp数据"+ gyrTempData);
                                    double[][] acc = DataUtils.listToArray(accTempData);
                                    double[][] mag = DataUtils.listToArray(magTempData);
                                    double[][] gyr = DataUtils.listToArray(gyrTempData);
                                    System.out.println("acc数据" + accData);
                                    System.out.println("mag数据" + magData);
                                    System.out.println("gyr数据" + gyrData);
                                    if(acc==null&&mag==null&&gyr==null){
                                        break;
                                    }
                                    saveRawFile(currentTime, acc, mag, gyr);
                                    if(acc!=null&&mag!=null&&gyr!=null) {
                                        featureVectors = SensorFeatureExtraction.extract(
                                            INTERVAL * MAX_GROUP_COUNT,
                                            2, acc, mag, gyr); }
                                            else{
                                        break;
                                    }
                                //System.out.println("特征矩阵"+ featureVectors);
                                //这个地方可能是太耗时就新写了个线程来运行
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //System.out.println("特征矩阵"+ featureVectors);
                                        for (double[] line : featureVectors) {
                                            FileUtils.writeFileFromNums(featureVectorsFilename, line, true, true, 1);
                                        }
                                    }
                                }).start();
                                //将用过的数组清零
                                accTempData = new ArrayList<>();
                                magTempData = new ArrayList<>();
                                gyrTempData = new ArrayList<>();
                            }
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                    }
               // } catch (Exception e) {
                //    e.printStackTrace();
               // }
            }
        }).start();

        return super.onStartCommand(intent, flags, startId);
    }

    public static void collect() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Long currentTime = event.timestamp;
        List<Double> data = new ArrayList<>();
        data.add(currentTime + .0);
        data.add(event.values[0] + .0);
        data.add(event.values[1] + .0);
        data.add(event.values[2] + .0);

        try {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    String a, b, c, s;
                    //低通滤波器过滤其他因素而只剩重力加速度g
                    final double alpha = 0.8;
                    List<Double> linear_acceleration = new ArrayList<>();
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
                    //移除重力因素
                    linear_acceleration.add(currentTime + .0);
                    linear_acceleration.add(event.values[0] - gravity[0]);
                    linear_acceleration.add(event.values[1] - gravity[1]);
                    linear_acceleration.add(event.values[2] - gravity[2]);
                    accRawData.add(linear_acceleration);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    magRawData.add(data);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    gyrRawData.add(data);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void saveRawFile(final Long currentTime,
                             final double[][] acc,
                             final double[][] mag,
                             final double[][] gyr) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String accFilename = accDir + currentTime + ".txt";
                String magFilename = magDir + currentTime + ".txt";
                String gyrFilename = gyrDir + currentTime + ".txt";
                for (double[] line : acc) {
                    FileUtils.writeFileFromNums(accFilename, line, true, true, 1);
                }
                for (double[] line : mag) {
                    FileUtils.writeFileFromNums(magFilename, line, true, true, 1);
                }
                for (double[] line : gyr) {
                    FileUtils.writeFileFromNums(gyrFilename, line, true, true, 1);
                }
            }
        }).start();
    }

    private double mean(double[] vector) {
        double mean = 0;
        for (double value : vector) {
            mean += value;
        }
        mean /= vector.length;
        return mean;
    }

    private double var(double[] vector) {
        double var = 0;
        double mean = mean(vector);
        for (double value : vector) {
            var += Math.pow(value - mean, 2);
        }
        var /= vector.length;
        return var;
    }
}
