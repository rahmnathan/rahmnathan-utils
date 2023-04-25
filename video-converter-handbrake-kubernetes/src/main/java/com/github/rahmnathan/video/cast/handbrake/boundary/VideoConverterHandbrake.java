package com.github.rahmnathan.video.cast.handbrake.boundary;

import com.github.rahmnathan.video.cast.handbrake.handbrake.HandbrakeServiceKubernetes;
import com.github.rahmnathan.video.converter.boundary.VideoConverter;
import com.github.rahmnathan.video.converter.data.SimpleConversionJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.File;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class VideoConverterHandbrake implements VideoConverter {
    private final HandbrakeServiceKubernetes handbrakeService = new HandbrakeServiceKubernetes();
    private final SimpleConversionJob simpleConversionJob;

    @Override
    public String get() {
        File inputFile = simpleConversionJob.getInputFile();
        String resultPath = simpleConversionJob.getOutputFile().getAbsolutePath();

        try {
            handbrakeService.convertMedia(simpleConversionJob);
        } catch (IOException e) {
            log.error("Failure converting video", e);
            resultPath = inputFile.getAbsolutePath();
        }

        return resultPath;
    }
}