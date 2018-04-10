package com.github.rahmnathan.video.cast.handbrake.data;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;

import java.io.File;

public class SimpleConversionJob {
    private FFprobe ffprobe;
    private File outputFile;
    private File inputFile;
    private FFmpeg ffmpeg;

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

    @Override
    public String toString() {
        return "SimpleConversionJob{" +
                "ffprobe=" + ffprobe +
                ", outputFile=" + outputFile +
                ", inputFile=" + inputFile +
                ", ffmpeg=" + ffmpeg +
                '}';
    }
}