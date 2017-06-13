package com.lwansbrough.RCTCamera;

import android.view.View;
import android.view.ViewGroup;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;

/**
 * Created by wwm on 2017-06-13.
 */

public  interface RCTCameraViewFinderBase  {
  void setCameraType(int type);

  void setLayoutParams(ViewGroup.LayoutParams layoutParams);

  void setFlashMode(int flashMode);

  void setCaptureMode(int captureMode);

  void setCaptureQuality(String captureQuality);

  void setTorchMode(int torchMode);

  void layout(int viewFinderPaddingX, int viewFinderPaddingY, int i, int i1);

  void capture(ReadableMap options, Promise promise);

  void stopCapture(Promise promise);

  void release();

  double getRatio();
}
