package org.genku.touchauth.Model;

import org.genku.touchauth.Util.DataUtils;

import java.util.List;

/**
 * Created by genku on 4/22/2017.
 */

public class SensorFeatureExtraction {

    //每秒50个特征向量，一个窗口100个向量，一个窗口采集2秒
    private static int VECTORS_PER_SECOND = 50;
    private static int VECTORS_PER_WINDOW = 100;
    private static double STEP_OFFSET = 0.5;

    //list类型转换成数组类型
    public static double[][] extract(double totalTime, double windowTime,
                                     List<List<Double>> accelerometer,
                                     List<List<Double>> magnetic,
                                     List<List<Double>> gyroscope) {
        return extract(totalTime, windowTime,
                DataUtils.listToArray(accelerometer),
                DataUtils.listToArray(magnetic),
                DataUtils.listToArray(gyroscope));
    }
    //提取所有窗口的特征
    public static double[][] extract(double totalTime, double windowTime,
                                     double[][] accelerometer,
                                     double[][] magnetic,
                                     double[][] gyroscope) {
        //先获得该组数据的窗口
        SensorWindow[] windows = getWindows(totalTime,
                windowTime, accelerometer, magnetic, gyroscope);
        //System.out.println("特征矩阵"+getAllFeatureVectors(windows));
        return getAllFeatureVectors(windows);
    }
    //划分窗口，这个totaltime和windowtime指代的是什么？
    //totaltime是我捕获的几组数据的总时间，windowstime是窗口时间。
    private static SensorWindow[] getWindows(double totalTime,
                                     double windowTime,
                                     double[][] accelerometer,
                                     double[][] magnetic,
                                     double[][] gyroscope) {

        int accVectorsPerWindow = (int) (windowTime * accelerometer.length / totalTime);
        int magVectorsPerWindow = (int) (windowTime * magnetic.length / totalTime);
        int gyrVectorsPerWindow = (int) (windowTime * gyroscope.length / totalTime);
        //STEP_OFFSET抵消
        int k = (int) (1 + (totalTime - windowTime) / (windowTime * (1 - STEP_OFFSET)));
        SensorWindow[] windows = new SensorWindow[k];
        for (int i = 0; i < k; ++i) {

            int accLeft = (int) (i * accVectorsPerWindow * (1- STEP_OFFSET));
            int accRight = i != k - 1 ? accLeft + accVectorsPerWindow - 1 : accelerometer.length - 1;

            int magLeft = (int) (i * magVectorsPerWindow * (1- STEP_OFFSET));
            int magRight = i != k - 1 ? magLeft + magVectorsPerWindow - 1 : magnetic.length - 1;

            int gyrLeft = (int) (i * gyrVectorsPerWindow * (1- STEP_OFFSET));
            int gyrRight = i != k - 1 ? gyrLeft + gyrVectorsPerWindow - 1 : gyroscope.length - 1;

            double[][] tempAcc = new double[3][accRight - accLeft + 1];
            double[][] tempMag = new double[3][magRight - magLeft + 1];
            double[][] tempGyr = new double[3][gyrRight - gyrLeft + 1];

            for (int a = 0; a < 3; ++a) {
                for (int b = accLeft, c = 0; b <= accRight; ++b, ++c) {
                    tempAcc[a][c] = accelerometer[b][a + 1];
                }
                for (int b = magLeft, c = 0; b <= magRight; ++b, ++c) {
                    tempMag[a][c] = magnetic[b][a + 1];
                }
                for (int b = gyrLeft, c = 0; b <= gyrRight; ++b, ++c) {
                    tempGyr[a][c] = gyroscope[b][a + 1];
                }
            }

            windows[i] = new SensorWindow(tempAcc, tempMag , tempGyr);
        }

        return windows;
    }

    private static double[][] getAllFeatureVectors(SensorWindow[] windows) {
        int N = windows.length;
        int M = windows[0].getFeatureVectors().length;
        double[][] ans = new double[N][M];
        for (int i = 0; i < N; ++i) {
            double[] temp = windows[i].getFeatureVectors();
            //System.out.println(ans);
            //将一个数组的指定个数元素复制到另一个数组中
            //将temp中索引从0开始的元素复制到ans[i]中索引从0开始的元素，复制元素的个数为M个
            System.arraycopy(temp, 0, ans[i], 0, M);
        }
       // System.out.println(ans);
        return ans;
    }
}
