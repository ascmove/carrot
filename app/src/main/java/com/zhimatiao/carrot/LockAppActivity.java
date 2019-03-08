package com.zhimatiao.carrot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

public class LockAppActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_app);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent(LockAppActivity.this, CarrotActivity.class);
            startActivity(intent);
            finish();//跳转页面的同事记得关闭当前页面.
        }
        return super.onKeyDown(keyCode, event);
    }
}
