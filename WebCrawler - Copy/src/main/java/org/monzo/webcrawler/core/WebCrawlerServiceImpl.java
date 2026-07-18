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

        return new WebCrawlerServiceImpl(
                Executors.newFixedThreadPool(CONCURRENCY_LEVEL, threadFactory),
                WebEngineObserver.instance(),
                rootURI);
    }

    public void enqueue(URI uri) {
        URI normalisedURI = urlFormatter.normaliseURL(uri);
        if(normalisedURI == null) return;

        String key = normalisedURI.toString();
        if(!linkHistory.add(key)) return; //Already seen

        webEngineObserver.incrementEnqueuedLinks();
        executorService.submit(new WebRequestParser(normalisedURI, this));
        log.debug("Enqueued normalised uri: {}", normalisedURI);
    }

    public void enqueueValidURLs(ParseResult parseResult) {
        String domain = urlFormatter.getDomainName(parseResult.uri());
        parseResult.links().stream()
                .map(urlFormatter::normaliseURL)
                .filter(Objects::nonNull)
                .filter(link -> domain.equalsIgnoreCase(urlFormatter.getDomainName(link)))
                .filter(link -> !linkHistory.contains(link.toString()))
                .forEach(this::enqueue);
    }

    @Override
    public WebEngineObserver start() {
        enqueue(rootURI);
        return webEngineObserver;
    }

    @Override
    public synchronized void processResults(ParseResult parseResult) {

        try {
            if(parseResult.isSuccess()) {
                enqueueValidURLs(parseResult);
            }
        } finally {
            webEngineObserver.incrementProcessedLinks();
            webEngineObserver.printProcessingReport();
            if(webEngineObserver.isTerminated()) {
                webEngineObserver.notifyTermination();

            }
        }
    }
}
