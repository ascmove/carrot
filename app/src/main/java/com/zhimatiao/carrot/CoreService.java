package com.zhimatiao.carrot;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.IBinder;

import java.util.ArrayList;

public class CoreService extends Service {

    private static int sampleRateInHz = 16000;
    private static int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int audioSource = MediaRecorder.AudioSource.MIC;
    private int bufferSizeInBytes = 2048;
    private AudioRecord audioRecord;
    private boolean isRecord = false;
    private long[] peaks = new long[3];
    private int requestId = 1;
    private int lock = 0;
    private double sensitivityThreshold = 0.02;
    private double stdMin = 5;
    private double stdMax = 8;

    public CoreService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Thread recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // 创建AudioRecord对象
                audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
                audioRecord.startRecording();
                // 录制状态为true
                isRecord = true;

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
                    readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
                    if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
                        audiodata_len = audiodata.length;
                        for (int i = 0; i < audiodata_len; i += 2) {
                            bytes[0] = audiodata[i];
                            bytes[1] = audiodata[i + 1];
                            if (lists.size() < 8000) {
                                lists.add(byte4ToShortInt(bytes, 0));
                            } else {
                                calculate(lists);
                                lists.clear();
                                lists.add(byte4ToShortInt(bytes, 0));
                            }
                        }
                    }
                }
            }
        });
        recordingThread.setName("ReadRecorderBufferThread");
        recordingThread.start();
    }

    private void calculate(ArrayList lists) {
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
        callBack(avg);
    }

    private void callBack(double avg) {
        broadCast(201, avg);
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
    }
}
