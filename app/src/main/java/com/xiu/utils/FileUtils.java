package com.xiu.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 文件操作的工具类
 */
public class FileUtils {

    //根据Uri取出文件所在的路径
    public static String getPath(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    //将一个输入流转换为一个字符串
    public static String formatStreamToString(InputStream stream) {
        if (stream != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] bytes = new byte[1024];
            int len = 0;
            try {
                while ((len = stream.read(bytes)) != -1) {
                    out.write(bytes, 0, len);
                }
                String str = out.toString();
                out.flush();
                out.close();
                stream.close();
                return str;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //将字符串文本保存为文件
    public static void TextToFile(final String strFilename, final String strBuffer) {
        try {
            // 创建文件对象
            File fileText = new File(strFilename);
            if (!fileText.getParentFile().exists()) {
                fileText.getParentFile().mkdirs();
            }
            // 向文件写入对象写入信息
            FileWriter fileWriter = new FileWriter(fileText);
            // 写文件
            fileWriter.write(strBuffer);
            // 关闭
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //执行下载文件到指定位置
    public static void downLoadFile(final String fromPath, final String savePath, final CallBack callBack) {
        if (fromPath != null && savePath != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(fromPath);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(20 * 1000);
                        conn.connect();
                        InputStream input = conn.getInputStream();
                        File file = new File(savePath);
                        if (!file.getParentFile().exists())
                            file.getParentFile().mkdirs();
                        OutputStream out = new FileOutputStream(file);
                        byte[] bytes = new byte[1024];
                        for (int len = 0; (len = input.read(bytes)) != -1; ) {
                            out.write(bytes, 0, len);
                        }
                        out.flush();
                        out.close();
                        input.close();
                        callBack.success(null);//下载成功
                    } catch (Exception e) {
                        e.printStackTrace();
                        callBack.failed(null);//下载失败
                    }
                }
            }).start();
        }
    }

    //复制文件
    public static void copyFile(String formPath, String toPath) {
        File fromFile = new File(formPath);
        File toFile = new File(toPath);
        if (!toFile.getParentFile().exists()) {
            toFile.getParentFile().mkdirs();
        }
        try {
            FileInputStream ins = new FileInputStream(fromFile);
            FileOutputStream out = new FileOutputStream(toFile);
            byte[] b = new byte[1024];
            int n = 0;
            while ((n = ins.read(b)) != -1) {
                out.write(b, 0, n);
            }
            ins.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //判断文件是否存在
    public static boolean existsFile(String path) {
        if (path != null && path.length() > 0) {
            File file = new File(path);
            if (file.exists())
                return true;
        }
        return false;
    }

    //删除文件夹下的所有文件
    public static boolean delAllFile(String path) {
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return flag;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                delAllFile(path + "/" + tempList[i]);//先删除文件夹里面的文件
                delFolder(path + "/" + tempList[i]);//再删除空文件夹
                flag = true;
            }
        }
        return flag;
    }

    //删除文件夹
    public static void delFolder(String folderPath) {
        try {
            delAllFile(folderPath); //删除完里面所有内容
            String filePath = folderPath;
            filePath = filePath.toString();
            java.io.File myFilePath = new java.io.File(filePath);
            myFilePath.delete(); //删除空文件夹
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
