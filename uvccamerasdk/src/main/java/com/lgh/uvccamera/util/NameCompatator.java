package com.lgh.uvccamera.util;

import android.util.Log;

import com.serenegiant.usb.Size;

import java.util.Comparator;

public class NameCompatator implements Comparator<Size> {
    @Override
    public int compare(Size size, Size t1) {
      return  (t1.height*t1.width)-(size.height*size.width);
    }
}
