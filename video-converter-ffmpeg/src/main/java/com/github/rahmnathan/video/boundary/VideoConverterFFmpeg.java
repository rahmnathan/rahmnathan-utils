package com.github.rahmnathan.video.boundary;

import com.github.rahmnathan.video.converter.VideoController;
import com.github.rahmnathan.video.converter.boundary.VideoConverter;
import com.github.rahmnathan.video.converter.data.SimpleConversionJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.Set;

public class VideoConverterFFmpeg implements VideoConverter {
    private final SimpleConversionJob simpleConversionJob;
    private final Set<String> activeConversions;
    private final VideoController videoController = new VideoController();
    private final Logger logger = LoggerFactory.getLogger(VideoConverterFFmpeg.class.getName());

    public VideoConverterFFmpeg(SimpleConversionJob simpleConversionJob, Set<String> activeConversions) {
        this.simpleConversionJob = simpleConversionJob;
        this.activeConversions = activeConversions;
    }

    @Override
    public String get() {
        MDC.put("Filename", simpleConversionJob.getInputFile().getName());
        String outputFilePath = simpleConversionJob.getOutputFile().getAbsolutePath();
        activeConversions.add(outputFilePath);

        boolean correctFormat = !simpleConversionJob.isForceConvert() && isCorrectFormat(simpleConversionJob);
        logger.info("Correct format? - {}", correctFormat);
        if (!correctFormat) {
            videoController.convertMedia(simpleConversionJob);
        }

        activeConversions.remove(outputFilePath);
        MDC.clear();

        return outputFilePath;
    }

    private boolean isCorrectFormat(SimpleConversionJob simpleConversionJob) {
        boolean correctVideoCodec = !simpleConversionJob.hasVideoCodec();
        boolean correctAudioCodec = !simpleConversionJob.hasAudioCodec();
        boolean correctFormat = !simpleConversionJob.hasContainerFormat();
        boolean correctVideoHeight = simpleConversionJob.getVideoHeight() == null;
        boolean correctVideoWidth = simpleConversionJob.getVideoWidth() == null;
        boolean correctVideoBitrate = simpleConversionJob.getVideoBitrate() == null;
        boolean correctAudioBitrate = simpleConversionJob.getAudioBitrate() == null;
        boolean correctFrameRate = simpleConversionJob.getFrameRate() == null;

        try {
            FFmpegProbeResult probeResult = simpleConversionJob.getFfprobe()
                    .probe(simpleConversionJob.getInputFile().getAbsolutePath());

            if(!correctFormat){
                String videoFormatName = probeResult.getFormat().format_name;
                logger.info("Container format - {}", videoFormatName);
                if(videoFormatName.toLowerCase().contains(simpleConversionJob.getContainerFormat().getFfmpegName())){
                    correctFormat = true;
                }
            }

            for (FFmpegStream stream : probeResult.getStreams()) {
                String codecName = stream.codec_name;
                logger.info("Stream codec - {}", codecName);

                if (codecName.toLowerCase().contains(simpleConversionJob.getAudioCodec().name().toLowerCase())){
                    correctAudioCodec = true;

                    if(stream.bit_rate <= simpleConversionJob.getAudioBitrate()){
                        correctAudioBitrate = true;
                    }
                }
                else if (codecName.toLowerCase().contains(simpleConversionJob.getVideoCodec().name().toLowerCase())) {
                    correctVideoCodec = true;

                    if(stream.bit_rate <= simpleConversionJob.getVideoBitrate()){
                        correctVideoBitrate = true;
                    }
                    if(stream.avg_frame_rate.doubleValue() <= simpleConversionJob.getFrameRate()){
                        correctFrameRate = true;
                    }
                    if(stream.width <= simpleConversionJob.getVideoWidth()) {
                        correctVideoWidth = true;
                    }
                    if(stream.height <= simpleConversionJob.getVideoHeight()) {
                        correctVideoHeight = true;
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to determine video format", e);
        }

        return correctVideoCodec && correctAudioCodec && correctFormat && correctAudioBitrate && correctVideoBitrate &&
                correctVideoHeight && correctVideoWidth && correctFrameRate;
    }
}