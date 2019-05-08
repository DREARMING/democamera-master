package com.mvcoder.facerecognize;

public interface IFaceRecognizeManager {

    void startFaceRecognize();


    void stopFaceRecognize();


    interface FindFaceListener{
        void onFaceFind();
    }

}
