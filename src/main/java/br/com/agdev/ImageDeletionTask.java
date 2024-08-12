package br.com.agdev;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractQueue;

public class ImageDeletionTask implements Runnable {

    private final Logger log = LogManager.getLogger(getClass());
    private final AbstractQueue<ImageInfo> outputQueue;
    private final ImageInfo imageInfo;

    public ImageDeletionTask(AbstractQueue<ImageInfo> outputQueue, ImageInfo imageInfo) {
        this.outputQueue = outputQueue;
        this.imageInfo = imageInfo;
    }

    @Override
    public void run() {
        log.info("Deleting image [{}]...", imageInfo);
        delete(imageInfo.path());
        log.info("Deleted image [{}] successfully", imageInfo);

        outputQueue.add(imageInfo);
    }

    private void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
