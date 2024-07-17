package com.lgh.test.send;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.LinearGradient;
import android.os.Environment;
import android.os.SystemClock;
import android.text.LoginFilter;
import android.util.Log;
import android.widget.Chronometer;
import android.widget.Toast;
import com.lgh.uvccamera.UVCCameraProxy;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 *  获取串口设备线程
 */
 class SerialReadThread extends Thread {
    private  Context context;
    private BufferedInputStream mInputStream;
    //列表传送的ver
    private  int list_transfer_ver;
    //传递的累加值
    private static int  liost_transfer_sum=0;
    //清单列表递增的个数
    private  Integer  voluntarily_add=0;
    //得到文件名称的ASCLL值
    private static String  message_ASCLL="0";
    //偏移量  已经传递过的文件数量
    private int offset=0;
    private  static int sum=0;
    public SerialReadThread(Context context,InputStream is) {
        this.context=context;
        mInputStream = new BufferedInputStream(is);
    }
    private  String baud;//切换的波特频
    private int BUFFER_SIZE=1024;//缓冲数组大小
    byte[] buf = new byte[BUFFER_SIZE];
    private int size;
    @Override
    public void run(){
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                try {
                    //可读数量
                    int available = mInputStream.available();
                    if (available > 0) {
                        size = mInputStream.read(buf);
                        String hexStr = ByteUtil.bytes2HexStr(buf, 0, size);
                        String substring = hexStr.substring(2,4);//获取命令字节
                        Log.e("收到的数据", hexStr);
                            /**
                             * 握手的回复
                             */
                            if (substring.equals("10")) {
                                //计算校验值
                                String s = IotUtils.messageVer(10, 1, 0, "");
                                //接收到消息之后立即回复
                                SendUtils.send("AA100100" + s);
                            }
                            /**
                             * 根据指令进行拍照
                             */
                            if (substring.equals("20")) {
                                //收到命令之后立即进行回复
                                String s1 = IotUtils.messageVer(20, 1, 0, "");
                                SendUtils.send("AA200100" + s1);
                                //拍照的时候先判断当前是否拥有usb相机
                                String get_phone_name = new UVCCameraProxy(context).checkDevice();
                                if (get_phone_name.equals("")){//返回
                                    String s2 = IotUtils.messageVer(21, 1, 0, "EE");
                                    SendUtils.send("AA2101EE"+s2);
                                }else{//获取到相机直接进行拍照
                                    String photo_name = AscllToString(hexStr.substring(6, hexStr.length()-2));
                                    Intent intent = new Intent();
                                    intent.setAction("com.shoot");
                                    intent.putExtra("shoot", photo_name);
                                    context.sendBroadcast(intent);
                                }
                            }
                        /**
                         *  传递获取图片
                         */
                        if (substring.equals("30")) {
                            /**
                             *   先判断当前是否可以进行文件的传递
                             *   进行切换不同的波特频
                             */
                            String file_name = AscllToString(hexStr.substring(50));//获取传输的名称
                            //需要传递文件
                            File file = new File("/storage/" +
                                    "emulated/" +
                                    "0/Android/" +
                                    "data/com.lgh.test/" +
                                    "cache/uvccamera/" +file_name+ ".jpg");
                            Log.e("当前是否存在", file_name+"");
                        //文件存在的话开始发送
              if (file.exists()) {
                String baud_rate = hexStr.substring(6,8);
                transmit_baud(baud_rate);//设置波特率
                SystemClock.sleep(1500);
                SharedPreferences bf =context.getSharedPreferences("bf",Context.MODE_PRIVATE);//切换波特频
                String yes = bf.getString("yes", "");
                Log.e("当前切换波特率的状态", yes+"");
                if (yes.equals("成功")){
                String block = getBlock(hexStr);//获取每次发送的字节数量
                Log.e("波特率切换成功","");
                 String[] split = block.split(",");//根据逗号进行分割
                 for(int i=0;i<split.length;i++){
                  Integer integer = new Integer(split[i]);//获取每次发送的数据大小
                  if (integer==0){
                      //结束之后进行切换成原来的波特频
                      transmit_baud("B4");
                      break;
                  }
                  offset= getOffset(hexStr, (8 + ((i-1) * 6)), (14 + ((i-1) * 6)),i); //获取偏移量
                  Log.e("这次的偏移量", "开始的位置"+(8 + ((i-1) * 6)) +"结束的位置"+
                          (14 + ((i-1) * 6))+"值"+offset);
                  byte[] bytes = new byte[integer];//每次传递的数组大小
                  try(RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")){
                      randomAccessFile.seek(0);
                      int read = randomAccessFile.read(bytes);//将读取到的文件值传入数组当中
                       if(read!=0){
                          if(read==-1){
                              transmit_baud("B4");//结束之后切换成原来的波特频115200
                              return;//已经读取完毕
                          }else if(read==integer){
                              SendUtils.sendFile(bytes);
                          }else{
                              byte[] data = Arrays.copyOfRange(bytes, 0, read);
                              SendUtils.sendFile(data);
                          }
                      }else{
                          String s1 = IotUtils.messageVer(30,
                                  1, 0, "EE");
                          SendUtils.send("AA3001EE" + s1);
                          break;
                      }
                  }catch (IOException e){
                      e.printStackTrace();
                  }
              }
                 transmit_baud("B4");//结束之后切换成原来的波特频115200
                   }else{
                       String no = IotUtils.messageVer(30,
                               1, 0, "EE");
                       SendUtils.send("AA3001EE" + no);
                       Log.e("波特频切切换失败", "");
                       return;
                   }
                   SharedPreferences.Editor edit = bf.edit();
                   edit.clear().commit();
               }
               else {
                   String s = IotUtils.messageVer(30,
                           1, 0, "EE");
                           SendUtils.send("AA3001EE" + s);
                       }
                   }
                  /**
                    * 获取文件的大小
                    */
                   if(substring.equals("40")){
                       String file_name = AscllToString(hexStr.substring(6,hexStr.length()-2));//裁剪获取名称
                       //将的到的值转换成对应的汉字
                       //根据图片的名称 获取图片的大小 单位是字节
                       File file = new File("/storage/" +
                               "emulated/" +
                               "0/Android/" +
                               "data/com.lgh.test/" +
                               "cache/uvccamera/" + file_name + ".jpg");
                       //判断当前的文件是否存在
                      if(!file.exists()){
                           //为获取到数据
                           String s = IotUtils.messageVer(40,
                                   4, 0, "EE");
                           SendUtils.send("AA4004EE"+s);
                       }else{
                           long length = file.length();
                           Log.e("得到恶文件大小", length+"");
                           String picture_size = IotUtils.longToHexString(length, 4);
                           String s1 = IotUtils.messageVer(40, 4,
                                   0, picture_size);
                           SendUtils.send("AA4004"+picture_size+s1);//返回文件大小
                       }
                   }
                   /**
                    * 删除指定的文件
                    */
                   if(substring.equals("50")){
                       String s = IotUtils.messageVer(50,
                               1, 0, "");
                       //收到之后立马回复
                       SendUtils.send("AA500100"+s);
                       //获取要删除的名称
                       String delete_file_name = hexStr.substring(6, hexStr.length() - 2);
                       Log.e("裁剪之后的数据",delete_file_name);
                       String delte_name = AscllToString(delete_file_name);
                       //删除指定的文件
                       boolean delete = new File("/storage/" +
                               "emulated/0/" +
                               "Android/" +
                               "data/" +
                               "com.lgh.test/" +
                               "cache/" +
                               "uvccamera/"+delte_name+".jpg").delete();
                       //根据删除的信息来回复
                       if (delete){
                           //删除成功
                           String s1 = IotUtils.messageVer(51,
                                   1, 0, "");
                           SendUtils.send("AA510100"+s1);
                       }else{
                           String s1 = IotUtils.messageVer(51,
                                   1, 0, "EE");
                           SendUtils.send("AA5101EE"+s1);
                           Log.e("删除失败",s1);
                         }
                   }
                   //  列出清单列表
                    if(substring.equals("60")) {
                        File[] files = new File("/storage" +
                                "/emulated/0/" +
                                "Android/data" +
                                "/com.lgh.test" +
                                "/cache/uvccamera").listFiles();
                        String files_length = IotUtils.intToHexString(files.length, 2);//返回两个字节
                        String files_length_ver = IotUtils.messageVer(60, 2, 0, files_length);
                        SendUtils.send("AA6002" + files_length + files_length_ver);//返回发送资源的条目数量
                        //获取个数
                        Arrays.sort(files); // 对文件数组按照升序排序
                   if (files.length > 0) {
                       //开始进行发送数据
                       for (int i = 0; i < files.length; i++) {
                           String groupNum = IotUtils.intToHexString(voluntarily_add, 2).toUpperCase();//自增的条目数转16进制
                           String list_name = files[i].getName().replaceAll(".jpg", "");
                           Log.i("名称", list_name);
                           String s1 = StringToSAsc(list_name).substring(1);
                           if (!s1.equals("")) {//不为空的情况下
                               //转换16进制 传递列表的字节长度
                               String list_name_length = IotUtils.intToHexString(s1.length(), 1).toUpperCase();
                               //校验位
                               list_transfer_ver = (98 + voluntarily_add + s1.length() + liost_transfer_sum) % 256;
                               String list_transfer_ver_16 = IotUtils.intToHexString(list_transfer_ver, 1).toUpperCase();
                               Log.e("每条数据的校验位", list_transfer_ver_16);
                               //文件名成都      自增长的条目   传递名称    最后的校验位
                               SendUtils.send("AA60" + list_name_length + groupNum + s1 + list_transfer_ver_16);
                           }
                           voluntarily_add++;
                           Thread.sleep(100);
                       }
                   } else {
                       Log.e("列表为0", "");
                   }
                 }
                 }else{
                     SystemClock.sleep(1);
                 }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
    }
    /**
     *切换波特频
     * B0 ：   115200 bps
     * B1 :    230400 bps
     * B2 :    460800 bps
     * B3 :    921600 bps
     * @param baud_rate  波特频的雷翔
     */
    public  void   transmit_baud(String baud_rate){
        if (baud_rate.equals("B0")){
             baud="115200";
        }else if(baud_rate.equals("B1")){
            baud="230400";
        }else if(baud_rate.equals("B2")){
            baud="460800";
        }else if(baud_rate.equals("B3")){
            baud="921600";
        }else if(baud_rate.equals("B4")){
            baud="115200";//切换回原来的波特频
        }else{
            baud="";
        }
        //进行波特率的切换
        Intent intent = new Intent();
        intent.setAction("com.take.buad");
        intent.putExtra("send",baud);
        context.sendBroadcast(intent);
    }
    /**
     *  获取命令中的每次发送的数据大小
     * @param message 发送的命令
     */
    public String  getBlock(String message){
        //根据得到的命令进行剪切
        int end3 = Integer.parseInt(message.substring(26, 32), 16);//  第四次传递的字节大小
        Log.e("end3", end3+"");
        int end2 = Integer.parseInt(message.substring(32, 38), 16);//  第三次传递的字节大小
        Log.e("end2", end2+"");
        int end1 = Integer.parseInt(message.substring(38, 44), 16);//  第二次传递的字节大小
        Log.e("end1", end1+"");
        int end0 = Integer.parseInt(message.substring(44, 50), 16);//  第一次传递的字节大小
        Log.e("end0", end0+"");
        return end3+","+end2+","+end1+","+end0;
    }
    /**
     * 获取偏移值
     * @param message  传递的命令
     */
    public  int   getOffset(String message,int start1,int start2,int i){
        if (i==0){
            return 0;
        }else{
            int start= Integer.parseInt(message.substring(start1,start2), 16);//  第四次的偏移量
            return start;
        }
    }
    /**
     * 将得到的值ascall转成对应的文字
     * @param file_name 需转换的名称
     * @return
     */
    public  String  AscllToString(String file_name){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < file_name.length(); i +=2) {
            String asciiCodeStr = file_name.substring(i, i + 2);          // 每两个字符组成一个 ASCII 码值
            int asciiCode = Integer.parseInt(asciiCodeStr);           // 将十六进制字符串转换为整数
            char ch = (char)asciiCode;                                    // 强制将 ASCII 码值转换为对应的字符
            sb.append(ch);
        }
        String text = sb.toString();               // 获得最终的文本
        System.out.println(text);                  // 输出结果为 "abcdefgh"
        return text;
    }
    /**
     * 将文字转成ascall骂
     * @param name
     * @return
     */
    private static String  StringToSAsc(String name) {
        liost_transfer_sum=0;
        message_ASCLL="1";
        //String转换成char类型
        char[] chars = name.toCharArray();
        System.out.println(" -----------");
        for (int i = 0; i < chars.length; i++) {
            //将转换后的值进行累加
            liost_transfer_sum+=(int)chars[i];
            message_ASCLL=message_ASCLL+(int)chars[i];
        }
        return message_ASCLL;
    }
    /**
     * 停止读线程
     */
    public void close() {
        try {
            mInputStream.close();
        } catch (IOException e) {
        } finally {
            super.interrupt();
        }
    }
    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @param size  实际长度
     * @return 十六进制字符串
     */
    private static String bytesToHexString(byte[] bytes, int size) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (bytes == null || size <= 0) {
            return null;
        }
        for (int i = 0; i < size; i++) {
            int v = bytes[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString().toUpperCase();
    }
    /**
     * 解析接收到的数据，返回对应的十进制数值
     *
     * @param buffer 接收到的数据
     * @param size   实际长度
     * @return 对应的十进制数值
     */
    private static int parseReceivedData(byte[] buffer, int size) {
        int value = -1;
        // 判断是否为数字01
        if (size == 1 && (buffer[0] & 0xff) == 0x01) {
            value = 1;
        }
        return value;
    }
}

