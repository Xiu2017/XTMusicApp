package com.xiu.api;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.xiu.dao.MusicDao;
import com.xiu.entity.Comment;
import com.xiu.entity.CommentList;
import com.xiu.entity.Msg;
import com.xiu.entity.Music;
import com.xiu.entity.MusicList;
import com.xiu.utils.JSSecret;
import com.xiu.utils.UrlParamPair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 网易云音乐接口
 */

public class NeteaseMusic {

    private Context context;

    public NeteaseMusic(Context context) {
        this.context = context;
    }


    //查询列表
    public void search(final String keywork, final int page) {
        final MusicList musicList = new MusicList();
        final List<Music> list = new ArrayList<>();

        UrlParamPair upp = NeteaseApi.SearchMusicList(keywork, "1", (page - 1) * 30, 30);
        String req_str = upp.getParas().toJSONString();
        Map map = JSSecret.getDatas(req_str);
        RequestBody formBody = new FormBody.Builder()
                .add("params", map.get("params").toString())
                .add("encSecKey", map.get("encSecKey").toString())
                .build();
        try {
            //构建一个请求对象
            Request request = new Request.Builder()
                    .removeHeader("User-Agent")
                    .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:57.0) Gecko/20100101 Firefox/57.0")
                    .addHeader("Accept", "*/*")
                    .addHeader("Cache-Control", "no-cache")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Host", "music.163.com")
                    .addHeader("Accept-Language", "zh-CN,en-US;q=0.7,en;q=0.3")
                    .addHeader("DNT", "1")
                    .addHeader("Pragma", "no-cache")
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .post(formBody)
                    .url("http://music.163.com/weapi/cloudsearch/get/web?csrf_token=").build();
            //构建一个Call对象
            okhttp3.Call call = new OkHttpClient().newCall(request);
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
                    try {
                        //通过response得到服务器响应内容
                        if (response.body() != null) {
                            String res = response.body().string();
                            JSONObject data = new JSONObject(res);
                            if (!data.isNull("result") && !data.getJSONObject("result").isNull("songs")) {
                                JSONArray json = data.getJSONObject("result").getJSONArray("songs");
                                if (json != null) {
                                    for (int i = 0; i < json.length(); i++) {
                                        JSONObject obj = json.getJSONObject(i);
                                        Music music = new Music();
                                        long size = 0;
                                        if (!obj.isNull("m")) {
                                            size = obj.getJSONObject("m").getLong("size");
                                        } else if (!obj.isNull("l")) {
                                            size = obj.getJSONObject("l").getLong("size");
                                        }
                                        music.setSize(size - (1024 * 768));
                                        music.setTitle(obj.getString("name"));
                                        if (obj.getJSONArray("ar").isNull(1)) {
                                            music.setArtist(obj.getJSONArray("ar").getJSONObject(0).getString("name"));
                                        } else {
                                            music.setArtist(obj.getJSONArray("ar").getJSONObject(0).getString("name")
                                                    + "、" + obj.getJSONArray("ar").getJSONObject(1).getString("name"));
                                        }
                                        music.setName(music.getArtist() + " - " + music.getTitle() + ".mp3");
                                        music.setAlbum(obj.getJSONObject("al").getString("name"));
                                        music.setAlbumPath(obj.getJSONObject("al").getString("picUrl"));
                                        String path = "http://music.163.com/song/media/outer/url?id=" + obj.getString("id") + ".mp3";
                                        music.setPath(path);
                                        //if (!dao.isExist(local, music)) {
                                        list.add(music);
                                        //}
                                    }
                                    //Log.d("size", json.length()+","+list.size());
                                    musicList.setList(list);
                                }
                            }

                            Intent kBroadcast = new Intent();
                            kBroadcast.setAction("sBroadcast");
                            kBroadcast.putExtra("what", Msg.SEARCH_RESULT);
                            kBroadcast.putExtra("list", musicList);
                            context.sendBroadcast(kBroadcast);
                        }
                    } catch (JSONException e) {
                        Intent kBroadcast = new Intent();
                        kBroadcast.setAction("sBroadcast");
                        kBroadcast.putExtra("what", Msg.SEARCH_ERROR);
                        context.sendBroadcast(kBroadcast);
                        e.printStackTrace();
                    }
                    //Log.d("call result", str);
                }
            });
        } catch (Exception e) {
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
    }

    //获取评论
    public void comment(String musicId) {
        final List<Comment> list = new ArrayList<>();
        final CommentList commentList = new CommentList();

        UrlParamPair upp = NeteaseApi.getDetailOfPlaylist(musicId);
        String req_str = upp.getParas().toJSONString();
        Map map = JSSecret.getDatas(req_str);
        RequestBody formBody = new FormBody.Builder()
                .add("params", map.get("params").toString())
                .add("encSecKey", map.get("encSecKey").toString())
                .build();
        try {
            //构建一个请求对象
            Request request = new Request.Builder()
                    .removeHeader("User-Agent")
                    .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:57.0) Gecko/20100101 Firefox/57.0")
                    .addHeader("Accept", "*/*")
                    .addHeader("Cache-Control", "no-cache")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Host", "music.163.com")
                    .addHeader("Accept-Language", "zh-CN,en-US;q=0.7,en;q=0.3")
                    .addHeader("DNT", "1")
                    .addHeader("Pragma", "no-cache")
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .post(formBody)
                    .url("http://music.163.com/weapi/v1/resource/comments/R_SO_4_" + musicId + "?csrf_token=").build();
            //构建一个Call对象
            okhttp3.Call call = new OkHttpClient().newCall(request);
            //异步执行请求
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@Nullable Call call, @NonNull IOException e) {
                    Intent kBroadcast = new Intent();
                    kBroadcast.setAction("cBroadcast");
                    kBroadcast.putExtra("what", Msg.SEARCH_ERROR);
                    context.sendBroadcast(kBroadcast);
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@Nullable Call call, @NonNull Response response) throws IOException {
                    try {
                        //通过response得到服务器响应内容
                        if (response.body() != null) {
                            String res = response.body().string();
                            JSONObject data = new JSONObject(res);
                            if(!data.isNull("hotComments")) {
                                JSONArray json = data.getJSONArray("hotComments");
                                if (json != null) {
                                    for (int i = 0; i < json.length(); i++) {
                                        JSONObject obj = json.getJSONObject(i);
                                        Comment comment = new Comment();
                                        comment.setUsername(obj.getJSONObject("user").getString("nickname"));
                                        comment.setTime(obj.getLong("time"));
                                        comment.setLikedCount(obj.getString("likedCount"));
                                        comment.setContent(obj.getString("content"));
                                        JSONArray bereq = obj.getJSONArray("beReplied");
                                        if (bereq.length() > 0) {
                                            JSONObject temp = bereq.getJSONObject(0);
                                            comment.setBeRepliedUser(temp.getJSONObject("user").getString("nickname"));
                                            comment.setBeRepliedContent(temp.getString("content"));
                                        }
                                        list.add(comment);
                                        if (i == json.length() - 1 && list.size() == json.length()) {
                                            i = 0;
                                            json = new JSONObject(res).getJSONArray("comments");
                                        }
                                    }
                                    commentList.setList(list);
                                }
                            }

                            Intent kBroadcast = new Intent();
                            kBroadcast.setAction("cBroadcast");
                            kBroadcast.putExtra("what", Msg.SEARCH_RESULT);
                            kBroadcast.putExtra("list", commentList);
                            context.sendBroadcast(kBroadcast);
                        }
                    } catch (JSONException e) {
                        Intent kBroadcast = new Intent();
                        kBroadcast.setAction("cBroadcast");
                        kBroadcast.putExtra("what", Msg.SEARCH_ERROR);
                        context.sendBroadcast(kBroadcast);
                        e.printStackTrace();
                    }
                    //Log.d("call result", str);
                }
            });
        } catch (Exception e) {
            Intent kBroadcast = new Intent();
            kBroadcast.setAction("cBroadcast");
            kBroadcast.putExtra("what", Msg.SEARCH_ERROR);
            context.sendBroadcast(kBroadcast);
            e.printStackTrace();
        }
    }
}
