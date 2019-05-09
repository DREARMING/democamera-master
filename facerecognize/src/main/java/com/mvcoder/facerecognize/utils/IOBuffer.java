package com.mvcoder.facerecognize.utils;

import java.io.ByteArrayOutputStream;

public class IOBuffer extends ByteArrayOutputStream {

    public IOBuffer(){
        super();
    }

    public IOBuffer(int size){
        super(size);
    }

    /**
     * you can't be change the buf
     * @return
     */
    public byte[] getByteArray(){
        if(size() == buf.length) return buf;
        return toByteArray();
    }

}
