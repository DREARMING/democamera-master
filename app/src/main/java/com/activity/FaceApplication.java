package com.activity;

import android.app.Application;
import android.os.Environment;

import com.example.administrator.democaream.BuildConfig;
import com.mvcoder.log.FileLoggingTree;
import com.util.CrashHandler;

import timber.log.Timber;

public class FaceApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.getInstance().init(this);
        if(BuildConfig.DEBUG){
            Timber.plant(new Timber.DebugTree());
        }else{
            String logFilePath =  Environment.getExternalStorageDirectory().getPath() + "/CrashTest/timber/";
            Timber.plant(new FileLoggingTree(logFilePath));
        }
        Timber.tag(FaceApplication.class.getSimpleName());
    }
}
