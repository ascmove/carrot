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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tencent.bugly.crashreport.CrashReport;

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
    static final String lockAppUrl = "https://www.jianshu.com/p/5ab77961fef1";
    // 参数配置
    static final double sensitivityThresholdDefault = 0.02;
    static final double sensitivityThresholdHeadset = 0.05;
    static final int REQUEST_CODE_RECORD_AUDIO = 670049;
    static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 670050;
    // 存入 assets中
    static String default_svm_model = "SM201902122125";
    static String svm_model_path = "";
    static boolean noiseAutoMode = false;
    static int noiseAutoNum = 340; // 约3分钟
    static double noiseRate = 0.99;
    static double sensitivityThresholdAuto = 0;
    // 0 手机振动 1 耳机声音
    static int noticeMode = 0;
    static int lastNoticeMode = 0;
    static int headsetHasMic = 0;
    static int serviceRuning = 0;
    static CarrotActivity mCarrotActivity;
    static boolean featureLogEnable = false;
    static boolean doNotEditView = false;
    // 共享变量
    static String logsPath = "";
    public double serviceStopTime = System.currentTimeMillis();
    PlayerManager playerManager;
    private int requestAudioStatus = 0;
    private int requestWriteStatus = 0;
    private TextView mTextView = null;
    private TextView mTextViewSensitivityAvg = null;
    private TextView mTextViewModelName = null;
    private TextView mTextViewNoticeMode = null;
    private TextView mTextViewInputSrc = null;
    private TextView mTextViewServiceStatus = null;
    private Button mButton = null;
    private Button mButtonStop = null;
    private CheckBox runLogCheckbox = null;
    private CheckBox noiseAutoCheckbox = null;
    private MsgReceiver msgReceiver;
    private FileWriter fw = null;
    private Boolean traceMode = false;

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

    /**
     * 获取APP的Context方便其他地方调用
     *
     * @return
     */
    public static Context getContext() {
        return mCarrotActivity;
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        boolean appKillHistory = getSharedPreferences("appKillHistory", false);
        setSharedPreferences("appKillHistory", false);
        if (appKillHistory) {
            Alerter.appKillAlerts(mCarrotActivity, new Alerter.appKillAlertsInterface() {
                @Override
                public void ignore() {
                    setSharedPreferences("appKillHistory", false);
                }

                @Override
                public void jumpActivity() {
                    setSharedPreferences("appKillHistory", false);
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    Uri content_url = Uri.parse(lockAppUrl);
                    intent.setData(content_url);
                    startActivity(intent);
                }
            });
        }

        if (serviceRuning == 1) {
            mTextView.setText(R.string.service_is_running);
        }
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
        TextView title = (TextView) findViewById(R.id.textview_title);
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                Uri content_url = Uri.parse(lockAppUrl);
                intent.setData(content_url);
                startActivity(intent);
            }
        });

        mButton = (Button) findViewById(R.id.button);
        mButtonStop = (Button) findViewById(R.id.button_stop);
        mTextView = (TextView) findViewById(R.id.textview_service_run);
        mTextViewSensitivityAvg = (TextView) findViewById(R.id.textview_sensitivity_avg);
        mTextViewModelName = (TextView) findViewById(R.id.textview_model_vername);
        mTextViewNoticeMode = (TextView) findViewById(R.id.textview_notice_mode);
        mTextViewInputSrc = (TextView) findViewById(R.id.textview_input_src);
        mTextViewServiceStatus = (TextView) findViewById(R.id.textview_service_status);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickButtonRun();
            }
        });
        mButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
//                long[] pattern = {10, 1000, 300}; // OFF/ON/OFF/ON
//                vibrator(pattern);
//                TextView startService = (TextView) findViewById(R.id.guide_start);
//                startService.setText("调试模式已启用");
//                traceMode = true;
                return false;
            }
        });
        mButtonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickButtonStop();
            }
        });
        noiseAutoCheckbox = (CheckBox) findViewById(R.id.noise_auto_checkBox);
        if (getSharedPreferences("noiseAutoState", false)) {
            noiseAutoMode = true;
            mTextViewSensitivityAvg.setText("灵敏度α：0%");
            noiseAutoCheckbox.setChecked(true);
        }
        noiseAutoCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                if (isChecked) {
                    if (!getSharedPreferences("noiseAutoReaded", false)) {
                        Alerter.noiseAutoAlert(mCarrotActivity, new Alerter.noiseAutoInterface() {
                            @Override
                            public void iknow() {
                                setSharedPreferences("noiseAutoState", isChecked);
                                setSharedPreferences("noiseAutoReaded", true);
                                noiseAutoMode = true;
                                mTextViewSensitivityAvg.setText("灵敏度α：0%");
                            }
                        });
                    } else {
                        setSharedPreferences("noiseAutoState", isChecked);
                        noiseAutoMode = true;
                        mTextViewSensitivityAvg.setText("灵敏度α：0%");
                    }
                } else {
                    noiseAutoMode = false;
                    mTextViewSensitivityAvg.setText(R.string.sensitivity_avg);
                }

//                setSharedPreferences("noiseAutoState", isChecked);
//                if (isChecked) {
//                    noiseAutoMode = true;
//                    mTextViewSensitivityAvg.setText("灵敏度α：0%");
//                } else {
//                    noiseAutoMode = false;
//                    mTextViewSensitivityAvg.setText(R.string.sensitivity_avg);
//                }
            }
        });
        runLogCheckbox = (CheckBox) findViewById(R.id.run_log_checkBox);
        //给CheckBox设置事件监听
        runLogCheckbox.setChecked(getSharedPreferences("featureLogState", false));
        runLogCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                if (isChecked) {
                    if (!getSharedPreferences("logAlertReaded", false)) {
                        Alerter.logAlert(mCarrotActivity, new Alerter.logAlertInterface() {
                            @Override
                            public void iknow() {
                                featureLogEnable = isChecked;
                                setSharedPreferences("featureLogState", isChecked);
                                setSharedPreferences("logAlertReaded", true);
                            }

                            @Override
                            public void refuse() {
                                featureLogEnable = false;
                                setSharedPreferences("featureLogState", false);
                                runLogCheckbox.setChecked(false);
                            }
                        });
                    } else {
                        featureLogEnable = isChecked;
                        setSharedPreferences("featureLogState", isChecked);
                    }
                } else {
                    featureLogEnable = isChecked;
                    setSharedPreferences("featureLogState", isChecked);
                }
            }
        });
        if (calType.equals("svm") && isNetworkConnected(mCarrotActivity)) {
            startService(new Intent(CarrotActivity.this, ModelUpdateService.class));
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
        // 展示新版功能提示
        final int versionCode = Utils.getVersionCode(mCarrotActivity);
        if (!getSharedPreferences("newWelcomeAlerts", "0").equals(versionCode + "")) {
            Alerter.newWelcomeAlerts(mCarrotActivity, new Alerter.newWelcomeAlertsInterface() {
                @Override
                public void jumpActivity() {
                    setSharedPreferences("newWelcomeAlerts", versionCode + "");
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    Uri content_url = Uri.parse(lockAppUrl);
                    intent.setData(content_url);
                    startActivity(intent);
                }

                @Override
                public void iknow() {
                    setSharedPreferences("newWelcomeAlerts", versionCode + "");
                }
            });
        } else {
            boolean appKillHistory = getSharedPreferences("appKillHistory", false);
            setSharedPreferences("appKillHistory", false);
            if (appKillHistory) {
                Alerter.appKillAlerts(mCarrotActivity, new Alerter.appKillAlertsInterface() {
                    @Override
                    public void ignore() {
                        setSharedPreferences("appKillHistory", false);
                    }

                    @Override
                    public void jumpActivity() {
                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.VIEW");
                        Uri content_url = Uri.parse(lockAppUrl);
                        intent.setData(content_url);
                        startActivity(intent);
                    }
                });
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (serviceRuning == 1) {
            stopCore();
        }
        Toast.makeText(mCarrotActivity, R.string.carrot_stoped, Toast.LENGTH_LONG).show();
        this.finish();
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
            startService(new Intent(CarrotActivity.this, CoreService.class));
            logStart();
            runLogCheckbox.setClickable(false);
            noiseAutoCheckbox.setClickable(false);
            mTextView.setText(R.string.service_starting);
            if (onStartVibratorEnable) {
                long[] pattern = {10, 50, 50, 50, 50}; // OFF/ON/OFF/ON
                vibrator(pattern);
            }
        } else {
            Toast.makeText(mCarrotActivity, R.string.service_is_running, Toast.LENGTH_LONG).show();
        }
    }

    private void stopCore() {
        serviceStopTime = System.currentTimeMillis();
        stopService(new Intent(CarrotActivity.this, CoreService.class));
        logEnd();
        runLogCheckbox.setClickable(true);
        noiseAutoCheckbox.setClickable(true);
        serviceRuning = 0;
        if (onEndVibratorEnable) {
            long[] pattern = {10, 200}; // OFF/ON/OFF/ON
            vibrator(pattern);
        }
    }

    private void onClickButtonStop() {
        if (serviceRuning == 1) {
            stopCore();
        } else {
            Toast.makeText(mCarrotActivity, R.string.service_not_running, Toast.LENGTH_LONG).show();
        }
    }

    private void logStart() {
        if (featureLogEnable) {
            if (fw == null) {
                Date now = new Date();
                SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
                String fileName = "/" + ft.format(now) + ".log";
                try {
                    File f = new File(mCarrotActivity.getExternalFilesDir("logs").getPath() + fileName);
                    if (!f.exists()) {
                        f.createNewFile();
                    }
                    fw = new FileWriter(f, true);
                    fw.append("Carrot running log start:\r\n");
                    fw.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void logEnd() {
        if (featureLogEnable) {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fw = null;
            }
        }
    }

    private void logEvent(String s) {
        if (featureLogEnable) {
            if (fw != null) {
                try {
                    fw.append(s + "\r\n");
                    fw.flush();
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
                onClickButtonRun();
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
                    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:MM:DD");
                    String time = formatter.format(new Date());
                    logEvent(time + " -1 1");
                    if (noticeMode == 0) {
                        // 震动提示
                        Thread vibratorThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                stopService(new Intent(CarrotActivity.this, CoreService.class));
                                serviceStopTime = System.currentTimeMillis();
                                long[] pattern = {10, 200, 300, 200, 300}; // OFF/ON/OFF/ON
                                vibrator(pattern);
                                try {
                                    Thread.sleep(1410);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if (serviceRuning == 0) {
                                    startService(new Intent(CarrotActivity.this, CoreService.class));
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
                    mTextView.setText(R.string.service_running);
                }
                if (status == 201) {
                    double avg = intent.getDoubleExtra("data", 0.00);
                    Date now = new Date();
                    SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                    logEvent(ft.format(now) + " " + String.format("%.6f", avg) + " 0");
                    if (doNotEditView == false) {
                        mTextViewServiceStatus.setText("特征强度：" + String.format("%.4f", avg));
                    }
                }
                if (status == 301 && doNotEditView == false) {
                    if (System.currentTimeMillis() - mCarrotActivity.serviceStopTime > 2000) {
                        setSharedPreferences("appKillHistory", true);
                        long[] pattern = {10, 1000, 1000, 1000, 10};
                        vibrator(pattern);
                    }
                    mTextView.setText(R.string.service_not_running);
                    mTextViewServiceStatus.setText(R.string.feature_value_empty);
                }
                if (status == 210001) {
                    String data = intent.getStringExtra("data");
                    updateModel(data);
                }
                if (status == 210319) {
                    String data = intent.getStringExtra("data");
                    mTextViewModelName.setText(data);
                }
                if (status == 210421) {
                    mTextViewSensitivityAvg.setText("灵敏度α：" + Math.round(intent.getIntExtra("data", 1) / (noiseAutoNum + 0.0) * 100) + "%");
                }
                if (status == 210422) {
                    double data = intent.getDoubleExtra("data", 0.05);
                    mTextViewSensitivityAvg.setText("灵敏度α：" + data);
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
                int state = intent.getIntExtra("state", 0);
                if (noticeMode == state) {
                    return;
                }
                if (serviceRuning == 1) {
                    serviceStopTime = System.currentTimeMillis();
                    stopService(new Intent(CarrotActivity.this, CoreService.class));
                    startService(new Intent(CarrotActivity.this, CoreService.class));
                }
                headsetHasMic = intent.getIntExtra("microphone", 0);
                if (headsetHasMic == 1) {
                    mTextViewInputSrc.setText(R.string.input_src_headset);
                    // 处理自适应降噪环节
                    if (!noiseAutoMode) {
                        mTextViewSensitivityAvg.setText("灵敏度α：" + sensitivityThresholdHeadset);
                    }
                }
                if (state == 0) {
                    // 耳机移除
                    noticeMode = 0;
                    mTextViewInputSrc.setText(R.string.input_src_phone);
                    mTextViewNoticeMode.setText(R.string.notice_mode_phone);
                    // 处理自适应降噪环节
                    if (!noiseAutoMode) {
                        mTextViewSensitivityAvg.setText("灵敏度α：" + sensitivityThresholdDefault);
                    }
                } else if (state == 1) {
                    // 耳机插入
                    if (!getSharedPreferences("safetyAlertReaded", false)) {
                        Alerter.safetyAlert(mCarrotActivity, new Alerter.safetyAlertInterface() {
                            @Override
                            public void iknow() {
                                setSharedPreferences("safetyAlertReaded", true);
                                noticeMode = 1;
                                mTextViewNoticeMode.setText(R.string.notice_mode_headset);
                            }
                        });
                    } else {
                        noticeMode = 1;
                        mTextViewNoticeMode.setText(R.string.notice_mode_headset);
                    }
                }
            }
        }
    }
}
