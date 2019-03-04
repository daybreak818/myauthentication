package org.genku.touchauth.Model;

import org.genku.touchauth.Util.AlgorithmUtils;
//import org.genku.touchauth.Util.Jama.Complex;
import org.genku.touchauth.Util.MathUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SensorWindow {

    // Select Features By These Tables
    private static final int[] MEAN_FLAG                = { 1,1,1, 1,1,1, 1,1,1 };
    private static final int[] MEDIAN_FLAG              = { 1,1,1, 1,1,1, 1,1,1 };
    private static final int[] VAR_FLAG                 = { 1,1,1, 1,1,1, 1,1,1 };
    private static final int[] MODE_FLAG                = { 0,0,0, 0,0,0, 0,0,0 };
    private static final int[] SKEWNESS_FLAG            = { 0,0,0, 0,0,0, 0,0,0 };
    private static final int[] KURTOSIS_FLAG            = { 0,0,0, 0,0,0, 0,0,0 };
    private static final int[] MAX_FLAG                 = { 1,1,1,  1,1,1, 1,1,1 };
    private static final int[] MIN_FLAG                 = { 1,1,1, 1,1,1, 1,1,1 };
    private static final int[] NUMOFLOCALPEAKS_FLAG     = { 0,0,0, 0,0,0, 0,0,0 };
    private static final int[] NUMOFLOCALCRESTS_FLAG    = { 0,0,0,  0,0,0, 0,0,0 };
    private static final int[] PRCTILE_FLAG             = { 0,0,0,0,0, 0,0,0,0,0, 0,0,0,0,0,
                                                            0,0,0,0,0, 0,0,0,0,0, 0,0,0,0,0,
                                                            0,0,0,0,0, 0,0,0,0,0, 0,0,0,0,0 };
    private static final int[] COMBSINGNALMAGI_FLAG     = { 0, 0, 0 };
    private static final int[] FFT_FLAG                 = { 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,
                                                            0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,
                                                            0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0 };
    private double[] featureVector;

    public SensorWindow(double[][] acc,  double[][] mag, double[][] gyr) {

        // Shape of data: 12 * length
        // lines: accX1, accX2, ..., accXn
        //        accY1, accY2, ..., accXn
        //        ...
        //        gyrZ1, gyrZ2, ..., gyrZn
        List<double[]> data = preProcessing(acc, mag, gyr);

        int sensorNum = 3;
        int dimensionPreSensor = 3;
        int prctileNum = 4;
        int fftNum = 8;

        double[] mean           = new double[sensorNum * dimensionPreSensor];
        double[] median         = new double[sensorNum * dimensionPreSensor];
        double[] var            = new double[sensorNum * dimensionPreSensor];
        double[] mode           = new double[sensorNum * dimensionPreSensor];
        double[] skewness       = new double[sensorNum * dimensionPreSensor];
        double[] kurtosis       = new double[sensorNum * dimensionPreSensor];
        double[] max            = new double[sensorNum * dimensionPreSensor];
        double[] min            = new double[sensorNum * dimensionPreSensor];
        double[] numOfLocPeaks  = new double[sensorNum * dimensionPreSensor];
        double[] numOfLocCrests = new double[sensorNum * dimensionPreSensor];
        double[] prctile        = new double[sensorNum * dimensionPreSensor * prctileNum];
        double[] combSignalMagi = new double[sensorNum];
        double[] fft            = new double[sensorNum * dimensionPreSensor * fftNum];
        //为什么这里的n是等于mean.length?
        //哦，因为这里面mean的定义和剩下的变量的定义是同维数的，sensorNum=3,dimensionPreSensor=3,9
       int n = mean.length;
        for (int i = 0; i < n; ++i) {
            double[] line = data.get(i);
            mean[i] = MathUtils.mean(line);
            median[i] = MathUtils.median(line);
            var[i] = MathUtils.variance(line, mean[i]);
            max[i] = MathUtils.max(line);
            min[i] = MathUtils.min(line);
            //这是啥？
            mode[i] = MathUtils.mode(line, max[i], min[i]);
            //啥？
            skewness[i] = MathUtils.skewness(line, mean[i], median[i], var[i]);
            //这又是啥？
            kurtosis[i] = MathUtils.kurtosis(line, mean[i], var[i]);
            numOfLocPeaks[i] = MathUtils.numberOfLocalPeaks(line);
            numOfLocCrests[i] = MathUtils.numberOfLocalCrests(line);

            double[] tmpPrctile = MathUtils.prctile(line);
            System.arraycopy(tmpPrctile, 0, prctile, i * prctileNum, prctileNum);

            double[] tmpFFT = MathUtils.fft(line);
            System.arraycopy(tmpFFT, 0, fft, i * fftNum, fftNum);
        }
        //一共三个传感器的三轴处理
        for (int i = 0; i < 3; ++i) {
            combSignalMagi[i] = MathUtils.combSignalMagi(data.get(i * 3), data.get(i * 3 + 1), data.get(i * 3 + 2));
        }

        List<Double> tmpFeatureVectors = new ArrayList<>();

        n = mean.length;
        for (int i = 0; i < n; ++i) {
            if (MEAN_FLAG[i] == 1) {
                tmpFeatureVectors.add(mean[i]);
            }
        }
        n = median.length;
        for (int i = 0; i < n; ++i) {
            if (MEDIAN_FLAG[i] == 1) {
                tmpFeatureVectors.add(median[i]);
            }
        }
        n = var.length;
        for (int i = 0; i < n; ++i) {
            if (VAR_FLAG[i] == 1) {
                tmpFeatureVectors.add(var[i]);
            }
        }

        n = mode.length;
        for (int i = 0; i < n; ++i) {
            if (MODE_FLAG[i] == 1) {
                tmpFeatureVectors.add(mode[i]);
            }
        }
        n = skewness.length;
        for (int i = 0; i < n; ++i) {
            if (SKEWNESS_FLAG[i] == 1) {
                tmpFeatureVectors.add(skewness[i]);
            }
        }
        n = kurtosis.length;
        for (int i = 0; i < n; ++i) {
            if (KURTOSIS_FLAG[i] == 1) {
                tmpFeatureVectors.add(kurtosis[i]);
            }
        }
        n = max.length;
        for (int i = 0; i < n; ++i) {
            if (MAX_FLAG[i] == 1) {
                tmpFeatureVectors.add(max[i]);
            }
        }
        n = min.length;
        for (int i = 0; i < n; ++i) {
            if (MIN_FLAG[i] == 1) {
                tmpFeatureVectors.add(min[i]);
            }
        }
        n = numOfLocPeaks.length;
        for (int i = 0; i < n; ++i) {
            if (NUMOFLOCALPEAKS_FLAG[i] == 1) {
                tmpFeatureVectors.add(numOfLocPeaks[i]);
            }
        }
        n = numOfLocCrests.length;
        for (int i = 0; i < n; ++i) {
            if (NUMOFLOCALCRESTS_FLAG[i] == 1) {
                tmpFeatureVectors.add(numOfLocCrests[i]);
            }
        }
        n = prctile.length;
        for (int i = 0; i < n; ++i) {
            if (PRCTILE_FLAG[i] == 1) {
                tmpFeatureVectors.add(prctile[i]);
            }
        }
        n = combSignalMagi.length;
        for (int i = 0; i < n; ++i) {
            if (COMBSINGNALMAGI_FLAG[i] == 1) {
                tmpFeatureVectors.add(combSignalMagi[i]);
            }
        }
        n = fft.length;
        for (int i = 0; i < n; ++i) {
            if (FFT_FLAG[i] == 1) {
                tmpFeatureVectors.add(fft[i]);
            }
        }

        featureVector = new double[tmpFeatureVectors.size()];
        for (int i = 0; i < tmpFeatureVectors.size(); ++i) {
            featureVector[i] = tmpFeatureVectors.get(i);
        }
    }

    public double[] getFeatureVectors() {
        return featureVector;
    }
    //对数据的预处理
    private List<double[]> preProcessing(double[][] acc, double[][] mag, double[][] gyr) {
        //标准化数据
        acc = setStandard(acc);
     //   ori = setStandard(ori);
        mag = setStandard(mag);
        gyr = setStandard(gyr);
        //构建了一个新的数据然后将标准化的数据都放进去
        List<double[]> data = new ArrayList<>();
        Collections.addAll(data, acc);
       // Collections.addAll(data, ori);
        Collections.addAll(data, mag);
        Collections.addAll(data, gyr);
        return data;
    }
    //标准化的过程--z-score标准化
    private double[][] setStandard(double[][] vectors) {
        //每一种传感器抽取数据时是x,y,z三轴的数据
        for (int i = 0; i < 3; ++i) {
            double mean = 0;
            //计算每一列的数据总和
            for (int j = 0; j < vectors[i].length; ++j) {
                mean += vectors[i][j];
            }
            //计算了每一列的均值
            mean /= vectors[i].length;
            double std = 0;
            for (int j = 0; j < vectors[i].length; ++j) {
                //pow平方
                std += Math.pow(vectors[i][j] - mean, 2);
            }
            //sqrt开平方
            std = Math.sqrt(std / vectors[i].length);
            for (int j = 0; j < vectors[i].length; ++j) {
                vectors[i][j] = (vectors[i][j] - mean) / std;
            }
        }
        return vectors;
    }
}
