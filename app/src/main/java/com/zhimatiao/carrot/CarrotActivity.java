package com.zhimatiao.carrot;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class CarrotActivity extends Activity {

    // 默认配置
    static final boolean onStartVibratorEnable = true;
    static final boolean onEndVibratorEnable = true;
    static final String PRIVATE_KEY = "LZXYeLsCtfqrLRovecveG6DxtOFtC1KV";
    static final String calType = "std";
    // 存入 assets中
    static String default_svm_model = "SM201902122125";
    static String svm_model_path = "";
    static final double sensitivityThresholdDefault = 0.02;
    static final double sensitivityThresholdHeadset = 0.05;

    static final int REQUEST_CODE_RECORD_AUDIO = 670049;
    static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 670050;
    PlayerManager playerManager;
    // 0 手机振动 1 耳机声音
    static int noticeMode = 0;
    static int headsetHasMic = 0;
    static int serviceRuning = 0;
    static CarrotActivity mCarrotActivity;
    static boolean featureLogEnable = false;
    static boolean doNotEditView = false;
    private int requestAudioStatus = 0;
    private int requestWriteStatus = 0;
    private TextView mTextView = null;
    private TextView mTextViewSensitivityAvg = null;
    private TextView mTextViewModelName = null;
    private TextView mTextViewNoticeMode = null;
    private TextView mTextViewServiceStatus = null;
    private Button mButton = null;
    private Button mButtonStop = null;
    private CheckBox runLogCheckbox = null;
    private MsgReceiver msgReceiver;
    private Intent intentAnalyseService;
    private Intent intentCoreService;
    private FileWriter fw = null;
    private BufferedWriter writer = null;

    // 共享变量
    static String logsPath = "";

    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrot);

        mCarrotActivity = this;
        CrashReport.initCrashReport(getApplicationContext(), "125efe2697", false);

        //动态注册广播接收器
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.zhimatiao.carrot.action.ALARM");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        intentFilter.addAction("BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED");
        registerReceiver(msgReceiver, intentFilter);

        if (ContextCompat.checkSelfPermission(this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAudioStatus = 1;
        }
        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestWriteStatus = 1;
        }

        mButton = (Button) findViewById(R.id.button);
        mButtonStop = (Button) findViewById(R.id.button_stop);
        mTextView = (TextView) findViewById(R.id.textview_service_run);
        mTextViewSensitivityAvg = (TextView) findViewById(R.id.textview_sensitivity_avg);
        mTextViewModelName = (TextView) findViewById(R.id.textview_model_vername);
        mTextViewNoticeMode = (TextView) findViewById(R.id.textview_notice_mode);
        mTextViewServiceStatus = (TextView) findViewById(R.id.textview_service_status);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickButtonRun();
            }
        });
        mButtonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickButtonStop();
            }
        });
        runLogCheckbox = (CheckBox) findViewById(R.id.run_log_checkBox);
        //给CheckBox设置事件监听
        runLogCheckbox.setChecked(getSharedPreferences("featureLogState", false));
        runLogCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                featureLogEnable = isChecked;
                setSharedPreferences("featureLogState", isChecked);
            }
        });
        if (calType.equals("svm") && isNetworkConnected(mCarrotActivity)) {
            intentAnalyseService = new Intent(CarrotActivity.this, ModelUpdateService.class);
            startService(intentAnalyseService);
        }
        playerManager = PlayerManager.getManager();
        String modelName = getSharedPreferences("verName", default_svm_model);
        mTextViewModelName.setText("模型版本：" + modelName);
        svm_model_path = getSharedPreferences("svmModelPath", "");
        if (svm_model_path.equals("")) {
            // 载入内置assets模型
            Utils.copyAssetsModel(mCarrotActivity, default_svm_model);
            svm_model_path = mCarrotActivity.getFilesDir().getAbsolutePath() + File.separator + default_svm_model;
            setSharedPreferences("svmModelPath", svm_model_path);
        }

        // 初始化共享静态变量 logsPath
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String time = formatter.format(new Date());
        String fileName = "/app-" + time + ".log";
        logsPath = mCarrotActivity.getExternalFilesDir("logs").getPath() + fileName;
        // 初始化共享静态变量 requestAudioStatus
        if (ContextCompat.checkSelfPermission(this, RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            requestAudioStatus = 1;
        }
        // 初始化共享静态变量 featureLogEnable
        featureLogEnable = getSharedPreferences("featureLogState", false);
    }

    private void onClickButtonRun() {
        if (ContextCompat.checkSelfPermission(this, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{RECORD_AUDIO}, REQUEST_CODE_RECORD_AUDIO);
            return;
        }
//        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
//            return;
//        }
//        final EditText et = new EditText(this);
//        new AlertDialog.Builder(this).setTitle("请输入模型标识")
//                .setView(et)
//                .setPositiveButton("载入", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        //按下确定键后的事件
//                        Toast.makeText(getApplicationContext(), et.getText().toString(), Toast.LENGTH_LONG).show();
//                    }
//                }).setNegativeButton("取消", null).show();
        if (requestAudioStatus != 1) {
            Alerter.showRecordPermissionAlerts(mCarrotActivity, new Alerter.showRecordPermissionAlertsInterface() {
                @Override
                public void requestPermission() {
                    requestPermissions(new String[]{RECORD_AUDIO}, REQUEST_CODE_RECORD_AUDIO);
                }
            });
            return;
        }
        if (serviceRuning == 0) {
            intentCoreService = new Intent(CarrotActivity.this, CoreService.class);
            startService(intentCoreService);
            logStart();
            runLogCheckbox.setClickable(false);
            mTextView.setText("启动中");
            if (onStartVibratorEnable) {
                long[] pattern = {10, 50, 50, 50, 50}; // OFF/ON/OFF/ON
                vibrator(pattern);
            }
        } else {
            Toast.makeText(mCarrotActivity, "服务已启动", Toast.LENGTH_LONG).show();
        }
    }

    private void onClickButtonStop() {
        if (serviceRuning == 1) {
            stopService(intentCoreService);
            logEnd();
            runLogCheckbox.setClickable(true);
            serviceRuning = 0;
            if (onEndVibratorEnable) {
                long[] pattern = {10, 200}; // OFF/ON/OFF/ON
                vibrator(pattern);
            }
        } else {
            Toast.makeText(mCarrotActivity, "服务未启动", Toast.LENGTH_LONG).show();
        }
    }

    private void logStart() {
        if (featureLogEnable) {
            if (writer == null && fw == null) {
                DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:MM:DD");
                String time = formatter.format(new Date());
                String fileName = "/app-" + time + ".log";
                try {
                    File f = new File(mCarrotActivity.getExternalFilesDir("logs").getPath() + fileName);
                    if (!f.exists()) {
                        f.createNewFile();
                    }
                    fw = new FileWriter(f, true);
                    writer = new BufferedWriter(fw, 1024);
                    writer.append("Carrot running log start:\r\n");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void logEnd() {
        if (featureLogEnable) {
            if (writer != null && fw != null) {
                try {
                    writer.flush();
                    fw.close();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                writer = null;
                fw = null;
            }
        }
    }

    private void logEvent(String s) {
        if (featureLogEnable) {
            if (writer != null && fw != null) {
                try {
                    writer.append(s + "\r\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean getSharedPreferences(String key, boolean defaults) {
        SharedPreferences sp = mCarrotActivity.getPreferences(MODE_PRIVATE);
        return sp.getBoolean(key, defaults);
    }

    public String getSharedPreferences(String key, String defaults) {
        SharedPreferences sp = mCarrotActivity.getPreferences(MODE_PRIVATE);
        return sp.getString(key, defaults);
    }

    public void setSharedPreferences(String key, boolean value) {
        SharedPreferences sp = this.getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public void setSharedPreferences(String key, String value) {
        SharedPreferences sp = this.getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public void setSharedPreferences(String key, int value) {
        SharedPreferences sp = this.getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private void vibrator(long[] pattern) {
        Vibrator vibrator = (Vibrator) mCarrotActivity.getSystemService(mCarrotActivity.VIBRATOR_SERVICE);
        vibrator.vibrate(pattern, -1);
    }

    private void updateModel(String json) {
        JSONObject jsonObject = null;
        try {
            jsonObject = JSON.parseObject(json);
            final String verCode = jsonObject.getString("verCode");
            final String signature = jsonObject.getString("signature");
            final String modelSign = jsonObject.getString("modelSign");
            final String verName = jsonObject.getString("verName");
            final String downLoadUrl = jsonObject.getString("path");
            String verCodeLocal = getSharedPreferences("verCodeLocal", "1");
            String modelIgnoreVer = getSharedPreferences("modelIgnoreVer", "0");
            if (!signature.equals(Utils.getStringSignatureSHA256(verName + verCode + downLoadUrl + modelSign + PRIVATE_KEY))) {
                return;
            }
            if (Integer.parseInt(modelIgnoreVer) < Integer.parseInt(verCode) && Integer.parseInt(verCodeLocal) < Integer.parseInt(verCode)) {
                setSharedPreferences("verName", verName);
                setSharedPreferences("downLoadUrl", downLoadUrl);
                Alerter.hasNewModelAlert(mCarrotActivity, new Alerter.hasNewModelAlertInterface() {
                    @Override
                    public void newModelDownload() {
                        Downloader.getInstance().download(mCarrotActivity, downLoadUrl, verName, new Downloader.OnDownloadListener() {
                            @Override
                            public void onDownloadSuccess() {
                                Looper.prepare();
                                File file = new File(mCarrotActivity.getFilesDir(), verName);
                                String sig = Utils.getFileSignatureSHA256(file);
                                if (sig.equals(modelSign)) {
                                    Toast.makeText(CarrotActivity.this, "更新完成", Toast.LENGTH_LONG).show();
                                    setSharedPreferences("currentModelName", verName);
                                    setSharedPreferences("verCodeLocal", Integer.parseInt(verCode) + "");
                                    default_svm_model = verName;
                                    svm_model_path = mCarrotActivity.getFilesDir().getAbsolutePath() + File.separator + default_svm_model;
                                    setSharedPreferences("svmModelPath", svm_model_path);
                                    Intent intent = new Intent();
                                    intent.putExtra("status", 210319);
                                    intent.putExtra("data", "模型版本：" + verName);
                                    intent.setAction("com.zhimatiao.carrot.action.ALARM");
                                    sendBroadcast(intent);
                                } else {
                                    file.delete();
                                    Toast.makeText(CarrotActivity.this, "模型文件校验失败", Toast.LENGTH_LONG).show();
                                }
                                Looper.loop();
                            }

                            @Override
                            public void onDownloadFailed() {
                                Looper.prepare();
                                Toast.makeText(CarrotActivity.this, "更新失败", Toast.LENGTH_LONG).show();
                                Looper.loop();
                            }
                        });

                    }

                    @Override
                    public void newModelIgnore() {
                        setSharedPreferences("modelIgnoreVer", verCode);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_RECORD_AUDIO) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestAudioStatus = 1;
            }
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                requestAudioStatus = -1;
                Alerter.showRecordPermissionAlerts(mCarrotActivity, new Alerter.showRecordPermissionAlertsInterface() {
                    @Override
                    public void requestPermission() {
                        requestPermissions(new String[]{RECORD_AUDIO}, REQUEST_CODE_RECORD_AUDIO);
                    }
                });
            }
        }
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestWriteStatus = 1;
            }
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                requestWriteStatus = -1;
                Alerter.showWriteSDPermissionAlerts(mCarrotActivity, new Alerter.showWriteSDPermissionAlertsInterface() {
                    @Override
                    public void requestPermission() {
                        requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(msgReceiver);
        super.onDestroy();
        logEnd();
    }

    /**
     * 获取APP的Context方便其他地方调用
     *
     * @return
     */
    public static Context getContext() {
        return mCarrotActivity;
    }

    /**
     * 广播接收器
     *
     * @author len
     */
    public class MsgReceiver extends BroadcastReceiver {
        //必须要重载的方法，用来监听是否有广播发送
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("com.zhimatiao.carrot.action.ALARM")) {
                int status = intent.getIntExtra("status", 0);
                if (status == 800) {
                    String timestamp = System.currentTimeMillis() + "";
                    logEvent(timestamp + " -1 1");
                    if (noticeMode == 0) {
                        // 震动提示
                        Thread vibratorThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                stopService(intentCoreService);
                                long[] pattern = {10, 200, 300, 200, 300}; // OFF/ON/OFF/ON
                                vibrator(pattern);
                                try {
                                    Thread.sleep(1410);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if (serviceRuning == 0) {
                                    startService(intentCoreService);
                                }
                            }
                        });
                        vibratorThread.setName("VibratorThread");
                        vibratorThread.start();
                    } else if (noticeMode == 1) {
                        // 耳机声音提示
                        Uri notification = Uri.parse("android.resource://" + mCarrotActivity.getPackageName() + "/" + R.raw.crystal_drop);
                        playerManager.play(notification);
                    }
                }
                if (status == 200 && doNotEditView == false) {
                    mTextView.setText("已启动");
                }
                if (status == 201) {
                    double avg = intent.getDoubleExtra("data", 0.00);
                    String timestamp = System.currentTimeMillis() + "";
                    logEvent(timestamp + " " + String.format("%.6f", avg) + " 0");
                    if (doNotEditView == false) {
                        mTextViewServiceStatus.setText("特征强度：" + String.format("%.4f", avg));
                    }
                }
                if (status == 301 && doNotEditView == false) {
                    mTextView.setText("服务未启动");
                    mTextViewServiceStatus.setText("特征强度：--");
                }
                if (status == 210001) {
                    String data = intent.getStringExtra("data");
                    updateModel(data);
                }
                if (status == 210319) {
                    String data = intent.getStringExtra("data");
                    mTextViewModelName.setText(data);
                }
            }
            if (action.equals("android.intent.action.SCREEN_OFF")) {
                doNotEditView = true;
            }
            if (action.equals("android.intent.action.SCREEN_ON")) {
                doNotEditView = false;
            }
            if (action.equals("android.intent.action.HEADSET_PLUG") || action.equals("BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED")) {
                // https://www.cnblogs.com/android-html5/archive/2012/03/11/2534085.html
                // todo 暂不支持蓝牙耳机
                // 0代表拔出，1代表插入
                if (serviceRuning == 1) {
                    stopService(intentCoreService);
                    startService(intentCoreService);
                }
                int state = intent.getIntExtra("state", 0);
                headsetHasMic = intent.getIntExtra("microphone", 0);
                if (headsetHasMic == 1) {
                    mTextViewSensitivityAvg.setText("灵敏度α：" + sensitivityThresholdHeadset);
                }
                if (state == 0) {
                    noticeMode = 0;
                    mTextViewNoticeMode.setText("提醒方式：机身震动");
                    mTextViewSensitivityAvg.setText("灵敏度α：" + sensitivityThresholdDefault);
                } else if (state == 1) {
                    if (!getSharedPreferences("safetyAlertReaded", false)) {
                        Alerter.safetyAlert(mCarrotActivity, new Alerter.safetyAlertInterface() {
                            @Override
                            public void iknow() {
                                setSharedPreferences("safetyAlertReaded", true);
                                noticeMode = 1;
                                mTextViewNoticeMode.setText("提醒方式：耳机铃声");
                            }
                        });
                    } else {
                        noticeMode = 1;
                        mTextViewNoticeMode.setText("提醒方式：耳机铃声");
                    }
                }
            }
        }
    }
}
