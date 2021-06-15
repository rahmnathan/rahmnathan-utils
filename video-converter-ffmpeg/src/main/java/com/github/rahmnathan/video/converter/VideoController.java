package com.github.rahmnathan.video.converter;

import com.github.rahmnathan.video.converter.data.SimpleConversionJob;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;

public class VideoController {
    private final Logger logger = LoggerFactory.getLogger(VideoController.class.getName());

    public void convertMedia(SimpleConversionJob conversionJob) {
        try {
            FFmpegJob ffmpegJob = VideoControllerUtils.buildFFmpegJob(conversionJob);
            ffmpegJob.run();
            FFmpegJob.State result = VideoControllerUtils.waitForResult(ffmpegJob);

            String existingFilePath = conversionJob.getInputFile().getAbsolutePath();
            if(result == FFmpegJob.State.FAILED){
                logger.info("Encoding failed: {}", existingFilePath);
            } else if(result == FFmpegJob.State.FINISHED){
                logger.info("Encoding finished: {}", existingFilePath);
                Files.delete(conversionJob.getInputFile().toPath());
            }
        } catch (IOException e) {
            logger.error("Failed to convert video", e);
        }
    }
}
