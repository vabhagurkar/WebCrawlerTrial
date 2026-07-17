package org.monzo.webcrawler.web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.monzo.webcrawler.core.WebCrawlerService;
import org.monzo.webcrawler.models.ParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

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
        log.info("Web parser is running.");
        try {
            Document doc = fetchAndParseDocument();
            List<URI> links = extractLinks(doc);
            webCrawlerService.processResults(new ParseResult(targetURL, links));

        } catch (Exception exception) {
            log.error("Exception during running the WebRequestParser: {}", exception);
            webCrawlerService.processResults(new ParseResult(targetURL, exception.getMessage()));
        }
    }

    private Document fetchAndParseDocument() {
        log.info("Fetching the document from: {}", targetURL.toString());
        String response = webClient.getURL(targetURL);
        log.trace("Response: {}", response);
        return Jsoup.parse(response);
    }

    private List<URI> extractLinks(Document doc) {
        log.debug("Extracting links from doc");
        return doc.select("a[href]")
                .eachAttr("abs:href")
                .stream()
                .map(URI::create)
                .toList();
    }
}
