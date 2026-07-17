package org.monzo.webcrawler.core;

import org.monzo.webcrawler.models.ParseResult;

public interface WebCrawlerService {
    WebEngineObserver start();
    void processResults(ParseResult parseResult);
}
