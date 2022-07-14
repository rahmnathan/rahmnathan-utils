package com.github.rahmnathan.video.cast.handbrake.handbrake;

import com.github.rahmnathan.video.converter.data.HandbrakePreset;
import com.github.rahmnathan.video.converter.data.SimpleConversionJob;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.junit.jupiter.api.Test;

import java.io.File;

public class HandbrakeServiceKubernetesIT {

    @Test
    public void convertVideoTest() throws Exception {
        HandbrakeServiceKubernetes handbrakeServiceKubernetes = new HandbrakeServiceKubernetes();

        SimpleConversionJob conversionJob = SimpleConversionJob.builder()
                .inputFile(File.createTempFile("blah", "blah"))
                .outputFile(new File("/tmp/another-test.mp4"))
                .ffprobe(new FFprobe("/usr/bin/ffprobe"))
                .ffmpeg(new FFmpeg("/usr/bin/ffmpeg"))
                .handbrakePreset(HandbrakePreset.CHROMECAST_1080p_60fps.getValue())
                .build();

        handbrakeServiceKubernetes.convertMedia(conversionJob);
    }
}