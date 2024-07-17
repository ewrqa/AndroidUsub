package com.lgh.test.send;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
/**
 * 发送命令和发送文件的工具类
 */
public  class SendUtils {
    private static StringBuffer cmdSb;
    private static Context context;
    private static String command;
    private static String schedule = "1";
    public SendUtils(Context context) {
        this.context = context;
    }
    /**
     * @description 通过按钮下发命令封装
     * @version V1.0
     * @author zhang
     * @date 2022/8/26 13:28
     * @update 2022/8/26 13:28
     */
    public  static void send(String cmd){
        SerialPortManager.instance().send(cmd);
    }
    /**
     * @param message  需要传递的文件数据
     */
    public static void sendFile(byte[] message){
        if (message!=null){
            SerialPortManager.instance().senFileComand(message);
        }else{
            String s = IotUtils.messageVer(30, 1, 0, "EE");
            send("AA3001EE" + s);
        }
    }
}
