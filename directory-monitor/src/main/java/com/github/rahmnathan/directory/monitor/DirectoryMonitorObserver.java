package com.github.rahmnathan.directory.monitor;

import java.io.File;
import java.nio.file.WatchEvent;

public interface DirectoryMonitorObserver {
    void directoryModified(WatchEvent.Kind event, File absolutePath);
}
