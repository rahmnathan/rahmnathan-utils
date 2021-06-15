package com.github.rahmnathan.video.converter;

import com.github.rahmnathan.video.converter.data.SimpleConversionJob;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VideoControllerUtilsTest {
    private static SimpleConversionJob conversionJob;

    @BeforeAll
    public static void initialize(){
        conversionJob = SimpleConversionJob.builder()
                .ffmpeg(mock(FFmpeg.class))
                .ffprobe(mock(FFprobe.class))
                .inputFile(new File(""))
                .outputFile(new File(""))
                .build();
    }

    @Test
    public void waitForResultTest(){
        FFmpegJob job = mock(FFmpegJob.class);
        when(job.getState()).thenReturn(FFmpegJob.State.FINISHED);

        assertEquals(FFmpegJob.State.FINISHED, VideoControllerUtils.waitForResult(job));
    }

    @Test
    public void createBuilderTest(){
        Validate.notNull(VideoControllerUtils.createBuilder(conversionJob));
    }
}
