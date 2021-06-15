package com.github.rahmnathan.video.converter.boundary;

import io.micrometer.core.instrument.Metrics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public interface VideoConverter extends Supplier<String> {
    AtomicInteger ACTIVE_CONVERSION_GAUGE = Metrics.gauge("handbrake.conversions.active", new AtomicInteger(0));

}
