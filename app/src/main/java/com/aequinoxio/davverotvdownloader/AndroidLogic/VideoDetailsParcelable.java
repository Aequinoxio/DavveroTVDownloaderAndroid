package com.aequinoxio.davverotvdownloader.AndroidLogic;

import android.os.Parcel;
import android.os.Parcelable;

import com.aequinoxio.davverotvdownloader.DownloadLogic.VideoDetails;
import com.aequinoxio.davverotvdownloader.DownloadLogic.VideoUrlAndQuality;

import java.util.List;

public class VideoDetailsParcelable extends VideoDetails implements Parcelable {

    protected VideoDetailsParcelable(String title, String mainUrl){
        super (title,mainUrl);
    }

    public VideoDetailsParcelable(VideoDetails videoDetails){
        super(videoDetails.getTitle(),videoDetails.getMainUrl());

        List<VideoUrlAndQuality> videoUrlAndQualityList = videoDetails.getVideoUrlQualityList();

        for (VideoUrlAndQuality videoUrlAndQuality:videoUrlAndQualityList){
//            VideoUrlAndQuality temp = new VideoUrlAndQualityParcelable(videoUrlAndQuality);
//            super.addVideoUrlQuality(temp.getQuality(), temp.getVideoUrl());
            super.addVideoUrlQuality(new VideoUrlAndQualityParcelable(videoUrlAndQuality));
        }
    }

    protected VideoDetailsParcelable(Parcel in) {
        super(in.readString(),in.readString());
        int arrayObjects=in.readInt();
        for (int i=0;i<arrayObjects;i++){
            addVideoUrlQuality(in.readString(),in.readString());
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getTitle());
        dest.writeString(getMainUrl());
        List<VideoUrlAndQuality> tempList = getVideoUrlQualityList();
        dest.writeInt(tempList.size());
        for (VideoUrlAndQuality itemList:tempList){
            dest.writeString(itemList.getQuality());
            dest.writeString(itemList.getVideoUrl());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VideoDetailsParcelable> CREATOR = new Creator<VideoDetailsParcelable>() {
        @Override
        public VideoDetailsParcelable createFromParcel(Parcel in) {
            return new VideoDetailsParcelable(in);
        }

        @Override
        public VideoDetailsParcelable[] newArray(int size) {
            return new VideoDetailsParcelable[size];
        }
    };
}
