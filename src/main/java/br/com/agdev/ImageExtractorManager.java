package br.com.agdev;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.AbstractQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageExtractorManager implements Runnable {

    private final Logger log = LogManager.getLogger(getClass());
    private final AbstractQueue<ImageInfo> inputQueue;
    private final AbstractQueue<ImageInfo> finalizationQueue;
    private final AbstractQueue<ImageInfo> conversionQueue;
    private final CountDownLatch countDownLatch;

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public ImageExtractorManager(
        AbstractQueue<ImageInfo> inputQueue,
        AbstractQueue<ImageInfo> finalizationQueue,
        AbstractQueue<ImageInfo> conversionQueue,
        CountDownLatch countDownLatch) {

        this.inputQueue = inputQueue;
        this.finalizationQueue = finalizationQueue;
        this.conversionQueue = conversionQueue;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        while (countDownLatch.getCount() > 0) {
            ImageInfo imageInfo = inputQueue.poll();
            if (imageInfo == null) {
                continue;
            }

            if (imageInfo.convert()) {
                executorService.execute(new ImageExtractorTask(conversionQueue, imageInfo));
            } else {
                executorService.execute(new ImageExtractorTask(finalizationQueue, imageInfo));
            }
        }

        log.info("[{}] finalized", getClass().getSimpleName());

        executorService.shutdown();
    }
}
