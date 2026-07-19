package org.monzo.webcrawler.models;

import java.net.URI;
import java.util.Collections;
import java.util.List;

public record ParseResult(URI uri, List<URI> links, String error) {
    public ParseResult(URI uri, List<URI> links) {
        this(uri, links, null);
    }

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
