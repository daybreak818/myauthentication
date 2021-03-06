package org.genku.touchauth.Model;

import android.support.annotation.Nullable;
import android.util.Log;

//import libsvm.svm;
//import libsvm.svm_model;
//import libsvm.svm_node;
//import libsvm.svm_parameter;
//import libsvm.svm_problem;
import org.genku.touchauth.libsvm.*;
/**
 * Created by genku on 4/9/2017.
 */

public class SVM {


    private svm_parameter param;
    public svm_model model;

    private svm_problem problem;

    public SVM() {
        param = new svm_parameter();
        //这里已经使用了一类分类器
        param.svm_type = svm_parameter.ONE_CLASS;
        param.kernel_type = svm_parameter.RBF;
        param.degree = 3;
        param.gamma = 0.001;	// 1/num_features
        param.coef0 = 0.0;
        param.nu = 0.001;
        param.cache_size = 200;
        param.C = 1;
        param.eps = 1e-5;
        param.p = 0.1;
        param.shrinking = 1;
        param.probability = 0;
        param.nr_weight = 0;
        param.weight_label = new int[0];
        param.weight = new double[0];


        // param.gamma = 0.5;
        // param.nr_weight = 2;
        // param.cache_size = 1000;
        // param.eps = 0.00001;
        // param.C = 1;
    }

    public SVM(svm_model model_) {
        model = model_;
    }

    public void train(double[][] vectors, double[] labels) {

        param.gamma = 1.0 / vectors[0].length;

        int rows = vectors.length;
        int cols = vectors[0].length;

        // SVM Problem数据集
        svm_node[][] data = new svm_node[rows][cols];
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                data[i][j] = new svm_node();
                data[i][j].index = j + 1;
                data[i][j].value = vectors[i][j];
            }
        }
        svm_problem problem = new svm_problem();
        problem.l = rows;
        problem.x = data;
        problem.y = labels;
        Log.d("Traintag","Train开始");
        model = svm.svm_train(problem, param);
        Log.d("Traintag","Train结束了");
    }

    public double predict(double[] vector) {

        int cols = vector.length;

        svm_node[] data = new svm_node[cols];
        for (int i = 0; i < cols; ++i) {
            data[i] = new svm_node();
            data[i].index = i + 1;
            data[i].value = vector[i];
        }

        //double[] probs = new double[2];
        double label = svm.svm_predict(model, data);

        return label;
    }
    //补充函数
    public double predict1(SVM model_,double[] vector) {

        int cols = vector.length;

        svm_node[] data = new svm_node[cols];
        for (int i = 0; i < cols; ++i) {
            data[i] = new svm_node();
            data[i].index = i + 1;
            data[i].value = vector[i];
        }

        double[] probs = new double[2];
        double label = svm.svm_predict(model, data);

        return label;
    }

    public svm_model getModel() {
        return model;
    }

    public static void save(SVM model_, String filename_) {
        try {
            svm.svm_save_model(filename_, model_.getModel());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public static SVM load(String filename_) {
        try {
            return new SVM(svm.svm_load_model(filename_));
        } catch (Exception e) {
            return null;
        }
    }
}
