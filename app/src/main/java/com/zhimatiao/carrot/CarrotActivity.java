package com.zhimatiao.carrot;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class CarrotActivity extends Activity {

    private static final int REQUEST_RECORD_AUDIO = 670049;
    private boolean NOT_SUPPORT = false;
    private TextView mTextView = null;
    private TextView mTextViewServiceStatus = null;
    private Button mButton = null;
    private Button mButtonStop = null;
    private MsgReceiver msgReceiver;
    private Intent intentAnalyseService;
    private Intent intentCoreService;
    public CarrotActivity mCarrotActivity;
    private int serviceRuning = 0;
    private boolean doNotEditView = false;

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
                    Thread vibratorThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Vibrator vibrator = (Vibrator) mCarrotActivity.getSystemService(mCarrotActivity.VIBRATOR_SERVICE);
                            long[] pattern = {10, 400, 300, 400, 300}; // OFF/ON/OFF/ON
                            vibrator.vibrate(pattern, -1);
                        }
                    });
                    vibratorThread.setName("VibratorThread");
                    vibratorThread.start();
                }
                if (status == 200 && doNotEditView == false) {
                    mTextView.setText("已启动");
                }
                if (status == 201 && doNotEditView == false) {
                    double avg = intent.getDoubleExtra("data", 0.00);
                    mTextViewServiceStatus.setText("特征强度：" + String.format("%.4f", avg));
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
        boolean audioPrimission = mayRequestRecordAudio();
        requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, 122211);
        requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, 211122);

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
        }
    }

    private void onClickButtonRun() {
        if (serviceRuning == 0) {
//        intentAnalyseService = new Intent(CarrotActivity.this, AnalyseService.class);
//        startService(intentAnalyseService);
            intentCoreService = new Intent(CarrotActivity.this, CoreService.class);
            startService(intentCoreService);
            mTextView.setText("启动中");
            serviceRuning = 1;
        } else {
            Toast.makeText(mCarrotActivity, "服务已启动", Toast.LENGTH_LONG).show();
        }
    }

    private void onClickButtonStop() {
        if (serviceRuning == 1) {
            stopService(intentCoreService);
            mTextView.setText("服务未启动");
            mTextViewServiceStatus.setText("特征强度：--");
            serviceRuning = 0;
        } else {
            mTextViewServiceStatus.setText("特征强度：--");
            Toast.makeText(mCarrotActivity, "服务未启动", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            }
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_DENIED) {

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
