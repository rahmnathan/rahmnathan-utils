package com.github.rahmnathan.directory.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class DirectoryMonitor {
    private static final Logger logger = LoggerFactory.getLogger(DirectoryMonitor.class.getName());
    private final Collection<DirectoryMonitorObserver> observers;
    private final Map<WatchKey, Path> keys = new HashMap<>();
    private final Set<Path> paths = new HashSet<>();
    private final ExecutorService executor;
    private WatchService watchService;
    private Consumer<Path> register;

    public DirectoryMonitor(Collection<DirectoryMonitorObserver> observers) {
        this.executor = Executors.newSingleThreadExecutor();
        this.observers = observers;
        startRecursiveWatcher();
    }

    private void notifyObservers(WatchEvent event, Path absolutePath) {
        observers.forEach(observer -> CompletableFuture.runAsync(() -> observer.directoryModified(event, absolutePath)));
    }

    public Set<Path> getPaths() {
        return paths;
    }

    public void registerDirectory(String pathToMonitor) {
        register.accept(Paths.get(pathToMonitor));
    }

    private void startRecursiveWatcher() {
        logger.info("Starting Recursive Watcher");

        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            logger.error("Failed getting watch service", e);
            return;
        }

        register = p -> {
            try {
                Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        logger.info("registering {} in watcher service", dir);
                        WatchKey watchKey = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE);
                        keys.put(watchKey, dir);
                        paths.add(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                logger.error("Failure registering directory in directory monitor", e);
            }
        };

        executor.submit(() -> {
            while (true) {
                final WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    logger.error("Error getting watch key from directory monitor", e);
                    continue;
                }

                final Path dir = keys.get(key);

                key.pollEvents().stream()
                        .map(e -> ((WatchEvent<Path>) e))
                        .forEach(event -> {
                            if (!event.kind().equals(OVERFLOW)) {
                                final Path absPath = dir.resolve(event.context());
                                notifyObservers(event, absPath);
                                if (absPath.toFile().isDirectory()) {
                                    register.accept(absPath);
                                }
                            }
                        });
                key.reset();
            }
        });
    }
}