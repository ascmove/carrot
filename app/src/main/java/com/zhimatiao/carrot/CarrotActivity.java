package com.zhimatiao.carrot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

//import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
//import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;

public class CarrotActivity extends Activity {

    private static final int REQUEST_RECORD_AUDIO = 670049;
    private boolean NOT_SUPPORT = false;
    private boolean runLogEnable = false;
    private boolean doNotEditView = false;
    private int serviceRuning = 0;
    private int requestAudioStatus = 0;
    private TextView mTextView = null;
    private TextView mTextViewServiceStatus = null;
    private Button mButton = null;
    private Button mButtonStop = null;
    private CheckBox runLogCheckbox = null;
    private MsgReceiver msgReceiver;
    private Intent intentAnalyseService;
    private Intent intentCoreService;
    public CarrotActivity mCarrotActivity;
    private FileWriter fw = null;
    private BufferedWriter writer = null;

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
            if (action == "com.zhimatiao.carrot.action.ALARM") {
                int status = intent.getIntExtra("status", 0);
                if (status == 800) {
                    String timestamp = System.currentTimeMillis() + "";
                    logEvent(timestamp + " -1 1");
                    Thread vibratorThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            stopService(intentCoreService);
                            logEnd();
                            runLogCheckbox.setClickable(true);
                            Vibrator vibrator = (Vibrator) mCarrotActivity.getSystemService(mCarrotActivity.VIBRATOR_SERVICE);
                            long[] pattern = {10, 400, 300, 400, 300}; // OFF/ON/OFF/ON
                            vibrator.vibrate(pattern, -1);
                            try {
                                Thread.sleep(1410);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            startService(intentCoreService);
                            logStart();
                            runLogCheckbox.setClickable(false);
                        }
                    });
                    vibratorThread.setName("VibratorThread");
                    vibratorThread.start();
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
            }
            if (action == "android.intent.action.SCREEN_OFF") {
                doNotEditView = true;
            }
            if (action == "android.intent.action.SCREEN_ON") {
                doNotEditView = false;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrot);

        mCarrotActivity = this;

        //动态注册广播接收器
        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.zhimatiao.carrot.action.ALARM");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        registerReceiver(msgReceiver, intentFilter);


        // 监测是否支持此安卓版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NOT_SUPPORT = false;
        } else {
            NOT_SUPPORT = true;
        }

        // 检查与请求必要权限
        requestPermissions(new String[]{RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
//        requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, 122211);
//        requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, 211122);

        if (!NOT_SUPPORT) {
            //绑定按钮事件
            mButton = (Button) findViewById(R.id.button);
            mButtonStop = (Button) findViewById(R.id.button_stop);
            mTextView = (TextView) findViewById(R.id.textview_service_run);
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
            //通过控件的ID来得到代表控件的对象
            runLogCheckbox = (CheckBox) findViewById(R.id.run_log_checkBox);
            //给CheckBox设置事件监听
            runLogCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    runLogEnable = isChecked;
                }
            });
        }
    }

    private void showRecordPermissionAlert(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.permission_title);
        builder.setMessage(R.string.permission_tip);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                requestPermissions(new String[]{RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            }
        });
        builder.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.exit(0);
            }
        });
        builder.show();
    }

    private void onClickButtonRun() {
        if (requestAudioStatus != 1) {
            showRecordPermissionAlert(mCarrotActivity);
            return;
        }
        if (serviceRuning == 0) {
//        intentAnalyseService = new Intent(CarrotActivity.this, AnalyseService.class);
//        startService(intentAnalyseService);
            intentCoreService = new Intent(CarrotActivity.this, CoreService.class);
            startService(intentCoreService);
            logStart();
            runLogCheckbox.setClickable(false);
            mTextView.setText("启动中");
            serviceRuning = 1;
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
        } else {
            Toast.makeText(mCarrotActivity, "服务未启动", Toast.LENGTH_LONG).show();
        }
    }

    private void logStart() {
        if (runLogEnable) {
            if (writer == null && fw == null) {
                DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                String time = formatter.format(new Date());
                String fileName = "/app-" + time + ".log";
                try {
                    File f = new File(mCarrotActivity.getExternalFilesDir("logs").getPath() + fileName);
                    if (!f.exists()) {
                        f.createNewFile();
                    }
                    fw = new FileWriter(f, true);
                    writer = new BufferedWriter(fw, 9000);
                    writer.append("\r\n" + time + "Carrot running log start:\r\n");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void logEnd() {
        if (runLogEnable) {
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
        if (runLogEnable) {
            if (writer != null && fw != null) {
                try {
                    writer.append(s + "\r\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestAudioStatus = 1;
            }
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                requestAudioStatus = -1;
                showRecordPermissionAlert(mCarrotActivity);
            }
        }
    }

    private boolean mayRequestRecordAudio() {
        if (checkSelfPermission(RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        requestPermissions(new String[]{RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        return false;
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(msgReceiver);
        super.onDestroy();
    }
}
