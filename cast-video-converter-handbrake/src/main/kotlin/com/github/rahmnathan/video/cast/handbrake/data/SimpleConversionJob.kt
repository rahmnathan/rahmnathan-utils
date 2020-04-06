package com.github.rahmnathan.video.cast.handbrake.data

import java.io.File

data class SimpleConversionJob(val outputFile: File, val inputFile: File, val correlationId: String?)
