package com.xiu.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * 用于传递序列化List
 */

public class MusicList implements Parcelable {

    private List<Music> list;

    public MusicList() {
    }

    public MusicList(List<Music> list) {
        this.list = list;
    }

    protected MusicList(Parcel in) {
        list = in.createTypedArrayList(Music.CREATOR);
    }

    public static final Creator<MusicList> CREATOR = new Creator<MusicList>() {
        @Override
        public MusicList createFromParcel(Parcel in) {
            return new MusicList(in);
        }

        @Override
        public MusicList[] newArray(int size) {
            return new MusicList[size];
        }
    };

    public List<Music> getList() {
        return list;
    }

    public void setList(List<Music> list) {
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
