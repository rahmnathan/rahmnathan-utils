package com.github.rahmnathan.directory.monitor;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.WatchEvent;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.*;

public class DirectoryMonitorListener implements FileAlterationListener {
    private final Logger logger = LoggerFactory.getLogger(DirectoryMonitorListener.class);
    private final Set<DirectoryMonitorObserver> observers;
    private final FileAlterationMonitor monitor;

    public DirectoryMonitorListener(FileAlterationMonitor monitor, Set<DirectoryMonitorObserver> observers) {
        this.monitor = monitor;
        this.observers = observers;
    }

    @Override
    public void onStart(FileAlterationObserver fileAlterationObserver) {

    }

    @Override
    public void onDirectoryCreate(File file) {
        notifyObservers(ENTRY_CREATE, file);
        monitor.addObserver(new FileAlterationObserver(file));
    }

    @Override
    public void onDirectoryChange(File file) {
    }

    @Override
    public void onDirectoryDelete(File file) {
        notifyObservers(ENTRY_DELETE, file);
    }

    @Override
    public void onFileCreate(File file) {
        notifyObservers(ENTRY_CREATE, file);
    }

    @Override
    public void onFileChange(File file) {
    }

    @Override
    public void onFileDelete(File file) {
        notifyObservers(ENTRY_DELETE, file);
    }

    @Override
    public void onStop(FileAlterationObserver fileAlterationObserver) {

    }

    private void notifyObservers(WatchEvent.Kind event, File file) {
        logger.info("Detected media event {} at path {}", event, file);
        observers.forEach(observer -> CompletableFuture.runAsync(() -> observer.directoryModified(event, file)));
    }
}
