package org.monzo.webcrawler.models;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * Result of an HTTP GET after following redirects.
 * {@code redirectChain} is normalised request → intermediate hops → final URI.
 */
public record FetchResult(URI requestedUri, URI finalUri, List<URI> redirectChain, String body) {
    public boolean redirected() {
        return redirectChain.size() > 1
                && !requestedUri.equals(finalUri);
    }
}