package org.monzo.webcrawler.core;

import org.monzo.webcrawler.models.ParseResult;

import java.net.URI;

public interface WebCrawlerService {
    WebEngineObserver start();
    void processResults(ParseResult parseResult);
    /**
     * Normalises {@code hop}, checks host/HTML rules, and adds it to the visited
     * set without enqueueing a new task.
     *
     * @return {@code false} if already visited, off-host, or non-HTML
     */
    boolean claimRedirectHop(URI hop);
}
