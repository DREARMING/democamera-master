package com.activity;

import android.app.Application;
import android.os.Environment;
import android.os.Process;

import com.mvcoder.log.BuildConfig;
import com.mvcoder.log.CrashManager;
import com.mvcoder.log.Log;
import com.mvcoder.log.utils.ProcessUtil;
import com.tencent.mars.xlog.Xlog;

public class FaceApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        /*JavaCrashHandler.getInstance().init(this);
        if(BuildConfig.DEBUG){
            Timber.plant(new Timber.DebugTree());
        }else{
            String logFilePath =  Environment.getExternalStorageDirectory().getPath() + "/CrashTest/timber/";
            Timber.plant(new FileLoggingTree(logFilePath));
        }
        Timber.tag(FaceApplication.class.getSimpleName());*/

        //必须先初始化日志库，CrashManager 依赖于日志库
        initXLog();
        //初始化 crash handler
        CrashManager.getInstance().init(getApplicationContext());
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
        if(processName != null){
            int index =  processName.lastIndexOf(".");
            if(index != -1){
                processName =  processName.substring(index + 1);
            }
        }
        //init xlog
        if (BuildConfig.DEBUG) {
            //第二个参数是日志库最低输入level，低于该level的日志全部不输出
            Xlog.open(true, Xlog.LEVEL_INFO, Xlog.AppednerModeAsync, cachePath, logPath + processName, label,  null);
            Xlog.setConsoleLogOpen(true);
        } else {
            Xlog.open(true, Xlog.LEVEL_INFO, Xlog.AppednerModeAsync, cachePath, logPath + processName, label, null);
            Xlog.setConsoleLogOpen(false);
        }
        Log.setLogImp(new Xlog());
    }

}
