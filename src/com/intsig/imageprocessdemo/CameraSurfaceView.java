package com.intsig.imageprocessdemo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * Created by fei_cen on 2016/12/6.
 */

public class CameraSurfaceView extends SurfaceView implements
		SurfaceHolder.Callback, Camera.PreviewCallback {

	private static final String TAG = "CameraSurfaceView";

	private Context mContext;
	private SurfaceHolder holder;
	private Camera mCamera;

	private int mScreenWidth;
	private int mScreenHeight;

	public CameraSurfaceView(Context context) {
		this(context, null);
	}

	public CameraSurfaceView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CameraSurfaceView(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;
		getScreenMetrix(context);
		initView();

	}

	private void getScreenMetrix(Context context) {
		WindowManager WM = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics outMetrics = new DisplayMetrics();
		WM.getDefaultDisplay().getMetrics(outMetrics);
		mScreenWidth = outMetrics.widthPixels;
		mScreenHeight = outMetrics.heightPixels;
	}

	private void initView() {
		holder = getHolder();// get surfaceHolder 是相机和surfaceView的链接介质
		holder.addCallback(this);//添加SurfaceHolder 的callback 在onresume onDestory的时候会相应调用surfaceCreated 等几个方法
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);// set type

	}

	/**
	 * 功能：打开摄像机 设置相机的分辨率 开始预览效果
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "surfaceCreated");
		if (mCamera == null) {
			mCamera = Camera.open();// open camera
			try {
				mCamera.setPreviewDisplay(holder);// show camera picture on
													// surface view
			} catch (IOException e) {
				e.printStackTrace();
			}
			setCameraParams(mCamera, mScreenWidth, mScreenHeight);
			mCamera.startPreview();
		}

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.i(TAG, "surfaceChanged");
	}

	/**
	 * 功能：关闭相机预览 释放相机资源
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "surfaceDestroyed");
		mCamera.stopPreview();// stop preview
		mCamera.release();// release camera
		mCamera = null;
		holder = null;
	}

	private void setCameraParams(Camera camera, int width, int height) {
		Log.i(TAG, "setCameraParams  width=" + width + "  height=" + height);
		Camera.Parameters parameters = mCamera.getParameters();
		// get camera support PictureSize list
		List<Camera.Size> pictureSizeList = parameters
				.getSupportedPictureSizes();
		for (Camera.Size size : pictureSizeList) {
			Log.i(TAG, "pictureSizeList size.width=" + size.width
					+ "  size.height=" + size.height);
		}
		/** get the proper size from the support list */
		Camera.Size picSize = getProperSize(pictureSizeList,
				((float) height / width));
		if (null == picSize) {
			Log.i(TAG, "null == picSize");
			picSize = parameters.getPictureSize();
		}
		Log.i(TAG, "picSize.width=" + picSize.width + "  picSize.height="
				+ picSize.height);
		// set the PictureSize of SurfaceView with the choose camera size
		float w = picSize.width;
		float h = picSize.height;
		parameters.setPictureSize(picSize.width, picSize.height);
		this.setLayoutParams(new FrameLayout.LayoutParams(
				(int) (height * (h / w)), height));

		/** get the proper size from the support list */
		List<Camera.Size> previewSizeList = parameters
				.getSupportedPreviewSizes();

		for (Camera.Size size : previewSizeList) {
			Log.i(TAG, "previewSizeList size.width=" + size.width
					+ "  size.height=" + size.height);
		}
		Camera.Size preSize = getProperSize(previewSizeList, ((float) height)
				/ width);
		if (null != preSize) {
			Log.i(TAG, "preSize.width=" + preSize.width + "  preSize.height="
					+ preSize.height);
			parameters.setPreviewSize(preSize.width, preSize.height);
		}

		parameters.setJpegQuality(100); // set the picture quality
		if (parameters
				.getSupportedFocusModes()
				.contains(
						android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
			parameters
					.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// contintus
																									// picture
																									// mode
		}

		mCamera.cancelAutoFocus();// auto focus。
		mCamera.setDisplayOrientation(90);// set PreviewDisplay
		mCamera.setParameters(parameters);

	}

	/**
	 * get the proper size form the support list default w:h = 4:3
	 * <p>
	 * notes：the w is the screen height the h is the screen width
	 * <p/>
	 */
	private Camera.Size getProperSize(List<Camera.Size> pictureSizeList,
			float screenRatio) {
		Log.i(TAG, "screenRatio=" + screenRatio);
		Camera.Size result = null;
		for (Camera.Size size : pictureSizeList) {
			float currentRatio = ((float) size.width) / size.height;
			if (currentRatio - screenRatio == 0) {
				result = size;
				break;
			}
		}

		if (null == result) {
			for (Camera.Size size : pictureSizeList) {
				float curRatio = ((float) size.width) / size.height;
				if (curRatio == 4f / 3) {// default w:h = 4:3
					result = size;
					break;
				}
			}
		}

		return result;
	}

	// take picture to callback
	private Camera.ShutterCallback shutter = new Camera.ShutterCallback() {
		@Override
		public void onShutter() {
			Log.i(TAG, "shutter");
		}
	};

	private Camera.PictureCallback raw = new Camera.PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera Camera) {
			Log.i(TAG, "raw");

		}
	};

	public void takePicture() {

		setCameraParams(mCamera, mScreenWidth, mScreenHeight);
		// when camera.takePiture is used，camera close preview， the time wo
		// should use startPreview() to restart open view
		mCamera.takePicture(null, null, jpegCallback);

	}

	public void setListener(OnCameraStatusListener listener) {
		this.listener = listener;
	}

	/**
	 * camera take picture listener
	 */
	public interface OnCameraStatusListener {

		void onCameraStopped(String data);
	}

	private OnCameraStatusListener listener;

	private Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera Camera) {
			BufferedOutputStream bos = null;
			Bitmap bm = null;
			String filePath = null;
			try {

				bm = BitmapFactory.decodeByteArray(data, 0, data.length);
				if (Environment.getExternalStorageState().equals(
						Environment.MEDIA_MOUNTED)) {
					Log.i(TAG, "Environment.getExternalStorageDirectory()="
							+ Environment.getExternalStorageDirectory());
					filePath = "/sdcard/dyk/" + System.currentTimeMillis()
							+ ".jpg";
					File file = new File(filePath);
					if (!file.exists()) {
						file.createNewFile();
					}
					bos = new BufferedOutputStream(new FileOutputStream(file));
					bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);

				} else {
					Toast.makeText(mContext, "hvae no sdcard",
							Toast.LENGTH_SHORT).show();
				}
				listener.onCameraStopped(filePath);// callback for the activity
													// listener
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					bos.flush();// flush
					bos.close();
					bm.recycle();
					if (mCamera != null) {
						mCamera.stopPreview();
						mCamera.startPreview();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	};

	// ********************************聚焦*每隔2秒聚焦********start******************************
	private static final int MSG_AUTO_FOCUS = 100;

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == MSG_AUTO_FOCUS) {
				autoFocus();
			}
		}
	};
	boolean isFocus = false;

	private void autoFocus() {
		if (mCamera != null) {
			try {
				mCamera.autoFocus(focusCallback);
				// mHandler.sendEmptyMessageDelayed(MSG_AUTO_FOCUS, 3000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("deprecation")
	AutoFocusCallback focusCallback = new AutoFocusCallback() {

		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			Log.d("OCR", "success==>" + success);

			if (success) {
				if (camera != null) {
					isFocus = true;
					mHandler.sendEmptyMessageDelayed(MSG_AUTO_FOCUS, 2000L);
					System.out.println("lz----" + "isFocus==" + isFocus);
				}
			} else {
				if (camera != null) {
					isFocus = false;
					mHandler.sendEmptyMessage(MSG_AUTO_FOCUS);
					Log.d("lz", "isFocus==" + isFocus);
					System.out.println("lz----" + "isFocus==" + isFocus);
				}
			}

		}
	};

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		try {
			Log.d("OCR", "onPreviewFrame==>");

			// mHandler.sendEmptyMessageDelayed(MSG_AUTO_FOCUS, 100);

		} catch (Exception e) {
			Log.d("OCR", "onPreviewFrame==>" + e.getMessage());
		}

	}

	// ********************************聚焦*每隔2秒聚焦********end******************************
	// **********************************相机屏幕点击事件******start******************************

	/**
	 * 功能：给surfaceview添加点击事件
	 */
	public OnTouchListener onTouchListenerSurfaceListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View arg0, MotionEvent event) {
			// TODO Auto-generated method stub
			focusOnTouch((int) event.getX(), (int) event.getY());

			return false;
		}
	};
	/**
	 * 功能：根据当前点击的坐标 计算定点聚焦的 矩形区域
	 */
	private void focusOnTouch(int x, int y) {
		Rect rect = new Rect(x - 100, y - 100, x + 100, y + 100);
		int left = rect.left * 2000 / mScreenWidth - 1000;
		int top = rect.top * 2000 / mScreenHeight - 1000;
		int right = rect.right * 2000 / mScreenWidth - 1000;
		int bottom = rect.bottom * 2000 / mScreenHeight - 1000;
		// 如果超出了(-1000,1000)到(1000, 1000)的范围，则会导致相机崩溃
		left = left < -1000 ? -1000 : left;
		top = top < -1000 ? -1000 : top;
		right = right > 1000 ? 1000 : right;
		bottom = bottom > 1000 ? 1000 : bottom;
		focusOnRect(new Rect(left, top, right, bottom));
	}

	/**
	 * 功能：对相机设置需要聚焦的 区域
	 */
	protected void focusOnRect(Rect rect) {
		if (mCamera != null) {
			Parameters parameters = mCamera.getParameters(); // 先获取当前相机的参数配置对象
			parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO); // 设置聚焦模式
			Log.d(TAG,
					"parameters.getMaxNumFocusAreas() : "
							+ parameters.getMaxNumFocusAreas());
			if (parameters.getMaxNumFocusAreas() > 0) {
				List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
				focusAreas.add(new Camera.Area(rect, 1000));
				parameters.setFocusAreas(focusAreas);
			}
			mCamera.cancelAutoFocus(); // 先要取消掉进程中所有的聚焦功能
			mCamera.setParameters(parameters); // 一定要记得把相应参数设置给相机
			mCamera.autoFocus(focusCallback);
		}
	}
	// **********************************相机屏幕点击事件******end******************************

}