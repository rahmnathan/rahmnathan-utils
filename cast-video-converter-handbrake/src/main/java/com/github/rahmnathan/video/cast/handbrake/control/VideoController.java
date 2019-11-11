package com.github.rahmnathan.video.cast.handbrake.control;

import com.github.rahmnathan.video.cast.handbrake.converter.VideoConverter;
import com.github.rahmnathan.video.cast.handbrake.data.SimpleConversionJob;
import com.github.rahmnathan.video.cast.handbrake.exception.VideoConversionException;
import io.micrometer.core.instrument.Metrics;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class VideoController implements Supplier<String> {
    private final Logger logger = LoggerFactory.getLogger(VideoController.class);
    private static final AtomicInteger ACTIVE_CONVERSION_GAUGE = Metrics.gauge("handbrake.conversions.active", new AtomicInteger(0));
    private static final Set<String> CONTAINER_FORMATS = Set.of("mp4", "matroska");
    private final VideoConverter videoConverter = new VideoConverter();
    private final SimpleConversionJob simpleConversionJob;
    private final Set<String> activeConversions;
    private FFprobe fFprobe;

    public VideoController(SimpleConversionJob simpleConversionJob, Set<String> activeConversions) {
        this.simpleConversionJob = simpleConversionJob;
        this.activeConversions = activeConversions;

        try {
            this.fFprobe = new FFprobe("ffprobe");
        } catch (IOException e) {
            logger.warn("Failure loading FFprobe. Won't be able to determine video formats.");
        }
    }

    @Override
    public String get() {
        File inputFile = simpleConversionJob.getInputFile();
        MDC.put("Filename", inputFile.getName());
        String resultPath = simpleConversionJob.getOutputFile().getAbsolutePath();

        boolean correctFormat = isCorrectFormat(simpleConversionJob);
        logger.info("Correct format? - {}", correctFormat);

        if (!correctFormat) {
            activeConversions.add(resultPath);
            ACTIVE_CONVERSION_GAUGE.getAndIncrement();
            try {
                videoConverter.convertMedia(simpleConversionJob);
                activeConversions.remove(resultPath);
            } catch (VideoConversionException e){
                logger.error("Failure converting video", e);
                activeConversions.remove(resultPath);
                resultPath = inputFile.getAbsolutePath();
            }
            ACTIVE_CONVERSION_GAUGE.getAndDecrement();
        } else {
            resultPath = inputFile.getAbsolutePath();
        }

        MDC.clear();
        return resultPath;
    }

    private boolean isCorrectFormat(SimpleConversionJob simpleConversionJob) {
        if(fFprobe == null) {
            logger.warn("FFprobe not available. Skipping conversion.");
            return true;
        }

        try {
            FFmpegProbeResult probeResult = fFprobe
                    .probe(simpleConversionJob.getInputFile().getAbsolutePath());

            String videoFormatName = probeResult.getFormat().format_name.toLowerCase();
            logger.info("Container format - {}", videoFormatName);

            Set<String> codecNames = probeResult.getStreams().stream()
                    .map(stream -> stream.codec_name)
                    .peek(codecName -> logger.info("Stream codec - {}", codecName))
                    .map(String::toLowerCase)
                    .collect(Collectors.toUnmodifiableSet());

            boolean correctVideoCodec = codecNames.stream().anyMatch(codecName -> codecName.contains("h264"));
            boolean correctAudioCodec = codecNames.stream().anyMatch(codecName -> codecName.contains("aac"));
            boolean correctContainerFormat = CONTAINER_FORMATS.stream().anyMatch(videoFormatName::contains);

            return correctVideoCodec && correctAudioCodec && correctContainerFormat;
        } catch (IOException e) {
            logger.error("Failed to determine video format. Skipping conversion.", e);
            return true;
        }
    }
}