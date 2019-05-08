package com.mvcoder.log;

import android.os.Build;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class FileLoggingTree extends Timber.Tree {

    private static final int MAX_TAG_LENGTH = 23;
    private static final int CALL_STACK_INDEX = 6;
    private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");
    private final String logFileDir;
    private volatile boolean init = false;
    private PrintWriter printWriter;


    public FileLoggingTree(@NonNull String logFileDir){
        this.logFileDir = logFileDir;
    }


    @Nullable
    protected String createStackElementTag(@NotNull StackTraceElement element) {
        String tag = element.getClassName();
        Matcher m = ANONYMOUS_CLASS.matcher(tag);
        if (m.find()) {
            tag = m.replaceAll("");
        }
        tag = tag.substring(tag.lastIndexOf('.') + 1);
        // Tag length limit was removed in API 24.
        if (tag.length() <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return tag;
        }
        return tag.substring(0, MAX_TAG_LENGTH);
    }

    final String getTag() {

        // DO NOT switch this to Thread.getCurrentThread().getStackTrace(). The test will pass
        // because Robolectric runs them on the JVM but on Android the elements are different.
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length <= CALL_STACK_INDEX) {
            throw new IllegalStateException(
                    "Synthetic stacktrace didn't have enough elements: are you using proguard?");
        }
        return createStackElementTag(stackTrace[CALL_STACK_INDEX]);
    }

    @Override
    protected void log(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t) {
        //只在 release 状态下用文件记录日志
        if(BuildConfig.DEBUG) return;
        if(!init){
            init = true;
            File fileDir = new File(logFileDir);
            if(!fileDir.exists()){
                fileDir.mkdirs();
            }
            try {
                printWriter = new PrintWriter(new FileWriter(new File(fileDir,"log.txt")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(printWriter == null) return;

        StringBuilder builder = new StringBuilder();
        //抄 DeggerTree 获取当前类名作为标志
        if(tag == null) {
            tag = getTag();
            builder.append(tag);
            builder.append(" : ");
        }
        builder.append(message);
        printWriter.println(builder.toString());
        printWriter.println();
        printWriter.println();
        printWriter.flush();
    }

    public void closeLog(){
        if(printWriter != null){
            printWriter.flush();
            printWriter.close();
        }
        init = false;
    }

}
