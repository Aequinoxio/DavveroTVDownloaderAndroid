package com.aequinoxio.davverotvdownloader.DownloadLogic;

import java.lang.ref.WeakReference;

public class VideoUrlAndQuality {
    String videoUrl;
    String quality;

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public VideoUrlAndQuality(String quality, String videoUrl) {
        this.videoUrl = videoUrl;
        this.quality = quality;
    }

    public VideoUrlAndQuality() {
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getQuality() {
        return quality;
    }

    @Override
    public String toString() {
        return quality ;
    }
}

