package com.lgh.test.send;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
/**
 * @description 本地队列
 * @version V1.0
 * @author zhang
 * @date 2022/8/26 14:00
 * @update 2022/8/26 14:00
 */
public class LocalQueue {
    private static BlockingQueue<RecvMessage> messageQueue;
    static {
        //本地队
        messageQueue = new LinkedBlockingQueue<>();
    }
    //获取消息队列
    public static BlockingQueue<RecvMessage> getMessageQueue() {
        return messageQueue;
    }
}
