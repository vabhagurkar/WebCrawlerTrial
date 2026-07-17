package org.monzo.webcrawler.exception;

import java.net.http.HttpResponse;

public class ClientErrorException extends HttpStatusException {

    public ClientErrorException(HttpResponse<String> httpResponse) {
        super(httpResponse.statusCode(), httpResponse.uri().toString());
    }
}
