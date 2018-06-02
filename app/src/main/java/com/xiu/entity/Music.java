package com.xiu.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * 保存音乐信息的实体类
 */

@DatabaseTable
public class Music implements Parcelable {
    @DatabaseField(generatedId = true)
    private int _id;  //编号
    @DatabaseField
    private String name;  //文件名
    @DatabaseField
    private String title;  //标题
    @DatabaseField
    private String artist;  //歌手
    @DatabaseField
    private String album;  //专辑
    @DatabaseField
    private int time;  //时长
    @DatabaseField
    private String path;  //路径
    @DatabaseField
    private String parentPath;  //父级目录
    @DatabaseField
    private String lyric;  //歌词路径
    @DatabaseField
    private String albumPath;  //专辑图片路径
    @DatabaseField
    private long date;  //添加时间
    @DatabaseField
    private long size;  //文件大小

    public Music() {
    }

    public Music(int _id, String name, String title, String artist, String album, int time, String path, String parentPath, String lyric, String albumPath, long date, long size) {
        this._id = _id;
        this.name = name;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.time = time;
        this.path = path;
        this.parentPath = parentPath;
        this.lyric = lyric;
        this.albumPath = albumPath;
        this.date = date;
        this.size = size;
    }

    protected Music(Parcel in) {
        _id = in.readInt();
        name = in.readString();
        title = in.readString();
        artist = in.readString();
        album = in.readString();
        time = in.readInt();
        path = in.readString();
        parentPath = in.readString();
        albumPath = in.readString();
        lyric = in.readString();
        date = in.readLong();
        size = in.readLong();
    }

    public static final Creator<Music> CREATOR = new Creator<Music>() {
        @Override
        public Music createFromParcel(Parcel in) {
            return new Music(in);
        }

        @Override
        public Music[] newArray(int size) {
            return new Music[size];
        }
    };

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public String getAlbumPath() {
        return albumPath;
    }

    public void setAlbumPath(String albumPath) {
        this.albumPath = albumPath;
    }

    public String getLyric() {
        return lyric;
    }

    public void setLyric(String lyric) {
        this.lyric = lyric;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(_id);
        parcel.writeString(name);
        parcel.writeString(title);
        parcel.writeString(artist);
        parcel.writeString(album);
        parcel.writeInt(time);
        parcel.writeString(path);
        parcel.writeString(parentPath);
        parcel.writeString(albumPath);
        parcel.writeString(lyric);
        parcel.writeLong(date);
        parcel.writeLong(size);
    }
}
