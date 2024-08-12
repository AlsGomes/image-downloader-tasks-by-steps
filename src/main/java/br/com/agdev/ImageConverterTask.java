package br.com.agdev;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;
import java.util.AbstractQueue;
import java.util.concurrent.Semaphore;

public class ImageConverterTask implements Runnable {

    private final Logger log = LogManager.getLogger(getClass());
    private final AbstractQueue<ImageInfo> outputQueue;
    private final ImageInfo imageInfo;
    private final Semaphore semaphore;

    public ImageConverterTask(AbstractQueue<ImageInfo> outputQueue, ImageInfo imageInfo, Semaphore semaphore) {
        this.outputQueue = outputQueue;
        this.imageInfo = imageInfo;
        this.semaphore = semaphore;
    }

    @Override
    public void run() {
        log.info("Converting image [{}]...", imageInfo);
        String pathString = imageInfo.path().toString();
        convert(pathString, pathString.substring(0, pathString.lastIndexOf('.') + 1) + "png");
        log.info("Converted image [{}] successfully", imageInfo);

        semaphore.release();
        outputQueue.add(imageInfo);
    }

    private void convert(String webpPath, String pngPath) {
        String executable = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "libwebp-0.4.1-linux-x86-64", "bin", "dwebp").toString();
        String[] args = new String[]{executable, webpPath, "-o", pngPath};

        try {
            Process exec = Runtime.getRuntime().exec(args);
            exec.waitFor();
        } catch (Exception e) {
            log.error("Error while converting [{}]", imageInfo, e);
        }
    }
}
