package com.mvcoder.facerecognize.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class PreviewPolicy implements IPreviewPolicy{

    private long lastPreviewTime = System.currentTimeMillis();
    private int previewNum = 0;
    private AtomicInteger previewFailNum = new AtomicInteger(0);
    //一秒2次
    private final static int MIN_PREVIEW_INTERVAL_TIME_IN_MILL = 500;

    /**
     * @return 判断是否可以继续预览下一帧
     */
    @Override
    public boolean canPreview() {
        boolean tooShort =  System.currentTimeMillis() - lastPreviewTime < MIN_PREVIEW_INTERVAL_TIME_IN_MILL;
        //如果距离上一帧的预览时间太短，将不可预览
        if(tooShort) return false;
        return !isFailNumTooMuch();
    }

    @Override
    public void incrementPreviewNum() {
        lastPreviewTime = System.currentTimeMillis();
        previewNum++;
    }

    @Override
    public int getPreviewFrameNums() {
        return previewNum;
    }

    @Override
    public void previewState(boolean success) {
        if(success) {
            previewFailNum.set(0);
        }else {
            previewFailNum.incrementAndGet();
        }
    }

    /**
     * 用于判断失败次数是否太多，太多将会延迟可预览的时间
     */
    private boolean isFailNumTooMuch(){
        //50次失败不算太多 -- 25秒
        if(previewFailNum.get() <= 50) return false;
        //50 - 200 次的话，可以1秒1次
        if(previewFailNum.get() > 50 && previewFailNum.get() <= 200 && System.currentTimeMillis() - lastPreviewTime <= 1000) return true;
        //超过50次以上 -- 175秒内都不成功,2秒1次
        if(previewFailNum.get() > 200 && System.currentTimeMillis() - lastPreviewTime <= 2000) return true;
        return false;
    }
}
