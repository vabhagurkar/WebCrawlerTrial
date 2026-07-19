package org.monzo.webcrawler.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks crawl progress and lets the CLI wait until work is finished.
 * Termination is defined as (enqueued == processed)
 * When this happens, notifyTermination() releases awaitTermination.
 */
public class WebEngineObserver {
    private static final Logger log = LoggerFactory.getLogger(WebEngineObserver.class);
    private final AtomicInteger totalEnqueuedLinks;
    private final AtomicInteger totalProcessedLinks;
    //Latch count down once when the crawl has no remaining work
    private final CountDownLatch countDownLatch;

    public WebEngineObserver(AtomicInteger totalEnqueuedLinks, AtomicInteger totalProcessedLinks, CountDownLatch countDownLatch) {
        this.totalEnqueuedLinks = totalEnqueuedLinks;
        this.totalProcessedLinks = totalProcessedLinks;
        this.countDownLatch = countDownLatch;
    }

    /**
     * Creates an observer with zero counts and a latch of 1.
     */
    public static WebEngineObserver instance() {
        return new WebEngineObserver(new AtomicInteger(), new AtomicInteger(), new CountDownLatch(1));
    }

    /**
     * Called when a new fetch/parse task is submitted.
     */
    public void incrementEnqueuedLinks() {
        totalEnqueuedLinks.incrementAndGet();
    }

    /**
     * Called when a page has been fully handled in parseResults.
     */
    public void incrementProcessedLinks() {
        totalProcessedLinks.incrementAndGet();
    }

    /**
     * @return true when every enqueued page has been processed.
     */
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

    /**
     *  Prints a short progress line after each processed page.
     */
    public void printProcessingReport() {
        System.out.println();
        System.out.printf("Enqueued links: " +  totalEnqueuedLinks.intValue() + "-- Processed Links: " + totalProcessedLinks.intValue());
    }
}