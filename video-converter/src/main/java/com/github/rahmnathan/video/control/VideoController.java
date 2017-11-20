package com.github.rahmnathan.video.control;

import com.github.rahmnathan.video.data.SimpleConversionJob;
import com.github.rahmnathan.video.converter.VideoConverter;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VideoController implements Runnable {
    private final SimpleConversionJob simpleConversionJob;
    private volatile Set<String> activeConversions;
    private final VideoConverter videoConverter = new VideoConverter();
    private final Logger logger = Logger.getLogger(VideoController.class.getName());

    public VideoController(SimpleConversionJob simpleConversionJob, Set<String> activeConversions) {
        this.simpleConversionJob = simpleConversionJob;
        this.activeConversions = activeConversions;
    }

    @Override
    public void run() {
        String outputFilePath = simpleConversionJob.getOutputFile().getAbsolutePath();
        activeConversions.add(outputFilePath);

        boolean correctFormat = isCorrectFormat(simpleConversionJob);
        logger.info("Correct format? - " + correctFormat);
        if (!correctFormat) {
            videoConverter.convertMedia(simpleConversionJob);
        }

        activeConversions.remove(outputFilePath);
    }

    private boolean isCorrectFormat(SimpleConversionJob simpleConversionJob) {
        if (simpleConversionJob.getFfprobe() == null) return true;

        boolean correctVideoCodec = simpleConversionJob.getVideoCodec() == null;
        boolean correctAudioCodec = simpleConversionJob.getAudioCodec() == null;

        try {
            FFmpegProbeResult probeResult = simpleConversionJob.getFfprobe()
                    .probe(simpleConversionJob.getInputFile().getAbsolutePath());

            for (FFmpegStream stream : probeResult.getStreams()) {
                String codecName = stream.codec_name;
                logger.info("Stream codec - " + codecName);
                if (codecName.equalsIgnoreCase(simpleConversionJob.getAudioCodec().name()))
                    correctAudioCodec = true;
                else if (codecName.equalsIgnoreCase(simpleConversionJob.getVideoCodec().name()))
                    correctVideoCodec = true;
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to determine video format", e);
        }

        return correctVideoCodec && correctAudioCodec;
    }
}