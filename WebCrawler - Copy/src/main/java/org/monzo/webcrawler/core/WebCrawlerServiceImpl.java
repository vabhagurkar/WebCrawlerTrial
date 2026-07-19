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

/**
 * Default Web crawler
 * Maintains in-memory visited sets, submits WebRequestParser
 * filters links and caps how many pages are enqueue
 */
public class WebCrawlerServiceImpl implements WebCrawlerService {

    private static final Logger log = LoggerFactory.getLogger(WebCrawlerServiceImpl.class);
    /** Parallel fetch workers */
    private static final int CONCURRENCY_LEVEL = 20;
    /** Default cap for demo runs; use {@link #create(URI, int)} with {@code 0} for no limit. */
    public static final int DEFAULT_MAX_PAGES = 50;
    /** Normalised URL strings already seen */
    private final Set<String> linkHistory;
    private final ExecutorService executorService;
    private final WebEngineObserver webEngineObserver;
    private final URI rootURI; //Seed host used for same-host checks
    private final int maxPages;
    private int pagesEnqueued;
    private final URLFormatter urlFormatter = new URLFormatter();

    public WebCrawlerServiceImpl(ExecutorService executorService, WebEngineObserver webEngineObserver,
                                 URI rootURI, int maxPages) {
        this.linkHistory = new HashSet<>();
        this.executorService = executorService;
        this.webEngineObserver = webEngineObserver;
        this.rootURI = rootURI;
        this.maxPages = maxPages;
        this.pagesEnqueued = 0;
    }

    /** Creates a crawler for rootURI wih DEFAULT_MAX_PAGES */
    public static WebCrawlerService create(URI rootURI) {
        return create(rootURI, DEFAULT_MAX_PAGES);
    }

    /**
     * Creates a crawler for rootURI with an optional page limit.
     * @param maxPages maximum pages to fetch; {@code 0} or negative means unlimited
     */
    public static WebCrawlerService create(URI rootURI, int maxPages) {
        log.info("Creating WebCrawlerService with rootURI {} (maxPages={})",
                rootURI, maxPages <= 0 ? "unlimited" : maxPages);

        ThreadFactory threadFactory = Thread.ofVirtual().name("worker-", 0).factory();
        URLFormatter formatter = new URLFormatter();
        URI normalisedRoot = formatter.normaliseURL(rootURI);
        if (normalisedRoot == null) {
            throw new IllegalArgumentException("Invalid root URI: " + rootURI);
        }

        return new WebCrawlerServiceImpl(
                Executors.newFixedThreadPool(CONCURRENCY_LEVEL, threadFactory),
                WebEngineObserver.instance(),
                normalisedRoot,
                maxPages);
    }

    /**
     * Adds uri to the visited set and submits a fetch/parse worker if new, same-host HTML and under the page limit
     */
    public synchronized void enqueue(URI uri) {
        if (hasReachedPageLimit()) {
            return; //already visited/ enqueued
        }
        URI normalised = urlFormatter.normaliseURL(uri);
        if (normalised == null || !urlFormatter.isHtmlResource(normalised)) {
            return;
        }
        String key = normalised.toString();
        if (!linkHistory.add(key)) {
            return;
        }
        pagesEnqueued++;
        webEngineObserver.incrementEnqueuedLinks();
        executorService.submit(new WebRequestParser(normalised, this));
        log.debug("Enqueued uri: {} ({}/{})", normalised, pagesEnqueued,
                maxPages <= 0 ? "∞" : maxPages);
    }

    /**
     * Expands limks from a successful page; same host as the seed, HTML only, not yet seen.
     */
    public synchronized void enqueueValidURLs(ParseResult parseResult) {
        if (hasReachedPageLimit()) {
            return;
        }
        parseResult.links().stream()
                .map(urlFormatter::normaliseURL)
                .filter(Objects::nonNull)
                .filter(link -> urlFormatter.isSameHost(link, rootURI)) // strict host vs seed, not page URI
                .filter(urlFormatter::isHtmlResource) // skip PDF, images, CSS, JS, …
                .filter(link -> !linkHistory.contains(link.toString()))
                .forEach(this::enqueue);
    }

    private boolean hasReachedPageLimit() {
        return maxPages > 0 && pagesEnqueued >= maxPages;
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
        printVisitedPage(parseResult);
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

    /**
     * Prints the visited URL and every link found on that page (requirement output).
     */
    private void printVisitedPage(ParseResult parseResult) {
        System.out.println();
        System.out.println("Visited: " + parseResult.uri());
        if (parseResult.isFailure()) {
            System.out.println("(failed: " + parseResult.error() + ")");
            System.out.println("Links found: []");
            return;
        }
        System.out.println("Links found (" + parseResult.links().size() + "):");
        if (parseResult.links().isEmpty()) {
            System.out.println("(none)");
            return;
        }
        for (URI link : parseResult.links()) {
            System.out.println(" - " + link);
        }
    }
}