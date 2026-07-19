package org.monzo.webcrawler.web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.monzo.webcrawler.core.WebCrawlerService;
import org.monzo.webcrawler.models.FetchResult;
import org.monzo.webcrawler.models.ParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.List​;

public class WebRequestParser implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(WebRequestParser.class);
    private final URI targetURL;
    private final WebClient webClient;
    private final WebCrawlerService webCrawlerService;

    public WebRequestParser(URI targetURL, WebCrawlerService webCrawlerService) {
        this(targetURL, new WebClient(), webCrawlerService);
    }

    public WebRequestParser(URI targetURL, WebClient webClient, WebCrawlerService webCrawlerService) {
        this.targetURL = targetURL;
        this.webClient = webClient;
        this.webCrawlerService = webCrawlerService;
    }

    @Override
    public void run() {
        log.info("Web parser is running for {}", targetURL);
        ParseResult result;
        try {
            FetchResult fetch = webClient.fetch(targetURL);
            Document doc = Jsoup.parse(fetch.body(), fetch.finalUri().toString());
            List<URI> links = extractLinks(doc);
            result = new ParseResult(fetch.finalUri(), links);
        } catch (Exception exception) {
            // Failed fetch/parse/redirect: skip this page and let the crawl continue.
            log.warn("Skipping broken page {}: {}", targetURL, exception.toString());
            result = new ParseResult(targetURL, errorMessage(exception));
        }
        // Always report once so processed count advances and the latch can complete.
        webCrawlerService.processResults(result);
    }

    private List<URI> extractLinks(Document doc) {
        log.debug("Extracting links from doc");
        List<URI> links = new ArrayList<>();
        for (String href : doc.select("a[href]").eachAttr("abs:href")) {
            if (href == null || href.isBlank()) {
                continue;
            }
            try {
                links.add(URI.create(href));
            } catch (IllegalArgumentException ex) {
                log.debug("Skipping malformed href on {}: {}", targetURL, href);
            }
        }
        return links;
    }

    private static String errorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }
}

