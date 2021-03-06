package org.genku.touchauth.Service;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

//import org.genku.touchauth.Model.NGramModel;
import org.genku.touchauth.Model.SVM;
import org.genku.touchauth.Model.SensorFeatureExtraction;
import org.genku.touchauth.Util.DataUtils;
import org.genku.touchauth.Util.FileUtils;
import org.genku.touchauth.libsvm.*;

import java.util.ArrayList;
import java.util.List;

public class SensorPredictingService extends Service implements SensorEventListener {

    public SensorPredictingService() {

    }

    public static double confidence;
    public static SVM model;

    public static double INTERVAL = 10;
    public static double WINDOW_INTERVAL = 2;

    public static int NUM_OF_CENTROIDS = 120;
    public static int NUM_OF_N = 2;

    public final String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Auth/Sensor/";
    public final String modelFilename = dir + "Model.txt";
    public final String trainFvFilename = dir + "FeatureVectors.txt";

;
    public final String accDir = dir + "Acc/";
    public final String magDir = dir + "Mag/";
    public final String gyrDir = dir + "Gyr/";
    public final String featureVectorsFilename = dir + "FeatureVectors.txt";

    private SensorManager sensorManager;
    private double[] gravity = {0, 0, 9.8};

    private List<List<Double>> accRawData = new ArrayList<>();
    private List<List<Double>> magRawData = new ArrayList<>();
    private List<List<Double>> gyrRawData = new ArrayList<>();

    private List<List<Double>> accTempData = new ArrayList<>();
    private List<List<Double>> magTempData = new ArrayList<>();
    private List<List<Double>> gyrTempData = new ArrayList<>();

    private int groupCount = 0;
    private static int MAX_GROUP_COUNT = 2;


    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {

        Log.d("sensorpredicttag","sensorpredict开始");
        FileUtils.makeRootDirectory(accDir);
        FileUtils.makeRootDirectory(magDir);
        FileUtils.makeRootDirectory(gyrDir);

        if (model == null) {
                //double[][] nums = FileUtils.readFileToMatrix(modelFilename);
                //double[][] centroids = FileUtils.readFileToMatrix(centroidsFilename);
                //model = new NGramModel(nums, centroids, NUM_OF_N);
                model = new SVM();
                double[][] fv = FileUtils.readSvmFileToMatrix(trainFvFilename);
                //构造标签向量
                // model = new NGramModel(NUM_OF_CENTROIDS, NUM_OF_N);
                //sensormodel = new SVM();
                double[] sensorLabel = new double[fv.length];
                for (int i = 0; i < sensorLabel.length; ++i) {
                             sensorLabel[i] = 1;
                          }
                //sensormodel = new SVM();
                model.train(fv,sensorLabel);
                model.save(model,modelFilename);
                //model.saveCentroids(centroidsFilename);
        }

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
//先把线程去掉
        new Thread(new Runnable() {
            @Override
            public void run() {
                Long startTime = System.currentTimeMillis();
                groupCount = 0;
                try {
                    while (true) {
                        Long currentTime = System.currentTimeMillis();
                        if (currentTime - startTime > INTERVAL * 1000) {
                            startTime = currentTime;

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

                            if (groupCount == MAX_GROUP_COUNT) {

                                groupCount = 0;

                                double[][] acc = DataUtils.listToArray(accTempData);
                                double[][] mag = DataUtils.listToArray(magTempData);
                                double[][] gyr = DataUtils.listToArray(gyrTempData);

                                saveRawFile(currentTime, acc, mag, gyr);
                                //新采集的数据
                                final double[][] featureVectors = SensorFeatureExtraction.extract(
                                        INTERVAL * MAX_GROUP_COUNT,
                                        2, acc,  mag, gyr);

                                 //先把这里的线程删掉
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        for (double[] line : featureVectors) {
                                            FileUtils.writeFileFromNums(featureVectorsFilename, line, true, true, 1);
                                        }
                                    }
                                }).start();
                                double positiveSum = 0;
                                for (double[] vector : featureVectors) {
                                    double ans = model.predict(vector);
                                    if(ans == 1) {
                                        positiveSum =positiveSum + ans;
                                    }
                                }
                                //看超过正确的比例
                                //featureVectors.length指的是featureVectors的行个数
                                confidence = positiveSum / featureVectors.length;
                               // confidence = model.predict(featureVectors);

                                accTempData = new ArrayList<>();
                                magTempData = new ArrayList<>();
                                gyrTempData = new ArrayList<>();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
                    final double alpha = 0.8;
                    List<Double> linear_acceleration = new ArrayList<>();
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
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
                if(acc!=null&mag!=null&gyr!=null) {
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
                else{
                    Thread.interrupted();
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
