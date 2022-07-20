package com.github.rahmnathan.video.cast.handbrake.boundary;

import com.github.rahmnathan.video.cast.handbrake.handbrake.HandbrakeServiceKubernetes;
import com.github.rahmnathan.video.converter.boundary.VideoConverter;
import com.github.rahmnathan.video.converter.data.SimpleConversionJob;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class VideoConverterHandbrake implements VideoConverter {
    private final HandbrakeServiceKubernetes handbrakeService = new HandbrakeServiceKubernetes();
    private final SimpleConversionJob simpleConversionJob;
    private final Set<String> activeConversions;

    @Override
    public String get() {
        File inputFile = simpleConversionJob.getInputFile();
        MDC.put("Filename", inputFile.getName());
        String resultPath = simpleConversionJob.getOutputFile().getAbsolutePath();

        activeConversions.add(resultPath);

        try {
            handbrakeService.convertMedia(simpleConversionJob);
            activeConversions.remove(resultPath);
        } catch (IOException e) {
            log.error("Failure converting video", e);
            activeConversions.remove(resultPath);
            resultPath = inputFile.getAbsolutePath();
        }

        MDC.clear();

        return resultPath;
    }
}