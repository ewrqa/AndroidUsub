package com.lgh.test;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.lgh.test.send.IotUtils;
import com.lgh.test.send.QueueUtils;
import com.lgh.test.send.RecvMessage;
import com.lgh.test.send.SendUtils;
import com.lgh.test.send.SerialConst;
import com.lgh.test.send.SerialPortManager;
import com.lgh.uvccamera.UVCCameraProxy;
import com.lgh.uvccamera.bean.PicturePath;
import com.lgh.uvccamera.callback.ConnectCallback;
import com.lgh.uvccamera.callback.PhotographCallback;
import com.lgh.uvccamera.callback.PictureCallback;
import com.lgh.uvccamera.callback.PreviewCallback;
import com.lgh.uvccamera.util.NameCompatator;
import com.serenegiant.usb.Size;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import ZtlApi.ZtlManager;
public class UVCCameraActivity extends AppCompatActivity implements View.OnClickListener {
    private TextureView mTextureView;
    //    private SurfaceView mSurfaceView;
    private ImageView mImageView1;
    private Spinner mSpinner;
    private UVCCameraProxy mUVCCamera;
    private boolean isFirst = true;
    private String path1;
    private PhotographBroadcastReceiver photographBroadcastReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uvc_camera);
        initView();
        initUVCCamera();
    }
    private void initView() {
        mTextureView = findViewById(R.id.textureView);
        mImageView1 = findViewById(R.id.imag1);
        mSpinner = findViewById(R.id.spinner);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {
                if (isFirst) {
                    isFirst = false;
                    return;
                }
                mUVCCamera.stopPreview();
                List<Size> list = mUVCCamera.getSupportedPreviewSizes();
                if (!list.isEmpty()) {
                    mUVCCamera.setPreviewSize(list.get(position).width, list.get(position).height);
                    mUVCCamera.startPreview();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        ZtlManager.GetInstance().setContext(UVCCameraActivity.this);
    }
    private void initUVCCamera() {
        mUVCCamera = new UVCCameraProxy(  this);
        // 已有默认配置，不需要可以不设置
        mUVCCamera.getConfig()
                .isDebug(true)
                .setPicturePath(PicturePath.APPCACHE)
                .setDirName("uvccamera")
                .setProductId(0)
                .setVendorId(0);
        mUVCCamera.setPreviewTexture(mTextureView);
//        mUVCCamera.setPreviewSurface(mSurfaceView);
        //注册usb的广播
        mUVCCamera.registerReceiver();
        mUVCCamera.setConnectCallback(new ConnectCallback() {
            @Override
            public void onAttached(UsbDevice usbDevice) {
                mUVCCamera.requestPermission(usbDevice);
            }
            @Override
            public void onGranted(UsbDevice usbDevice, boolean granted) {
                if (granted) {
                    mUVCCamera.connectDevice(usbDevice);
                }
            }
            @Override
            public void onConnected(UsbDevice usbDevice) {
                mUVCCamera.openCamera();
            }
            @Override
            public void onCameraOpened() {
                Size size = showAllPreviewSizes();
                List<Size> supportedPreviewSizes = mUVCCamera.getSupportedPreviewSizes();
                Collections.sort(supportedPreviewSizes, new NameCompatator());
                for (Size cat : supportedPreviewSizes) {
                    Log.e("当前支持的像素列表", cat.toString());
                }
                mUVCCamera.setPreviewSize(size.width,size.height);
                mUVCCamera.startPreview();
                Log.e("当前的分辨率1",mUVCCamera.getPreviewSize().width+"");
                Log.e("当前的分辨率2",mUVCCamera.getPreviewSize().height+"");
            }
            @Override
            public void onDetached(UsbDevice usbDevice) {
                mUVCCamera.closeCamera();
            }
        });

        mUVCCamera.setPhotographCallback(new PhotographCallback() {
            @Override
            public void onPhotographClick() {
                String s = TimeUtil.currentTime();
                mUVCCamera.takePicture(s + ".jpg");
            }
        });
        mUVCCamera.setPreviewCallback(new PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] yuv) {
            }
        });
        mUVCCamera.setPictureTakenCallback(new PictureCallback() {
            @Override
            public void onPictureTaken(String path) {
                //获取拍照结束之后的路径
                path1 = path;
                mImageView1.setImageURI(null);
                if (path!=null){
                    mImageView1.setImageURI(Uri.parse(path));
                    String mimeType = "image/jpeg"; // 文件类型
                    MediaScannerConnection.scanFile(UVCCameraActivity.this, new String[]{path}, new String[]{mimeType}, null);
                }
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 扫描新的图片文件
        MediaScannerConnection.scanFile(this, new String[]{path1}, null, null);
    }
    @Override
    protected void onResume() {
        super.onResume();
        PhotographBroadcastReceiver photographBroadcastReceiver = new PhotographBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.shoot");
        registerReceiver(photographBroadcastReceiver,intentFilter);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn1:
                mUVCCamera.startPreview();
                break;
            case R.id.btn2:
                mUVCCamera.stopPreview();
                break;
            case R.id.take_picture:
                //拍照
                String s = new UVCCameraProxy(UVCCameraActivity.this).checkDevice();
                if (s.equals("")){
                    Toast.makeText(this, "当前没有相机无法进行拍照", Toast.LENGTH_SHORT).show();
                }else{
                    boolean b = mUVCCamera.takePicture("B"+".jpg");
                    if(b){
                        Toast.makeText(this, "成功", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(this, "失败", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.imag1:
                jump2ImageActivity(path1);
                break;
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if(photographBroadcastReceiver!=null){
            unregisterReceiver(photographBroadcastReceiver);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (photographBroadcastReceiver!=null){
            unregisterReceiver(photographBroadcastReceiver);
        }
        mUVCCamera.unregisterReceiver();
    }
    private void jump2ImageActivity(String path) {
        Intent intent = new Intent(this, ImageActivity.class);
        intent.putExtra("path", path);
        startActivity(intent);
    }
//    进行获取相机所支持的分辨率列表
    private Size  showAllPreviewSizes() {
        isFirst = true;
        List<Size> previewList = mUVCCamera.getSupportedPreviewSizes();
        Collections.sort(previewList, new NameCompatator());
        List<String> previewStrs = new ArrayList<>();
        for (Size size : previewList) {
            previewStrs.add(size.width+"*"+size.height);
        }
        Size size = previewList.get(0);
        ArrayAdapter adapter = new ArrayAdapter(UVCCameraActivity.this,
                android.R.layout.simple_spinner_item, previewStrs);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        return  size;
    }
    //通过接收广播来进行拍照
    class PhotographBroadcastReceiver extends BroadcastReceiver {
        private boolean b;
        @Override
        public void onReceive(Context context, Intent intent) {
            String shoot = intent.getStringExtra("shoot");
            if (shoot != null) {
                //进行拍
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        b = mUVCCamera.takePicture(shoot + ".jpg");
                        if(b){
                            try {
                                String s2 = IotUtils.messageVer(21,
                                        1, 0, "");
                                SendUtils.send("AA210100" + s2);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }else{
                            try {
                                String s2 = IotUtils.messageVer(21,
                                        1, 0, "EE");
                                SendUtils.send("AA2101EE"+s2);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Log.e("拍照失败","");
                        }
                    }
                }).start();
            }
        }
    }
}
