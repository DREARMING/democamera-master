package com.activity;

import android.app.Application;
import android.os.Environment;

import com.mvcoder.log.BuildConfig;
import com.tencent.mars.xlog.Log;
import com.tencent.mars.xlog.Xlog;

public class FaceApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        /*CrashHandler.getInstance().init(this);
        if(BuildConfig.DEBUG){
            Timber.plant(new Timber.DebugTree());
        }else{
            String logFilePath =  Environment.getExternalStorageDirectory().getPath() + "/CrashTest/timber/";
            Timber.plant(new FileLoggingTree(logFilePath));
        }
        Timber.tag(FaceApplication.class.getSimpleName());*/
        initXLog();
    }


    private void initXLog() {
        System.loadLibrary("c++_shared");
        System.loadLibrary("marsxlog");

        String packageName = getPackageName();
        String label = getPackageManager().getApplicationLabel(getApplicationInfo()).toString();

        final String SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath();
        final String logPath = SDCARD + "/" + packageName + "/log";

        // this is necessary, or may cash for SIGBUS
        final String cachePath = this.getFilesDir() + "/xlog";

        //init xlog
        if (BuildConfig.DEBUG) {
            Xlog.appenderOpen(Xlog.LEVEL_DEBUG, Xlog.AppednerModeAsync, cachePath, logPath, label, 0, null);
            Xlog.setConsoleLogOpen(true);
        } else {
            Xlog.appenderOpen(Xlog.LEVEL_INFO, Xlog.AppednerModeAsync, cachePath, logPath, label, 0,null);
            Xlog.setConsoleLogOpen(false);
        }
        Log.setLogImp(new Xlog());
    }

}
