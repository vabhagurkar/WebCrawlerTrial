package org.monzo.webcrawler.core;

import org.monzo.webcrawler.models.ParseResult;
import org.monzo.webcrawler.utils.URLFormatter;
import org.monzo.webcrawler.web.WebRequestParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class WebCrawlerServiceImpl implements WebCrawlerService {

    private static final Logger log = LoggerFactory.getLogger(WebCrawlerServiceImpl.class);
    private static final int CONCURRENCY_LEVEL = 20;
    private final Set<String> linkHistory;
    private final ExecutorService executorService;
    private final WebEngineObserver webEngineObserver;
    private final URI rootURI;
    private final URLFormatter urlFormatter = new URLFormatter();

    public WebCrawlerServiceImpl(ExecutorService executorService, WebEngineObserver webEngineObserver, URI rootURI) {
        this.linkHistory = new HashSet<>();
        this.executorService = executorService;
        this.webEngineObserver = webEngineObserver;
        this.rootURI = rootURI;
    }

    public static WebCrawlerService create(URI rootURI) {
        log.info("Creating WebCrawlerService with rootURI {}", rootURI);

        ThreadFactory threadFactory = Thread.ofVirtual().name("worker-", 0).factory();
        URLFormatter formatter = new URLFormatter();
        URI normalisedRoot = formatter.normaliseURL(rootURI);
        if (normalisedRoot == null) {
            throw new IllegalArgumentException("Invalid root URI: " + rootURI);
        }

        return new WebCrawlerServiceImpl(
                Executors.newFixedThreadPool(CONCURRENCY_LEVEL, threadFactory),
                WebEngineObserver.instance(),
                normalisedRoot);
    }

    public void enqueue(URI uri) {
        URI normalised = urlFormatter.normaliseURL(uri);
        if (normalised == null || !urlFormatter.isHtmlResource(normalised)) {
            return;
        }
        String key = normalised.toString();
        if (!linkHistory.add(key)) {
            return;
        }
        webEngineObserver.incrementEnqueuedLinks();
        executorService.submit(new WebRequestParser(normalised, this));
        log.debug("Enqueued uri: {}", normalised);
    }

    public void enqueueValidURLs(ParseResult parseResult) {
        parseResult.links().stream()
                .map(urlFormatter::normaliseURL)
                .filter(Objects::nonNull)
                .filter(link -> urlFormatter.isSameHost(link, rootURI)) // strict host vs seed, not page URI
                .filter(urlFormatter::isHtmlResource) // skip PDF, images, CSS, JS, …
                .filter(link -> !linkHistory.contains(link.toString()))
                .forEach(this::enqueue);
    }

    /**
     * Records a redirect landing in the visited set (no new worker task).
     * Stops A→B→A style re-crawls when B (or a later hop) was already seen.
     */
    @Override
    public synchronized boolean claimRedirectHop(URI hop) {
        URI normalised = urlFormatter.normaliseURL(hop);
        if (normalised == null || !urlFormatter.isHtmlResource(normalised)) {
            log.warn("Rejecting redirect hop (non-HTML or invalid): {}", hop);
            return false;
        }
        if (!urlFormatter.isSameHost(normalised, rootURI)) {
            log.warn("Rejecting redirect hop (left seed host): {}", normalised);
            return false;
        }

        String key = normalised.toString();
        if (!linkHistory.add(key)) {
            log.info("Redirect hop already visited: {}", normalised);
            return false;
        }
        log.debug("Recorded redirect hop in visited set: {}", normalised);
        return true;
    }

    @Override
    public WebEngineObserver start() {
        enqueue(rootURI);
        return webEngineObserver;
    }

    @Override
    public synchronized void processResults(ParseResult parseResult) {
        try {
            if (parseResult.isFailure()) {
                log.warn("Skipping failed page {}: {}", parseResult.uri(), parseResult.error());
            } else {
                enqueueValidURLs(parseResult);
            }
        } catch (Exception exception) {
            // Keep the crawl alive even if link expansion misbehaves for one page.
            log.warn("Error processing {}: {} — continuing crawl",
                    parseResult.uri(), exception.toString());
        } finally {
            webEngineObserver.incrementProcessedLinks();
            webEngineObserver.printProcessingReport();
            if (webEngineObserver.isTerminated()) {
                webEngineObserver.notifyTermination();
            }
        }
    }
}

