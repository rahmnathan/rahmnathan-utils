package com.github.rahmnathan.video.cast.handbrake.converter;

import com.github.rahmnathan.video.cast.handbrake.data.SimpleConversionJob;
import com.github.rahmnathan.video.cast.handbrake.exception.VideoConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoConverter {
    private final Logger logger = LoggerFactory.getLogger(VideoConverter.class.getName());

    public void convertMedia(SimpleConversionJob conversionJob) throws VideoConversionException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("HandBrakeCLI", "-i", conversionJob.getInputFile().getAbsolutePath(),
                "-o", conversionJob.getOutputFile().getAbsolutePath());

        try {
            Process process = builder.start();
            CompletableFuture.runAsync(withMdc(new StreamConsumer(process.getInputStream(), logger::info)));
            if(process.waitFor() == 0){
                logger.info("Video conversion successful. Removing input file.");
                conversionJob.getInputFile().delete();
            } else {
                withMdc(new StreamConsumer(process.getErrorStream(), logger::error)).run();
                throw new VideoConversionException("Failure converting video: " + conversionJob.toString());
            }
        } catch (IOException | InterruptedException e){
            logger.error("Failure converting media", e);
        }
    }

    private static Runnable withMdc(Runnable runnable) {
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        return () -> {
            MDC.setContextMap(mdc);
            runnable.run();
        };
    }

    private static class StreamConsumer implements Runnable {
        private final Pattern pattern = Pattern.compile("\\d?\\d(?=.\\d\\d %)");
        private Consumer<String> consumer;
        private InputStream inputStream;

        private StreamConsumer(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            Set<String> set = new HashSet<>();
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .filter(s -> {
                        Matcher matcher = pattern.matcher(s);
                        if (matcher.find()) {
                            return set.add(matcher.group());
                        }

                        return true;
                    })
                    .forEach(consumer);

        }
    }
}
