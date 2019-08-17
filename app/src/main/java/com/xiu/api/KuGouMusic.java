package com.xiu.api;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.xiu.dao.MusicDao;
import com.xiu.entity.Msg;
import com.xiu.entity.Music;
import com.xiu.entity.MusicList;
import com.xiu.utils.StorageUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 酷狗音乐接口
 */

public class KuGouMusic {

    private Context context;
    private MusicDao dao;

    public KuGouMusic(Context context) {
        this.context = context;
        this.dao = new MusicDao(context);
    }

    //查询列表
    //http://mobilecdn.kugou.com/api/v3/search/song?format=jsonp&keyword=搜索内容&page=1&pagesize=10&showtype=1&callback=kgJSONP238513750
    public void search(final String keywork, final int page) {
        final MusicList musicList = new MusicList();
        final List<Music> list = new ArrayList<>();
        //final List<Music> local = dao.getMusicData(keywork);
        String searchUrl = "http://mobilecdn.kugou.com/api/v3/search/song?format=jsonp&keyword=" + keywork +
                "&page=" + page + "&pagesize=30&showtype=1";

        //构建一个请求对象
        Request request = new Request.Builder().url(searchUrl).build();
        //构建一个Call对象
        okhttp3.Call call = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
                .newCall(request);
        //异步执行请求
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@Nullable Call call, @NonNull IOException e) {
                if (page == 1) {
                    musicList.setList(list);
                }
                Intent kBroadcast = new Intent();
                kBroadcast.setAction("sBroadcast");
                kBroadcast.putExtra("what", Msg.SEARCH_ERROR);
                kBroadcast.putExtra("list", musicList);
                context.sendBroadcast(kBroadcast);
                e.printStackTrace();
            }

            @Override
            public void onResponse(@Nullable Call call, @NonNull Response response) throws IOException {
                //通过response得到服务器响应内容
                String str = "";
                if(response.body() != null){
                    str = response.body().string();
                    str = str.substring(1, str.length() - 1);
                }
                try {
                    JSONArray json = new JSONObject(str)
                            .getJSONObject("data")
                            .getJSONArray("info");
                    if (json != null) {
                        for (int i = 0; i < json.length(); i++) {
                            JSONObject obj = json.getJSONObject(i);
                            Music music = new Music();
                            music.setSize(obj.getLong("filesize"));
                            music.setTitle(obj.getString("songname_original"));
                            music.setArtist(obj.getString("singername"));
                            music.setName(music.getArtist() + " - " + music.getTitle() + ".mp3");
                            music.setPath(obj.getString("hash"));
                            //52Log.d("hash", music.getPath());
                            //if (!dao.isExist(local, music)) {
                                list.add(music);
                            //}
                        }
                        musicList.setList(list);
                    }

                    Intent kBroadcast = new Intent();
                    kBroadcast.setAction("sBroadcast");
                    kBroadcast.putExtra("what", Msg.SEARCH_RESULT);
                    kBroadcast.putExtra("list", musicList);
                    context.sendBroadcast(kBroadcast);
                } catch (JSONException e) {
                    Intent kBroadcast = new Intent();
                    kBroadcast.setAction("sBroadcast");
                    kBroadcast.putExtra("what", Msg.SEARCH_ERROR);
                    context.sendBroadcast(kBroadcast);
                }
                //Log.d("call result", str);
            }
        });
    }

    //获取歌曲链接
    //http://m.kugou.com/app/i/getSongInfo.php?hash=2b616f6ab9f8655210fd823b900085cc&cmd=playInfo
    public void musicUrl(final Music music) {
        String hash = music.getPath();
        StorageUtil util = new StorageUtil(context);
        String innerSD = util.innerSDPath();
        String extSD = util.extSDPath();
        if (hash.contains("http://") || hash.contains(innerSD + "") || hash.contains(extSD + "")) {
            Intent kBroadcast = new Intent();
            kBroadcast.setAction("sBroadcast");
            kBroadcast.putExtra("what", Msg.GET_MUSIC_PATH);
            kBroadcast.putExtra("music", music);
            context.sendBroadcast(kBroadcast);
            return;
        }

        //http://www.kugou.com/yy/index.php?r=play/getdata&hash=
//        String url = "http://m.kugou.com/app/i/getSongInfo.php?hash="+hash+"&cmd=playInfo";
        String url = "http://www.kugou.com/yy/index.php?r=play/getdata&hash=" + hash;
//        String url = "https://wwwapi.kugou.com/yy/index.php?r=play/getdata&hash=" + hash;
        //构建一个请求对象
        //Request request = new Request.Builder().url(url).build();
        //构建一个请求对象
        Request request = new Request.Builder()
                .removeHeader("User-Agent")
                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                .addHeader("Cache-Control", "max-age=0")
                .addHeader("Connection", "keep-alive")
                .addHeader("Cookie", "kg_mid=917aec0734eebfbc2cda15a8fdf066ee; ACK_SERVER_10015=%7B%22list%22%3A%5B%5B%22gzlogin-user.kugou.com%22%5D%5D%7D; Hm_lvt_aedee6983d4cfc62f509129360d6bb3d=1566048962; kg_dfid=07gy9j3Chhtn0MNvE81I9fhf; kg_dfid_collect=d41d8cd98f00b204e9800998ecf8427e; Hm_lpvt_aedee6983d4cfc62f509129360d6bb3d=1566050138")
                .addHeader("If-Modified-Since", "Sat, 17 Aug 2019 14:17:00 GMT")
                .addHeader("Upgrade-Insecure-Requests", "1")
                .addHeader("Host", "www.kugou.com")
//                .addHeader("Accept-Encoding", "gzip, deflate")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
                //.post(formBody)
                .url(url).build();
        //构建一个Call对象
        okhttp3.Call call = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
                .newCall(request);
        //异步执行请求
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@Nullable Call call, @NonNull IOException e) {
                Intent kBroadcast = new Intent();
                kBroadcast.setAction("sBroadcast");
                kBroadcast.putExtra("what", Msg.GET_MUSIC_ERROR);
                context.sendBroadcast(kBroadcast);
                e.printStackTrace();
            }

            @Override
            public void onResponse(@Nullable Call call, @NonNull Response response) throws IOException {
                //通过response得到服务器响应内容
                String str = response.body().string();
                //Log.d("str", str);
                try {
                    JSONObject json = new JSONObject(str).getJSONObject("data");
                    //Log.i("json", json.toString());
                    String path = json.getString("play_url");
                    path = path.replace("https://", "http://");
                    music.setPath(path);
                    music.setTime(json.getInt("timelength"));
                    //music.setName(music.getArtist()+" - "+music.getTitle()+".mp3");
                    music.setAlbum(json.getString("album_name"));
                    music.setAlbumPath(json.getString("img"));
                    music.setLyric(json.getString("lyrics"));
                    //Log.d("lrc",music.getLyricPath());

                    Intent kBroadcast = new Intent();
                    kBroadcast.setAction("sBroadcast");
                    kBroadcast.putExtra("what", Msg.GET_MUSIC_PATH);
                    kBroadcast.putExtra("music", music);
                    context.sendBroadcast(kBroadcast);
                } catch (JSONException e) {
                    Intent kBroadcast = new Intent();
                    kBroadcast.setAction("sBroadcast");
                    kBroadcast.putExtra("what", Msg.GET_MUSIC_ERROR);
                    context.sendBroadcast(kBroadcast);
                    e.printStackTrace();
                }
            }
        });
    }
}
