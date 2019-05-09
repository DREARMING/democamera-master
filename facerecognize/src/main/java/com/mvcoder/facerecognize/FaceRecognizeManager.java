package com.mvcoder.facerecognize;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.mvcoder.facerecognize.utils.ByteArrayOutputStreamCounter;
import com.mvcoder.facerecognize.utils.FaceHelper;
import com.mvcoder.facerecognize.utils.IPreviewPolicy;
import com.mvcoder.facerecognize.utils.PreviewPolicy;
import com.mvcoder.facerecognize.view.AutoFitTextureView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;

public class FaceRecognizeManager implements IFaceRecognizeManager {

    private FindFaceListener findFaceListener;
    private final static String TAG_TEXTURE_VIEW = "TAG_TEXTUREVIEW";
    private Camera mCamera;
    private int cameraId = -1;

    //private volatile boolean isFaceRecRunning = false;
    private AtomicInteger faceRecRunningState = new AtomicInteger(0);

    /**
     * 预览界面和拍摄到的图片 需要旋转到正确的视角的角度
     */
    private int mOrienta;

    private int displayOrientation = 0;

    /**
     * 用来渲染摄像头画面的 TextView 的长宽，用于确定Camera的预览界面的宽高
     */
    private Point mTextureViewPoint;

    private FaceRecPreviewCallback previewCallback;

    private HandlerThread mFaceHandlerThread;
    private volatile Handler mFaceHandle;
    private FaceHelper mFaceHelper = FaceHelper.getInstance();

    private static volatile FaceRecognizeManager manager;

    public static FaceRecognizeManager getInstance(){
        if(manager == null){
            synchronized (FaceRecognizeManager.class){
                if(manager == null){
                    manager = new FaceRecognizeManager();
                }
            }
        }
        return manager;
    }

    @Override
    public void startFaceRecognize(@NonNull Context context,@NonNull FrameLayout frameLayout,@NonNull Rect rect) {
        if(!faceRecRunningState.compareAndSet(0, 1)) return;
        /*if(isFaceRecRunning) return;
        isFaceRecRunning = true;*/
        //开始人脸识别线程
        runFaceRecThread();
        //UI操作，注意线程环境
        AutoFitTextureView textureView = new AutoFitTextureView(context);
        textureView.setTag(TAG_TEXTURE_VIEW);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(rect.width(),rect.height());
        layoutParams.topMargin=rect.top;
        layoutParams.leftMargin=rect.left;
        textureView.setLayoutParams(layoutParams);
        setSurfaceTextureListener(context, textureView);

        //添加到容器类中，当可见之后，将会初始化摄像头
        frameLayout.addView(textureView,0);
    }

    @Override
    public void stopFaceRecognize(@NonNull FrameLayout surfaceContainer) {
        //if(!isFaceRecRunning) return;
        if(!faceRecRunningState.compareAndSet(2,3)) return;
        if(mCamera != null) {
            View view = surfaceContainer.findViewWithTag(TAG_TEXTURE_VIEW);
            if (view == null) throw new IllegalStateException("can't find textureview!!");
            surfaceContainer.removeView(view);
        }
    }

    @Override
    public void setFindFaceListener(FindFaceListener findFaceListener) {
        this.findFaceListener = findFaceListener;
    }

    private void setSurfaceTextureListener(final Context context, AutoFitTextureView textureView) {
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(final SurfaceTexture surface, int width, int height) {
                //main 线程
                Timber.d("surface texture size, width : %d, height: %d", width, height);
                if(mTextureViewPoint == null){
                    int w = width;
                    int h = height;
                    if(width < height){
                        w = height;
                        h = width;
                    }
                    mTextureViewPoint = new Point(w, h);
                }
                if(mCamera == null && mFaceHandle != null){
                    mFaceHandle.post(new Runnable() {
                        @Override
                        public void run() {
                            //耗时操作，异步执行
                            openCamera(context, surface);
                            faceRecRunningState.compareAndSet(1,2);
                        }
                    });

                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if(mFaceHandle != null){
                    mFaceHandle.post(new Runnable() {
                        @Override
                        public void run() {
                            closeCamera();
                            stopFaceRecThread();
                        }
                    });
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    private void runFaceRecThread(){
        //提高线程的优先级，让人脸识别操作调度得密集点
        mFaceHandlerThread = new HandlerThread("FaceHandlerThread", Thread.MAX_PRIORITY);
        mFaceHandlerThread.start();
        //耗时，等待 mFaceHandlerThread 初始化looper才行
        mFaceHandle = new Handler(mFaceHandlerThread.getLooper());
    }

    private  void stopFaceRecThread(){
        //结束线程
        if(mFaceHandlerThread != null)
            mFaceHandlerThread.quit();
        mFaceHandle = null;
        mFaceHandlerThread = null;
        Timber.d("FaceHandlerThread stop");
        //这里是一次人脸识别的终点
        //isFaceRecRunning = false;
        faceRecRunningState.compareAndSet(3,0);

    }

    private synchronized void closeCamera() {
        if(mCamera != null){
            mCamera.setOneShotPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Timber.d("camera close");
        }
    }

    private void openCamera(Context context, SurfaceTexture surface) {
        try {
            if (mCamera == null) {
                //如果找不到前置摄像头，将会找后置摄像头
                cameraId = getFrontCameraId();
                mCamera = Camera.open(cameraId);//可以根据ID使用不同的摄像头
            }
            mCamera.setPreviewTexture(surface);
        } catch (RuntimeException e) {
            e.printStackTrace();
            Timber.e(e);
        } catch (IOException e) {
            e.printStackTrace();
            Timber.e(e);
        }
        if(mCamera == null){
            Timber.e("获取摄像头失败");
            throw new IllegalStateException("无法打开Camera，请检查设备是否具备摄像头");
        }
        initCameraParas(context);
        startPreview();
    }

    private void initCameraParas(Context context){
        //纠正预览界面角度
        //mOrienta 记录预览画面的偏移
        mOrienta = setCameraDisplayOrientation(context, cameraId, mCamera);

        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();//获得相机预览所支持的大小。
        Timber.d("supportPreviewSize num is : %d", previewSizes != null ? previewSizes.size() : 0);
        if(previewSizes == null || previewSizes.size() == 0) {
            throw new IllegalStateException("未知错误，无法获取摄像头的支持预览界面大小");
        }
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (Camera.Size cs : previewSizes) {
            sb.append((index++));
            sb.append("=:");
            sb.append(cs.width).append(",").append(cs.height).append("  ");
        }
        Timber.d("support priviewsize :  %s", sb.toString());
        int bestPreviewIndex = 0;
        if (mTextureViewPoint != null) {
            bestPreviewIndex = findBestPreviewSizeIndex(previewSizes, mTextureViewPoint);
            Timber.d("textureivew size, width : %d, height: %d", mTextureViewPoint.x, mTextureViewPoint.y);
            Timber.d("textureivew bestIndex is : %d, size - width: %d, height: %d", bestPreviewIndex,
                    previewSizes.get(bestPreviewIndex).width, previewSizes.get(bestPreviewIndex).height);
        }
        Camera.Size size1 = previewSizes.get(bestPreviewIndex);//default 2,4
        Timber.d("prepare start preview , widht : %s, height : %s", size1.width, size1.height);
        parameters.setPreviewSize(size1.width, size1.height);


        Camera.Size previewSize = mCamera.new Size(size1.width, size1.height);
        mCamera.setParameters(parameters);

        ///初始化预览 callback
        if(previewCallback == null) {
            //拍摄的照片都需要用Matrix进行旋转，每个Matrix都是用这个旋转角度，基本不会发生变化，可以重用一个Matrix呢
            previewCallback = new FaceRecPreviewCallback(previewSize, getRotateMatrix(mOrienta));
        }
    }


    private void startPreview(){
        if(mCamera != null){
            //设置一次性预览监听
            mCamera.setOneShotPreviewCallback(previewCallback);
            mCamera.startPreview();
            Timber.d("camera start preview");
        }
    }

    private int getFrontCameraId() {
        int cameraNums = Camera.getNumberOfCameras();
        for (int i = 0; i < cameraNums; i++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return i;
            }
        }
        Timber.d("没有前置摄像头");
        if (cameraNums > 0) {
            Timber.d("采用后置摄像头");
            return 0;
        }
        return -1;
    }

    /**
     * 用来设置 Camera 的预览角度
     *
     * @param cameraId
     * @param camera
     */
    private int setCameraDisplayOrientation(@NonNull Context context, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;   // compensate the mirror
            Timber.d("前置摄像头，显示Orientation is : %d", result);
        } else {
            // back-facing
            result = (info.orientation - degrees + 360) % 360;
            Timber.d("后置摄像头，显示Orientation is : %d", result);
        }
        //当前Activity旋转的角度
        this.displayOrientation = degrees;

        camera.setDisplayOrientation(result);
        return result;
    }

    private static int findBestPreviewSizeIndex(@NonNull List<Camera.Size> previewSizes, @NonNull Point textureResoulution) {
        int bestIndex = 0;
        int diff = Integer.MAX_VALUE;
        for (int i = 0; i < previewSizes.size(); i++) {
            Camera.Size size = previewSizes.get(i);
            int newX = size.width;
            int newY = size.height;
            int newDiff = Math.abs(newX - textureResoulution.x) + Math.abs(newY - textureResoulution.y);
            if (newDiff == 0) {
                bestIndex = i;
                break;
            } else if (newDiff < diff) {
                bestIndex = i;
                diff = newDiff;
            }
        }
        return bestIndex;
    }


    class FaceRecPreviewCallback implements Camera.PreviewCallback{

        private int index;

        private IPreviewPolicy previewPolicy = new PreviewPolicy();

        private Camera.Size previewSize;
        private Matrix matrix;
        /**
         * 用于记录 jpg buffer 最大的字节数，避免申请过多字节
         */
        private ByteArrayOutputStreamCounter counter = new ByteArrayOutputStreamCounter(8192);

        public FaceRecPreviewCallback(@NonNull Camera.Size previewSize, @NonNull Matrix matrix){
            this.previewSize = previewSize;
            this.matrix = matrix;
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if(!previewPolicy.canPreview()) {
                resetPreviewCallback();
                return;
            }
            //增加预览次数
            previewPolicy.incrementPreviewNum();
            Timber.i("收到相机回调：onpreviewframe() , index : %d", index++);
            if (data != null && data.length > 0 && mFaceHandle != null) {
                mFaceHandle.post(new FaceRecTask(data, previewSize, matrix, counter, previewPolicy));
            }else{
                //记录预览失败
                previewPolicy.previewState(false);
                resetPreviewCallback();
            }
        }
    }

    private void resetPreviewCallback(){
        if(mCamera != null){
            mCamera.setOneShotPreviewCallback(previewCallback);
        }
    }

    private Matrix getRotateMatrix(int orientation){
        Matrix mMatrix = new Matrix();
        switch (orientation) {
            case 90:
                mMatrix.postRotate(270);
                break;
            case 270:
                mMatrix.postRotate(90);
                break;
            default:
                mMatrix.postRotate(orientation);
                break;
        }
        return mMatrix;
    }

    class FaceRecTask implements Runnable {

        private byte[] mData;
        private Camera.Size previewSize;
        private Matrix matrix;
        private ByteArrayOutputStreamCounter counter;
        private IPreviewPolicy previewPolicy;

        public FaceRecTask(@NonNull byte[] data, @NonNull Camera.Size preiviewSize, @NonNull Matrix mOrientMatrix,
                           @NonNull ByteArrayOutputStreamCounter counter,@NonNull IPreviewPolicy previewPolicy) {
            mData = data;
            this.previewSize = preiviewSize;
            this.matrix = mOrientMatrix;
            this.counter = counter;
            this.previewPolicy = previewPolicy;
        }

        @Override
        public void run() {
            Bitmap bitmap = null;
            Bitmap mFaceBitmap = null;
            long startTime = System.currentTimeMillis(), endTime = 0;
            String logMsg = " ";
            //int type = -1;
            boolean findFace = false;
            ByteArrayOutputStream mBitmapOutput = null;
            try {
                int width = previewSize.width;
                int height = previewSize.height;
                YuvImage yuvImage = new YuvImage(mData, ImageFormat.NV21, width, height, null);
                mData = null;   //方便内存回收
                //设置初始化大小，防止多次grow造成的严重的性能损耗
                mBitmapOutput = new ByteArrayOutputStream(counter.getMaxBufferSize());
                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, mBitmapOutput);
                /*//必须要一致，否则做不到优化一倍内存
                Timber.d("YUVImage size ： %d, IOBuffer size is : %d", yuvImage.getYuvData().length, mBitmapOutput.size());*/
                counter.save(mBitmapOutput.size());

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;//必须设置为565，否则无法检测

                bitmap = BitmapFactory.decodeByteArray(mBitmapOutput.toByteArray(), 0, mBitmapOutput.size(), options);
                if (bitmap != null) {
                    mBitmapOutput.reset();
                    mFaceBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                    if(BuildConfig.DEBUG) {
                        endTime = System.currentTimeMillis();
                        logMsg = "识别人脸前耗时时间：" + (endTime - startTime);
                    }
                    FaceDetector.Face[] faces = mFaceHelper.findFaces(mFaceBitmap);
                    if(BuildConfig.DEBUG) {
                        logMsg = logMsg + ",==识别人脸时间:" + (System.currentTimeMillis() - endTime) + ",mOrienta:" + mOrienta + ",w:" + mFaceBitmap.getWidth() + ",h:" + mFaceBitmap.getHeight() + ",degrees:" + displayOrientation;//width:"+mBitMap.getWidth()+",height:"+mBitMap.getHeight()+",
                        Timber.d(logMsg);
                    }
                    //FaceDetector.Face facePostion = null;
                    if (faces != null) {
                        for (FaceDetector.Face face : faces) {
                            if (face == null) {
                                bitmap.recycle();
                                bitmap = null;
                                mFaceBitmap.recycle();
                                mFaceBitmap = null;
                                mBitmapOutput.close();
                                mBitmapOutput = null;
                                //Logger.e("无人脸");
                                //type = 0;
                                break;
                            } else {
                                //Logger.e("有人脸");
                               // facePostion = face;
                                findFace = true;
                                Timber.d("有人脸");
                                //type = 1;
                                break;
                            }
                        }
                        if(findFace && findFaceListener != null){
                            findFaceListener.onFaceFind(mFaceBitmap);
                            previewPolicy.previewState(true);
                            resetPreviewCallback();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bitmap != null) {
                    bitmap.recycle();
                    bitmap = null;
                }
                if (mFaceBitmap != null && !findFace) {
                    mFaceBitmap.recycle();
                    mFaceBitmap = null;
                }
                if (mBitmapOutput != null) {
                    try {
                        mBitmapOutput.close();
                        mBitmapOutput = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //没有找到人脸信息，赶紧重拍
                if (!findFace) {
                    previewPolicy.previewState(false);
                    resetPreviewCallback();
                }
            }
        }
    }

}
