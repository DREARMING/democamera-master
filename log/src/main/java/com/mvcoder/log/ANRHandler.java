package com.mvcoder.log;

import android.app.ActivityManager;
import android.content.Context;
import android.os.FileObserver;
import android.os.Process;

import java.io.File;
import java.util.List;

import static android.os.FileObserver.CLOSE_WRITE;

public class ANRHandler {

    private final String TAG = ANRHandler.class.getSimpleName();


    private final static String ANR_TRACE_FILE = "/data/anr/traces.txt";
    private final static String ANR_TRACE_DIR = "/data/anr/";

    private static volatile ANRHandler anrHandler;

    private OnAnrListenr listenr;

    public static ANRHandler getInstance(){
        if(anrHandler == null) {
            synchronized (ANRHandler.class){
                if(anrHandler == null)
                    anrHandler = new ANRHandler();
            }
        }
        return anrHandler;
    }

    public void init(Context context){
        startAnrListener(context);
    }

    public void setAnrListenr(OnAnrListenr onAnrListenr){
        this.listenr = onAnrListenr;
    }

    public void stopListenAnr(){
        if(fileObserver != null){
            fileObserver.stopWatching();
        }
        fileObserver = null;
    }

    public interface OnAnrListenr {

        /**
         * 无法监听 /data/anr/ 目录写状态改变事件时回调，意味着无法监听 anr 的发生
         */
        void onAnrListenFail();

        /**
         *  当应用发生 anr 时，在 FileObserver 线程回调该接口
         *
         * @param stateInfo 发生 ANR 的进程状态信息，可以获取 anr 的 cause reason
         * @param tracesFilePath  /data/anr/traces 的文件路径，可以用于拷贝或者文件读取，备案 anr 的堆栈信息
         */
        void onAnr(ActivityManager.ProcessErrorStateInfo stateInfo, String tracesFilePath);
    }

    private FileObserver fileObserver;

    private synchronized void startAnrListener(final Context context) {
        File file = new File(ANR_TRACE_DIR);

        //如果系统从来没有发生过 anr，anr 目录不存在，将会导致无法监听anr.同时 anr 目录不能由用户创建
        if(!file.exists() || !file.canRead()) {
            Log.e(TAG,"不能监听ANR事件, /data/anr/ 目录不可读或不存在");
            //anr 目录不存在或不可读取
            if(listenr != null) listenr.onAnrListenFail();
            return;
        }
        Log.d(TAG, "startANRListener: ");
        //FileObserver 只能监听已经存在的目录或文件
        fileObserver = new FileObserver(ANR_TRACE_DIR, CLOSE_WRITE) {
            public void onEvent(int event, String path) {
                Log.d(TAG,"on write end Event: %s", path);
                if (path != null && path.contains("trace")) {
                    filiterANR("/data/anr/" + path, context);
                }
            }
        };
        try {
            fileObserver.startWatching();
            Log.d(TAG, "start anr monitor!");
        } catch (Throwable var2) {
            fileObserver = null;
            Log.d(TAG, "start anr monitor failed!");
        }

    }

    private long lastTimes = 0;

    private void filiterANR(String path, Context context) {
        Log.i(TAG, "current thread is %s", Thread.currentThread().getName());
        /*try {
            long nowTime = System.currentTimeMillis();
            *//*if(nowTime - lastTimes < 10000L) {
                Timber.d("should not process ANR too Fre in 10000");
            } else {*//*
            lastTimes = nowTime;*/
            ActivityManager.ProcessErrorStateInfo errorStateInfo = findError(context, 10000L);
            if (errorStateInfo == null) {
                Log.d(TAG,"proc state is unvisiable!");
            } else if (errorStateInfo.pid == Process.myPid()) {
                Log.d(TAG, "not mind proc!" + errorStateInfo.processName);
                Log.d(TAG,"process name : %s", errorStateInfo.processName);
                Log.d(TAG,"short message: %s", errorStateInfo.shortMsg);
                Log.d(TAG,"long message : %s", errorStateInfo.longMsg);
                Log.d(TAG,"stackTrace : %s", errorStateInfo.stackTrace);
               /* File directory = new File(anrDir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                File traceFile = new File(anrDir, "ANR_TRACE.txt");*/

                Log.d(TAG,"trace file path: %s", path);

                if(listenr != null){
                    listenr.onAnr(errorStateInfo, path);
                }

                //copyFile(traceFile.getAbsolutePath(), path);
            } else {
                Log.d(TAG,"found visiable anr , start to process!");
            }
            // }
       /* } catch (Throwable throwable) {
            Timber.e(throwable);
        }*/
    }


    private ActivityManager.ProcessErrorStateInfo findError(Context context, long time) {
        /*time = time < 0L ? 0L : time;*/
        //z.c("to find!", new Object[0]);
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            Log.d(TAG,"can't find anr error,because activityManager is null");
            return null;
        }
        long var5 = time;
        int index = 0;
        do {
            List<ActivityManager.ProcessErrorStateInfo> errorStateInfoList = activityManager.getProcessesInErrorState();
            if (errorStateInfoList != null) {
                /*if(errorStateInfoList.size() == 0) {
                    Timber.d("error info list size : 0");
                }*/
                for (ActivityManager.ProcessErrorStateInfo errorStateInfo : errorStateInfoList) {
                    Log.d(TAG,"condition : %d", errorStateInfo.condition);
                    //筛选出 error 状态为2（ANR）,并且进程id是当前进程的anr
                    if (errorStateInfo.condition == 2) {
                        Log.d(TAG,"found!" + errorStateInfo.processName + "," + errorStateInfo.shortMsg + "," + errorStateInfo.longMsg + ",");
                        return errorStateInfo;
                    }
                }
            }/*else{
                Timber.d("error info list == null");
            }*/
        } while ((long) (index++) < var5);
        return null;
    }

}
