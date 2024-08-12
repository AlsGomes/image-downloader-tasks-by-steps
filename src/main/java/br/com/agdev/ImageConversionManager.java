package br.com.agdev;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.AbstractQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class ImageConversionManager implements Runnable {

    private final Logger log = LogManager.getLogger(getClass());
    private final AbstractQueue<ImageInfo> inputQueue;
    private final AbstractQueue<ImageInfo> outputQueue;
    private final CountDownLatch countDownLatch;
    private final Semaphore semaphore;

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public ImageConversionManager(
        AbstractQueue<ImageInfo> inputQueue,
        AbstractQueue<ImageInfo> outputQueue,
        CountDownLatch countDownLatch,
        Semaphore semaphore) {

        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.countDownLatch = countDownLatch;
        this.semaphore = semaphore;
    }

    @Override
    public void run() {
        while (countDownLatch.getCount() > 0) {
            ImageInfo imageInfo = inputQueue.poll();
            if (imageInfo == null) {
                continue;
            }

            try {
                semaphore.acquire();
                executorService.execute(new ImageConverterTask(outputQueue, imageInfo, semaphore));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        log.info("[{}] finalized", getClass().getSimpleName());
        executorService.shutdown();
    }
}
