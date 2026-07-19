package org.monzo.webcrawler.models;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * Outcome of fetching and parsing one page for the crawler.
 */
public record ParseResult(URI uri, List<URI> links, String error) {
    /**
     * Successful parse: links present, no error.
     */
    public ParseResult(URI uri, List<URI> links) {
        this(uri, links, null);
    }

    /**
     * Failed parse: no present, error message set.
     */
    public ParseResult(URI uri, String error) {
        this(uri, Collections.emptyList(), error);
    }

    public boolean isSuccess() {
        return !isFailure();
    }

    public boolean isFailure() {
        return error != null && !error.isBlank();
    }
}
