package com.xiu.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 事件实体类
 */

public class Event implements Parcelable{
    private int eid;
    private Object content;

    public Event() {
    }

    public Event(int eid, Object content) {
        this.eid = eid;
        this.content = content;
    }

    protected Event(Parcel in) {
        eid = in.readInt();
    }

    public static final Creator<Event> CREATOR = new Creator<Event>() {
        @Override
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };

    public int getEid() {
        return eid;
    }

    public void setEid(int eid) {
        this.eid = eid;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(eid);
    }
}
