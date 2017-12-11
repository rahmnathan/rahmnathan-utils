package com.github.rahmnathan.video.converter;

import com.github.rahmnathan.video.data.SimpleConversionJob;
import net.bramp.ffmpeg.job.FFmpegJob;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VideoConverter {
    private final Logger logger = Logger.getLogger(VideoConverter.class.getName());

    public void convertMedia(SimpleConversionJob conversionJob) {
        VideoConverterUtils.validateParams(conversionJob);

        try {
            FFmpegJob ffmpegJob = VideoConverterUtils.buildFFmpegJob(conversionJob);
            ffmpegJob.run();
            FFmpegJob.State result = VideoConverterUtils.waitForResult(ffmpegJob);

            String existingFilePath = conversionJob.getInputFile().getAbsolutePath();
            if(result == FFmpegJob.State.FAILED){
                logger.info("Encoding failed: " + existingFilePath);
            } else if(result == FFmpegJob.State.FINISHED){
                logger.info("Encoding finished: " + existingFilePath);
                conversionJob.getInputFile().delete();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to convert video", e);
        }
    }
}
