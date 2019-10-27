package com.github.rahmnathan.directory.monitor;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.*;
import java.util.*;

public class DirectoryMonitor {
    private static final Logger logger = LoggerFactory.getLogger(DirectoryMonitor.class.getName());

    public DirectoryMonitor(Set<String> mediaPaths, Set<DirectoryMonitorObserver> observers) {
        logger.info("Starting Recursive Watcher Service with {} observers.", observers.size());

        FileAlterationMonitor monitor = new FileAlterationMonitor();
        FileAlterationListener listener = new DirectoryMonitorListener(monitor, observers);

        mediaPaths.stream().map(Paths::get).forEach(p -> {
            logger.info("registering {} in watcher service", p);
            FileAlterationObserver observer = new FileAlterationObserver(p.toFile());
            observer.addListener(listener);
            monitor.addObserver(observer);
        });

        try {
            monitor.start();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}