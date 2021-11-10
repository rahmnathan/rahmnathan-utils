package com.github.rahmnathan.video.cast.handbrake.handbrake;

import com.github.rahmnathan.video.converter.data.SimpleConversionJob;
import com.github.rahmnathan.video.converter.exception.VideoConverterException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class HandbrakeService {

    public void convertMedia(SimpleConversionJob conversionJob) throws VideoConverterException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("HandBrakeCLI",
                "-Z", "Chromecast 2160p60 4K HEVC Surround",
                "-i", conversionJob.getInputFile().getAbsolutePath(),
                "-o", conversionJob.getOutputFile().getAbsolutePath());

        try {
            Process process = builder.start();
            CompletableFuture.runAsync(withMdc(new StreamConsumer(process.getInputStream(), log::info)));
            if(process.waitFor() == 0){
                log.info("Video conversion successful. Removing input file.");
                conversionJob.getInputFile().delete();
            } else {
                withMdc(new StreamConsumer(process.getErrorStream(), log::error)).run();
                throw new VideoConverterException("Failure converting video: " + conversionJob);
            }
        } catch (IOException | InterruptedException e){
            log.error("Failure converting media", e);
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
        private final Consumer<String> consumer;
        private final InputStream inputStream;

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
