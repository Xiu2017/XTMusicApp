package com.xiu.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 评论实体类
 */

public class Comment implements Parcelable {
    private String username;
    private long time;
    private String likedCount;
    private String content;
    private String beRepliedUser;
    private String beRepliedContent;

    public Comment() {
    }

    public Comment(String username, long time, String likedCount, String content, String beRepliedUser, String beRepliedContent) {
        this.username = username;
        this.time = time;
        this.likedCount = likedCount;
        this.content = content;
        this.beRepliedUser = beRepliedUser;
        this.beRepliedContent = beRepliedContent;
    }

    protected Comment(Parcel in) {
        username = in.readString();
        time = in.readLong();
        likedCount = in.readString();
        content = in.readString();
        beRepliedUser = in.readString();
        beRepliedContent = in.readString();
    }

    public static final Creator<Comment> CREATOR = new Creator<Comment>() {
        @Override
        public Comment createFromParcel(Parcel in) {
            return new Comment(in);
        }

        @Override
        public Comment[] newArray(int size) {
            return new Comment[size];
        }
    };

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getLikedCount() {
        return likedCount;
    }

    public void setLikedCount(String likedCount) {
        this.likedCount = likedCount;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getBeRepliedUser() {
        return beRepliedUser;
    }

    public void setBeRepliedUser(String beRepliedUser) {
        this.beRepliedUser = beRepliedUser;
    }

    public String getBeRepliedContent() {
        return beRepliedContent;
    }

    public void setBeRepliedContent(String beRepliedContent) {
        this.beRepliedContent = beRepliedContent;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(username);
        parcel.writeLong(time);
        parcel.writeString(likedCount);
        parcel.writeString(content);
        parcel.writeString(beRepliedUser);
        parcel.writeString(beRepliedContent);
    }
}
