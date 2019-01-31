package com.zhimatiao.carrot;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class AnalyseService extends Service {

    private long[] peaks = new long[3];
    private int requestId = 1;
    private int lock = 0;
    private double sensitivityThreshold = 0.02;
    private double stdMin = 5;
    private double stdMax = 8;

    public AnalyseService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String sd_card = Environment.getExternalStorageDirectory().toString();
//                String file = sd_card + "/carrot/2m.16k.wav";
//                String file = sd_card + "/carrot/44m.16k.wav";
                String file = sd_card + "/carrot/14m.wav";
                try {
                    InputStream fileIn = new FileInputStream(file);
                    BufferedInputStream in = new BufferedInputStream(fileIn, 1048576);
                    byte bytes[] = new byte[2];
                    int window_size = 8000;
                    int iter = 0;
                    int sumall = 0;
                    while (in.read(bytes) != -1) {
                        int www = Math.abs(byte4ToInt(bytes, 0));
                        sumall += www;
                        iter++;
                        if (iter >= window_size) {
                            iter = 0;
                            callBack(((float) sumall / window_size) / 32768);
                            sumall = 0;
                        }
                    }
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void callBack(double avg) {
        if (avg >= sensitivityThreshold && lock <= 0) {
            lock = 6;
            peaks[0] = peaks[1];
            peaks[1] = peaks[2];
            peaks[2] = requestId;
            double mean = (peaks[0]+peaks[1]+peaks[2])/3;
            double dst = Math.pow(peaks[0]-mean,2)+Math.pow(peaks[1]-mean,2)+Math.pow(peaks[2]-mean,2);
            double std = Math.sqrt(dst/3);
            if (std >= stdMin && std <= stdMax) {
                Intent intent = new Intent();
                intent.putExtra("status", 205);
                intent.setAction("com.zhimatiao.carrot.action.ALARM");
                sendBroadcast(intent);
            }
        }
        lock--;
        requestId++;
        if (requestId >= 72000) {
            requestId = 0;
        }
    }

    /**
     * byte数组转换为int整数
     *
     * @param bytes byte数组
     * @param off   开始位置
     * @return int整数
     */
    private int byte4ToInt(byte[] bytes, int off) {
        int s = 0;
        short b0 = (short) (bytes[off] & 0xff);
        short b1 = (short) (bytes[off + 1] & 0xff);
        b1 <<= 8;
        s = (int) (b0 | b1);
        return s;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //发送广播
        Intent intent = new Intent();
        intent.putExtra("status", 500);
        intent.setAction("com.zhimatiao.carrot.action.ALARM");
        sendBroadcast(intent);
    }

    public class Binder extends android.os.Binder {
        public AnalyseService getService() {
            return AnalyseService.this;
        }
    }
}
