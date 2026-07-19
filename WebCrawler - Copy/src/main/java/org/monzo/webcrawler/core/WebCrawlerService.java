package org.monzo.webcrawler.core;

import org.monzo.webcrawler.models.ParseResult;
import java.net.URI;

/**
 * Contract for the same host crawl.
 * Implementations own the visited set, enqueue workers, expand links from parseResults, and redirect hops so the
 * seed host is never left.
 */
public interface WebCrawlerService {

    /** Enqueues the seed URL and returns the observer used to wait for the completion */
    WebEngineObserver start();

    /** Handles page outcomes: prints visited URL + links */
    void processResults(ParseResult parseResult);

    /**
     * Normalizes hop, checks host/HTML rules, and adds it to the visited
     * set without enqueueing a new task.
     * @return false if already visited, off-host, or non-HTML
     */
    boolean claimRedirectHop(URI hop);
}
