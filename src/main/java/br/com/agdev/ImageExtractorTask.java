package br.com.agdev;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.AbstractQueue;

public class ImageExtractorTask implements Runnable {

    private final Logger log = LogManager.getLogger(getClass());
    private final AbstractQueue<ImageInfo> outputQueue;
    private final ImageInfo imageInfo;

    public ImageExtractorTask(AbstractQueue<ImageInfo> outputQueue, ImageInfo imageInfo) {
        this.outputQueue = outputQueue;
        this.imageInfo = imageInfo;
    }

    @Override
    public void run() {
        log.info("Extracting image [{}]...", imageInfo);
        extractImage(imageInfo.url(), imageInfo.path().toString());
        log.info("Image extracted successfully: [{}]", imageInfo);

        outputQueue.add(imageInfo);
    }

    private void extractImage(String url, String path) {
        try {
            URL imageUrl = URL.of(URI.create(url), null);
            HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
            connection.setRequestMethod("GET");

            InputStream inputStream = connection.getInputStream();
            FileOutputStream outputStream = new FileOutputStream(path);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();
            connection.disconnect();
        } catch (Exception e) {
            log.error("Error while extracting image [{}]", imageInfo, e);
        }
    }
}
