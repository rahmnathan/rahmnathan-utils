package com.github.rahmnathan.video.converter.data;

public enum ContainerFormat {
    MP4("mp4"),
    MKV("matroska");

    private final String ffmpegName;

    ContainerFormat(String ffmpegName) {
        this.ffmpegName = ffmpegName;
    }

    public String getFfmpegName() {
        return ffmpegName;
    }
}
