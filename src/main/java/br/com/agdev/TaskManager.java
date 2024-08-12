package br.com.agdev;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class TaskManager {

    private static final Logger LOG = LogManager.getLogger(TaskManager.class);

    private static final AbstractQueue<ImageInfo> EXTRACTION_QUEUE = new ConcurrentLinkedQueue<>();
    private static final AbstractQueue<ImageInfo> CONVERSION_QUEUE = new ConcurrentLinkedQueue<>();
    private static final AbstractQueue<ImageInfo> DELETION_QUEUE = new ConcurrentLinkedQueue<>();
    private static final AbstractQueue<ImageInfo> FINALIZATION_QUEUE = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) {
        int permits = 45;
        int steps = 5;
        LOG.info("Initiating process with [{}] permits, executing [{}] by [{}] steps...", permits, steps, steps);

        List<Summary> summaryList = new ArrayList<>();

        for (int i = 0; i <= permits; i += steps) {
            int permitsPerTime = i;
            if (i == 0) {
                permitsPerTime = 1;
            }
            Semaphore semaphore = new Semaphore(permitsPerTime);

            LOG.info("Initiating process with [{}] threads...", permitsPerTime);
            Summary summary = execute(semaphore, permitsPerTime);
            LOG.info("Process with [{}] threads finished!", permitsPerTime);

            summaryList.add(summary);
        }

        SummaryGenerator.generateSummary(summaryList);

    }

    private static Summary execute(Semaphore semaphore, int permits) {
        Instant initialTime = Instant.now();

        int pages = 41;
        CountDownLatch countDownLatch = new CountDownLatch(pages);

        ExecutorService imageExtractorExecutor = null;
        ExecutorService conversionExecutor = null;
        ExecutorService deletionExecutor = null;
        ExecutorService finalizationExecutor = null;

        try {
            imageExtractorExecutor = Executors.newSingleThreadExecutor();
            conversionExecutor = Executors.newSingleThreadExecutor();
            deletionExecutor = Executors.newSingleThreadExecutor();
            finalizationExecutor = Executors.newSingleThreadExecutor();

            imageExtractorExecutor.execute(new ImageExtractorManager(EXTRACTION_QUEUE, FINALIZATION_QUEUE, CONVERSION_QUEUE, countDownLatch));
            conversionExecutor.execute(new ImageConversionManager(CONVERSION_QUEUE, DELETION_QUEUE, countDownLatch, semaphore));
            deletionExecutor.execute(new ImageDeletionManager(DELETION_QUEUE, FINALIZATION_QUEUE, countDownLatch));
            finalizationExecutor.execute(new ImageFinalizationManager(FINALIZATION_QUEUE, countDownLatch));

            for (int i = 1; i <= pages; i++) {
                String page = String.format("%04d", i);
                String prePageFileName = "Boruto-Two-Blue-Vortex-Capitulo-4_page-";
                String postPageFileName = "";
                String extension = ".webp";
                String filename = prePageFileName + page + postPageFileName + extension;
                String urlBase = "https://leitordemanga.com/wp-content/uploads/WP-manga/data/manga_65d48dc6127d0/";
                String urlMid = "2d98fedc0da277a0af0818e42a2f6621/";
                String url = urlBase + urlMid + filename;
                String folder = "boruto-tbb-cap4-multitask-by-step";

                Path pathWithoutFile = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", folder);
                if (Files.notExists(pathWithoutFile)) {
                    Files.createDirectories(pathWithoutFile);
                }

                Path path = pathWithoutFile.resolve(filename);

                boolean convert = extension.equals(".webp");
                EXTRACTION_QUEUE.add(new ImageInfo(url, path, convert));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (imageExtractorExecutor != null) {
                imageExtractorExecutor.shutdown();
            }
            if (conversionExecutor != null) {
                conversionExecutor.shutdown();
            }
            if (deletionExecutor != null) {
                deletionExecutor.shutdown();
            }
            if (finalizationExecutor != null) {
                finalizationExecutor.shutdown();
            }
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Instant finalTime = Instant.now();
        long elapsedTime = ChronoUnit.SECONDS.between(initialTime, finalTime);

        LOG.info("Process finalized. The process took [{}] seconds to complete", elapsedTime);
        return new Summary("Tasks by steps", elapsedTime, permits);
    }
}
