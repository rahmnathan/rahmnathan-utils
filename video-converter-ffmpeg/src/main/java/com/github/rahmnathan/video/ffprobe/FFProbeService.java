package com.github.rahmnathan.video.ffprobe;

import com.github.rahmnathan.video.converter.data.SimpleConversionJob;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;

import java.io.IOException;

@Slf4j
public class FFProbeService {

    public boolean isCorrectFormat(SimpleConversionJob simpleConversionJob) {
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
                log.info("Container format - {}", videoFormatName);
                if(videoFormatName.toLowerCase().contains(simpleConversionJob.getContainerFormat().getFfmpegName())){
                    correctFormat = true;
                }
            }

            for (FFmpegStream stream : probeResult.getStreams()) {
                String codecName = stream.codec_name;
                log.info("Stream codec - {}", codecName);

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
            log.error("Failed to determine video format", e);
        }

        return correctVideoCodec && correctAudioCodec && correctFormat && correctAudioBitrate && correctVideoBitrate &&
                correctVideoHeight && correctVideoWidth && correctFrameRate;
    }
}
