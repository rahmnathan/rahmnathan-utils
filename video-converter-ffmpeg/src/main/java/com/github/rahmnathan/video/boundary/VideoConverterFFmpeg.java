package com.github.rahmnathan.video.boundary;

import com.github.rahmnathan.video.ffprobe.FFProbeService;
import com.github.rahmnathan.video.ffmpeg.FFmpegService;
import com.github.rahmnathan.video.converter.boundary.VideoConverter;
import com.github.rahmnathan.video.converter.data.SimpleConversionJob;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class VideoConverterFFmpeg implements VideoConverter {
    private final AtomicInteger activeConversionsGauge = Metrics.gauge("ffmpeg.conversions.active", new AtomicInteger(0));
    private final FFProbeService ffProbeService = new FFProbeService();
    private final FFmpegService ffmpegService = new FFmpegService();
    private final SimpleConversionJob simpleConversionJob;
    private final Set<String> activeConversions;

    public VideoConverterFFmpeg(SimpleConversionJob simpleConversionJob, Set<String> activeConversions) {
        this.simpleConversionJob = simpleConversionJob;
        this.activeConversions = activeConversions;
    }

    @Override
    public String get() {
        MDC.put("Filename", simpleConversionJob.getInputFile().getName());
        String outputFilePath = simpleConversionJob.getOutputFile().getAbsolutePath();
        activeConversions.add(outputFilePath);

        boolean correctFormat = !simpleConversionJob.isForceConvert() && ffProbeService.isCorrectFormat(simpleConversionJob);
        log.info("Correct format? - {}", correctFormat);
        if (!correctFormat) {
            activeConversionsGauge.getAndIncrement();
            ffmpegService.convertMedia(simpleConversionJob);
            activeConversionsGauge.getAndDecrement();
        }

        activeConversions.remove(outputFilePath);
        MDC.clear();

        return outputFilePath;
    }
}