/**
 * Created by Fabrice Armisen (farmisen@gmail.com) on 1/4/16.
 * Android video recording support by Marc Johnson (me@marc.mn) 4/2016
 */

package com.lwansbrough.RCTCamera;

import android.content.ContentValues;
import android.content.Intent;
import android.hardware.Camera;
import android.media.*;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Surface;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import static android.app.Activity.RESULT_OK;

public class RCTCameraModule extends ReactContextBaseJavaModule
        implements MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener, LifecycleEventListener {
    private static final String TAG = "RCTCameraModule";
    private static final int ACTION_TAKE_VIDEO = 130;
    public static final int RCT_CAMERA_ASPECT_FILL = 0;
    public static final int RCT_CAMERA_ASPECT_FIT = 1;
    public static final int RCT_CAMERA_ASPECT_STRETCH = 2;
    public static final int RCT_CAMERA_CAPTURE_MODE_STILL = 0;
    public static final int RCT_CAMERA_CAPTURE_MODE_VIDEO = 1;
    public static final int RCT_CAMERA_CAPTURE_TARGET_MEMORY = 0;
    public static final int RCT_CAMERA_CAPTURE_TARGET_DISK = 1;
    public static final int RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL = 2;
    public static final int RCT_CAMERA_CAPTURE_TARGET_TEMP = 3;
    public static final int RCT_CAMERA_ORIENTATION_AUTO = Integer.MAX_VALUE;
    public static final int RCT_CAMERA_ORIENTATION_PORTRAIT = Surface.ROTATION_0;
    public static final int RCT_CAMERA_ORIENTATION_PORTRAIT_UPSIDE_DOWN = Surface.ROTATION_180;
    public static final int RCT_CAMERA_ORIENTATION_LANDSCAPE_LEFT = Surface.ROTATION_90;
    public static final int RCT_CAMERA_ORIENTATION_LANDSCAPE_RIGHT = Surface.ROTATION_270;
    public static final int RCT_CAMERA_TYPE_FRONT = 1;
    public static final int RCT_CAMERA_TYPE_BACK = 2;
    public static final int RCT_CAMERA_FLASH_MODE_OFF = 0;
    public static final int RCT_CAMERA_FLASH_MODE_ON = 1;
    public static final int RCT_CAMERA_FLASH_MODE_AUTO = 2;
    public static final int RCT_CAMERA_TORCH_MODE_OFF = 0;
    public static final int RCT_CAMERA_TORCH_MODE_ON = 1;
    public static final int RCT_CAMERA_TORCH_MODE_AUTO = 2;
    public static final String RCT_CAMERA_CAPTURE_QUALITY_PREVIEW = "preview";
    public static final String RCT_CAMERA_CAPTURE_QUALITY_HIGH = "high";
    public static final String RCT_CAMERA_CAPTURE_QUALITY_MEDIUM = "medium";
    public static final String RCT_CAMERA_CAPTURE_QUALITY_LOW = "low";
    public static final String RCT_CAMERA_CAPTURE_QUALITY_1080P = "1080p";
    public static final String RCT_CAMERA_CAPTURE_QUALITY_720P = "720p";
    public static final String RCT_CAMERA_CAPTURE_QUALITY_480P = "480p";
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private static ReactApplicationContext _reactContext;
    private RCTSensorOrientationChecker _sensorOrientationChecker;

    private MediaRecorder mMediaRecorder;
    private long MRStartTime;
    private File mVideoFile;
    private Camera mCamera = null;
    private Promise mRecordingPromise = null;
    private ReadableMap mRecordingOptions;
    private Boolean mSafeToCapture = true;

    public RCTCameraModule(ReactApplicationContext reactContext) {
        super(reactContext);
        _reactContext = reactContext;
        _sensorOrientationChecker = new RCTSensorOrientationChecker(_reactContext);
        _reactContext.addLifecycleEventListener(this);
    }

    public static ReactApplicationContext getReactContextSingleton() {
        return _reactContext;
    }

    /**
     * Callback invoked on new MediaRecorder info.
     * <p>
     * See https://developer.android.com/reference/android/media/MediaRecorder.OnInfoListener.html
     * for more information.
     *
     * @param mr    MediaRecorder instance for which this callback is being invoked.
     * @param what  Type of info we have received.
     * @param extra Extra code, specific to the info type.
     */
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
            if (mRecordingPromise != null) {
                releaseMediaRecorder(); // release the MediaRecorder object and resolve promise
            }
        }
    }

    /**
     * Callback invoked when a MediaRecorder instance encounters an error while recording.
     * <p>
     * See https://developer.android.com/reference/android/media/MediaRecorder.OnErrorListener.html
     * for more information.
     *
     * @param mr    MediaRecorder instance for which this callback is being invoked.
     * @param what  Type of error that has occurred.
     * @param extra Extra code, specific to the error type.
     */
    public void onError(MediaRecorder mr, int what, int extra) {
        // On any error, release the MediaRecorder object and resolve promise. In particular, this
        // prevents leaving the camera in an unrecoverable state if we crash in the middle of
        // recording.
        if (mRecordingPromise != null) {
            releaseMediaRecorder();
        }
    }

    @Override
    public String getName() {
        return "RCTCameraModule";
    }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        return Collections.unmodifiableMap(new HashMap<String, Object>() {
            {
                put("Aspect", getAspectConstants());
                put("BarCodeType", getBarCodeConstants());
                put("Type", getTypeConstants());
                put("CaptureQuality", getCaptureQualityConstants());
                put("CaptureMode", getCaptureModeConstants());
                put("CaptureTarget", getCaptureTargetConstants());
                put("Orientation", getOrientationConstants());
                put("FlashMode", getFlashModeConstants());
                put("TorchMode", getTorchModeConstants());
            }

            private Map<String, Object> getAspectConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("stretch", RCT_CAMERA_ASPECT_STRETCH);
                        put("fit", RCT_CAMERA_ASPECT_FIT);
                        put("fill", RCT_CAMERA_ASPECT_FILL);
                    }
                });
            }

            private Map<String, Object> getBarCodeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        // @TODO add barcode types
                    }
                });
            }

            private Map<String, Object> getTypeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("front", RCT_CAMERA_TYPE_FRONT);
                        put("back", RCT_CAMERA_TYPE_BACK);
                    }
                });
            }

            private Map<String, Object> getCaptureQualityConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("low", RCT_CAMERA_CAPTURE_QUALITY_LOW);
                        put("medium", RCT_CAMERA_CAPTURE_QUALITY_MEDIUM);
                        put("high", RCT_CAMERA_CAPTURE_QUALITY_HIGH);
                        put("photo", RCT_CAMERA_CAPTURE_QUALITY_HIGH);
                        put("preview", RCT_CAMERA_CAPTURE_QUALITY_PREVIEW);
                        put("480p", RCT_CAMERA_CAPTURE_QUALITY_480P);
                        put("720p", RCT_CAMERA_CAPTURE_QUALITY_720P);
                        put("1080p", RCT_CAMERA_CAPTURE_QUALITY_1080P);
                    }
                });
            }

            private Map<String, Object> getCaptureModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("still", RCT_CAMERA_CAPTURE_MODE_STILL);
                        put("video", RCT_CAMERA_CAPTURE_MODE_VIDEO);
                    }
                });
            }

            private Map<String, Object> getCaptureTargetConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("memory", RCT_CAMERA_CAPTURE_TARGET_MEMORY);
                        put("disk", RCT_CAMERA_CAPTURE_TARGET_DISK);
                        put("cameraRoll", RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL);
                        put("temp", RCT_CAMERA_CAPTURE_TARGET_TEMP);
                    }
                });
            }

            private Map<String, Object> getOrientationConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("auto", RCT_CAMERA_ORIENTATION_AUTO);
                        put("landscapeLeft", RCT_CAMERA_ORIENTATION_LANDSCAPE_LEFT);
                        put("landscapeRight", RCT_CAMERA_ORIENTATION_LANDSCAPE_RIGHT);
                        put("portrait", RCT_CAMERA_ORIENTATION_PORTRAIT);
                        put("portraitUpsideDown", RCT_CAMERA_ORIENTATION_PORTRAIT_UPSIDE_DOWN);
                    }
                });
            }

            private Map<String, Object> getFlashModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("off", RCT_CAMERA_FLASH_MODE_OFF);
                        put("on", RCT_CAMERA_FLASH_MODE_ON);
                        put("auto", RCT_CAMERA_FLASH_MODE_AUTO);
                    }
                });
            }

            private Map<String, Object> getTorchModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("off", RCT_CAMERA_TORCH_MODE_OFF);
                        put("on", RCT_CAMERA_TORCH_MODE_ON);
                        put("auto", RCT_CAMERA_TORCH_MODE_AUTO);
                    }
                });
            }
        });
    }




    /**
     * Release media recorder following video capture (or failure to start recording session).
     * <p>
     * See "Capturing Videos" at https://developer.android.com/guide/topics/media/camera.html for
     * a guideline of steps and more information in general.
     */
    private void releaseMediaRecorder() {
        // Must record at least a second or MediaRecorder throws exceptions on some platforms
        long duration = System.currentTimeMillis() - MRStartTime;
        if (duration < 1500) {
            try {
                Thread.sleep(1500 - duration);
            } catch (InterruptedException ex) {
                Log.e(TAG, "releaseMediaRecorder thread sleep error.", ex);
            }
        }

        // Release actual MediaRecorder instance.
        if (mMediaRecorder != null) {
            // Stop recording video.
            try {

                mMediaRecorder.stop(); // stop the recording
            } catch (Exception ex) {
                Log.e(TAG, "Media recorder stop error.", ex);
            }

            // Optionally, remove the configuration settings from the recorder.
            mMediaRecorder.reset();

            // Release the MediaRecorder.
            mMediaRecorder.release();

            // Reset variable.
            mMediaRecorder = null;
        }

        // Lock the camera so that future MediaRecorder sessions can use it by calling
        // Camera.lock(). Note this is not required on Android 4.0+ unless the
        // MediaRecorder.prepare() call fails.
        if (mCamera != null) {
            mCamera.lock();
        }

        if (mRecordingPromise == null) {
            return;
        }

        File f = new File(mVideoFile.getPath());
        if (!f.exists()) {
            mRecordingPromise.reject(new RuntimeException("There is nothing recorded."));
            mRecordingPromise = null;
            return;
        }

        f.setReadable(true, false); // so mediaplayer can play it
        f.setWritable(true, false); // so can clean it up

        WritableMap response = new WritableNativeMap();
        switch (mRecordingOptions.getInt("target")) {
            case RCT_CAMERA_CAPTURE_TARGET_MEMORY:
                byte[] encoded = convertFileToByteArray(mVideoFile);
                response.putString("data", new String(encoded, Base64.DEFAULT));
                mRecordingPromise.resolve(response);
                f.delete();
                break;
            case RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL:
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DATA, mVideoFile.getPath());
                values.put(MediaStore.Video.Media.TITLE, mRecordingOptions.hasKey("title") ? mRecordingOptions.getString("title") : "video");

                if (mRecordingOptions.hasKey("description")) {
                    values.put(MediaStore.Video.Media.DESCRIPTION, mRecordingOptions.hasKey("description"));
                }

                if (mRecordingOptions.hasKey("latitude")) {
                    values.put(MediaStore.Video.Media.LATITUDE, mRecordingOptions.getString("latitude"));
                }

                if (mRecordingOptions.hasKey("longitude")) {
                    values.put(MediaStore.Video.Media.LONGITUDE, mRecordingOptions.getString("longitude"));
                }

                values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                _reactContext.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                addToMediaStore(mVideoFile.getAbsolutePath());
                response.putString("path", Uri.fromFile(mVideoFile).toString());
                mRecordingPromise.resolve(response);
                break;
            case RCT_CAMERA_CAPTURE_TARGET_TEMP:
            case RCT_CAMERA_CAPTURE_TARGET_DISK:
                response.putString("path", Uri.fromFile(mVideoFile).toString());
                mRecordingPromise.resolve(response);
        }

        mRecordingPromise = null;
    }

    public static byte[] convertFileToByteArray(File f) {
        byte[] byteArray = null;
        try {
            InputStream inputStream = new FileInputStream(f);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024 * 8];
            int bytesRead;

            while ((bytesRead = inputStream.read(b)) != -1) {
                bos.write(b, 0, bytesRead);
            }

            byteArray = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteArray;
    }


    @ReactMethod
    public void capture(final ReadableMap options, final Promise promise) {
        if (RCTCameraView.cameraView != null)
            RCTCameraView.cameraView.capture(options, promise);

    }




    @ReactMethod
    public void stopCapture(final Promise promise) {
        if (RCTCameraView.cameraView != null)
            RCTCameraView.cameraView.stopCapture(promise);
    }

    @ReactMethod
    public void hasFlash(ReadableMap options, final Promise promise) {
        Camera camera = RCTCamera.getInstance().acquireCameraInstance(options.getInt("type"));
        if (null == camera) {
            promise.reject("No camera found.");
            return;
        }
        List<String> flashModes = camera.getParameters().getSupportedFlashModes();
        promise.resolve(null != flashModes && !flashModes.isEmpty());
    }






    private void addToMediaStore(String path) {
        MediaScannerConnection.scanFile(_reactContext, new String[]{path}, null, null);
    }

    /**
     * LifecycleEventListener overrides
     */
    @Override
    public void onHostResume() {
        // ... do nothing
    }

    @Override
    public void onHostPause() {
        // On pause, we stop any pending recording session
        if (mRecordingPromise != null) {
            releaseMediaRecorder();
        }
    }

    @Override
    public void onHostDestroy() {
        // ... do nothing
    }


}
