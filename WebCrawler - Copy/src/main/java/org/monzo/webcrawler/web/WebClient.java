package org.monzo.webcrawler.web;

import org.monzo.webcrawler.exception.ClientErrorException;
import org.monzo.webcrawler.exception.ServerErrorException;
import org.monzo.webcrawler.exception.WebClientException;
import org.monzo.webcrawler.models.FetchResult;
import org.monzo.webcrawler.utils.URLFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WebClient {

    private static final Logger log = LoggerFactory.getLogger(WebClient.class);
    private static final int MAX_REDIRECTS = 3;

    private final HttpClient httpClient;

    WebClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    public WebClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    private HttpResponse<String> send(URI uri) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        try {
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (WebClientException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new WebClientException(exception.getMessage());
        }
    }

    private void checkResponseStatus(HttpResponse<String> httpResponse) {
        int responseStatusCode = httpResponse.statusCode();

        if(responseStatusCode >= 400 && responseStatusCode <= 499) throw new ClientErrorException(httpResponse);
        if(responseStatusCode >= 500 && responseStatusCode <= 599) throw new ServerErrorException(httpResponse);
    }

    /** Skip responses that are clearly not HTML (e.g. PDF/image served without a file extension). */
    private void checkHtmlContentType(HttpResponse<String> httpResponse) {
        httpResponse.headers().firstValue("content-type").ifPresent(contentType -> {
            String normalised = contentType.toLowerCase();
            if (normalised.contains("text/html") || normalised.contains("application/xhtml")) {
                return;
            }
            throw new WebClientException("Skipping non-HTML content-type: " + contentType);
        });
    }

    /**
     * GET with manual redirect following. Each hop is normalised; cycles in the
     * redirect chain throw {@link WebClientException}.
     */
    public FetchResult fetch(URI uri) {
        URI current = new URLFormatter().normaliseURL(uri);
        List<URI> chain = new ArrayList<>();
        Set<String> seenInChain = new HashSet<>();
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            if (!seenInChain.add(current.toString())) {
                throw new WebClientException("Redirect loop detected at " + current);
            }
            chain.add(current);
            log.debug("GET URL (hop {}): {}", hop, current);
            HttpResponse<String> response = send(current);
            int status = response.statusCode();
            if (isRedirect(status)) {
                URI next = resolveRedirect(current, response);
                continue;
            }

            checkResponseStatus(response);
            checkHtmlContentType(response);
            return new FetchResult(chain.getFirst(), current, List.copyOf(chain), response.body());
        }
        throw new WebClientException("Too many redirects (>" + MAX_REDIRECTS + ") from " + uri);
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private URI resolveRedirect(URI current, HttpResponse<String> response) {
        String location = response.headers().firstValue("location")
                .orElseThrow(() -> new WebClientException(
                        "Redirect " + response.statusCode() + " without Location from " + current));
        try {
            return current.resolve(URI.create(location));
        } catch (IllegalArgumentException exception) {
            throw new WebClientException("Invalid redirect Location: " + location);
        }
    }

    public FetchResult fetch(URI targetURL, Object claimRedirectHop) {
        return null;
    }
}