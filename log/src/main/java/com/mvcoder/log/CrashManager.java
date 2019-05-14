package com.mvcoder.log;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class CrashManager {

    private final String TAG = CrashManager.class.getSimpleName();

    private static volatile CrashManager crashManager;

    private OnCrashListener onCrashListener;

    private ANRHandler anrHandler;

    private AtomicBoolean initState = new AtomicBoolean(false);

    public static CrashManager getInstance(){
        if(crashManager == null){
            synchronized (CrashManager.class){
                if(crashManager == null) {
                    crashManager = new CrashManager();
                }
            }
        }
        return crashManager;
    }

    public void init(Context context){
        this.init(context, true, true);
    }

    public void init(Context context, boolean javaCrashEnable, boolean anrEnable){
        if(!initState.compareAndSet(false, true)) return;
        if(javaCrashEnable){
            Log.i(TAG, "set java crash listener");
            initJavaCrash(context);
        }
        if(anrEnable){
            Log.i(TAG, "set anr listener");
            initAnr(context);
        }
    }

    public void setOnCrashListener(OnCrashListener listener){
        this.onCrashListener = listener;
    }

    private void initJavaCrash(Context context){
        JavaCrashHandler javaCrashHandler = new JavaCrashHandler();
        javaCrashHandler.setOnCrashListener(new JavaCrashHandler.OnCrashListener() {
            @Override
            public void onCrash(Thread thread, Throwable ex) {
                saveJavaCrashLog(thread, ex);
            }
        });
        javaCrashHandler.init(context);
    }

    private void saveJavaCrashLog(Thread thread, Throwable ex) {
        String stackTraceString = getStackTraceString(ex);
        //记录致命 Exception 的堆栈信息
        Log.f(TAG, stackTraceString);

        if(onCrashListener != null){
            onCrashListener.onJavaCrash(thread, ex);
        }
    }


    private void initAnr(final Context context){
        anrHandler = new ANRHandler();
        anrHandler.setAnrListenr(new ANRHandler.OnAnrListenr() {
            @Override
            public void onAnrListenFail() {
                Log.e(TAG,"不能监听 anr 文件");
                //清空
                anrHandler.setAnrListenr(null);
                anrHandler = null;
                if(onCrashListener != null){
                    onCrashListener.onAnrListenFail();
                }
            }

            @Override
            public void onAnr(@NonNull ActivityManager.ProcessErrorStateInfo stateInfo, String tracesFilePath) {
                saveAnrLog(context, stateInfo, tracesFilePath);
                if(onCrashListener != null){
                    onCrashListener.onAnr(stateInfo, tracesFilePath);
                }
            }
        });
        anrHandler.init(context);
    }

    private void saveAnrLog(Context context, ActivityManager.ProcessErrorStateInfo stateInfo, String traceFilePath){
        //拷贝 /data/anr/traces.txt，方便查询堆栈信息
        String packageName = context.getPackageName();
        final String SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath();
        final String anrDir = SDCARD + "/" + packageName + "/anr/";
        final File anrDirFile = new File(anrDir);
        if(!anrDirFile.exists()){
            boolean success = anrDirFile.mkdirs();
            if(!success) {
                Log.e(TAG, "want to save anr traces.txt, but can't create the anr directory : %s", anrDirFile.getAbsoluteFile());
                return;
            }
        }

        cleanAnrTracesFile(anrDirFile);

        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
        String currentTime = dateFormat.format(date);

        StringBuilder builder = new StringBuilder();
        builder.append("OnAnrHappen : time is ").append(currentTime).append("\n")
                .append("ProcessName : ").append(stateInfo.processName).append("\n")
                .append("Cause Reason : \n")
                .append(stateInfo.shortMsg).append("\n")
                .append(stateInfo.longMsg);

        //保存 Anr 发生的 cause reason 日志
        Log.e(TAG, builder.toString());

        //拷贝 traces.txt 到 anr 目录
        File copyTraceFile = new File(anrDir + "traces_" + currentTime + ".txt");
        copyFile(traceFilePath, copyTraceFile.getAbsolutePath());

    }


    /**
     * 让 anr 日志的个数最多不超过 3 个
     *
     * @param dirFile anr 文件目录
     */
    private void cleanAnrTracesFile(File dirFile){
        File[] fileList = dirFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains("traces");
            }
        });

        if(fileList != null && fileList.length >= 3){
            Arrays.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File file1, File file2) {
                    //升序排列
                    return (int) (file1.lastModified() - file2.lastModified());
                }
            });

            //删掉超过5个的数量
            for(int i = 0; i < fileList.length - 5; i++){
                File delItem =  fileList[i];
                delItem.delete();
            }
        }
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

    private void copyFile(String path, String target) {
        byte[] bs = new byte[4096];
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            File sourceFile = new File(path);
            if(!sourceFile.exists()) {
                Log.e(TAG, "copy file, but source file not exist");
                return;
            }

            File targetFile = new File(target);
            if(targetFile.getParentFile() == null) return;
            if(!targetFile.getParentFile().exists()){
                boolean success =  targetFile.getParentFile().mkdirs();
                if(!success) return;
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


    public interface OnCrashListener {

        /**
         * 当程序发生 Java Crash 时回调，请同步处理回调结果，保存异常信息，因为回调完成后，JavaCrashHandler 会中止程序运行
         *
         * @param thread 发生未捉捕异常的 Thread
         * @param ex 异常的堆栈信息
         */
        void onJavaCrash(Thread thread, Throwable ex);

        /**
         * 无法监听 /data/anr/ 目录写状态改变事件时回调，意味着无法监听 anr 的发生
         */
        void onAnrListenFail();

        /**
         *  当应用发生 anr 时，回调该接口
         *
         * @param stateInfo 发生 ANR 的进程状态信息，可以获取 anr 的 cause reason
         * @param tracesFilePath  /data/anr/traces 的文件路径，可以用于拷贝或者文件读取，备案 anr 的堆栈信息
         */
        void onAnr(ActivityManager.ProcessErrorStateInfo stateInfo, String tracesFilePath);
    }

}
