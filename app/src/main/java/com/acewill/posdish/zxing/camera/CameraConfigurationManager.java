/*
 * Copyright (C) 2008 ZXing authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.acewill.posdish.zxing.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 描述: 相机辅助类，主要用于设置相机的各类参数�?? 核心方法有两个： initFromCameraParameters：计算了屏幕分辨率和当前�?适合的相机像�? setDesiredCameraParameters：读取配置设置相机的对焦模式、闪光灯模式等等
 */
@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
public final class CameraConfigurationManager {
	private static final String TAG = "CameraConfiguration";
	private static final int MIN_PREVIEW_PIXELS = 480 * 320;
	private static final double MAX_ASPECT_DISTORTION = 0.15;
	private static final int TEN_DESIRED_ZOOM = 27;
	private static final Pattern COMMA_PATTERN = Pattern.compile(",");
	private final Context context;

	// 屏幕分辨�?
	private Point screenResolution;
	// 相机分辨�?
	private Point cameraResolution;

	public CameraConfigurationManager(Context context) {
		this.context = context;
	}

	public void initFromCameraParameters(Camera camera) {
		Camera.Parameters parameters = camera.getParameters();
		WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		Point theScreenResolution = new Point();
		theScreenResolution = getDisplaySize(display);

		screenResolution = theScreenResolution;
		Log.i(TAG, "Screen resolution: " + screenResolution);
//		if (ScanActivity.mScreenState == ScanActivity.HORIZONTAL) {
			cameraResolution = findBestPreviewSizeValue(parameters, screenResolution);
//		} else if (ScanActivity.mScreenState == ScanActivity.VERTICAL) {
//			/** 因为换成了竖屏显示，�?以不替换屏幕宽高得出的预览图是变形的 */
//			Point screenResolutionForCamera = new Point();
//			screenResolutionForCamera.x = screenResolution.x;
//			screenResolutionForCamera.y = screenResolution.y;
//			if (screenResolution.x < screenResolution.y) {
//				screenResolutionForCamera.x = screenResolution.y;
//				screenResolutionForCamera.y = screenResolution.x;
//			}
//			cameraResolution = findBestPreviewSizeValue(parameters, screenResolutionForCamera);
//		}
	}

	private Point getDisplaySize(final Display display) {
		final Point point = new Point();
		try {
			display.getSize(point);
		} catch (NoSuchMethodError ignore) {
			point.x = display.getWidth();
			point.y = display.getHeight();
		}
		return point;
	}

	/**
	   * Sets the camera up to take preview images which are used for both preview and decoding.
	   * We detect the preview format here so that buildLuminanceSource() can build an appropriate
	   * LuminanceSource subclass. In the future we may want to force YUV420SP as it's the smallest,
	   * and the planar Y can be used for barcode scanning without a copy in some cases.
	   */
	  void setDesiredCameraParameters(Camera camera) {
	    Camera.Parameters parameters = camera.getParameters();
		List<Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
		int position =0;
		if(supportedPreviewSizes.size()>2){
			position=supportedPreviewSizes.size()/2+1;//supportedPreviewSizes.get();
		}else {
			position=supportedPreviewSizes.size()/2;
		}

//		int width = supportedPreviewSizes.get(position).width;
//		int height = supportedPreviewSizes.get(position).height;
		int width = 240*2;
		int height = 240*2;
	    Log.d(TAG, "Setting preview size: " + cameraResolution);
	    camera.setDisplayOrientation(90);
	    cameraResolution.x=width;
	    cameraResolution.y=height;
	    parameters.setPreviewSize(width,height);
	    setFlash(parameters);
	    setZoom(parameters);
	    //setSharpness(parameters);
	    camera.setParameters(parameters);
	  }

	  private void setFlash(Camera.Parameters parameters) {
		    // FIXME: This is a hack to turn the flash off on the Samsung Galaxy.
		    // And this is a hack-hack to work around a different value on the Behold II
		    // Restrict Behold II check to Cupcake, per Samsung's advice
		    //if (Build.MODEL.contains("Behold II") &&
		    //    CameraManager.SDK_INT == Build.VERSION_CODES.CUPCAKE) {
		    if (Build.MODEL.contains("Behold II") ) { // 3 = Cupcake
		      parameters.set("flash-value", 1);
		    } else {
		      parameters.set("flash-value", 2);
		    }
		    // This is the standard setting to turn the flash off that all devices should honor.
		    parameters.set("flash-mode", "off");
		  }

		  private void setZoom(Camera.Parameters parameters) {

		    String zoomSupportedString = parameters.get("zoom-supported");
		    if (zoomSupportedString != null && !Boolean.parseBoolean(zoomSupportedString)) {
		      return;
		    }

		    int tenDesiredZoom = TEN_DESIRED_ZOOM;

		    String maxZoomString = parameters.get("max-zoom");
		    if (maxZoomString != null) {
		      try {
		        int tenMaxZoom = (int) (10.0 * Double.parseDouble(maxZoomString));
		        if (tenDesiredZoom > tenMaxZoom) {
		          tenDesiredZoom = tenMaxZoom;
		        }
		      } catch (NumberFormatException nfe) {
		        Log.w(TAG, "Bad max-zoom: " + maxZoomString);
		      }
		    }

		    String takingPictureZoomMaxString = parameters.get("taking-picture-zoom-max");
		    if (takingPictureZoomMaxString != null) {
		      try {
		        int tenMaxZoom = Integer.parseInt(takingPictureZoomMaxString);
		        if (tenDesiredZoom > tenMaxZoom) {
		          tenDesiredZoom = tenMaxZoom;
		        }
		      } catch (NumberFormatException nfe) {
		        Log.w(TAG, "Bad taking-picture-zoom-max: " + takingPictureZoomMaxString);
		      }
		    }

		    String motZoomValuesString = parameters.get("mot-zoom-values");
		    if (motZoomValuesString != null) {
		      tenDesiredZoom = findBestMotZoomValue(motZoomValuesString, tenDesiredZoom);
		    }

		    String motZoomStepString = parameters.get("mot-zoom-step");
		    if (motZoomStepString != null) {
		      try {
		        double motZoomStep = Double.parseDouble(motZoomStepString.trim());
		        int tenZoomStep = (int) (10.0 * motZoomStep);
		        if (tenZoomStep > 1) {
		          tenDesiredZoom -= tenDesiredZoom % tenZoomStep;
		        }
		      } catch (NumberFormatException nfe) {
		        // continue
		      }
		    }

		    // Set zoom. This helps encourage the user to pull back.
		    // Some devices like the Behold have a zoom parameter
		    if (maxZoomString != null || motZoomValuesString != null) {
		      parameters.set("zoom", String.valueOf(tenDesiredZoom / 10.0));
		    }

		    // Most devices, like the Hero, appear to expose this zoom parameter.
		    // It takes on values like "27" which appears to mean 2.7x zoom
		    if (takingPictureZoomMaxString != null) {
		      parameters.set("taking-picture-zoom", tenDesiredZoom);
		    }
		  }
		  private static int findBestMotZoomValue(CharSequence stringValues, int tenDesiredZoom) {
			    int tenBestValue = 0;
			    for (String stringValue : COMMA_PATTERN.split(stringValues)) {
			      stringValue = stringValue.trim();
			      double value;
			      try {
			        value = Double.parseDouble(stringValue);
			      } catch (NumberFormatException nfe) {
			        return tenDesiredZoom;
			      }
			      int tenValue = (int) (10.0 * value);
			      if (Math.abs(tenDesiredZoom - value) < Math.abs(tenDesiredZoom - tenBestValue)) {
			        tenBestValue = tenValue;
			      }
			    }
			    return tenBestValue;
			  }

	public void setDesiredCameraParameters(Camera camera, boolean safeMode) {
		Camera.Parameters parameters = camera.getParameters();
		if (parameters == null) {
			Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
			return;
		}
		Log.i(TAG, "Initial camera parameters: " + parameters.flatten());
		if (safeMode) {
			Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
		}

		parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
		camera.setParameters(parameters);

		Camera.Parameters afterParameters = camera.getParameters();
		Size afterSize = afterParameters.getPreviewSize();
		if (afterSize != null && (cameraResolution.x != afterSize.width || cameraResolution.y != afterSize.height)) {
			Log.w(TAG, "Camera said it supported preview size " + cameraResolution.x + 'x' + cameraResolution.y + ", but after setting it, preview size is " + afterSize.width + 'x' + afterSize.height);
			cameraResolution.x = afterSize.width;
			cameraResolution.y = afterSize.height;
		}

		/** 设置相机预览为竖�? */
		camera.setDisplayOrientation(90);
	}

	public Point getCameraResolution() {
		return cameraResolution;
	}

	public Point getScreenResolution() {
		return screenResolution;
	}

	/**
	 * 从相机支持的分辨率中计算出最适合的预览界面尺�?
	 *
	 * @param parameters
	 * @param screenResolution
	 * @return
	 */
	private Point findBestPreviewSizeValue(Camera.Parameters parameters, Point screenResolution) {
		List<Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
		if (rawSupportedSizes == null) {
			Log.w(TAG, "Device returned no supported preview sizes; using default");
			Size defaultSize = parameters.getPreviewSize();
			return new Point(defaultSize.width, defaultSize.height);
		}

		// Sort by size, descending
		List<Size> supportedPreviewSizes = new ArrayList<Size>(rawSupportedSizes);
		Collections.sort(supportedPreviewSizes, new Comparator<Size>() {
			@Override
			public int compare(Size a, Size b) {
				int aPixels = a.height * a.width;
				int bPixels = b.height * b.width;
				if (bPixels < aPixels) {
					return -1;
				}
				if (bPixels > aPixels) {
					return 1;
				}
				return 0;
			}
		});

		if (Log.isLoggable(TAG, Log.INFO)) {
			StringBuilder previewSizesString = new StringBuilder();
			for (Size supportedPreviewSize : supportedPreviewSizes) {
				previewSizesString.append(supportedPreviewSize.width).append('x').append(supportedPreviewSize.height).append(' ');
			}
			Log.i(TAG, "Supported preview sizes: " + previewSizesString);
		}

		double screenAspectRatio = (double) screenResolution.x / (double) screenResolution.y;

		// Remove sizes that are unsuitable
		Iterator<Size> it = supportedPreviewSizes.iterator();
		while (it.hasNext()) {
			Size supportedPreviewSize = it.next();
			int realWidth = supportedPreviewSize.width;
			int realHeight = supportedPreviewSize.height;
			if (realWidth * realHeight < MIN_PREVIEW_PIXELS) {
				it.remove();
				continue;
			}

			boolean isCandidatePortrait = realWidth < realHeight;
			int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
			int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;

			double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
			double distortion = Math.abs(aspectRatio - screenAspectRatio);
			if (distortion > MAX_ASPECT_DISTORTION) {
				it.remove();
				continue;
			}

			if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
				Point exactPoint = new Point(realWidth, realHeight);
				Log.i(TAG, "Found preview size exactly matching screen size: " + exactPoint);
				return exactPoint;
			}
		}

		// If no exact match, use largest preview size. This was not a great
		// idea on older devices because
		// of the additional computation needed. We're likely to get here on
		// newer Android 4+ devices, where
		// the CPU is much more powerful.
		if (!supportedPreviewSizes.isEmpty()) {
			Size largestPreview = supportedPreviewSizes.get(0);
			Point largestSize = new Point(largestPreview.width, largestPreview.height);
			Log.i(TAG, "Using largest suitable preview size: " + largestSize);
			return largestSize;
		}

		// If there is nothing at all suitable, return current preview size
		Size defaultPreview = parameters.getPreviewSize();
		Point defaultSize = new Point(defaultPreview.width, defaultPreview.height);
		Log.i(TAG, "No suitable preview sizes, using default: " + defaultSize);

		return defaultSize;
	}
}
