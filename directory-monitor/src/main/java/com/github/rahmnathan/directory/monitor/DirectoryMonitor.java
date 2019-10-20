package com.github.rahmnathan.directory.monitor;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;

public class DirectoryMonitor {
    private static final Logger logger = LoggerFactory.getLogger(DirectoryMonitor.class.getName());
    private final Set<Path> paths = new HashSet<>();

    public DirectoryMonitor(String[] mediaPaths, Set<DirectoryMonitorObserver> observers) {
        logger.info("Starting Recursive Watcher Service with {} observers.", observers.size());

        FileAlterationMonitor monitor = new FileAlterationMonitor();
        FileAlterationListener listener = new DirectoryMonitorListener(monitor, observers);

        Consumer<Path> register = p -> {
            try {
                Files.walkFileTree(p, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        logger.info("registering {} in watcher service", dir);
                        FileAlterationObserver observer = new FileAlterationObserver(dir.toFile());
                        observer.addListener(listener);
                        monitor.addObserver(observer);
                        paths.add(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                logger.error("Failure registering directory in directory monitor", e);
            }
        };

        Arrays.stream(mediaPaths).map(Paths::get).forEach(register);

        try {
            monitor.start();
        } catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    public Set<Path> getPaths() {
        return paths;
    }
}