package com.github.rahmnathan.video.data;

import com.github.rahmnathan.video.codec.AudioCodec;
import com.github.rahmnathan.video.codec.ContainerFormat;
import com.github.rahmnathan.video.codec.VideoCodec;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class SimpleConversionJob {
    private Set<ContainerFormat> containerFormat = new HashSet<>();
    private AudioCodec audioCodec;
    private VideoCodec videoCodec;
    private FFprobe ffprobe;
    private File outputFile;
    private File inputFile;
    private FFmpeg ffmpeg;

    public Set<ContainerFormat> getContainerFormats() {
        return containerFormat;
    }

    public AudioCodec getAudioCodec() {
        return audioCodec;
    }

    public VideoCodec getVideoCodec() {
        return videoCodec;
    }

    public boolean hasContainerFormat(){
        return containerFormat != null && containerFormat.size() != 0;
    }

    public boolean hasAudioCodec(){
        return audioCodec != null;
    }

    public boolean hasVideoCodec(){
        return videoCodec != null;
    }

    public FFmpeg getFfmpeg() {
        return ffmpeg;
    }

    public FFprobe getFfprobe() {
        return ffprobe;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public File getInputFile() {
        return inputFile;
    }

    public static class Builder {
        private SimpleConversionJob conversionJob = new SimpleConversionJob();

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder setContainerFormat(Set<ContainerFormat> containerFormat) {
            conversionJob.containerFormat = containerFormat;
            return this;
        }

        public Builder setVideoCodec(VideoCodec videoCodec) {
            conversionJob.videoCodec = videoCodec;
            return this;
        }

        public Builder setAudioCodec(AudioCodec audioCodec) {
            conversionJob.audioCodec = audioCodec;
            return this;
        }

        public Builder setFfmpeg(FFmpeg ffmpeg) {
            conversionJob.ffmpeg = ffmpeg;
            return this;
        }

        public Builder setFfprobe(FFprobe ffprobe) {
            conversionJob.ffprobe = ffprobe;
            return this;
        }

        public Builder setOutputFile(File outputFile) {
            conversionJob.outputFile = outputFile;
            return this;
        }

        public Builder setInputFile(File inputFile) {
            conversionJob.inputFile = inputFile;
            return this;
        }

        public SimpleConversionJob build() {
            SimpleConversionJob result = conversionJob;
            conversionJob = new SimpleConversionJob();

            return result;
        }
    }
}