package com.zhimatiao.carrot.mfcc;

/**
 * 预处理过程
 *
 * @author wanggang
 */
public class PreProcess {

    float[] originalSignal;// initial extracted PCM,
    public int noOfFrames;// calculated total no of frames
    int samplePerFrame;// how many samples in one frame
    int framedArrayLength;// how many samples in framed array
    public float[][] framedSignal;
    float[] hammingWindow;
    int samplingRate;

    /**
     * constructor, all steps are called frm here
     *
     * @param originalSignal extracted PCM data
     * @param samplePerFrame how many samples in one frame,=660 << frameDuration, typically
     *                       30; samplingFreq, typically 22Khz
     * @param samplingRate
     */
    public PreProcess(float[] originalSignal, int samplePerFrame, int samplingRate) {
        this.originalSignal = originalSignal;
        this.samplePerFrame = samplePerFrame;
        this.samplingRate = samplingRate;

        normalizePCM();
        doFraming();
        doWindowing();
    }

    private void normalizePCM() {
        float max = originalSignal[0];
        for (int i = 1; i < originalSignal.length; i++) {
            if (max < Math.abs(originalSignal[i])) {
                max = Math.abs(originalSignal[i]);
            }
        }
        for (int i = 0; i < originalSignal.length; i++) {
            originalSignal[i] = originalSignal[i] / max;
        }
    }

    /**
     * divides the whole signal into frames of samplerPerFrame
     */
    private void doFraming() {
        // calculate no of frames, for framing

        noOfFrames = originalSignal.length / samplePerFrame;
        framedSignal = new float[noOfFrames][samplePerFrame];
        for (int i = 0; i < noOfFrames; i++) {
            int startIndex = (i * samplePerFrame);
            for (int j = 0; j < samplePerFrame; j++) {
                framedSignal[i][j] = originalSignal[startIndex + j];
            }
        }
    }

    /**
     * does hamming window on each frame
     */
    private void doWindowing() {
        // prepare hammingWindow
        hammingWindow = new float[samplePerFrame + 1];
        // prepare for through out the data
        for (int i = 1; i <= samplePerFrame; i++) {

            hammingWindow[i] = (float) (0.54 - 0.46 * (Math.cos(2 * Math.PI * i / samplePerFrame)));
        }
        // do windowing
        for (int i = 0; i < noOfFrames; i++) {
            for (int j = 0; j < samplePerFrame; j++) {
                framedSignal[i][j] = framedSignal[i][j] * hammingWindow[j + 1];
            }
        }
    }
}
