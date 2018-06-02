package com.xiu.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Administrator on 2017/12/26.
 */

public class HttpUtils {

    public static void doPost(final String httpPath, final String data, final CallBack callBack) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(httpPath);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(10 * 1000);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setInstanceFollowRedirects(true);
                    conn.connect();
                    if (data != null && data.length() > 0) {
                        OutputStream out = conn.getOutputStream();
                        out.write(data.getBytes());
                        out.close();
                    }
                    callBack.success(FileUtils.formatStreamToString(conn.getInputStream()));
                } catch (IOException e) {
                    e.printStackTrace();
                    callBack.failed("操作出错");
                } finally {
                    if (conn != null)
                        conn.disconnect();
                }

            }
        }).start();
    }

    public static void doGet(final String httpPath, final CallBack callBack) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(httpPath);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10 * 1000);
                    conn.setInstanceFollowRedirects(true);
                    conn.connect();
                    callBack.success(FileUtils.formatStreamToString(conn.getInputStream()));
                } catch (IOException e) {
                    e.printStackTrace();
                    callBack.failed("操作出错");
                } finally {
                    if (conn != null)
                        conn.disconnect();
                }

            }
        }).start();
    }
}
