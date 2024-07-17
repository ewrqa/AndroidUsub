package com.lgh.test.send;
import android.util.Log;
/** 收到的日志 */
public class RecvMessage implements IMessage {
    //得到的命令和信息
    private String command;
    private String message;
    public RecvMessage(String command) throws InterruptedException {
        this.command = command;
       LocalQueue.getMessageQueue().offer(this);
    }
    @Override
    public String getMessage() {
        return message;
    }
    @Override
    public boolean isToSend() {
        return false;
    }
    public String getCommand() {
        return command;
    }
    @Override
    public String toString() {
        return "RecvMessage{" + "command='" + command + '\'' + ", message='" + message + '\'' + '}';
    }
}
