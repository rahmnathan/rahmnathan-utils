package com.github.rahmnathan.video.cast.handbrake.converter;

import com.github.rahmnathan.video.cast.handbrake.data.SimpleConversionJob;
import com.github.rahmnathan.video.cast.handbrake.exception.VideoConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoConverter {
    private final Logger logger = LoggerFactory.getLogger(VideoConverter.class.getName());
    private static final String CORRELATION_ID_HEADER = "x-correlation-id";

    public void convertMedia(SimpleConversionJob conversionJob) throws VideoConversionException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("HandBrakeCLI", "-i", conversionJob.getInputFile().getAbsolutePath(),
                "-o", conversionJob.getOutputFile().getAbsolutePath());

        try {
            Process process = builder.start();
            CompletableFuture.runAsync(new StreamConsumer(process.getInputStream(), MDC.get(CORRELATION_ID_HEADER), logger::info));
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
        private final Pattern pattern = Pattern.compile("\\d?\\d(?=.\\d\\d %)");
        private Consumer<String> consumer;
        private InputStream inputStream;
        private String correlationId;

        private StreamConsumer(InputStream inputStream, String correlationId, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
            this.correlationId = correlationId;
        }

        @Override
        public void run() {
            MDC.put(CORRELATION_ID_HEADER, correlationId);
            Set<String> set = new HashSet<>();
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .filter(s -> {
                        Matcher matcher = pattern.matcher(s);
                        if(matcher.find()){
                            return set.add(matcher.group());
                        }

                        return true;
                    })
                    .forEach(consumer);

            MDC.clear();
        }
    }
}
