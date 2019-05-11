package com.activity;

import android.os.Bundle;
import android.os.FileObserver;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.example.administrator.democaream.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class ANRTestActivity extends AppCompatActivity {


    @BindView(R.id.btANR)
    Button btANR;
    @BindView(R.id.btRegister)
    Button btRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anrtest);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.btANR, R.id.btRegister})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btANR:
                testANR();
                break;
            case R.id.btRegister:
                registerTraceObserver();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fileObserver != null) fileObserver.stopWatching();
    }

    private void testANR() {
        try {
            for (int i = 1; i < 1000; i++) {
                Thread.sleep(10 * 1000);
            }
            Timber.d("test anr");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private FileObserver fileObserver;

    private void registerTraceObserver() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (fileObserver == null) {
                    Timber.d("start watch trace.txt");
                    final String filepath = "/data/anr/";
                    fileObserver = new FileObserver(filepath, 8) {
                        @Override
                        public void onEvent(int event, @Nullable String path) {
                            Timber.d("traces directory on event");
                            if (path != null && path.contains("trace")) {
                                Timber.d("traces text write end : %s", path);
                            }
                        }
                    };
                    fileObserver.startWatching();
                }
            }
        }).start();
    }
}
