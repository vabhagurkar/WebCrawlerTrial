package org.monzo.webcrawler.web;

import org.monzo.webcrawler.exception.ClientErrorException;
import org.monzo.webcrawler.exception.ServerErrorException;
import org.monzo.webcrawler.exception.WebClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class WebClient {

    private static final Logger log = LoggerFactory.getLogger(WebClient.class);
    private static final WebClient WEB_CLIENT = new WebClient();

    private final HttpClient httpClient;

    WebClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    public WebClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String getURL(URI uri) {
        log.debug("Get URL: {}", uri);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> httpResponse = send(httpRequest);
        return httpResponse.body();
    }

    private HttpResponse<String> send(HttpRequest httpRequest) {
        HttpResponse<String> httpResponse = null;
        try {
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        } catch (Exception exception) {
            throw new WebClientException(exception.getMessage());
        }
        checkResponseStatus(httpResponse);
        return httpResponse;
    }

    private void checkResponseStatus(HttpResponse<String> httpResponse) {
        int responseStatusCode = httpResponse.statusCode();

        if(responseStatusCode >= 400 && responseStatusCode <= 499) throw new ClientErrorException(httpResponse);
        if(responseStatusCode >= 500 && responseStatusCode <= 599) throw new ServerErrorException(httpResponse);
    }
}