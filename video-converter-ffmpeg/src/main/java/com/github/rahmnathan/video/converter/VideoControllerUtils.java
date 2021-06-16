package com.github.rahmnathan.video.converter;

import com.github.rahmnathan.video.converter.data.AudioCodec;
import com.github.rahmnathan.video.converter.data.ContainerFormat;
import com.github.rahmnathan.video.converter.data.SimpleConversionJob;
import com.github.rahmnathan.video.converter.data.VideoCodec;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;

import java.io.IOException;

@Slf4j
@UtilityClass
class VideoControllerUtils {

    static FFmpegJob buildFFmpegJob(SimpleConversionJob conversionJob) throws IOException {
        String existingFilePath = conversionJob.getInputFile().getAbsolutePath();
        FFmpegProbeResult ffmpegProbeResult = conversionJob.getFfprobe().probe(existingFilePath);

        FFmpegBuilder fFmpegBuilder = createBuilder(conversionJob);
        FFmpegExecutor ffmpegExecutor = new FFmpegExecutor(conversionJob.getFfmpeg(), conversionJob.getFfprobe());

        return ffmpegExecutor.createJob(fFmpegBuilder, progress -> {
            double duration = ffmpegProbeResult.getFormat().duration;
            double percentComplete = Double.valueOf((progress.out_time_ns / duration) / 10000000);
            if(percentComplete % 1 == 0){
                log.info("{} Encoding progress -> {}%", existingFilePath, percentComplete);
            }
        });
    }

    static FFmpegBuilder createBuilder(SimpleConversionJob conversionJob) {
        FFmpegOutputBuilder outputBuilder = new FFmpegBuilder()
                .setInput(conversionJob.getInputFile().getAbsolutePath())
                .overrideOutputFiles(true)
                .addOutput(conversionJob.getOutputFile().getAbsolutePath());

        ContainerFormat containerFormats = conversionJob.getContainerFormat();
        if(containerFormats != null){
            outputBuilder.setFormat(containerFormats.getFfmpegName());
        }

        AudioCodec audioCodec = conversionJob.getAudioCodec();
        if(audioCodec != null){
            outputBuilder.setAudioCodec(audioCodec.getEncoder());
        }

        VideoCodec videoCodec = conversionJob.getVideoCodec();
        if(videoCodec != null){
            outputBuilder.setVideoCodec(videoCodec.getEncoder());
        }

        Integer videoWidth = conversionJob.getVideoWidth();
        if(videoWidth != null){
            outputBuilder.setVideoWidth(videoWidth);
        }

        Integer videoHeight = conversionJob.getVideoHeight();
        if(videoHeight != null) {
            outputBuilder.setVideoHeight(videoHeight);
        }

        Long audioBitrate = conversionJob.getAudioBitrate();
        if(audioBitrate != null) {
            outputBuilder.setAudioBitRate(audioBitrate);
        }

        Long videoBitrate = conversionJob.getVideoBitrate();
        if(videoBitrate != null) {
            outputBuilder.setVideoBitRate(videoBitrate);
        }

        Double frameRate = conversionJob.getFrameRate();
        if(frameRate != null) {
            outputBuilder.setVideoFrameRate(frameRate);
        }

        return outputBuilder.done();
    }

    static FFmpegJob.State waitForResult(FFmpegJob job){
        FFmpegJob.State result = FFmpegJob.State.FAILED;

        while (true) {
            if(job.getState() == FFmpegJob.State.FAILED){
                break;
            } else if (job.getState() == FFmpegJob.State.FINISHED){
                result = FFmpegJob.State.FINISHED;
                break;
            }

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                log.error("Thread sleep operation interrupted", e);
                break;
            }
        }

        return result;
    }
}
