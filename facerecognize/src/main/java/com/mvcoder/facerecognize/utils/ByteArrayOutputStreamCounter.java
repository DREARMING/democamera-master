package com.mvcoder.facerecognize.utils;

import timber.log.Timber;

public class ByteArrayOutputStreamCounter {

    private volatile int max;

    public ByteArrayOutputStreamCounter(int max){
        this.max = max;
    }

    public int getMaxBufferSize(){
        return max;
    }

    public void save(int size){
        if(size > max){
            Timber.d("max jpg buffer size change, old size : %d, new size : %d", max, size);
            this.max = size;
        }
    }

}
