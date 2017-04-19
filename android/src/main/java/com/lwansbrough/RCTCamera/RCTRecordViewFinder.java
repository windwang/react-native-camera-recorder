package com.lwansbrough.RCTCamera;

import android.content.Context;
import android.os.Environment;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.wind.camera.MediaRecorderBase;
import com.wind.camera.MediaRecorderNative;
import com.wind.camera.VCamera;
import com.wind.camera.model.AutoVBRMode;
import com.wind.camera.model.MediaObject;
import com.wind.camera.model.MediaRecorderConfig;
import com.wind.camera.util.DeviceUtils;
import com.wind.camera.util.FileUtils;
import com.yixia.videoeditor.adapter.UtilityAdapter;

import java.io.File;
import java.io.SyncFailedException;
import java.util.List;

import static com.wind.camera.MediaRecorderBase.SMALL_VIDEO_WIDTH;

/**
 * Created by wwm on 2017-04-17.
 */

public class RCTRecordViewFinder extends SurfaceView implements
        MediaRecorderBase.OnErrorListener, View.OnClickListener, MediaRecorderBase.OnPreparedListener,
        MediaRecorderBase.OnEncodeListener {

    /**
     * SDK视频录制对象
     */
    private MediaRecorderBase mMediaRecorder;

    MediaRecorderConfig.Builder builder;

    /**
     * 视频信息
     */
    private MediaObject mMediaObject;
    private int aspect;
    private int captureMode;
    private int cameraType;
    private String captureQuality;
    private int torchMode;
    private int flashMode;
    private int orientation;
    private boolean barcodeScannerEnabled;
    private List<String> barCodeTypes;
    private double ratio = 1080f / 1920f;

    private Promise promise;


    public RCTRecordViewFinder(Context context, int type) {
        super(context);

        initSmallVideo(context);


    }

    public void initSmallVideo(Context context) {

//        getHolder().addCallback(this);
//        setFocusable(true);
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
//            getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//        }
//        setFocusableInTouchMode(true);
//        requestFocus();

        // 设置拍摄视频缓存路径
        File dcim = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        if (DeviceUtils.isZte()) {
            if (dcim.exists()) {
                VCamera.setVideoCachePath(dcim + "/hschool/");
            } else {
                VCamera.setVideoCachePath(dcim.getPath().replace("/sdcard/",
                        "/sdcard-ext/")
                        + "/hschool/");
            }
        } else {
            VCamera.setVideoCachePath(dcim + "/hschool/");
        }
        // 开启log输出,ffmpeg输出到logcat
        VCamera.setDebugMode(true);

        // 初始化拍摄SDK，必须
        VCamera.initialize(context);
    }


    /**
     * 初始化拍摄SDK
     */
    private void initMediaRecorder() {


        builder = new MediaRecorderConfig.Builder()
                .doH264Compress(new AutoVBRMode(18)

                )
                .setMediaBitrateConfig(new AutoVBRMode(18)

                )
                .smallVideoWidth(480)
                .smallVideoHeight(360)
                .recordTimeMax(6000 * 1000)
                .maxFrameRate(20)
                .captureThumbnailsTime(1)
                .recordTimeMin((int) (1.5 * 1000));

        this.ratio = 360f / 480;

        UtilityAdapter.freeFilterParser();
        UtilityAdapter.initFilterParser();


        mMediaRecorder = new MediaRecorderNative();

        mMediaRecorder.setOnErrorListener(this);
        mMediaRecorder.setOnEncodeListener(this);
        mMediaRecorder.setOnPreparedListener(this);


        File f = new File(VCamera.getVideoCachePath());
        if (!FileUtils.checkFile(f)) {
            f.mkdirs();
        }
        String key = String.valueOf(System.currentTimeMillis());
        mMediaObject = mMediaRecorder.setOutputDirectory(key,
                VCamera.getVideoCachePath() + key);
        mMediaRecorder.setSurfaceHolder(this.getHolder());
        mMediaRecorder.prepare();

        // mMediaRecorder.startPreview();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        initMediaRecorder();
    }

    /**
     * 初始化画布
     */
    private void initSurfaceView() {
//        final int w = DeviceUtils.getScreenWidth(this.getContext());
//        //  ((RelativeLayout.LayoutParams) mBottomLayout.getLayoutParams()).topMargin = (int) (w / (SMALL_VIDEO_WIDTH / (MediaRecorderBase.SMALL_VIDEO_HEIGHT * 1.0f)));
//        int width = w;
        this.ratio = MediaRecorderBase.SMALL_VIDEO_HEIGHT / (MediaRecorderBase.SMALL_VIDEO_WIDTH * 1.0f);

    }

//    @Override
//    protected void onLayout(boolean changed, int l, int t, int r, int b) {
//
//    }


    public void setAspect(int aspect) {
        this.aspect = aspect;

    }

    public int getAspect() {
        return aspect;
    }

    public void setCaptureMode(int captureMode) {
        this.captureMode = captureMode;
    }

    public int getCaptureMode() {
        return captureMode;
    }

    public void setCameraType(int cameraType) {
        this.cameraType = cameraType;
    }

    public int getCameraType() {
        return cameraType;
    }

    public void setCaptureQuality(String captureQuality) {
        this.captureQuality = captureQuality;

    }

    public String getCaptureQuality() {
        return captureQuality;
    }

    public void setTorchMode(int torchMode) {
        this.torchMode = torchMode;
    }

    public int getTorchMode() {
        return torchMode;
    }

    public void setFlashMode(int flashMode) {
        this.flashMode = flashMode;
    }

    public int getFlashMode() {
        return flashMode;
    }


    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setBarcodeScannerEnabled(boolean barcodeScannerEnabled) {
        this.barcodeScannerEnabled = barcodeScannerEnabled;
    }

    public boolean isBarcodeScannerEnabled() {
        return barcodeScannerEnabled;
    }

    public void setBarCodeTypes(List<String> barCodeTypes) {
        this.barCodeTypes = barCodeTypes;
    }

    public List<String> getBarCodeTypes() {
        return barCodeTypes;
    }

    public double getRatio() {

        return ratio;

    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }


    public void capture(ReadableMap options, Promise promise) {
        this.promise = promise;

        String quality = options.getString("quality");
        switch (quality) {
            case RCTCameraModule.RCT_CAMERA_CAPTURE_QUALITY_LOW:
                builder.smallVideoWidth(320)
                        .smallVideoHeight(240);
                break;
            case RCTCameraModule.RCT_CAMERA_CAPTURE_QUALITY_MEDIUM:
                builder.smallVideoWidth(480)
                        .smallVideoHeight(360);
                break;
            case RCTCameraModule.RCT_CAMERA_CAPTURE_QUALITY_HIGH:
                builder.smallVideoWidth(720)
                        .smallVideoHeight(540);
                break;
            case RCTCameraModule.RCT_CAMERA_CAPTURE_QUALITY_PREVIEW:
                builder.smallVideoWidth(720)
                        .smallVideoHeight(540);
                break;
            case RCTCameraModule.RCT_CAMERA_CAPTURE_QUALITY_480P:
                builder.smallVideoWidth(853)
                        .smallVideoHeight(480);
                break;
            case RCTCameraModule.RCT_CAMERA_CAPTURE_QUALITY_720P:
                builder.smallVideoWidth(1280)
                        .smallVideoHeight(720);
                break;
            case RCTCameraModule.RCT_CAMERA_CAPTURE_QUALITY_1080P:
                builder.smallVideoWidth(1920)
                        .smallVideoHeight(1080);
                break;
        }


        MediaRecorderConfig config = builder.build();
        MediaRecorderBase.mediaRecorderConfig = config.getMediaBitrateConfig();
        MediaRecorderBase.compressConfig = config.getCompressConfig();
        MediaRecorderBase.doH264Compress = config.isDoH264Compress();
        MediaRecorderBase.MAX_FRAME_RATE = config.getMaxFrameRate();
        MediaRecorderBase.MIN_FRAME_RATE = config.getMinFrameRate();
        MediaRecorderBase.SMALL_VIDEO_HEIGHT = config.getSmallVideoHeight();
        MediaRecorderBase.mVideoBitrate = config.getVideoBitrate();
        MediaRecorderBase.mediaRecorderConfig = config.getMediaBitrateConfig();
        MediaRecorderBase.compressConfig = config.getCompressConfig();
        MediaRecorderBase.CAPTURE_THUMBNAILS_TIME = config.getCaptureThumbnailsTime();

        //MediaRecorderBase.CAPTURE_THUMBNAILS_TIME=1;


////        if( options.hasKey("compress")&&options.getBoolean("compress"))
//        {
//            BaseMediaBitrateConfig  compressMode = new AutoVBRMode();
//           MediaRecorderBase.compressConfig=compressMode;
//        }


        MediaObject.MediaPart part = mMediaRecorder.startRecord();
        if (part == null) {
            promise.reject("START", new SyncFailedException("start fail"));
        }
    }

    public void stopCapture(Promise promise) {
        this.mMediaRecorder.stopRecord();

        promise.resolve("Finished recording.");

        mMediaRecorder.startEncoding();

    }


    @Override
    public void onClick(View v) {

    }

    @Override
    public void onPrepared() {
        initSurfaceView();
    }

    @Override
    public void onVideoError(int what, int extra) {

    }

    @Override
    public void onAudioError(int what, String message) {

    }

    @Override
    public void onEncodeStart() {

    }

    @Override
    public void onEncodeProgress(int progress) {

    }

    @Override
    public void onEncodeComplete() {
        if (this.promise != null) {
            Promise videoPromise = this.promise;
            this.promise = null;
            WritableMap map = Arguments.createMap();
            map.putString("thumb", mMediaObject.getOutputVideoThumbPath());
            map.putString("path", mMediaObject.getOutputTempTranscodingVideoPath());
            videoPromise.resolve(map);
        }
    }

    @Override
    public void onEncodeError() {

    }
}
