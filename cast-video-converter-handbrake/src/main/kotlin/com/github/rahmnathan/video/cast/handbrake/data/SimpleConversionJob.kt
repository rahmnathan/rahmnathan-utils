package com.github.rahmnathan.video.cast.handbrake.data

import net.bramp.ffmpeg.FFprobe
import java.io.File

data class SimpleConversionJob(val ffprobe: FFprobe, val outputFile: File, val inputFile: File)
