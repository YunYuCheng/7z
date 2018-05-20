package com.example.merry;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Any on 2018/5/19.
 */

public class StorageBean implements Parcelable {
    private String path;
    private String mounted;
    private boolean removable;


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMounted() {
        return mounted;
    }

    public void setMounted(String mounted) {
        this.mounted = mounted;
    }

    public boolean getRemovable() {
        return removable;
    }

    public void setRemovable(boolean removable) {
        this.removable = removable;
    }



    @Override
    public int describeContents() {
        return 0;
    }


    public StorageBean() {

    }

    public StorageBean(Parcel in) {
        this.path = in.readString();
        this.mounted = in.readString();
        this.removable = in.readByte() != 0;

    }

    public static final Parcelable.Creator<StorageBean> CREATOR = new Parcelable.Creator<StorageBean>() {
        @Override
        public StorageBean createFromParcel(Parcel source) {
            return new StorageBean(source);
        }

        @Override
        public StorageBean[] newArray(int size) {
            return new StorageBean[size];
        }
    };


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // TODO Auto-generated method stub
        dest.writeString(this.path);
        dest.writeString(this.mounted);
        dest.writeByte(removable ? (byte) 1 : (byte) 0);
    }
}
