package com.vinda.ucvcamera;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Toast;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.usbcameracommon.UVCCameraHandlerMultiSurface;
import com.serenegiant.usbcameracommon.UvcCameraDataCallBack;
import com.serenegiant.widget.AspectRatioTextureView;
import com.serenegiant.widget.CameraViewInterface;
import com.serenegiant.widget.UVCCameraTextureView;
import com.yuan.camera_test.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 显示多路摄像头
 */
public class MainActivity extends BaseActivity implements CameraDialog.CameraDialogParent {
    private static final boolean DEBUG = true;
    private static final String TAG = "MainActivity";

    private static final float[] BANDWIDTH_FACTORS = {1f, 0.6f};

    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;
    // uvc camera
    // camera first
    // single surface
    private UVCCameraHandler mHandlerFirst;
    // with monitor surface
    private UVCCameraHandlerMultiSurface mCameraHandlerFirst;
    private CameraViewInterface mUVCCameraViewFirst;
    private ImageButton mCaptureButtonFirst;
    private Surface mFirstPreviewSurface;
    // camera third
    // single surface
    private UVCCameraHandler mHandlerThird;
    // with monitor surface
    private UVCCameraHandlerMultiSurface mCameraHandlerThird;
    private CameraViewInterface mUVCCameraViewThird;
    private ImageButton mCaptureButtonThird;
    private Surface mThirdPreviewSurface;

    // origin camera
    private AspectRatioTextureView mTextureView;
    private Surface mTextureViewSurface;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;
    private List<Surface> mSurfaceList = new ArrayList<>();

    // monitor surface control
    private AspectRatioTextureView MonTextureView;
    private Surface MonTextureViewSurface;
    private UVCCameraHandlerMultiSurface Current_Handler=null;
    private boolean Ori_inUse;

    //voice
    private RecordUtils recordUtils;
    private static final int GET_RECODE_AUDIO = 1;
    private static String[] PERMISSION_AUDIO = {
            Manifest.permission.RECORD_AUDIO
    };

// permission
    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
//Live Streaming
    private AlertDialog mDialog;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surface_view_camera);
        findViewById(R.id.RelativeLayout1).setOnClickListener(mOnClickListener);
        verifyAudioPermissions(this);
        recordUtils = new RecordUtils(this);
        clickRecorder();
        // Muti-camera part
        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        result_Mon_surface();
        resultFirstCamera();
        resultThirdCamera();
        resultOriCamera();
        findViewById(R.id.bottom_home_1).setOnClickListener(v -> {
            if(mCameraHandlerFirst.isOpened()){
                // clean the mon surface
                mCameraCaptureSession.close();
                if(Ori_inUse){
                    remove_ori_Cam_preivew();
                }
                if (Current_Handler!=null){
                    Current_Handler.removeSurface(MonTextureViewSurface.hashCode());
                    Current_Handler=mCameraHandlerFirst;
                }else {
                    Current_Handler=mCameraHandlerFirst;
                }

                // add Mon Surface
                mCameraHandlerFirst.addSurface(mUVCCameraViewFirst.getSurface().hashCode(),mUVCCameraViewFirst.getSurface(),false);
                mCameraHandlerFirst.addSurface(MonTextureViewSurface.hashCode(),MonTextureViewSurface,false);
                mCameraHandlerFirst.startPreview();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCaptureButtonFirst.setVisibility(View.VISIBLE);
                    }
                });
            }
            Toast.makeText(MainActivity.this, "You clicked Swich Button1",
                    Toast. LENGTH_SHORT). show();
        });
        findViewById(R.id.bottom_home_2).setOnClickListener(v -> {
            if(!Ori_inUse){
                // clean the mon surface
                if (Current_Handler!=null){
                    Current_Handler.removeSurface(MonTextureViewSurface.hashCode());
                    Current_Handler=null;
                }
                // add surface
                create_ori_Mon();
            }

            Toast.makeText(MainActivity.this, "You clicked Swich Button2",
                    Toast. LENGTH_SHORT). show();
        });
        findViewById(R.id.bottom_home_3).setOnClickListener(v -> {
            if(mCameraHandlerThird.isOpened()){
                // clean the mon surface
                mCameraCaptureSession.close();
                if(Ori_inUse){
                    remove_ori_Cam_preivew();
                }
                if (Current_Handler!=null){
                    Current_Handler.removeSurface(MonTextureViewSurface.hashCode());
                    Current_Handler=mCameraHandlerThird;
                }else {
                    Current_Handler=mCameraHandlerThird;
                }
                // add mon surface
                mCameraHandlerThird.addSurface(mUVCCameraViewThird.getSurface().hashCode(),mUVCCameraViewThird.getSurface(),false);
                mCameraHandlerThird.addSurface(MonTextureViewSurface.hashCode(),MonTextureViewSurface,false);
                mCameraHandlerThird.startPreview();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCaptureButtonThird.setVisibility(View.VISIBLE);
                    }
                });

            }
            Toast.makeText(MainActivity.this, "You clicked Swich Button3",
                    Toast. LENGTH_SHORT). show();

        });
        findViewById(R.id.main_home).setOnClickListener(v -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = View.inflate(this, R.layout.dialog_layout, null);
            final CheckBox Check_RTSP = (CheckBox) findViewById(R.id.RTSP);
            final CheckBox Check_RTMP = (CheckBox) findViewById(R.id.RTMP);
            view.findViewById(R.id.not_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDialog.dismiss();
                }
            });
            view.findViewById(R.id.ok_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO:  点确定的操作
                    Toast.makeText(MainActivity.this,"Begin",Toast. LENGTH_SHORT).show();
                    mDialog.dismiss();
                }
            });
            mDialog = builder.setView(view).create();
            mDialog.show();

        });

    }

    public static void verifyAudioPermissions(MainActivity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.RECORD_AUDIO);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSION_AUDIO, GET_RECODE_AUDIO);
        }
    }


    /**
     * 带有回调数据的初始化
     */
    private void resultFirstCamera() {
        mUVCCameraViewFirst = (CameraViewInterface) findViewById(R.id.camera_view_first);
        //设置显示宽高
        mUVCCameraViewFirst.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        ((UVCCameraTextureView) mUVCCameraViewFirst).setOnClickListener(mOnClickListener);
        mCaptureButtonFirst = (ImageButton) findViewById(R.id.capture_button_first);
        mCaptureButtonFirst.setOnClickListener(mOnClickListener);
        mCaptureButtonFirst.setVisibility(View.INVISIBLE);
        mCameraHandlerFirst = UVCCameraHandlerMultiSurface.createHandler(this, mUVCCameraViewFirst, 1,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);
    }

//    UvcCameraDataCallBack firstDataCallBack = new UvcCameraDataCallBack() {
//        @Override
//        public void getData(byte[] data) {
//            //if (DEBUG) Log.v(TAG, "数据回调:" + data.length);
//            //Toast.makeText(MainActivity.this, "数据回调:" + data.length, Toast.LENGTH_SHORT).show();
//        }
//    };

    private void resultThirdCamera() {
        mUVCCameraViewThird = (CameraViewInterface) findViewById(R.id.camera_view_third);
        mUVCCameraViewThird.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        ((UVCCameraTextureView) mUVCCameraViewThird).setOnClickListener(mOnClickListener);
        mCaptureButtonThird = (ImageButton) findViewById(R.id.capture_button_third);
        mCaptureButtonThird.setOnClickListener(mOnClickListener);
        mCaptureButtonThird.setVisibility(View.INVISIBLE);
        mCameraHandlerThird = UVCCameraHandlerMultiSurface.createHandler(this, mUVCCameraViewThird, 1,
                UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT);
    }

    private void result_Mon_surface(){
        MonTextureView = findViewById(R.id.camera_view_Mon);
        MonTextureView.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        MonTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                MonTextureViewSurface = new Surface(MonTextureView.getSurfaceTexture());

            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            }
        });
    }

    private void resultOriCamera(){
        mTextureView = findViewById(R.id.texture_camera_ori);
        mTextureView.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                mTextureViewSurface = new Surface(mTextureView.getSurfaceTexture());
                mSurfaceList.add(mTextureViewSurface);
                mSurfaceList.add(MonTextureViewSurface);
                open_Ori_camera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            }
        });
    }
    private boolean isRequiredPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    private void open_Ori_camera() {
        if (!isRequiredPermissionsGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS_CODE);
        }
        CameraManager cameraManager = null;
        Ori_inUse=true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                //get all camera id
                String[] cameraIdList = cameraManager.getCameraIdList();
                for (String id : cameraIdList){
                    Log.d(TAG, "逻辑ID：" + id );
                }
                cameraManager.openCamera(cameraManager.getCameraIdList()[0], camera_state_callback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

    }
    CameraDevice.StateCallback camera_state_callback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            try {
                mCameraDevice.createCaptureSession(mSurfaceList, session_state_callback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
        }
    };

    CameraCaptureSession.StateCallback session_state_callback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mCameraCaptureSession = session;
            try {
                CaptureRequest.Builder request_builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                request_builder.addTarget(mTextureViewSurface);
                request_builder.addTarget(MonTextureViewSurface);
                CaptureRequest request = request_builder.build();

                mCameraCaptureSession.setRepeatingRequest(request, null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        }
    };

    private void remove_ori_Cam_preivew(){
        Ori_inUse=false;
        camera_state_callback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                mCameraDevice = camera;
                try {
                    mCameraDevice.createCaptureSession(mSurfaceList, session_state_callback, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
            }
        };
        session_state_callback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                mCameraCaptureSession = session;
                try {
                    CaptureRequest.Builder request_builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    request_builder.addTarget(mTextureViewSurface);
                    //request_builder.addTarget(MonTextureViewSurface);
                    CaptureRequest request = request_builder.build();
                    mCameraCaptureSession.setRepeatingRequest(request, null, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            }
        };
        mSurfaceList.remove(MonTextureViewSurface);
        if (!isRequiredPermissionsGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS_CODE);
        }
        CameraManager cameraManager = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                //get all camera id
                String[] cameraIdList = cameraManager.getCameraIdList();
                for (String id : cameraIdList){
                    Log.d(TAG, "逻辑ID：" + id );
                }
                cameraManager.openCamera(cameraManager.getCameraIdList()[0], camera_state_callback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }


    }

    private void create_ori_Mon(){
        Ori_inUse=true;
        final SurfaceTexture st=MonTextureView.getSurfaceTexture();
        MonTextureViewSurface=new Surface(st);
        mSurfaceList.clear();
        mSurfaceList.add(mTextureViewSurface);
        mSurfaceList.add(MonTextureViewSurface);
        camera_state_callback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                mCameraDevice = camera;
                try {
                    mCameraDevice.createCaptureSession(mSurfaceList, session_state_callback, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
            }
        };
        session_state_callback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                mCameraCaptureSession = session;
                try {
                    CaptureRequest.Builder request_builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    request_builder.addTarget(mTextureViewSurface);
                    request_builder.addTarget(MonTextureViewSurface);
                    CaptureRequest request = request_builder.build();
                    mCameraCaptureSession.setRepeatingRequest(request, null, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            }
        };
        if (!isRequiredPermissionsGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS_CODE);
        }
        CameraManager cameraManager = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                //get all camera id
                String[] cameraIdList = cameraManager.getCameraIdList();
                for (String id : cameraIdList){
                    Log.d(TAG, "逻辑ID：" + id );
                }
                cameraManager.openCamera(cameraManager.getCameraIdList()[0], camera_state_callback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }


    }


    @Override
    protected void onStart() {
        super.onStart();
        mUSBMonitor.register();
    }

    @Override
    protected void onStop() {
        //mHandlerFirst.close();
        if (mUVCCameraViewFirst != null)
            mUVCCameraViewFirst.onPause();
        mCaptureButtonFirst.setVisibility(View.INVISIBLE);
        mUSBMonitor.unregister();//usb管理器解绑
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mHandlerFirst != null) {
            mHandlerFirst = null;
        }

        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }

        mUVCCameraViewFirst = null;
        mCaptureButtonFirst = null;
        super.onDestroy();
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            switch (view.getId()) {
                case R.id.camera_view_first:
                    if (mCameraHandlerFirst!= null) {
                        if (!mCameraHandlerFirst.isOpened()) {
                            CameraDialog.showDialog(MainActivity.this);
                        } else {
                            mCameraHandlerFirst.close();
                            setCameraButton();
                        }
                    }
                    break;
                case R.id.camera_view_third:
                    if (mCameraHandlerThird!= null) {
                        if (!mCameraHandlerThird.isOpened()) {
                            CameraDialog.showDialog(MainActivity.this);
                        } else {
                            mCameraHandlerThird.close();
                            setCameraButton();
                        }
                    }
                    break;
                case R.id.texture_camera_ori:
                    Toast.makeText(MainActivity.this, "texture_ori", Toast.LENGTH_SHORT).show();
                    //open_Ori_camera();
                    break;


        }
    }
    };

    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onAttach:" + device);
            Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            //设备连接成功
            if (DEBUG) Log.v(TAG, "onConnect:" + device);
            Toast.makeText(MainActivity.this, "onConnect:" + device, Toast.LENGTH_SHORT).show();
            if (!mCameraHandlerFirst.isOpened()) {
                mCameraHandlerFirst.open(ctrlBlock);
                mCameraHandlerFirst.addSurface(mUVCCameraViewFirst.getSurface().hashCode(),mUVCCameraViewFirst.getSurface(),false);
                mCameraHandlerFirst.startPreview();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCaptureButtonFirst.setVisibility(View.VISIBLE);
                    }
                });

            } else if (!mCameraHandlerThird.isOpened()) {
                mCameraHandlerThird.open(ctrlBlock);
                mCameraHandlerThird.addSurface(mUVCCameraViewThird.getSurface().hashCode(),mUVCCameraViewThird.getSurface(),false);
                mCameraHandlerThird.startPreview();
                runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCaptureButtonThird.setVisibility(View.VISIBLE);
                }
            });
        }}

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            if (DEBUG) Log.v(TAG, "onDisconnect:" + device);
            if ((mHandlerFirst != null) && !mHandlerFirst.isEqual(device)) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mHandlerFirst.close();
                        if (mFirstPreviewSurface != null) {
                            mFirstPreviewSurface.release();
                            mFirstPreviewSurface = null;
                        }
                        setCameraButton();
                    }
                }, 0);
            } else if ((mHandlerThird != null) && !mHandlerThird.isEqual(device)) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mHandlerThird.close();
                        if (mThirdPreviewSurface != null) {
                            mThirdPreviewSurface.release();
                            mThirdPreviewSurface = null;
                        }
                        setCameraButton();
                    }
                }, 0);

            }}

        @Override
        public void onDettach(final UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onDettach:" + device);
            Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
            if (DEBUG) Log.v(TAG, "onCancel:");
        }
    };

    /**
     * to access from CameraDialog
     *
     * @return
     */
    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setCameraButton();
                }
            }, 0);
        }
    }

    private void setCameraButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ((mHandlerFirst != null) && !mHandlerFirst.isOpened() && (mCaptureButtonFirst != null)) {
                    mCaptureButtonFirst.setVisibility(View.INVISIBLE);
                }
                if ((mHandlerThird != null) && !mHandlerThird.isOpened() && (mCaptureButtonThird != null)) {
                    mCaptureButtonThird.setVisibility(View.INVISIBLE);
                }

            }
        }, 0);
    }

    private void clickRecorder() {
        recordUtils.startRecorder(new RecordUtils.OnRecordListener() {
            @Override
            public void onRecordSuccess() {
            }

            @Override
            public void onRecordFail() {
                Log.i("tan6458", "录音失败请重新录音:");
            }
        });
    }



}
