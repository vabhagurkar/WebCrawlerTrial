package org.monzo.webcrawler.exception;

import java.net.http.HttpResponse;

public class ServerErrorException extends HttpStatusException {

    public ServerErrorException(HttpResponse<String> httpResponse) {
        super(httpResponse.statusCode(), httpResponse.uri().toString());
    }
}

