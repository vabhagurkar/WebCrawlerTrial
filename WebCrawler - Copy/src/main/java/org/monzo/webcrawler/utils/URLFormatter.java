package org.monzo.webcrawler.utils;

import org.apache.commons.validator.routines.UrlValidator;
import org.monzo.webcrawler.exception.InvalidInputURLException;

import java.net.MalformedURLException;
import java.net.URI;

public class URLFormatter {

    private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[]{"http", "https"});

    public URI parseAndValidateURL(String inputURL) throws MalformedURLException {
        try {
            if(null == inputURL || inputURL.isBlank()) throw new InvalidInputURLException("URL cannot be empty or " +
                    "blank.");

            inputURL = checkProtocolAndAdd(inputURL);

            if(URL_VALIDATOR.isValid(inputURL)) {
                throw new MalformedURLException("Invalid URL format.");
            }

            return new URI(inputURL);

        } catch (Exception exception) {
            throw new MalformedURLException(exception.getMessage());
        }
    }

    public String getDomainName(URI uri) {
        String host = uri.getHost();
        return (host != null && host.startsWith("www") ? host.substring(4) : host);
    }

    public String checkProtocolAndAdd(String inputURL) {
        if(!inputURL.toLowerCase().startsWith("http")) {
            inputURL = "https://" + inputURL;
        }
        return inputURL;
    }
}