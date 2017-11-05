package com.github.rahmnathan.video.converter;

import com.github.rahmnathan.video.data.SimpleConversionJob;
import net.bramp.ffmpeg.job.FFmpegJob;
import org.apache.commons.lang3.Validate;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VideoConverterUtilsTest {

    @Test
    public void waitForResultTest(){
        FFmpegJob job = mock(FFmpegJob.class);
        when(job.getState()).thenReturn(FFmpegJob.State.FINISHED);

        Assert.assertEquals(FFmpegJob.State.FINISHED, VideoConverterUtils.waitForResult(job));
    }

    @Test
    public void createBuilderTest(){
        SimpleConversionJob conversionJob = SimpleConversionJob.Builder.newInstance()
                .setInputFile(new File(""))
                .setOutputFile(new File(""))
                .build();

        Validate.notNull(VideoConverterUtils.createBuilder(conversionJob));
    }
}
