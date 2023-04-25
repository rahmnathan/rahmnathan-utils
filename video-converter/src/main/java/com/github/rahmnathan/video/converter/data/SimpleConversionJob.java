package com.github.rahmnathan.video.converter.data;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.io.File;

@Getter
@Builder
public class SimpleConversionJob {
    private final String handbrakePreset;
    @NonNull private final File outputFile;
    @NonNull private final File inputFile;
}