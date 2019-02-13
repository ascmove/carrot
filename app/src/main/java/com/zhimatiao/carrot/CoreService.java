package com.zhimatiao.carrot;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.IBinder;

import com.zhimatiao.carrot.mfcc.MFCC;

import java.io.IOException;
import java.util.ArrayList;

import static com.zhimatiao.carrot.CarrotActivity.calType;
import static com.zhimatiao.carrot.CarrotActivity.serviceRuning;

public class CoreService extends Service {

    private static int sampleRateInHz = 16000;
    private static int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private static int bufferSizeInBytes = 2048;
    private static int avgReadSize = 8000;
    private static int svmReadSize = 4096;

    // for function buffer
    private static int bufferLen = svmReadSize;
    private static int bufferP = 0;
    private static float[] norm = new float[bufferLen];
    private static double[] hanning;
    private static int pointStartAt = 0;
    private static double sum = 0;
    private int audioSource = MediaRecorder.AudioSource.DEFAULT;
    private AudioRecord audioRecord;
    private boolean isRecord = false;
    private long[] peaks = new long[3];
    private int requestId = 1;
    private int lock = 0;
    private double sensitivityThreshold = CarrotActivity.sensitivityThresholdDefault;
    private double stdMin = 5;
    private double stdMax = 8;

    public CoreService() {
    }

    private static double[] hanningWin(int len) {
        double[] hanning = new double[len];
        for (int i = 0; i < len; i++) {
            hanning[i] = 0.5 * (1 - Math.cos(2 * Math.PI * (i + 1) / (8192 + 1)));
        }
        return hanning;
    }

    private static double[] applyHanning(double[] norm) {
        int normLength = norm.length;
        int hanningLength = hanning.length;
        for (int i = 0; i < normLength; i++) {
            if (i >= hanningLength) {
                norm[i] = norm[i] * hanning[normLength - i - 1];
            } else {
                norm[i] = norm[i] * hanning[i];
            }
        }
        return norm;
    }

    private static Complex[] toComplexList(double[] arl) {
        int len = arl.length;
        Complex[] src = new Complex[len];
        for (int i = 0; i < len; i++) {
            src[i] = new Complex(arl[i], 0);
        }
        return src;
    }

    private static double[] getFeature(Complex[] dst) {
        double[] feature = new double[40];
        double sum = 0;
        for (int f = 0; f < 40; f++) {
            sum = 0;
            for (int i = 100 * f; i < 100 * f + 100; i++) {
                sum += Math.log10(dst[i].abs());
            }
            feature[f] = sum / 100;
        }
        return feature;
    }

    private static String dumpFeature(double[] f) {
        String s = "";
        for (double t:f) {
            s += " " + String.format("%.4f", t);
        }
        return s;
    }

    private static int byte4ToInt(byte[] bytes, int off) {
        int s = 0;
        short b0 = (short) (bytes[off] & 0xff);
        short b1 = (short) (bytes[off + 1] & 0xff);
        b1 <<= 8;
        s = (int) (b0 | b1);
        return s;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (serviceRuning == 0) {
            if (CarrotActivity.noticeMode == 1) {
                if (CarrotActivity.headsetHasMic == 0) {
                    audioSource = MediaRecorder.AudioSource.CAMCORDER;
                } else {
                    sensitivityThreshold = CarrotActivity.sensitivityThresholdHeadset;
                }
            }
            if (calType.equals("svm")) {
                hanning = hanningWin(bufferLen / 2);
            }
            Thread recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // 创建AudioRecord对象
                    audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
                    try {
                        audioRecord.startRecording();
                    } catch (IllegalStateException e) {
                        stopSelf();
                    }
                    // 录制状态为true
                    isRecord = true;
                    // 录制状态为true
                    serviceRuning = 1;

                    // https://blog.csdn.net/ownwell/article/details/8114121/
                    // new一个byte数组用来存一些字节数据，大小为缓冲区大小
                    byte[] audiodata = new byte[bufferSizeInBytes];
                    int readsize = 0;
                    int audiodata_len = 0;
                    byte bytes[] = new byte[2];
                    ArrayList lists = new ArrayList();
                    //发送广播
                    broadCast(200);
                    while (isRecord == true) {
                        try {
                            readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
                        } catch (Exception e) {
                            stopSelf();
                        }
                        if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
                            audiodata_len = audiodata.length;
                            for (int i = 0; i < audiodata_len; i += 2) {
                                bytes[0] = audiodata[i];
                                bytes[1] = audiodata[i + 1];
                                if (calType.equals("std")) {
                                    if (lists.size() < avgReadSize) {
                                        lists.add(byte4ToShortInt(bytes, 0));
                                    } else {
                                        calculateAvg(lists);
                                        lists.clear();
                                        lists.add(byte4ToShortInt(bytes, 0));
                                    }
                                } else if (calType.equals("svm")) {
                                    calculateSvm(byte4ToInt(bytes, 0));
                                }
                            }
                        }
                    }
                }
            });
            recordingThread.setName("ReadRecorderBufferThread");
            recordingThread.start();
        } else {
            stopSelf();
        }
    }

    /**
     * std
     *
     * @param lists
     */
    private void calculateAvg(ArrayList lists) {
        double sum = 0;
        short listsGet = 0;
        int listsGetInt;
        double avg = 0;
        int len = lists.size();
        for (int i = 0; i < len; i += 1) {
            listsGet = (short) lists.get(i);
            listsGetInt = listsGet;
            sum += Math.abs(listsGetInt);
        }
        avg = sum / len / 32768;
        callBackAvg(avg);
    }

    private void calculateSvm(float ibytes) {
        // 逐字节调用
        norm[bufferP] = ibytes;
        sum += Math.abs(norm[bufferP]);
        bufferP++;
        pointStartAt++;
//        Utils.fileDumper(CarrotActivity.getContext(), "wav", pointStartAt + " " + ibytes);
        // 窗长度bufferLen
        if (bufferP >= bufferLen) {
            double avg = sum / bufferLen / 32768;
            if (CarrotActivity.doNotEditView == false) {
                broadCast(201, avg);
            }
            if (avg >= sensitivityThreshold) {
//            if (true) {
//                for (double f:norm) {
//                    Utils.fileDumper(CarrotActivity.getContext(), "wewee", f+"");
//                }
//                double[] f = new double[0];
                double[] f = MFCC.get(norm);
                try {
                    double v = Utils.svmPredict(f, CarrotActivity.svm_model_path);
                    if (v > 0) {
                        broadCast(800);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                String s = dumpFeature(f);
//                Utils.fileDumper(CarrotActivity.getContext(), "fet", (pointStartAt - bufferLen / 2) + s);
//                System.out.println((pointStartAt-bufferLen/2)+s);
            }
//            System.out.println(pointStartAt);
            // 淡入半个窗长度
            System.arraycopy(norm, bufferLen / 2, norm, 0, bufferLen / 2);
            bufferP = bufferLen / 2;
            sum = 0;
        }
    }

    private void callBackAvg(double avg) {
        if (CarrotActivity.doNotEditView == false) {
            broadCast(201, avg);
        }
        if (avg >= sensitivityThreshold && lock <= 0) {
            lock = 6;
            peaks[0] = peaks[1];
            peaks[1] = peaks[2];
            peaks[2] = requestId;
            double mean = (peaks[0] + peaks[1] + peaks[2]) / 3;
            double dst = Math.pow(peaks[0] - mean, 2) + Math.pow(peaks[1] - mean, 2) + Math.pow(peaks[2] - mean, 2);
            double std = Math.sqrt(dst / 3);
            if (std >= stdMin && std <= stdMax) {
                broadCast(800);
            }
        }
        lock--;
        requestId++;
        if (requestId >= 72000) {
            requestId = 0;
        }
    }

    /**
     * byte数组转换为short int整数
     *
     * @param bytes byte数组
     * @param off   开始位置
     * @return int整数
     */
    private short byte4ToShortInt(byte[] bytes, int off) {
        short s = 0;
        short b0 = (short) (bytes[off] & 0xff);
        short b1 = (short) (bytes[off + 1] & 0xff);
        b1 <<= 8;
        s = (short) (b0 | b1);
        return s;
    }

    private void broadCast(int status) {
        Intent intent = new Intent();
        intent.putExtra("status", status);
        intent.setAction("com.zhimatiao.carrot.action.ALARM");
        sendBroadcast(intent);
    }

    private void broadCast(int status, double data) {
        Intent intent = new Intent();
        intent.putExtra("status", status);
        intent.putExtra("data", data);
        intent.setAction("com.zhimatiao.carrot.action.ALARM");
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        if (audioRecord != null) {
            isRecord = false;//停止文件写入
            audioRecord.stop();
            audioRecord.release();//释放资源
            audioRecord = null;
        }
        broadCast(301);
        // 录制状态为true
        serviceRuning = 0;
    }
}
