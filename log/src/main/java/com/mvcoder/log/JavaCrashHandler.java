package com.mvcoder.log;

import android.content.Context;
import android.os.Environment;
import android.os.Process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mvcoder on 2017/7/15.
 */

public class JavaCrashHandler implements UncaughtExceptionHandler {

    private static final String TAG = JavaCrashHandler.class.getSimpleName();
    private static JavaCrashHandler instatnce = new JavaCrashHandler();
    private static final String PATH = Environment.getExternalStorageDirectory().getPath() + "/CrashTest/log/";
    private static final String FILE_NAME = "Crash";
    private static final String FILE_NAME_SUFFIX = ".txt";
    private static final long CRASH_OUT_DATE = 5 * 60 * 1000; //5分钟
    private UncaughtExceptionHandler mDefaultCrashHandler;
    private Context mContext;
    private OnCrashListener onCrashListener;

    public JavaCrashHandler() {
    }

    public static JavaCrashHandler getInstance() {
        if (instatnce == null) {
            synchronized (JavaCrashHandler.class) {
                if (instatnce == null)
                    instatnce = new JavaCrashHandler();
            }
        }
        return instatnce;
    }

    public void init(Context context) {
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        Log.d(TAG, "mDefaultCrashHandler == null ? %b", mDefaultCrashHandler == null);
        mContext = context.getApplicationContext();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public void setOnCrashListener(OnCrashListener onCrashListener){
        this.onCrashListener = onCrashListener;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Log.printErrStackTrace(TAG, ex, "have a uncaughtException\n");
        if(onCrashListener != null){
            onCrashListener.onCrash(thread, ex);
        }
        /*try {
            Timber.e(ex, "UncaughtException on %s", thread.getName());
            dumpExceptionToSDCard(ex);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ex.printStackTrace();

        //关闭日志文件
        List<Timber.Tree> forest = Timber.forest();
        for (Timber.Tree tree : forest) {
            if (tree instanceof FileLoggingTree) {
                ((FileLoggingTree) tree).closeLog();
            }
        }*/
        if (mDefaultCrashHandler != null) {
            mDefaultCrashHandler.uncaughtException(thread, ex);
        } else {
            Process.killProcess(Process.myPid());
        }
    }

    private void dumpExceptionToSDCard(Throwable ex) throws IOException {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return;
        }
        File dir = new File(PATH);
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (System.currentTimeMillis() - file.lastModified() > CRASH_OUT_DATE)
                    file.delete();
            }
            //dir.delete();
        } else {
            dir.mkdirs();
        }
        long current = System.currentTimeMillis();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(current));
        File file = new File(PATH + FILE_NAME + time + FILE_NAME_SUFFIX);
        if (!file.exists()) {
            file.createNewFile();
        }
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
        pw.println(time);
        ex.printStackTrace(pw);
        pw.close();
    }

    interface OnCrashListener {
        /**
         * 当程序发生 Java Crash 时回调，请同步处理回调结果，保存异常信息，因为回调完成后，JavaCrashHandler 会中止程序运行
         *
         * @param thread 发生未捉捕异常的 Thread
         * @param ex 异常的堆栈信息
         */
        void onCrash(Thread thread, Throwable ex);
    }
/*

    private FileObserver fileObserver;

    private synchronized void startANRListener() {
        Timber.d("startANRListener: ");
        fileObserver = new FileObserver("/data/anr/", CLOSE_WRITE) {
            public void onEvent(int event, String path) {
                Timber.d("on write end Event: %s", path);
                if (path != null) {
                    if (path.contains("trace")) {
                        filiterANR("/data/anr/" + path);
                    }
                }
            }
        };
        try {
            fileObserver.startWatching();
            Timber.d("start anr monitor!");

        } catch (Throwable var2) {
            fileObserver = null;
            Timber.d("start anr monitor failed!");

        }

    }

    private long lastTimes = 0;

    private void copyFile(String target, String path) {
        byte[] bs = new byte[1024 * 8];
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            File sourceFile = new File(path);
            if(!sourceFile.exists()) return;

            File targetFile = new File(target);
            if(targetFile.getParentFile() == null)return;
            if(!targetFile.getParentFile().exists()){
                targetFile.getParentFile().mkdirs();
            }
            fis = new FileInputStream(new File(path));
            fos = new FileOutputStream(target);
            int len = -1;
            while ((len = fis.read(bs)) != -1) {
                fos.write(bs, 0, len);
            }
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) fis.close();
                if(fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void filiterANR(String path) {
        try {
            long nowTime = System.currentTimeMillis();
            */
/*if(nowTime - lastTimes < 10000L) {
                Timber.d("should not process ANR too Fre in 10000");
            } else {*//*

            lastTimes = nowTime;
            ActivityManager.ProcessErrorStateInfo errorStateInfo = findError(mContext, 10000L);
            if (errorStateInfo == null) {
                Timber.d("proc state is unvisiable!");
            } else if (errorStateInfo.pid == Process.myPid()) {
                Log.d(TAG, "not mind proc!" + errorStateInfo.processName);
                Timber.d("process name : %s", errorStateInfo.processName);
                Timber.d("short message: %s", errorStateInfo.shortMsg);
                Timber.d("long message : %s", errorStateInfo.longMsg);
                Timber.d("stackTrace : %s", errorStateInfo.stackTrace);
                File directory = new File(PATH);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                File traceFile = new File(PATH, "ANR_TRACE.txt");

                Timber.d("trace file path: %s", traceFile.getAbsolutePath());
                copyFile(traceFile.getAbsolutePath(), path);
                */
/*TraceFileHelper.a(path, traceFile.getAbsolutePath(), errorStateInfo.processName);*//*


                    */
/*StringBuilder anrLogBuilder = new StringBuilder();
                    anrLogBuilder.append(TAG)
                            .append(" : ")
                            .append("Found ANR in ").append(errorStateInfo.processName).append("\n\n")
                            .append("Cause Reason : ").append(errorStateInfo.shortMsg)

                    String msg = "Found ANR in !"+errorStateInfo.processName+":\r\n "+errorStateInfo.longMsg+"\n\n";
                    String crashFileName = "";
                    crashFileName = saveANRInfoToFile(msg);
                    if(saveListener != null){
                        saveListener.crashFileSaveTo(crashFileName);
                    }else {
                        restartApp();
                    }*//*

            } else {
                Timber.d("found visiable anr , start to process!");
            }
            // }
        } catch (Throwable throwable) {
            Timber.e(throwable);
        }
    }


    private ActivityManager.ProcessErrorStateInfo findError(Context context, long time) {
        time = time < 0L ? 0L : time;
        //z.c("to find!", new Object[0]);
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            Timber.e("can't find anr error,because activityManager is null");
            return null;
        }
        long var5 = time;
        int index = 0;
        do {
            List<ActivityManager.ProcessErrorStateInfo> errorStateInfoList = activityManager.getProcessesInErrorState();
            if (errorStateInfoList != null) {
                */
/*if(errorStateInfoList.size() == 0) {
                    Timber.d("error info list size : 0");
                }*//*

                for (ActivityManager.ProcessErrorStateInfo errorStateInfo : errorStateInfoList) {
                    Timber.d("condition : %d", errorStateInfo.condition);
                    //筛选出 error 状态为2（ANR）,并且进程id是当前进程的anr
                    if (errorStateInfo.condition == 2) {
                        Timber.d("found!" + errorStateInfo.processName + "," + errorStateInfo.shortMsg + "," + errorStateInfo.longMsg + ",");
                        return errorStateInfo;
                    }
                }
            }*/
/*else{
                Timber.d("error info list == null");
            }*//*

        } while ((long) (index++) < var5);
        return null;
    }
*/

}
