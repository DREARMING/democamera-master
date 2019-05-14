package com.activity;

import android.os.Bundle;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;

import com.example.administrator.democaream.R;
import com.mvcoder.log.Log;

public class TestProcessActivity extends AppCompatActivity {

    private final String TAG = TestProcessActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_process);
        Log.d(TAG, "this is other process : %d", Process.myPid());
        String str = null;
        Log.d(TAG, "len : %d" ,str.length());
    }
}
