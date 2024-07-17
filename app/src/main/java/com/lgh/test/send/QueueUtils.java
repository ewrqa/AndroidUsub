package com.lgh.test.send;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
/**
 * @description 本队队列工具类
 * @version V1.0
 * @author zhang
 * @date 2022/8/26 14:01
 * @update 2022/8/26 14:01
 */
public class QueueUtils {
    //自定义消息队列
    //首页实时数据的消息队列
    private static BlockingQueue<RecvMessage> blQueue = LocalQueue.getMessageQueue();
    //获取信息
    public static  RecvMessage getMsg() {
        try {
            RecvMessage poll =blQueue.poll(1, TimeUnit.SECONDS);
            if(poll!=null){
                return poll;
            }else{
                return null;
            }
        } catch (InterruptedException e) {
            return null;
        }
    }
    //清空数据
    public static  void cleartNsg(){
        blQueue.clear();
    }
}
