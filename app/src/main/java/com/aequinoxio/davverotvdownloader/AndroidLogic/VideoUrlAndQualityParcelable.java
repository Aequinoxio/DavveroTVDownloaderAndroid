package com.aequinoxio.davverotvdownloader.AndroidLogic;

import android.os.Parcel;
import android.os.Parcelable;

import com.aequinoxio.davverotvdownloader.DownloadLogic.VideoUrlAndQuality;

public class VideoUrlAndQualityParcelable extends VideoUrlAndQuality implements Parcelable {

    public VideoUrlAndQualityParcelable(String quality, String videoUrl) {
        super(quality, videoUrl);
    }

    public VideoUrlAndQualityParcelable(VideoUrlAndQuality videoUrlAndQuality) {
        super(videoUrlAndQuality.getQuality(),videoUrlAndQuality.getVideoUrl());
    }

    protected VideoUrlAndQualityParcelable(Parcel in) {
        setVideoUrl(in.readString());
        setQuality(in.readString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getVideoUrl());
        dest.writeString(getQuality());
    }

    public static final Parcelable.Creator<VideoUrlAndQualityParcelable> CREATOR = new Parcelable.Creator<VideoUrlAndQualityParcelable>() {
        @Override
        public VideoUrlAndQualityParcelable createFromParcel(Parcel in) {
            return new VideoUrlAndQualityParcelable(in);
        }

        @Override
        public VideoUrlAndQualityParcelable[] newArray(int size) {
            return new VideoUrlAndQualityParcelable[size];
        }
    };
}
