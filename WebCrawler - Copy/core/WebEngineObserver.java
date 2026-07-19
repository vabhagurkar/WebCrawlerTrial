package org.monzo.webcrawler.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class WebEngineObserver {
    private static final Logger log = LoggerFactory.getLogger(WebEngineObserver.class);
    private final AtomicInteger totalEnqueuedLinks;
    private final AtomicInteger totalProcessedLinks;
    private final CountDownLatch countDownLatch;

    public WebEngineObserver(AtomicInteger totalEnqueuedLinks, AtomicInteger totalProcessedLinks, CountDownLatch countDownLatch) {
        this.totalEnqueuedLinks = totalEnqueuedLinks;
        this.totalProcessedLinks = totalProcessedLinks;
        this.countDownLatch = countDownLatch;
    }

    public static WebEngineObserver instance() {
        return new WebEngineObserver(new AtomicInteger(), new AtomicInteger(), new CountDownLatch(1));
    }

    public void incrementEnqueuedLinks() {
        totalEnqueuedLinks.incrementAndGet();
    }

    public void incrementProcessedLinks() {
        totalProcessedLinks.incrementAndGet();
    }

    public boolean isTerminated() {
        return totalEnqueuedLinks.intValue() == totalProcessedLinks.intValue();
    }

    public void awaitTermination() throws InterruptedException {
        log.debug("waiting...");
        countDownLatch.await();
        log.debug("process completed...");
    }

    public void notifyTermination() {
        countDownLatch.countDown();
    }

    public void printProcessingReport() {
        System.out.println("Enqueued links: " +  totalEnqueuedLinks.intValue() + "-- Processed Links: " + totalProcessedLinks.intValue());
    }
}
