package com.zhimatiao.carrot;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Downloader {

    private static Downloader downloader;
    private final OkHttpClient okHttpClient;

    private Downloader() {
        okHttpClient = new OkHttpClient();
    }

    public static Downloader getInstance() {
        if (downloader == null) {
            downloader = new Downloader();
        }
        return downloader;
    }

    /**
     * get请求
     *
     * @param address
     * @param callback
     */

    public void get(String address, okhttp3.Callback callback) {
        OkHttpClient client = new OkHttpClient();
        FormBody.Builder builder = new FormBody.Builder();
        FormBody body = builder.build();
        Request request = new Request.Builder()
                .url(address)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void download(final Context mCarrotActivity, final String url, final String modelName, final OnDownloadListener listener) {
        Downloader.getInstance().get(url, new okhttp3.Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                listener.onDownloadFailed();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;//输入流
                FileOutputStream fos = null;//输出流
                try {
                    is = response.body().byteStream();//获取输入流
                    if (is != null) {
                        File file = new File(mCarrotActivity.getFilesDir(), modelName);
                        fos = new FileOutputStream(file);
                        byte[] buf = new byte[1024];
                        int ch = -1;
                        while ((ch = is.read(buf)) != -1) {
                            fos.write(buf, 0, ch);
                        }
                    }
                    fos.flush();
                    // 下载完成
                    if (fos != null) {
                        fos.close();
                    }
                    listener.onDownloadSuccess();
                } catch (Exception e) {
                    listener.onDownloadFailed();
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                    }
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (IOException e) {
                    }
                }
            }
        });
    }

    public interface OnDownloadListener {
        void onDownloadSuccess();

        void onDownloadFailed();
    }
}