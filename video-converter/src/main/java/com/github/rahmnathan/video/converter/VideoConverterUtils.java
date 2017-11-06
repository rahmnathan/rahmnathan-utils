package com.github.rahmnathan.video.converter;

import com.github.rahmnathan.video.data.SimpleConversionJob;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.util.logging.Logger;

class VideoConverterUtils {
    private static final Logger logger = Logger.getLogger(VideoConverterUtils.class.getName());

    static void validateParams(SimpleConversionJob simpleConversionJob) {
        Validate.notNull(simpleConversionJob, "Conversion data is null");
        Validate.notNull(simpleConversionJob.getFfmpeg(), "FFmpeg is null");
        Validate.notNull(simpleConversionJob.getFfprobe(), "FFprobe is null");
        Validate.notNull(simpleConversionJob.getInputFile(), "Input file is null");
        Validate.notNull(simpleConversionJob.getOutputFile(), "Output file is null");
    }

    static FFmpegJob buildFFmpegJob(SimpleConversionJob conversionJob) throws IOException {
        String existingFilePath = conversionJob.getInputFile().getAbsolutePath();
        FFmpegProbeResult ffmpegProbeResult = conversionJob.getFfprobe().probe(existingFilePath);

        FFmpegBuilder fFmpegBuilder = createBuilder(conversionJob);
        FFmpegExecutor ffmpegExecutor = new FFmpegExecutor(conversionJob.getFfmpeg(), conversionJob.getFfprobe());

        return ffmpegExecutor.createJob(fFmpegBuilder, progress -> {
            double duration = ffmpegProbeResult.getFormat().duration;
            int percentage = Double.valueOf((progress.out_time_ms / duration) / 10000).intValue();
            logger.info(existingFilePath + " Encoding progress -> " + percentage + "%");
        });
    }

    static FFmpegBuilder createBuilder(SimpleConversionJob conversionJob) {
        FFmpegOutputBuilder outputBuilder = new FFmpegBuilder()
                .setInput(conversionJob.getInputFile().getAbsolutePath())
                .overrideOutputFiles(true)
                .addOutput(conversionJob.getOutputFile().getAbsolutePath());

        if (conversionJob.getAudioCodec() != null)
            outputBuilder.setAudioCodec(conversionJob.getAudioCodec().getEncoder());
        if (conversionJob.getVideoCodec() != null)
            outputBuilder.setVideoCodec(conversionJob.getVideoCodec().getEncoder());

        return outputBuilder.done();
    }

    static FFmpegJob.State waitForResult(FFmpegJob job){
        FFmpegJob.State result = FFmpegJob.State.FAILED;

        while (true) {
            if(job.getState() == FFmpegJob.State.FAILED){
                result = FFmpegJob.State.FAILED;
                break;
            } else if (job.getState() == FFmpegJob.State.FINISHED){
                result = FFmpegJob.State.FINISHED;
                break;
            }

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                logger.severe(e.toString());
                break;
            }
        }

        return result;
    }
}
