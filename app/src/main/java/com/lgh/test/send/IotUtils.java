package com.lgh.test.send;
import android.util.Log;

import java.util.Arrays;
import java.util.List;
/**
 * @description 物联相关工具类
 * @version V1.0
 * @author zhang
 * @date 2021/11/23 14:00
 * @update 2021/11/23 14:00
 */
public class IotUtils {
    private static int i;
    /**
     * @Title:intToHexString @Description:10进制数字转成16进制
     *
     * @param a 转化数据
     * @param len 占用字节数
     * @return
     * @throws
     */
    public static String intToHexString(int a, int len) {
        len <<= 1;
        String hexString = Integer.toHexString(a);
        int b = len - hexString.length();
        if (b > 0) {
            for (int i = 0; i < b; i++) {
                hexString = "0" + hexString;
            }
        }
        return hexString;
    }

    /**
     * @param a 转化数据
     * @param len 占用字节数
     * @return
     * @throws
     */
    public static String  longToHexString(long a, int len) {
        len <<= 1;
        //将long 类型转换成int类型
        String hexString = Long.toHexString(a);
        int b = len - hexString.length();
        if (b > 0) {
            for (int i = 0; i < b; i++) {
                hexString = "0" + hexString;
            }
        }
        return hexString;
    }



    /**
     * @Title:hexString2Bytes @Description:16进制字符串转字节数组
     *
     * @param src 16进制字符串
     * @return 字节数组
     */
    public static byte[] hexString2Bytes(String src) {
        int l = src.length() / 2;
        byte[] ret = new byte[l];
        for (int i = 0; i < l; i++) {
            ret[i] = (byte) Integer.valueOf(src.substring(i * 2, i * 2 + 2), 16).byteValue();
        }
        return ret;
    }
    /**
     * 补码转10进制带符号值
     * @param param
     * @return
     */
    public static int complementToDec(String param){
        List<String> map = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F");
        String[] split = param.toUpperCase().split("");
        int source = 0;
        for (int i = 0;i<split.length;i++){
            source += map.indexOf(split[i]) * Math.pow(16,split.length-i-1);
        }

        if((source&0x80)>0){
            source = - ( ~(source-0x01) & 0x7F );
        }
        return source;
    }
     /**
     * 串口传递数据最后的校验位
     */
    public  static  String   messageVer(int command_word,int byte_amount,int  transmit_byte,String transmit_message){
        if(!transmit_message.equals("")){
            i = Integer.parseInt(transmit_message, 16);
        }
        //将传递的数据进行转
        int i1 = (Integer.parseInt(command_word + byte_amount + transmit_byte + "", 16) + IotUtils.i)%256;
        //转换成 16进制
        String hexString = Integer.toHexString(i1);
        return  hexString;
    }
}
