package com.xiu.api;

import android.net.Uri;

import com.xiu.utils.Base64;
import com.xiu.utils.CallBack;
import com.xiu.utils.FileUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 酷狗歌词接口
 */

public class KuGouLrc {

    public static void searchLrc(final String name, final int duration, final CallBack callBack) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //建立连接 -- 查找歌曲
                    String urlStr = "http://lyrics.kugou.com/search?ver=1&man=yes&client=pc&keyword=" + name + "&duration=" + duration + "&hash=";
                    URL url = new URL(encodeUrl(urlStr));  //字符串进行URL编码
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.connect();

                    //读取流 -- JSON歌曲列表
                    InputStream input = conn.getInputStream();
                    String res = FileUtils.formatStreamToString(input);  //流转字符串

                    JSONObject json1 = new JSONObject(res);  //字符串读取为JSON
                    JSONArray json2 = json1.getJSONArray("candidates");
                    if (json2.length() == 0) {
                        callBack.failed("找不到歌词");
                        return;
                    }
                    JSONObject json3 = json2.getJSONObject(0);

                    //建立连接 -- 查找歌词
                    urlStr = "http://lyrics.kugou.com/download?ver=1&client=pc&id=" + json3.get("id") + "&accesskey=" + json3.get("accesskey") + "&fmt=lrc&charset=utf8";
                    url = new URL(encodeUrl(urlStr));
                    conn = (HttpURLConnection) url.openConnection();
                    conn.connect();

                    //读取流 -- 歌词
                    input = conn.getInputStream();
                    res = FileUtils.formatStreamToString(input);
                    JSONObject json4 = new JSONObject(res);

                    //获取歌词base64，并进行解码
                    String base64 = json4.getString("content");
                    final String lyric = Base64.getFromBASE64(base64);
                    callBack.success(lyric);
                } catch (Exception e) {
                    e.printStackTrace();
                    callBack.failed("找不到歌词");
                }
            }
        }).start();
    }

    //字符串编码URL
    private static String encodeUrl(String url) {
        return Uri.encode(url, "-![.:/,%?&=]");
    }
}
