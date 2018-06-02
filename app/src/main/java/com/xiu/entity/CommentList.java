package com.xiu.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * 评论集合
 */

public class CommentList implements Parcelable{
    private List<Comment> list;

    public CommentList() {
    }

    public CommentList(List<Comment> list) {
        this.list = list;
    }

    protected CommentList(Parcel in) {
        list = in.createTypedArrayList(Comment.CREATOR);
    }

    public static final Creator<CommentList> CREATOR = new Creator<CommentList>() {
        @Override
        public CommentList createFromParcel(Parcel in) {
            return new CommentList(in);
        }

        @Override
        public CommentList[] newArray(int size) {
            return new CommentList[size];
        }
    };

    public List<Comment> getList() {
        return list;
    }

    public void setList(List<Comment> list) {
        this.list = list;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(list);
    }
}
