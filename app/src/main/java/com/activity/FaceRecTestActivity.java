package com.activity;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.administrator.democaream.R;
import com.mvcoder.facerecognize.FaceRecognizeManager;
import com.mvcoder.facerecognize.IFaceRecognizeManager;

import java.util.Random;

import timber.log.Timber;

public class FaceRecTestActivity extends AppCompatActivity {

    private Button btStart;
    private Button btClose;
    private Button btPos;
    private Button btSize;
    private ImageView ivFace;
    private FrameLayout faceRecContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_rec_test);
        initView();
    }

    private void initView() {
        btStart = findViewById(R.id.btStartFaceRec);
        btClose = findViewById(R.id.btCloseFaceRec);
        btPos = findViewById(R.id.btChangePos);
        btSize = findViewById(R.id.btChangeSize);
        ivFace = findViewById(R.id.ivFace);
        faceRecContainer = findViewById(R.id.fl_rec_container);

        MyOnclickListener listener = new MyOnclickListener();
        btStart.setOnClickListener(listener);
        btClose.setOnClickListener(listener);
        btPos.setOnClickListener(listener);
        btSize.setOnClickListener(listener);
    }


    class MyOnclickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btStartFaceRec:
                    startFaceRec();
                    break;
                case R.id.btCloseFaceRec:
                    stopFaceRec();
                    break;
                case R.id.btChangePos:
                    changePos();
                    break;
                case R.id.btChangeSize:
                    changeSize();
                    break;
            }
        }
    }

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private int left = 0;
    private int top = 0;
    private int width = 600;
    private int height = 600;

    private void startFaceRec(){
        Rect rect = new Rect(left, top, left + width, top + height);
        FaceRecognizeManager.getInstance().setFindFaceListener(new IFaceRecognizeManager.FindFaceListener() {
            @Override
            public void onFaceFind(final Bitmap bitmap) {
                Timber.d("on face find");
                if(bitmap != null && !bitmap.isRecycled()){
                    bitmap.recycle();
                }
               /* mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        ivFace.setImageBitmap(bitmap);
                    }
                });*/
            }
        });
        FaceRecognizeManager.getInstance().startFaceRecognize(this, faceRecContainer, rect);
    }

    private void stopFaceRec(){
        FaceRecognizeManager.getInstance().stopFaceRecognize(faceRecContainer);
    }

    private void changePos(){
        int containerWidth =  faceRecContainer.getWidth();
        int containerHeight = faceRecContainer.getHeight();
        Random random = new Random(System.currentTimeMillis());
        left = random.nextInt(containerWidth - width);
        top = random.nextInt(containerHeight - height);
    }

    private void changeSize(){
        int containerWidth =  faceRecContainer.getWidth();
        int containerHeight = faceRecContainer.getHeight();
        Random random = new Random(System.currentTimeMillis());
        width = random.nextInt(containerWidth - left);
        height = random.nextInt(containerHeight - height);
    }

}
