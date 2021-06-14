package com.github.rahmnathan.video.converter;

import com.github.rahmnathan.video.exception.VideoConversionException;
import com.github.rahmnathan.video.data.SimpleConversionJob;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class VideoConverter {
    private final Logger logger = LoggerFactory.getLogger(VideoConverter.class.getName());

    public void convertMedia(SimpleConversionJob conversionJob) {
        VideoConverterUtils.validateParams(conversionJob);

        try {
            FFmpegJob ffmpegJob = VideoConverterUtils.buildFFmpegJob(conversionJob);
            ffmpegJob.run();
            FFmpegJob.State result = VideoConverterUtils.waitForResult(ffmpegJob);

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

    public InputStream stream(SimpleConversionJob conversionJob) throws VideoConversionException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("ffmpeg", "-i", conversionJob.getInputFile().getAbsolutePath(),
                "-f", "matroska", "-vcodec", "h264", "-acodec", "aac", "-ac2", "pipe:1");

        try {
            Process process = builder.start();
            return process.getInputStream();
        } catch (IOException e){
            throw new VideoConversionException("Failure streaming video.", e);
        }
    }
}
