package com.github.rahmnathan.video.cast.handbrake.converter;

import com.github.rahmnathan.video.cast.handbrake.data.SimpleConversionJob;
import com.github.rahmnathan.video.cast.handbrake.exception.VideoConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class VideoConverter {
    private final Logger logger = LoggerFactory.getLogger(VideoConverter.class.getName());

    public void convertMedia(SimpleConversionJob conversionJob) throws VideoConversionException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("HandBrakeCLI", "-i", conversionJob.getInputFile().getAbsolutePath(),
                "-o", conversionJob.getOutputFile().getAbsolutePath());

        try {
            Process process = builder.start();
            CompletableFuture.runAsync(new StreamConsumer(process.getInputStream(), logger::info));
            if(process.waitFor() == 0){
                logger.info("Video conversion successful. Removing input file.");
                conversionJob.getInputFile().delete();
            } else {
                throw new VideoConversionException("Failure converting video: " + conversionJob.toString());
            }
        } catch (IOException | InterruptedException e){
            logger.error("Failure converting media", e);
        }
    }

    private static class StreamConsumer implements Runnable {
        private Consumer<String> consumer;
        private InputStream inputStream;

        private StreamConsumer(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
        }
    }
}
