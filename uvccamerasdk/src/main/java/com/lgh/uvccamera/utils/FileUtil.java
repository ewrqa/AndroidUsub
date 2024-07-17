package com.lgh.uvccamera.utils;

import static android.opengl.ETC1.getHeight;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

import com.lgh.uvccamera.R;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * 描述：文件存取工具类
 * 作者：liugh
 * 日期：2018/12/25
 * 版本：v2.0.0
 */
public class FileUtil {

    /**
     * 判断当前系统中是否存在外部存储器（一般为SD卡）
     *
     * @return 当前系统中是否存在外部存储器
     */
    public static boolean hasExternalStorage() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 获取外部存储器（一般为SD卡）的路径
     *
     * @return 外部存储器的绝对路径
     */
    public static String getExternalStoragePath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    /**
     * 获取SD卡目录
     *
     * @param foderName
     * @param
     * @return
     */
    public static File getSDCardDir(String foderName) {
        if (!hasExternalStorage()) {
            return null;
        }
        return new File(getExternalStoragePath() + File.separator + foderName);
    }
    /**
     * 获取SD卡文件
     *
     * @param foderName
     * @param fileName
     * @return
     */
    public static File getSDCardFile(String foderName, String fileName) {
        File foder = getSDCardDir(foderName);
        if (foder == null) {
            return null;
        }
        if (!foder.exists()) {
            if (!foder.mkdirs()) {
                return null;
            }
        }
        return new File(foder, fileName);
    }
    /**
     * 获取缓存目录
     *
     * @param context
     * @param dirName
     * @return
     */
    public static String getDiskCacheDir(Context context, String dirName) {
        String cachePath;
        if ((Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable())
                && context.getExternalCacheDir() != null) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return cachePath + File.separator + dirName;
    }
    /**
     * 获取缓存目录文件
     *
     * @param context
     * @param dirName
     * @param fileName
     * @return
     */
    public static File getCacheFile(Context context, String dirName, String fileName) {

        File dirFile = new File(getDiskCacheDir(context, dirName));
        if (!dirFile.exists()) {
            //不存在则创建
            dirFile.mkdirs();
        }
        return new File(dirFile.getPath() + File.separator + fileName);
    }
    /**
     * 删除文件或文件夹
     *
     * @param dirFile
     * @return
     */
    public static boolean deleteFile(File dirFile) {
        if (!dirFile.exists()) {
            return false;
        }
        if (dirFile.isFile()) {
            return dirFile.delete();
        } else {
            for (File file : dirFile.listFiles()) {
                deleteFile(file);
            }
        }
        return dirFile.delete();
    }

    /**
     * 将yuv格式byte数组转化成jpeg图片并保存
     *
     * @param file
     * @param yuv
     * @param width
     * @param height
     */
    public static String saveYuv2Jpeg(File file, byte[] yuv, int width, int height) {
        return saveBitmap(file, ImageUtil.yuv2Bitmap(yuv, width, height));
    }

    /**
     * 将yuv格式byte数组转化成jpeg图片并保存
     *
     * @param file
     * @param yuv
     * @param width
     * @param height
     * @param rotation
     */
    public static String saveYuv2Jpeg(File file, byte[] yuv, int width, int height, float rotation) {
        return saveBitmap(file, ImageUtil.yuv2Bitmap(yuv, width, height, rotation));
    }
    /**
     * 保存bitmap
     *
     * @param file
     * @param bitmap
     */
    public static String saveBitmap(File file, Bitmap bitmap) {
        if (file == null || bitmap == null) {
            return null;
        }
        try {   FileOutputStream fos = new FileOutputStream(file);
            // bitmap  字体的大小   字体的颜色   是否清理缓存

//            Bitmap 添加水印 = addTextWatermark(bitmap, 25, Color.WHITE,  true);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null;
            }
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }
    /**
     * 给图片添加水印 当需要换行的时候，推荐使用StaticLayout 这种实现方式
     * @param src 原bitmap
     * @param textSize 文字大小
     * @param recycle  是否回收bitmap，建议true
     * */
    public  static  Bitmap addTextWatermark(Bitmap src, int textSize, int color,  boolean recycle) {
        Objects.requireNonNull(src, "src is null");
        Bitmap ret = src.copy(src.getConfig(), true);
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        Canvas canvas = new Canvas(ret);
        paint.setColor(color);
        paint.setTextSize(textSize);
        //画笔所展示得我文字
        String content = "时间-" + new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date());
        String pattern = "模式-光控";
        String workable_hatch="工作舱-测报仓";
        String company="上太科技";
        Rect rect = new Rect();
        paint.setTextAlign(Paint.Align.CENTER);
//        单行文字的实现代码
//        paint.getTextBounds(content, 0, content.length(),path);
//        canvas.drawText(content, x, y, paint);
        int yStart = 500; //展示水印的
        float v = 100;
        paint.getTextBounds(content, 0, content.length(), rect);
        //设置文字的间距
        float characterSpacing = (v + yStart) / (float) content.length();
        // 遍历字符串中的每个字符，根据计算出的字符间距和起始坐标绘制文本
        for (int i = 0; i < content.length(); i++) {
            //得到每个文字
            String character = String.valueOf(content.charAt(i));
            //x  设置距离左边的位置  y 设置距离上面的位置
            canvas.drawText(character, 20, 50+i * characterSpacing, paint);
        }
        for (int i = 0; i < pattern.length(); i++) {
            //得到每个文字
            String character = String.valueOf(pattern.charAt(i));
            //x  设置距离左边的位置  y 设置距离上面的位置
            canvas.drawText(character, canvas.getWidth()-30, 50+i * characterSpacing, paint);
        }
        for (int i = 0; i < workable_hatch.length(); i++) {
            //得到每个文字
            String character = String.valueOf(workable_hatch.charAt(i));
            //x  设置距离左边的位置  y 设置距离上面的位置
            canvas.drawText(character, canvas.getWidth()-30, 290+i * characterSpacing, paint);
        }
        for (int i = 0; i < company.length(); i++) {
            //得到每个文字
            String character = String.valueOf(company.charAt(i));
            //x  设置距离左边的位置  y 设置距离上面的位置
            canvas.drawText(character, canvas.getWidth()-30, canvas.getHeight()-130+i * characterSpacing, paint);
        }
//        canvas.translate(x, y);
//        StaticLayout myStaticLayout = new StaticLayout(content, paint, canvas.getWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
//        myStaticLayout.draw(canvas);
        if (recycle && !src.isRecycled()) src.recycle();
        return ret;
    }
}
