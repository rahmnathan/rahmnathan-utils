package com.github.rahmnathan.video.converter.data;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;

import java.io.File;

@Getter
@Builder
public class SimpleConversionJob {
    private final ContainerFormat containerFormat;
    private final AudioCodec audioCodec;
    private final VideoCodec videoCodec;
    @NonNull private final FFprobe ffprobe;
    @NonNull private final File outputFile;
    @NonNull private final File inputFile;
    @NonNull private final FFmpeg ffmpeg;

    public boolean hasVideoCodec() {
        return videoCodec != null;
    }

    public boolean hasAudioCodec() {
        return audioCodec != null;
    }

    public boolean hasContainerFormat() {
        return containerFormat != null;
    }
}