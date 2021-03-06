/**
 * Created by Fabrice Armisen (farmisen@gmail.com) on 1/3/16.
 */

package com.lwansbrough.RCTCamera;

import android.content.Context;
import android.hardware.SensorManager;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;

import java.util.List;

public class RCTCameraView extends ViewGroup {
  private OrientationEventListener _orientationListener;
  private final Context _context;
  private RCTCameraViewFinderBase _viewFinder = null;
  private int _actualDeviceOrientation = -1;
  private int _aspect = RCTCameraModule.RCT_CAMERA_ASPECT_FIT;
  private int _captureMode = RCTCameraModule.RCT_CAMERA_CAPTURE_MODE_STILL;
  private String _captureQuality = "high";
  private int _torchMode = -1;
  private int _flashMode = -1;

  public static RCTCameraView cameraView = null;


  public RCTCameraView(Context context) {
    super(context);
    this._context = context;
    cameraView = this;

//
//    _orientationListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
//      @Override
//      public void onOrientationChanged(int orientation) {
//        if (setActualDeviceOrientation(_context)) {
//          layoutViewFinder();
//        }
//      }
//    };
//
//    if (_orientationListener.canDetectOrientation()) {
//      _orientationListener.enable();
//    } else {
//      _orientationListener.disable();
//    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    layoutViewFinder(left, top, right, bottom);
  }

  @Override
  public void onViewAdded(View child) {
    if (this._viewFinder == child) return;
    // remove and readd view to make sure it is in the back.
    // @TODO figure out why there was a z order issue in the first place and fix accordingly.
    this.removeView((View) this._viewFinder);
    this.addView((View) this._viewFinder, 0);
  }

  public void setAspect(int aspect) {
    this._aspect = aspect;
    if (this._captureMode != RCTCameraModule.RCT_CAMERA_CAPTURE_MODE_VIDEO)
      layoutViewFinder();
  }

  public void setCameraType(final int type) {
    if (null != this._viewFinder) {
      this._viewFinder.setCameraType(type);
      if (this._captureMode != RCTCameraModule.RCT_CAMERA_CAPTURE_MODE_VIDEO)
        getCameraInstance().adjustPreviewLayout(type);

    } else {
      createViewFinder(type);
    }
  }

  private void createViewFinder(int type) {
    if (this._captureMode == RCTCameraModule.RCT_CAMERA_CAPTURE_MODE_VIDEO) {
      _viewFinder = new RCTRecordViewFinder(_context, type);
    } else {

      _viewFinder = new RCTCameraViewFinder(_context, type);
    }
    _viewFinder.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    if (-1 != this._flashMode) {
      _viewFinder.setFlashMode(this._flashMode);
    }
    if (-1 != this._torchMode) {
      _viewFinder.setFlashMode(this._torchMode);
    }
    this._viewFinder.setCaptureMode(this._captureMode);
    addView((View) _viewFinder);
  }

  public void setCaptureMode(final int captureMode) {
    this._captureMode = captureMode;
    if (this._viewFinder != null) {
      this._viewFinder.setCaptureMode(captureMode);
    }
  }

  public void setCaptureQuality(String captureQuality) {
    this._captureQuality = captureQuality;
    if (this._viewFinder != null) {
      this._viewFinder.setCaptureQuality(captureQuality);
    }
  }

  public void setTorchMode(int torchMode) {
    this._torchMode = torchMode;
    if (this._viewFinder != null) {
      this._viewFinder.setTorchMode(torchMode);
    }
  }

  public void setFlashMode(int flashMode) {
    this._flashMode = flashMode;
    if (this._viewFinder != null) {
      this._viewFinder.setFlashMode(flashMode);
    }
  }

  public void setOrientation(int orientation) {
    if (this._captureMode != RCTCameraModule.RCT_CAMERA_CAPTURE_MODE_VIDEO)
      getCameraInstance().setOrientation(orientation);

    if (this._viewFinder != null) {
      layoutViewFinder();
    }
  }

  public void setBarcodeScannerEnabled(boolean barcodeScannerEnabled) {
    if (this._captureMode != RCTCameraModule.RCT_CAMERA_CAPTURE_MODE_VIDEO)
      getCameraInstance().setBarcodeScannerEnabled(barcodeScannerEnabled);
  }

  public void setBarCodeTypes(List<String> types) {
    if (this._captureMode != RCTCameraModule.RCT_CAMERA_CAPTURE_MODE_VIDEO)
      getCameraInstance().setBarCodeTypes(types);
  }

  private boolean setActualDeviceOrientation(Context context) {
    int actualDeviceOrientation = getDeviceOrientation(context);
    if (_actualDeviceOrientation != actualDeviceOrientation) {
      _actualDeviceOrientation = actualDeviceOrientation;
      if (this._captureMode != RCTCameraModule.RCT_CAMERA_CAPTURE_MODE_VIDEO)
        getCameraInstance().setActualDeviceOrientation(_actualDeviceOrientation);
      return true;
    } else {
      return false;
    }
  }

  private RCTCamera getCameraInstance() {

    return RCTCamera.getInstance() == null ? RCTCamera.createInstance(getDeviceOrientation(this._context)) : RCTCamera.getInstance();
  }


  private int getDeviceOrientation(Context context) {
    return ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getOrientation();
  }

  private void layoutViewFinder() {
    layoutViewFinder(this.getLeft(), this.getTop(), this.getRight(), this.getBottom());
  }

  private void layoutViewFinder(int left, int top, int right, int bottom) {
    if (null == _viewFinder) {
      return;
    }
    if (right - left == 0) return;

    float width = right - left;
    float height = bottom - top;
    int viewfinderWidth;
    int viewfinderHeight;
    double ratio;
    switch (this._aspect) {
      case RCTCameraModule.RCT_CAMERA_ASPECT_FIT:
        ratio = this._viewFinder.getRatio();
        if (ratio * height > width) {
          viewfinderHeight = (int) (width / ratio);
          viewfinderWidth = (int) width;
        } else {
          viewfinderWidth = (int) (ratio * height);
          viewfinderHeight = (int) height;
        }
        break;
      case RCTCameraModule.RCT_CAMERA_ASPECT_FILL:
        ratio = this._viewFinder.getRatio();
        if (ratio * height < width) {
          viewfinderHeight = (int) (width / ratio);
          viewfinderWidth = (int) width;
        } else {
          viewfinderWidth = (int) (ratio * height);
          viewfinderHeight = (int) height;
        }
        break;
      default:
        viewfinderWidth = (int) width;
        viewfinderHeight = (int) height;
    }

    int viewFinderPaddingX = (int) ((width - viewfinderWidth) / 2);
    int viewFinderPaddingY = (int) ((height - viewfinderHeight) / 2);

    this._viewFinder.layout(viewFinderPaddingX, viewFinderPaddingY, viewFinderPaddingX + viewfinderWidth, viewFinderPaddingY + viewfinderHeight);
    this.postInvalidate(this.getLeft(), this.getTop(), this.getRight(), this.getBottom());
  }

  public void capture(ReadableMap options, Promise promise) {
    this._viewFinder.capture(options, promise);
  }

  public void stopCapture(Promise promise) {
    this._viewFinder.stopCapture(promise);
  }


  public void release() {

    this._viewFinder.release();
    ;
  }
}
