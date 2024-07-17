package com.lgh.test.send;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * B0：115200 bps
 * B1: 230400 bps
 * B2: 460800 bps999
 * B3: 921600 bps·
 */
public class SerialConst {
    // 串口列表
    public static String[] M_DEVICES = null;
    public static String BAUD_RATES = "115200";
    // 默认串口设备
    public static String DEFAULT_DEVICE_INFO = "/dev/ttyS1";
    // 当前选中的串口设备
    public static Device CURRENT_DEVICE = null;
    // 串口状态
    public static boolean DEVICE_STATUS = false;
}
