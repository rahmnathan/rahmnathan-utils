package com.github.rahmnathan.video.converter.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HandbrakePreset {
    CHROMECAST_4K_60fps("Chromecast 2160p60 4K HEVC Surround"),
    CHROMECAST_1080p_60fps("Chromecast 1080p60 Surround"),
    CHROMECAST_1080p_30fps("Chromecast 1080p30 Surround");

    private final String value;
}
