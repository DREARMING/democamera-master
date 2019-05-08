package com.util;

import android.content.Context;
import android.os.Environment;
import android.os.Process;

import com.mvcoder.log.FileLoggingTree;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

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


}
