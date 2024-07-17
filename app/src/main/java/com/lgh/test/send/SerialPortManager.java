package com.lgh.test.send;
import android.content.Context;
import android.os.HandlerThread;
import android.serialport.SerialPort;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
/** Created by Administrator on 2017/3/28 0028. */
public class SerialPortManager {
    //线程池 统一关闭线程
    private SerialReadThread mReadThread;
    private OutputStream mOutputStream;
    private HandlerThread mWriteThread;
    //判断是否同时在执行
    private SerialPort mSerialPort;
    private Queue<String> queueMsg = new ConcurrentLinkedQueue<String>();//线程安全到队列
    private Context context;
    private static class InstanceHolder {
        public static SerialPortManager sManager = new SerialPortManager();
    }
    public static SerialPortManager instance() {
        return InstanceHolder.sManager;
    }
    SerialPortManager() {
    }
    //创建一个消息队列来管理线程
    /**
     * 打开串口
     * @param device 串口的设备
     * @return
     */
    public SerialPort open(Context context,Device device) {
        this.context=context;
        return open(device.getPath(), device.getBaudrate());
    }
    /**
     * 打开串口
     * @param devicePath
     * @param baudrateString
     * @return
     */
    public SerialPort open(String devicePath, String baudrateString) {
        try {
            //路径
            File device = new File(devicePath);
            int baurate = Integer.parseInt(baudrateString);
            //串口
            mSerialPort = SerialPort // 串口对象
                    .newBuilder(device, baurate) // 串口地址地址，波特率
                    .dataBits(8) // 数据位,默认8；可选值为5~8
                    .stopBits(1) // 停止位，默认1；1:1位停止位；2:2位停止位
                    .parity(0) // 校验位；0:无校验位(NONE，默认)；1:奇校验位(ODD);2:偶校验位(EVEN)
                    .build(); // 打开串口并返回
            mOutputStream = mSerialPort.getOutputStream();
            mWriteThread = new HandlerThread("write-thread");
            mWriteThread.start();
            mReadThread = new SerialReadThread(context,mSerialPort.getInputStream());
            mReadThread.start();

            return mSerialPort;
        } catch (Throwable tr) {
            //关闭串口
            close();
            return null;
        }
    }
    /** 关闭串口 */
    public void close() {
        if (mReadThread!=null){
            mReadThread.close();
        }
        if (mOutputStream != null) {
              try {
                mOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mWriteThread != null) {
            mWriteThread.quit();
        }

        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }

    }
    //存入到发送的消息队列当中
    public  void  send(String msg){
            //将命令存入消息队列里面
            queueMsg.offer(msg);
            sendCommand();
    }
    /** 发送命令包 */
    public void sendCommand() {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String poll = queueMsg.poll();
            // TODO: 2018/3/22
            byte[] bytes = ByteUtil.hexStr2bytes(poll);
            if(mOutputStream!=null){
                try {
                    mOutputStream.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                Log.e("mOutputStream","空" );
            }
    }
    /**发送的文件数据*/
    public  void senFileComand(byte[] message){
        System.out.println("数组的地址为：" + Integer.toHexString(System.identityHashCode(message)));
        try {
            if(mOutputStream!=null){
                mOutputStream.write(message);
            }else{
                Log.e("当前是空的", ":");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
