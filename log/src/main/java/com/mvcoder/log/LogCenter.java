package com.mvcoder.log;

import android.content.Context;
import android.os.Environment;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.mvcoder.log.utils.ProcessUtil;
import com.tencent.mars.xlog.Xlog;

import java.io.File;

public class LogCenter {

    private static final String TAG = LogCenter.class.getSimpleName();

    //anr 目录用于保存 anr/traces.txt 文件的拷贝副本
    private final static String DIR_ANR = "anr";
    private final static String DIR_LOG = "log";
    private final static String DIR_CACHE = "xlog";

    /**
     * 日志目录
     */
    private String logDir;





    static class Builder {

        boolean listenJavaCrash = true;
        boolean anrEnable = true;
        private String logDir;
        private String cacheDir;
        private boolean encryptEnable = false;
        private boolean consoleLogEnable = BuildConfig.DEBUG;
        private String pubKey = null;
        private boolean canWriteLog2File = !BuildConfig.DEBUG;
        /**
         * 至少什么级别的日志才会写进文件
         */
        private int minLevel = Xlog.LEVEL_INFO;

        public Builder listenJavaCrash(boolean javaCrashEnable){
            this.listenJavaCrash = javaCrashEnable;
            return this;
        }

        public Builder listenANR(boolean anrEnable){
            this.anrEnable = anrEnable;
            return this;
        }

        public Builder logDir(String path){
            this.logDir = path;
            return this;
        }

        public Builder encryEnable(boolean enable, String pubKey){
            this.encryptEnable = enable;
            this.pubKey = pubKey;
            return this;
        }

        public Builder cacheDir(String cacheDir){
            this.cacheDir = cacheDir;
            return this;
        }

        public Builder consoleLogEnable(boolean enable){
            this.consoleLogEnable = enable;
            return this;
        }

        public Builder writeLog2File(boolean enable){
            this.canWriteLog2File = enable;
            return this;
        }

        public Builder writeLog2File(boolean enable, int minLevel){
            this.canWriteLog2File = enable;
            this.minLevel = minLevel;
            return this;
        }

        public void build(Context context){
            if(TextUtils.isEmpty(logDir)){
                String packageName = context.getPackageName();
                logDir = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + File.separator + packageName + File.separator + DIR_LOG;
            }
            if(TextUtils.isEmpty(cacheDir)){
                cacheDir = context.getFilesDir()+ File.separator + DIR_CACHE;
            }
            if(canWriteLog2File){
                //获取进程名, 因为 xlog 只支持1个进程1个日志文件，这里用进程区分，是为了支持多进程
                String processName = ProcessUtil.getProcessName(Process.myPid());
                Log.d(TAG, "进程名 : " + processName);
                //获取应用名
                String lable = context.getPackageManager().getApplicationLabel(context.getApplicationInfo()).toString();
                Xlog.open(true, minLevel, Xlog.AppednerModeAsync, cacheDir + File.separator + processName,
                        logDir, lable, pubKey);


            }
        }


    }

}
