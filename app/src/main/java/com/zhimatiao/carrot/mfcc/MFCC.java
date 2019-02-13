package com.zhimatiao.carrot.mfcc;

import java.util.ArrayList;
import java.util.List;

import com.zhimatiao.carrot.mfcc.feature.FeatureVector;

public class MFCC {

    private static final int SAMPLING_RATE = 16000; // (int) fc.getRate();
    private static final int SAMPLE_PER_FRAME = 512; // 512,23.22ms
    private static final int FEATURE_DIMENSION = 39;
    private FeatureExtract featureExtract;
    private PreProcess prp;
    private List<double[]> allFeaturesList = new ArrayList<double[]>();

    public MFCC() {
    }

    /**
     * 主函数
     */
    public static double[] get(float[] fff) {
        MFCC mfcc = new MFCC();
        return mfcc.writeFeaturesIris(fff);
    }

    public double[] writeFeaturesIris(float[] fff) {
        return getFeature(fff);
    }

    /**
     * 提取单个音频的特征数据
     */
    private double[] getFeature(float[] fff) {
        int totalFrames = 0;
        FeatureVector feature = extractFeatureFromFile(fff);
        for (int k = 0; k < feature.getNoOfFrames(); k++) {
            allFeaturesList.add(feature.getFeatureVector()[k]);
            totalFrames++;
        }
        // 行代表帧数，列代表特征
        double allFeatures[][] = new double[totalFrames][FEATURE_DIMENSION];
        for (int i = 0; i < totalFrames; i++) {
            double[] tmp = allFeaturesList.get(i);
            allFeatures[i] = tmp;
        }
        // 计算每帧对应特征的平均值
        double avgFeatures[] = new double[FEATURE_DIMENSION];
        for (int j = 0; j < FEATURE_DIMENSION; j++) { // 循环每列
            double tmp = 0.0d;
            for (int i = 0; i < totalFrames; i++) { // 循环每行
                tmp += allFeatures[i][j];
            }
            avgFeatures[j] = tmp / totalFrames;
        }
        return avgFeatures;
    }

    private FeatureVector extractFeatureFromFile(float[] arrAmp) {
        prp = new PreProcess(arrAmp, SAMPLE_PER_FRAME, SAMPLING_RATE);
        featureExtract = new FeatureExtract(prp.framedSignal, SAMPLING_RATE, SAMPLE_PER_FRAME);
        featureExtract.makeMfccFeatureVector();
        return featureExtract.getFeatureVector();
    }

}
