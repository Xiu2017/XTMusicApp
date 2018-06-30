package com.xiu.api;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * QQ音乐接口
 */

public class QQMusic {

    private Context context;
    //private MusicDao dao;

    public QQMusic(Context context) {
        this.context = context;
        //this.dao = new MusicDao(context);
    }

    //查询列表
    //http://s.music.qq.com/fcgi-bin/music_search_new_platform?t=0&aggr=1&cr=1&loginUin=0&format=json&inCharset=GB2312&outCharset=utf-8&platform=jqminiframe.json&needNewCode=0&catZhida=0&remoteplace=sizer.newclient.next_song&w=搜索&n=数量&p=页数
    public void search(final String keywork, final int page) {
        final MusicList musicList = new MusicList();
        final List<Music> list = new ArrayList<>();
        //final List<Music> local = dao.getMusicData(keywork);
        String searchUrl = "http://s.music.qq.com/fcgi-bin/music_search_new_platform?" +
                "t=0&aggr=1&cr=1&loginUin=0&format=json&" +
                "inCharset=GB2312&outCharset=utf-8&platform=jqminiframe.json&" +
                "needNewCode=0&catZhida=0&remoteplace=sizer.newclient.next_song&" +
                "w=" + keywork + "&n=" + 30 + "&p=" + page;

        //构建一个请求对象
        Request request = new Request.Builder().url(searchUrl).build();
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
                //通过response得到服务器响应内容
                String str = response.body().string();
                try {
                    JSONArray json = new JSONObject(str)
                            .getJSONObject("data")
                            .getJSONObject("song")
                            .getJSONArray("list");
                    if (json != null) {
                        for (int i = 0; i < json.length(); i++) {
                            JSONObject obj = json.getJSONObject(i);
                            if(obj.toString().contains("&amp;#")){
                                continue;
                            }
                            Music music = new Music();

                            String[] arr = obj.getString("f").split("\\|");
                            //String unstr = obj.getString("f").replace("&amp;#", "%u");
                            //Log.d("unstr", unstr+"");
                            String lrcId;
                            String size;
                            String id;
                            String albumId;
                            int time;
                            if (arr.length == 25) {
                                lrcId = arr[0];
                                size = arr[12];
                                id = arr[20];
                                albumId = arr[22];
                                time = Integer.parseInt(arr[7]) * 1000;
                            } else {
                                continue;
                            }
                            music.setLyric(lrcId);
                            music.setPath(id);
                            //5-10240
                            music.setSize(size.equals("0") ? 0 : Long.parseLong(size));
                            music.setAlbumPath(albumId);
                            music.setTime(time);

                            music.setTitle(obj.getString("fsong"));

                            String fsinger2 = "";
                            if (obj.has("fsinger2") && obj.getString("fsinger2").length() > 0) {
                                fsinger2 = "、" + obj.getString("fsinger2");
                            }
                            music.setArtist(obj.getString("fsinger") + fsinger2);
                            String album = obj.getString("albumName_hilight")
                                            .replace("<span class=\"c_tx_highlight\">","")
                                            .replace("</span>","");
                            music.setAlbum(album);
                            music.setName(music.getArtist() + " - " + music.getTitle() + ".m4a");
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
                    e.printStackTrace();
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
    //获取key的API -- http://base.music.qq.com/fcgi-bin/fcg_musicexpress.fcg?json=3&loginUin=0&format=jsonp&inCharset=GB2312&outCharset=GB2312&notice=0&platform=yqq&needNewCode=0
    //获取专辑图片的API -- http://imgcache.qq.com/music/photo/mid_album_500/2/9/0003g8Rq1cgI29.jpg
    //获取歌词的API -- http://music.qq.com/miniportal/static/lyric/14/101369814.xml
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

        String url = "http://www.douqq.com/qqmusic/qqapi.php";
        //构建一个请求对象
        RequestBody formBody = new FormBody.Builder().add("mid", music.getPath()).build();
        Request request = new Request.Builder().url(url).post(formBody).build();
        //构建一个Call对象
        okhttp3.Call call = new OkHttpClient().newCall(request);
        //异步执行请求
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@Nullable Call call, @NonNull IOException e) {
                bakMusicUrl(music);
            }

            @Override
            public void onResponse(@Nullable Call call, @NonNull Response response) throws IOException {
                //通过response得到服务器响应内容
                String str = response.body().string()
                        .replace("\"{","{")
                        .replace("}\"","}")
                        .replace("\\","");
                Log.d("str", str);
                try {
                    JSONObject json = new JSONObject(str);
                    String path = json.getString("mp3_l");
                    path += "|" + json.getString("mp3_h");
                    path += "|" + json.getString("flac");
                    if(path == null || path.length() < 20){
                        bakMusicUrl(music);
                    }else {
                        music.setPath(path);
                        music.setName(music.getName().replace("m4a","mp3"));

                        //拼接专辑图片链接
                        String albumId = music.getAlbumPath();
                        String albumUrl = null;
                        if(albumId.length() > 2){
                            albumUrl = "http://imgcache.qq.com/music/photo/mid_album_500/"
                                    + albumId.substring(albumId.length() - 2, albumId.length() - 1) + "/"
                                    + albumId.substring(albumId.length() - 1, albumId.length()) + "/"
                                    + albumId + ".jpg";
                        }
                        music.setAlbumPath(albumUrl);

                        //拼接歌词链接 -- 有时间再做

                        Intent kBroadcast = new Intent();
                        kBroadcast.setAction("sBroadcast");
                        kBroadcast.putExtra("what", Msg.GET_MUSIC_PATH);
                        kBroadcast.putExtra("music", music);
                        context.sendBroadcast(kBroadcast);
                    }
                } catch (JSONException e) {
                    bakMusicUrl(music);
                }
            }
        });
    }

    private void bakMusicUrl(final Music music){
        String url = "http://base.music.qq.com/fcgi-bin/fcg_musicexpress.fcg?json=3&loginUin=0&format=jsonp&inCharset=GB2312&outCharset=GB2312&notice=0&platform=yqq&needNewCode=0";
        Request request = new Request.Builder().url(url).build();
        //构建一个Call对象
        okhttp3.Call call = new OkHttpClient().newCall(request);
        //异步执行请求
        call.enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Intent kBroadcast = new Intent();
                kBroadcast.setAction("sBroadcast");
                kBroadcast.putExtra("what", Msg.GET_MUSIC_ERROR);
                context.sendBroadcast(kBroadcast);
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String str = response.body().string();
                try {
                    str = str.replace("jsonCallback(", "").replace(");", "");
                    JSONObject json = new JSONObject(str);
                    //根据获取到的key值拼接音乐链接
                    String urlstr = json.getString("sip");
                    //Log.d("sip", urlstr);
                    String[] urls = urlstr.replace("[", "")
                            .replace("]", "")
                            .replace("\"", "")
                            .replace("\\", "")
                            .split(",");
                    String key = json.getString("key");
                    String musicUrl = urls[1] + "C100" + music.getPath() + ".m4a?vkey=" + key + "&fromtag=0";
                    music.setPath(musicUrl);
                    music.setSize(music.getSize() / 5 - 10240);

                    //拼接专辑图片链接
                    String albumId = music.getAlbumPath();
                    String albumUrl = null;
                    if(albumId.length() > 2){
                        albumUrl = "http://imgcache.qq.com/music/photo/mid_album_500/"
                                + albumId.substring(albumId.length() - 2, albumId.length() - 1) + "/"
                                + albumId.substring(albumId.length() - 1, albumId.length()) + "/"
                                + albumId + ".jpg";
                    }
                    music.setAlbumPath(albumUrl);

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
