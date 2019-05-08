package com.mvcoder.facerecognize.utils;

public interface IPreviewPolicy {

    boolean canPreview();

    void incrementPreviewNum();

    int getPreviewFrameNums();

    void previewState(boolean success);

}
