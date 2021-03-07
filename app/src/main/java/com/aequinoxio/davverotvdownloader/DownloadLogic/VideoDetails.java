package com.aequinoxio.davverotvdownloader.DownloadLogic;

import java.util.ArrayList;
import java.util.List;

public class VideoDetails {
    private String title;
    private String mainUrl;
    private List<VideoUrlAndQuality> videoUrlAndQualityList;

    public VideoDetails(String title, String mainUrl) {
        this.title = title;
        this.mainUrl = mainUrl;
        videoUrlAndQualityList = new ArrayList<>();
    }

    public VideoDetails() {
    }

    public String getTitle() {
        return title;
    }

    public String getMainUrl() {
        return mainUrl;
    }

    public List<VideoUrlAndQuality> getVideoUrlQualityList() {
        return videoUrlAndQualityList;
    }

    public void addVideoUrlQuality(String quality, String videoUrl){
        videoUrlAndQualityList.add(new VideoUrlAndQuality(quality,videoUrl));
    }

    public void addVideoUrlQuality(VideoUrlAndQuality videoUrlAndQuality){
        videoUrlAndQualityList.add(videoUrlAndQuality);
    }
}

