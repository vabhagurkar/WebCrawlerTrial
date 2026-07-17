package org.monzo.webcrawler.exception;

public class HttpStatusException extends WebClientException {

    private final int statusCode;
    private final String url;

    public HttpStatusException(int statusCode, String url) {
        super("Status code is: " + statusCode);
        this.statusCode = statusCode;
        this.url = url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getUrl() {
        return url;
    }
}
