package com.lgh.test;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by lzy on 2017/4/19 0019.
 * 时间工具类
 */
public class TimeUtil {
    public static final SimpleDateFormat DEFAULT_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //获取当前的时间
    public static String currentTime() {
        Date date = new Date();
        return DEFAULT_FORMAT.format(date);
    }
    //获取纯数字的时间、
    public  static Integer  nowNumberTiMe(){
        SimpleDateFormat mMddHHmm = new SimpleDateFormat("MMddHHmm");
        Date date = new Date();
        String format = mMddHHmm.format(date);
        Log.e("当前的时间",format);
        return new Integer(format);
    }
}
