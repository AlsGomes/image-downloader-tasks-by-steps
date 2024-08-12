package br.com.agdev;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.AbstractQueue;
import java.util.concurrent.CountDownLatch;

public class ImageFinalizationManager implements Runnable {

    private final Logger log = LogManager.getLogger(getClass());
    private final AbstractQueue<ImageInfo> inputQueue;
    private final CountDownLatch countDownLatch;

    public ImageFinalizationManager(
        AbstractQueue<ImageInfo> inputQueue,
        CountDownLatch countDownLatch) {

        this.inputQueue = inputQueue;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        while (countDownLatch.getCount() > 0) {
            ImageInfo imageInfo = inputQueue.poll();
            if (imageInfo == null) {
                continue;
            }

            log.info("Finished process for [{}]", imageInfo);
            countDownLatch.countDown();
        }
    }
}
