package com.zhimatiao.carrot;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ModelUpdateService extends Service {

    private String baseUrl = "https://distribution-bucket.oss-cn-beijing.aliyuncs.com/carrot-app/";
    private String jsonUrl = baseUrl + "model-version.json";

    public ModelUpdateService() {
    }

    public static String get(final String url) {
        final StringBuilder sb = new StringBuilder();
        FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                BufferedReader br = null;
                InputStreamReader isr = null;
                URLConnection conn;
                try {
                    URL geturl = new URL(url);
                    conn = geturl.openConnection();//创建连接
                    conn.connect();//get连接
                    try {
                        isr = new InputStreamReader(conn.getInputStream());//输入流
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    br = new BufferedReader(isr);
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);//获取输入流数据
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {//执行流的关闭
                    if (br != null) {
                        try {
                            if (br != null) {
                                br.close();
                            }
                            if (isr != null) {
                                isr.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return sb.toString();
            }
        });
        new Thread(task).start();
        String s = null;
        try {
            s = task.get();//异步获取返回值
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        get();
    }

    private void broadCast(int status, String data) {
        Intent intent = new Intent();
        intent.putExtra("status", status);
        intent.putExtra("data", data);
        intent.setAction("com.zhimatiao.carrot.action.ALARM");
        sendBroadcast(intent);
    }

    private void get() {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url(jsonUrl).method("GET", null).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                stopSelf();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                broadCast(210001, result);
                stopSelf();
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
