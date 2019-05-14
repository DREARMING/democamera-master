package com.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.example.administrator.democaream.R;
import com.mvcoder.log.BuildConfig;
import com.mvcoder.log.Log;
import com.mvcoder.log.utils.ProcessUtil;
import com.tencent.mars.xlog.Xlog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class ANRTestActivity extends AppCompatActivity {

    private final static String TAG = ANRTestActivity.class.getSimpleName();


    @BindView(R.id.btANR)
    Button btANR;
    @BindView(R.id.btRegister)
    Button btRegister;
    @BindView(R.id.btJavaCrash)
    Button btJavaCrash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anrtest);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.btANR, R.id.btRegister, R.id.btJavaCrash})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btANR:
                testANR();
                //printLog();
                break;
            case R.id.btRegister:
                printLog();
                //registerTraceObserver();
                //joinToOtherProcessActivity();
                //switchLogDate();
                break;
            case R.id.btJavaCrash:
                testJavaCrash();
                break;
        }
    }

    private void printLog(){
        Log.d(TAG, "print a test debug log");
        Log.i(TAG, "print a test info log");
    }

    private void switchLogDate() {
        Log.appenderFlush(true);
        Log.appenderClose();
        initXLog();

    }

    private void initXLog() {
        String packageName = getPackageName();
        String label = getPackageManager().getApplicationLabel(getApplicationInfo()).toString();

        final String SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath();
        final String logPath = SDCARD + "/" + packageName + "/log/";

        // this is necessary, or may cash for SIGBUS
        final String cachePath = this.getFilesDir() + "/xlog";

        //获取进程名, 因为 xlog 只支持1个进程1个日志文件，这里用进程区分，是为了支持多进程
        String processName = ProcessUtil.getProcessName(Process.myPid());
        if (processName != null) {
            int index = processName.lastIndexOf(".");
            if (index != -1) {
                processName = processName.substring(index + 1);
            }
        }
        //init xlog
        if (BuildConfig.DEBUG) {
            Xlog.open(true, Xlog.LEVEL_DEBUG, Xlog.AppednerModeAsync, cachePath, logPath + processName, label, null);
            Xlog.setConsoleLogOpen(true);
        } else {
            Xlog.open(true, Xlog.LEVEL_INFO, Xlog.AppednerModeAsync, cachePath, logPath + processName, label, null);
            Xlog.setConsoleLogOpen(false);
        }
        Log.setLogImp(new Xlog());
    }

    private void joinToOtherProcessActivity() {
        Intent intent = new Intent(this, TestProcessActivity.class);
        startActivity(intent);
    }

    private void testJavaCrash() {
        String str = null;
        Log.d(TAG, "str len : %d", str.length());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fileObserver != null) fileObserver.stopWatching();
    }

    private void testANR() {
        try {
            for (int i = 1; i < 1000; i++) {
                Thread.sleep(10 * 1000);
            }
            Log.d(TAG, "test anr");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private FileObserver fileObserver;

    private void registerTraceObserver() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (fileObserver == null) {
                    Timber.d("start watch trace.txt");
                    final String filepath = "/data/anr/";
                    fileObserver = new FileObserver(filepath, 8) {
                        @Override
                        public void onEvent(int event, @Nullable String path) {
                            Log.d(TAG, "traces directory on event");
                            if (path != null && path.contains("trace")) {
                                Log.d(TAG, "traces text write end : %s", path);
                            }
                        }
                    };
                    fileObserver.startWatching();
                }
            }
        }).start();
    }
}
