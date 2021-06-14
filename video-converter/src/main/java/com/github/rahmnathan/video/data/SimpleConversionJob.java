package com.github.rahmnathan.video.data;

import com.github.rahmnathan.video.codec.AudioCodec;
import com.github.rahmnathan.video.codec.ContainerFormat;
import com.github.rahmnathan.video.codec.VideoCodec;
import lombok.Builder;
import lombok.Getter;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;

import java.io.File;
import java.io.OutputStream;
import java.util.Set;

@Getter
@Builder
public class SimpleConversionJob {
    private final ContainerFormat containerFormat;
    private final OutputStream outputStream;
    private final AudioCodec audioCodec;
    private final VideoCodec videoCodec;
    private final FFprobe ffprobe;
    private final File outputFile;
    private final File inputFile;
    private final FFmpeg ffmpeg;

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