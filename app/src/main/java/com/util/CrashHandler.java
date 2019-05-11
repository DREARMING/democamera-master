package com.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Process;
import android.util.Log;

import com.mvcoder.log.FileLoggingTree;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

import static android.os.FileObserver.CLOSE_WRITE;

/**
 * Created by mvcoder on 2017/7/15.
 */

public class CrashHandler implements UncaughtExceptionHandler {

    private static final String TAG = CrashHandler.class.getSimpleName();
    private static CrashHandler instatnce = new CrashHandler();
    private static final String PATH = Environment.getExternalStorageDirectory().getPath() + "/CrashTest/log/";
    private static final String FILE_NAME = "Crash";
    private static final String FILE_NAME_SUFFIX = ".txt";
    private static final long CRASH_OUT_DATE = 5 * 60 * 1000; //5分钟
    private UncaughtExceptionHandler mDefaultCrashHandler;
    private Context mContext;

    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        if (instatnce == null) {
            synchronized (CrashHandler.class) {
                if (instatnce == null)
                    instatnce = new CrashHandler();
            }
        }
        return instatnce;
    }

    public void init(Context context) {
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        mContext = context.getApplicationContext();
        Thread.setDefaultUncaughtExceptionHandler(this);
        startANRListener();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
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
        }

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
            /*if(nowTime - lastTimes < 10000L) {
                Timber.d("should not process ANR too Fre in 10000");
            } else {*/
            lastTimes = nowTime;
            ActivityManager.ProcessErrorStateInfo errorStateInfo = findError(mContext, 10000L);
            if (errorStateInfo == null) {
                Timber.d("proc state is unvisiable!");
            } else if (errorStateInfo.pid == android.os.Process.myPid()) {
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
                /*TraceFileHelper.a(path, traceFile.getAbsolutePath(), errorStateInfo.processName);*/

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
                    }*/
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
                /*if(errorStateInfoList.size() == 0) {
                    Timber.d("error info list size : 0");
                }*/
                for (ActivityManager.ProcessErrorStateInfo errorStateInfo : errorStateInfoList) {
                    Timber.d("condition : %d", errorStateInfo.condition);
                    //筛选出 error 状态为2（ANR）,并且进程id是当前进程的anr
                    if (errorStateInfo.condition == 2) {
                        Timber.d("found!" + errorStateInfo.processName + "," + errorStateInfo.shortMsg + "," + errorStateInfo.longMsg + ",");
                        return errorStateInfo;
                    }
                }
            }/*else{
                Timber.d("error info list == null");
            }*/
        } while ((long) (index++) < var5);
        return null;
    }

    /* *//**
     * 保存错误信息到文件中
     *
     * @return
     *//*
    private String saveANRInfoToFile(String anrmsg) {

        StringBuilder anrlog = readFile(ANR_TRACE_FILEPATH);
        if(!TextUtils.isEmpty(anrlog)){
            mDeviceCrashInfo.put(ANR_TRACE, anrlog);
        }
        mDeviceCrashInfo.put(STACK_TRACE, anrmsg);
        String fileName = "";
        try {
            long timestamp = System.currentTimeMillis();
            FileOutputStream trace;
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HHmmss", Locale.getDefault());
            String time = format.format(new Date(timestamp));
            fileName = "crash-" + time + CRASH_REPORTER_EXTENSION;
            String filePath;
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                filePath = mContext.getExternalFilesDir("Crash").getAbsolutePath();
            } else {
                filePath = mContext.getFilesDir().getAbsolutePath();
            }
            File file = new File(filePath, fileName);
            trace = new FileOutputStream(file);
            Log.d(TAG, "saveANRInfoToFile: 3");
            mDeviceCrashInfo.storeToXML(trace, "crashLog");
            Log.d(TAG, "saveANRInfoToFile: 4");
            trace.flush();
            trace.close();
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing report file..."  + fileName, e);
        }
        return null;
    }

    public static StringBuilder readFile(String filePath) {
        File file = new File(filePath);
        StringBuilder fileContent = new StringBuilder("");
        if (file == null || !file.isFile()) {
            return null;
        }
        BufferedReader reader = null;
        try {
            InputStreamReader is = new InputStreamReader(new FileInputStream(file), "UTF-8");
            reader = new BufferedReader(is);
            String line = null;
            while ((line = reader.readLine()) != null && fileContent.length()<=10240) {
                if (!fileContent.toString().equals("")) {
                    fileContent.append("\r\n");
                }
                fileContent.append(line);
            }
            reader.close();
            Log.d("duanyl", "readFile: "+fileContent);
            return fileContent;
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred. ", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new RuntimeException("IOException occurred. ", e);
                }
            }
        }
    }*/

}
