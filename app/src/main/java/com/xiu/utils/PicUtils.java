package com.xiu.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.ArrayMap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 图片转为jpg
 */

public class PicUtils {
    private static final String TAG = "PicUtils";
    private static final String PNG = "png";
    private static final String JPG = "jpg";
    private static final String JPEG = "jpeg";
    private static final String BMP = "bmp";
    // private static final String[] imgSuffixes = {PNG, JPG, JPEG, BMP};
    private static final List<String> fileSuffixes = Arrays.asList(PNG, JPG, JPEG, BMP);
    // 缓存文件头信息-文件头信息
    private static final ArrayMap<String, String> mFileTypes = new ArrayMap<String, String>();

    static {
        // images
        mFileTypes.put("FFD8FFE0", JPG);
        mFileTypes.put("89504E47", PNG);
        mFileTypes.put("424D5A52", BMP);
    }

    /**
     * 指定文件夹中的图片文件转成JPG格式
     *
     * @param dir 图片的所在文件夹路径
     */
    public static void ImgToJPG(File dir) {
        File[] files = dir.listFiles();
        String filePath = "";
        for (int i = 0; i < files.length; i++) {
            //先通过后缀名，过滤非图片
            String fileType = files[i].getName().substring(files[i].getName().lastIndexOf('.') + 1);
            if (fileSuffixes.contains(fileType.toLowerCase())) {
                filePath = files[i].getPath();
                String imgType = mFileTypes.get(getFileHeader(filePath)); //获取真正的文件头
                if (!TextUtils.isEmpty(imgType) && !imgType.equals(JPG)) {
                    convertToJpg(filePath, filePath.substring(0, filePath.lastIndexOf('.') + 1) + JPG);
                }
            }
        }
    }

    /**
     * 转换成JPG格式图片 并将原照片删除
     *
     * @param pngFilePath png或者bmp照片
     * @param jpgFilePath jpg照片
     */
    public static void convertToJpg(String pngFilePath, String jpgFilePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(pngFilePath);
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(jpgFilePath));
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)) {
                bos.flush();
            }
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            bitmap.recycle();
            bitmap = null;
        }
        //删除非JPG照片
        if (!pngFilePath.equals(jpgFilePath)) {
            File oldImg = new File(pngFilePath);
            oldImg.delete();
        }
    }


    /**
     * 根据文件路径获取文件头信息
     *
     * @param filePath 文件路径
     * @return 文件头信息
     */
    private static String getFileHeader(String filePath) {
        FileInputStream is = null;
        String value = null;
        try {
            is = new FileInputStream(filePath);
            byte[] b = new byte[4];
            is.read(b, 0, b.length);
            value = bytesToHexString(b);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * 将要读取文件头信息的文件的byte数组转换成string类型表示
     *
     * @param src 要读取文件头信息的文件的byte数组
     * @return 文件头信息
     */
    private static String bytesToHexString(byte[] src) {
        StringBuilder builder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return "";
        }
        for (int i = 0; i < src.length; i++) {
            // 以十六进制（基数 16）无符号整数形式返回一个整数参数的字符串表示形式，并转换为大写
            String hv = Integer.toHexString(src[i] & 0xFF).toUpperCase();
            if (hv.length() < 2) {
                builder.append(0);
            }
            builder.append(hv);
        }
        return builder.toString();
    }
}
