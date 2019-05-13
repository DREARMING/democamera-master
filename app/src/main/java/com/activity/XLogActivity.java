package com.activity;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.example.administrator.democaream.R;
import com.tencent.mars.xlog.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class XLogActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private final String TAG = XLogActivity.class.getSimpleName();

    private Button btWriteLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xlog);
        initView();
        Log.i(TAG, "测试XLog -- 这条语句会不会写入xlog呢？在没有外部存储之前");
        requestPermission();
    }

    private void initView() {
        btWriteLog = findViewById(R.id.btWriteLog);
        btWriteLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "on click listener");
                Log.i(TAG,"line 1 log \nline 2 log\nline 3 log");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "on other thread...%s", Thread.currentThread().getName());
                        //Log.i(TAG, getLongText());
                        String str = null;
                        try {
                            System.out.println(str.length());
                        }catch (NullPointerException e){
                            e.printStackTrace();
                            Log.e(TAG,getStackTraceString(e));
                        }
                    }
                }).start();
            }
        });
    }

    private String getStackTraceString(Throwable t) {
        // Don't replace this with Log.getStackTraceString() - it hides
        // UnknownHostException, which is not what we want.
        StringWriter sw = new StringWriter(256);
        PrintWriter pw = new PrintWriter(sw, false);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    private String getLongText(){
        String testStr = "TestString"; //10 * 1024 *
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < 102400; i++){
            builder.append(testStr).append(i);
        }
        return builder.toString();
    }

    @AfterPermissionGranted(1)
    private void requestPermission() {
        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        if(!EasyPermissions.hasPermissions(this, permissions)){
            EasyPermissions.requestPermissions(this, "",1, permissions);
            return;
        }
        Log.i(TAG,"info 类日志信息");
        Log.w(TAG,"警告类日志");
        Log.e(TAG,"错误日志");
        Log.appenderFlush(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.appenderClose();
    }
}
