package com.mvcoder.facerecognize;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.widget.FrameLayout;

public interface IFaceRecognizeManager {

    void startFaceRecognize(@NonNull Context context, @NonNull FrameLayout frameLayout, @NonNull Rect rect);


    void stopFaceRecognize(@NonNull FrameLayout surfaceContainer);

    void setFindFaceListener(FindFaceListener listener);

    interface FindFaceListener{
        void onFaceFind(Bitmap bitmap);
    }

}
