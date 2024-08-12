package br.com.agdev;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.AbstractQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageDeletionManager implements Runnable {

    private final Logger log = LogManager.getLogger(getClass());
    private final AbstractQueue<ImageInfo> inputQueue;
    private final AbstractQueue<ImageInfo> outputQueue;
    private final CountDownLatch countDownLatch;

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public ImageDeletionManager(
        AbstractQueue<ImageInfo> inputQueue,
        AbstractQueue<ImageInfo> outputQueue,
        CountDownLatch countDownLatch) {

        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        while (countDownLatch.getCount() > 0) {
            ImageInfo imageInfo = inputQueue.poll();
            if (imageInfo == null) {
                continue;
            }

            executorService.execute(new ImageDeletionTask(outputQueue, imageInfo));
        }

        log.info("[{}] finalized", getClass().getSimpleName());

        executorService.shutdown();
    }
}
