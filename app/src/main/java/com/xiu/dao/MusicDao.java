package com.xiu.dao;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.provider.MediaStore;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.xiu.entity.Music;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作音乐表的类
 */

public class MusicDao {

    private Context context;
    private Dao dao;

    public MusicDao(Context context) {
        this.context = context;
        DbHelper dbHelper = DbHelper.getInstance(context);
        try {
            dao = dbHelper.getDao(Music.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //根据添加时间查询歌曲
    public List<Music> selMusicByDate() {
        QueryBuilder builder = dao.queryBuilder();
        try {
            builder.orderBy("date", false);
            return builder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //根据文件名和时长删除歌曲
    public void delMusicByNameAndTime(String name, long size) {
        QueryBuilder builder = dao.queryBuilder();
        try {
            String str = name.replace("'", "''").replace("\"", "\"\"");
            builder.where().eq("name", str).and().eq("size", size);
            dao.delete(builder.query());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //删除歌曲
    public boolean delMusicById(Music music) {
        try {
            dao.delete(music);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //更新音乐信息
    public boolean updMusic(Music music) {
        try {
            dao.update(music);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //将音乐添加到播放记录
    public void addToHistory(Music music) {
        music.setDate(System.currentTimeMillis());
        try {
            QueryBuilder builder = dao.queryBuilder();
            String str = music.getName().replace("'", "''").replace("\"", "\"\"");
            builder.where().eq("name", str)
                    .and().eq("size", music.getSize());
            List<Music> list = builder.query();
            if (list != null && list.size() > 0) {
                dao.update(music);
            } else {
                dao.create(music);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //ContentResolver中获取音乐列表
    public List<Music> getMusicData() {
        List<Music> list = new ArrayList<>();
        String[] columns = {  //要查询的列
                MediaStore.Audio.Media.TITLE,  //标题
                MediaStore.Audio.Media.ARTIST,  //歌手
                MediaStore.Audio.Media.ALBUM,  //专辑
                MediaStore.Audio.Media.DISPLAY_NAME,  //文件名
                MediaStore.Audio.Media.DURATION,  //时长
                MediaStore.Audio.Media.DATA,  //歌曲路径
                MediaStore.Audio.Media.SIZE  //大小
        };
        //要筛选的格式
        String where = MediaStore.Audio.Media.DISPLAY_NAME + " like '%.mp3'" +
                " or " + MediaStore.Audio.Media.DISPLAY_NAME + " like '%.ape'" +
                " or " + MediaStore.Audio.Media.DISPLAY_NAME + " like '%.aac'" +
                " or " + MediaStore.Audio.Media.DISPLAY_NAME + " like '%.ogg'" +
                " or " + MediaStore.Audio.Media.DISPLAY_NAME + " like '%.wav'" +
                " or " + MediaStore.Audio.Media.DISPLAY_NAME + " like '%.flac'" +
                " or " + MediaStore.Audio.Media.DISPLAY_NAME + " like '%.m4a'";
        ContentResolver cr = context.getContentResolver();
        if (cr != null) {
            //获取所有歌曲
            Cursor cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    columns, where, null, MediaStore.Audio.Media.TITLE + " COLLATE LOCALIZED");
            if (null == cursor) {
                return null;
            }
            if (cursor.moveToFirst()) {
                Music music;
                do {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    if (!new File(path).exists()) {
                        continue;
                    }
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    if ("<unknown>".equals(artist)) {
                        String[] str = title.split(" - ");
                        if (str.length == 2) {
                            artist = str[0];
                            title = str[1];
                        } else {
                            artist = "未知";
                        }
                    }
                    String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                    //if (isRepeat(list, title, artist, album)) continue;    //去掉重复歌曲
                    int time = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                    long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));

                    music = new Music();
                    music.setTitle(title);
                    music.setArtist(artist);
                    music.setAlbum(album);
                    music.setTime(time);
                    music.setPath(path);
                    music.setName(name);
                    music.setSize(size);

                    list.add(music);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return list;
    }

    //SQL特殊字符转义
    private static String ConvertSql(String sql){
        sql = sql.replace("'","''");
        sql = sql.replace("\"","\"\"");
        // 这句话一定要在下面两个语句之前，否则作为转义符的方括号会被当作数据被再次处理
        sql = sql.replace("[", "[[]");
        sql = sql.replace("_", "[_]");
        sql = sql.replace("%", "[%]");
        return sql;
    }

    //ContentResolver中获取音乐列表（带查询条件）
    public List<Music> getMusicData(String keywork) {
        //防止SQL语句注入
        keywork = ConvertSql(keywork);

        List<Music> list = new ArrayList<>();
        String[] columns = {  //要查询的列
                MediaStore.Audio.Media.TITLE,  //标题
                MediaStore.Audio.Media.ARTIST,  //歌手
                MediaStore.Audio.Media.ALBUM,  //专辑
                MediaStore.Audio.Media.DISPLAY_NAME,  //文件名
                MediaStore.Audio.Media.DURATION,  //时长
                MediaStore.Audio.Media.DATA,  //歌曲路径
                MediaStore.Audio.Media.SIZE  //大小
        };
        //要筛选的格式
        String where = "(" + MediaStore.Audio.Media.DISPLAY_NAME + " like '%.mp3'" +
                " or " + MediaStore.Audio.Media.DISPLAY_NAME + " like '%.ape'" +
                " or " + MediaStore.Audio.Media.DISPLAY_NAME + " like '%.aac'" +
                " or " + MediaStore.Audio.Media.DISPLAY_NAME + " like '%.ogg'" +
                " or " + MediaStore.Audio.Media.DISPLAY_NAME + " like '%.wav'" +
                " or " + MediaStore.Audio.Media.DISPLAY_NAME + " like '%.flac'" +
                " or " + MediaStore.Audio.Media.DISPLAY_NAME + " like '%.m4a')" +
                //要筛选的条件
                " and (" + MediaStore.Audio.Media.TITLE + " like '%" + keywork + "%'" +
                " or " + MediaStore.Audio.Media.ARTIST + " like '%" + keywork + "%'" +
                " or " + MediaStore.Audio.Media.ALBUM + " like '%" + keywork + "%'" +
                " or " + MediaStore.Audio.Media.DISPLAY_NAME + " like '%" + keywork + "%')";

        ContentResolver cr = context.getContentResolver();
        if (cr != null) {
            //获取所有歌曲
            Cursor cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    columns, where, null, MediaStore.Audio.Media.TITLE + " COLLATE LOCALIZED");
            if (null == cursor) {
                return null;
            }
            if (cursor.moveToFirst()) {
                Music music;
                do {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    if (!new File(path).exists()) {
                        continue;
                    }
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    if ("<unknown>".equals(artist)) {
                        String[] str = title.split(" - ");
                        if (str.length == 2) {
                            artist = str[0];
                            title = str[1];
                        } else {
                            artist = "未知";
                        }
                    }
                    String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    if (isRepeat(list, title, artist, album)) continue;    //去掉重复歌曲
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                    int time = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                    long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));

                    music = new Music();
                    music.setTitle(title);
                    music.setArtist(artist);
                    music.setAlbum(album);
                    music.setTime(time);
                    music.setPath(path);
                    music.setName(name);
                    music.setSize(size);

                    list.add(music);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return list;
    }

    //根据音乐名称、艺术家、唱片集来判断是否重复包含了
    private boolean isRepeat(List<Music> list, String title, String artist, String album) {
        for (Music music : list) {
            if (title.equals(music.getTitle()) && artist.equals(music.getArtist()) && album.equals(music.getAlbum())) {
                return true;
            }
        }
        return false;
    }

    //检测歌曲是否存在本地
    public boolean isExist(List<Music> list, Music music) {
        if (list == null || list.size() == 0) return false;
        for (int i = 0; i < list.size(); i++) {
            Music m = list.get(i);
            if (music.getName().equals(m.getName()) && music.getSize() == m.getSize()) {
                return true;
            }
        }
        return false;
    }

    //获取音乐文件自带专辑封面的Bitmap
    public Bitmap getAlbumBitmap(String url, int defaultRes) {
        if (url == null || url.contains("http://"))
            return BitmapFactory.decodeResource(context.getResources(), defaultRes);
        Bitmap bitmap = null;
        //能够获取多媒体文件元数据的类
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(url); //设置数据源
            byte[] embedPic = retriever.getEmbeddedPicture(); //得到字节型数据
            if (embedPic != null) {
                bitmap = BitmapFactory.decodeByteArray(embedPic, 0, embedPic.length); //转换为图片
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            retriever.release();
        }
        return bitmap == null ? BitmapFactory.decodeResource(context.getResources(), defaultRes) : bitmap;
    }
}
