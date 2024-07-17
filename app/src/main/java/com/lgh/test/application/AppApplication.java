package com.lgh.test.application;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.serialport.SerialPortFinder;
import android.text.LoginFilter;
import android.util.Log;
import android.widget.Toast;

import com.bulong.rudeness.RudenessScreenHelper;
import com.lgh.test.TimeUtil;
import com.lgh.test.UVCCameraActivity;
import com.lgh.test.send.Device;
import com.lgh.test.send.SendUtils;
import com.lgh.test.send.SerialConst;
import com.lgh.test.send.SerialPortManager;

import ZtlApi.ZtlManager;
import io.reactivex.disposables.SerialDisposable;
public class AppApplication extends Application {
    private  String buad;
    private SharedPreferences.Editor edit;
    @Override
    public void onCreate() {
        super.onCreate();
        new RudenessScreenHelper(this, 1920).activate();
        initDevice();
        //用于存储波特频是是否切换成功的sp
        SharedPreferences bf = getSharedPreferences("bf", Context.MODE_PRIVATE);
        edit = bf.edit();
        //接收切换波特频的广播
        GetBuadBroadcastReceiver getBuadBroadcastReceiver = new GetBuadBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.take.buad");
        registerReceiver(getBuadBroadcastReceiver,filter);
    }
    private void initDevice() {
        //获取当前的串口个数
        SerialPortFinder serialPortFinder = new SerialPortFinder();
        SerialConst.M_DEVICES = serialPortFinder.getAllDevicesPath();
        if (SerialConst.M_DEVICES.length == 0) {
            Toast.makeText(this, "未找到串口设备", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "获取到了串口设备", Toast.LENGTH_SHORT).show();
        }
        if(buad!=null){
            String baudrate = SerialConst.CURRENT_DEVICE.getBaudrate();
            if (baudrate.equals(buad)){//如果与当前的波特频一致的话则不需要进行切换
                Log.e("当前波特频与修改的一致", "无需修改");
                edit.putString("yes","成功");
                edit.commit();
            }else{
                SerialConst.CURRENT_DEVICE = new Device(SerialConst.DEFAULT_DEVICE_INFO,buad);//设置切换的波特率
                if(SerialConst.CURRENT_DEVICE.getBaudrate().equals(buad)){
                    Log.e("当前的串口设备",SerialConst.CURRENT_DEVICE.getBaudrate());
                    switchSerialPort();
                    edit.putString("yes","成功");
                    edit.commit();
                }else{
                    edit.putString("yes","失败");
                    edit.commit();
                }
            }
        }else{
            SerialConst.CURRENT_DEVICE = new Device(SerialConst.DEFAULT_DEVICE_INFO, SerialConst.BAUD_RATES);
            Log.e("当前的串口设备",SerialConst.CURRENT_DEVICE.getBaudrate());
            switchSerialPort();
        }
    }
    /**
     * @description 打开串口
     * @version V1.0
     * @author zhang
     * @date 2022/8/12 14:02
     * @update 2022/8/12 14:02
     */
    private void switchSerialPort() {
        SerialConst.DEVICE_STATUS = SerialPortManager.instance().open(getApplicationContext(), SerialConst.CURRENT_DEVICE) != null;
        if (SerialConst.DEVICE_STATUS) {
            Toast.makeText(this, "串口打开成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "串口打开失败", Toast.LENGTH_SHORT).show();
        }
    }
        /**
         * 根据广播获取到需要切换的波特频
         */
        class GetBuadBroadcastReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                String send = intent.getStringExtra("send");
                if(send!=null){
                   Log.e("得到的切换值",send);
                    if(!send.equals("")){
                        Log.e("得到的波特频",send);
                        buad=send;
                        initDevice();
                    }else{
                        edit.putString("yes","失败");
                        edit.commit();
                    }
                }
            }
        }
}
