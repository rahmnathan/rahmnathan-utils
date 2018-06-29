package com.github.rahmnathan.video.cast.handbrake.control;

import com.github.rahmnathan.video.cast.handbrake.converter.VideoConverter;
import com.github.rahmnathan.video.cast.handbrake.data.SimpleConversionJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class VideoController {
    private final VideoConverter videoConverter = new VideoConverter();
    private final Logger logger = LoggerFactory.getLogger(VideoController.class.getName());

    public String convertVideo(SimpleConversionJob simpleConversionJob, Set<String> activeConversions) {
        File inputFile = simpleConversionJob.getInputFile();
        MDC.put("Filename", inputFile.getName());
        String resultPath = simpleConversionJob.getOutputFile().getAbsolutePath();

        boolean correctFormat = isCorrectFormat(simpleConversionJob);
        logger.info("Correct format? - {}", correctFormat);

        if (!correctFormat) {
            activeConversions.add(resultPath);
            videoConverter.convertMedia(simpleConversionJob);
            activeConversions.remove(resultPath);
        } else {
            resultPath = inputFile.getAbsolutePath();
        }

        MDC.clear();
        return resultPath;
    }

    private boolean isCorrectFormat(SimpleConversionJob simpleConversionJob) {
        if (simpleConversionJob.getFfprobe() == null) return true;

        boolean correctVideoCodec = false;
        boolean correctAudioCodec = false;
        boolean correctFormat = false;

        try {
            FFmpegProbeResult probeResult = simpleConversionJob.getFfprobe()
                    .probe(simpleConversionJob.getInputFile().getAbsolutePath());

                String videoFormatName = probeResult.getFormat().format_name;
                logger.info("Container format - {}", videoFormatName);

                Set<String> containerFormats = new HashSet<>();
                containerFormats.add("mp4");
                containerFormats.add("matroska");

                for(String containerFormat : containerFormats) {
                    if (videoFormatName.toLowerCase().contains(containerFormat)) {
                        correctFormat = true;
                    }
                }

            for (FFmpegStream stream : probeResult.getStreams()) {
                String codecName = stream.codec_name;
                logger.info("Stream codec - {}", codecName);

                if (codecName.toLowerCase().contains("aac"))
                    correctAudioCodec = true;
                else if (codecName.toLowerCase().contains("h264"))
                    correctVideoCodec = true;
            }
        } catch (IOException e) {
            logger.error("Failed to determine video format", e);
        }

        return correctVideoCodec && correctAudioCodec && correctFormat;
    }
}