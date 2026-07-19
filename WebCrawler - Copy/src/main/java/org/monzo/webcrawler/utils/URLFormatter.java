package org.monzo.webcrawler.utils;

import org.apache.commons.validator.routines.UrlValidator;
import org.monzo.webcrawler.exception.InvalidInputURLException;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * URL helpers for seed validation, normalisation, same-host checks and HTML filtering.
 * Used by the CLI, the crawler and WebClient.
 */
public class URLFormatter {

    /** Accepts only http and https schemes for seed input. */
    private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[]{"http", "https"});
    /** Path extensions treated as non-crawler assets (not enqueued even on the seed host. */
    private static final Set<String> NON_HTML_EXTENSIONS = Set.of(
            // documents
            "pdf",
            // images
            "png", "jpg", "jpeg", "gif", "webp", "svg", "ico", "bmp", "tif", "tiff", "avif",
            // styles / scripts
            "css", "js", "mjs", "map",
            // fonts / binary often linked from pages
            "woff", "woff2", "ttf", "eot", "otf"
    );

    /** Validates user seed input: rejects blank values, adds https if needed and checks the URL with Commons
     * validator.*/
    public URI parseAndValidateURL(String inputURL) throws MalformedURLException {
        try {
            if(inputURL == null || inputURL.isBlank()) throw new InvalidInputURLException("URL cannot be empty or " +
                    "blank.");

            inputURL = checkProtocolAndAdd(inputURL);

            if(!URL_VALIDATOR.isValid(inputURL)) {
                throw new MalformedURLException("Invalid URL format.");
            }

            return new URI(inputURL);

        } catch (Exception exception) {
            throw new MalformedURLException(exception.getMessage());
        }
    }

    public String checkProtocolAndAdd(String inputURL) {
        if(!inputURL.toLowerCase().startsWith("http")) {
            inputURL = "https://" + inputURL;
        }
        return inputURL;
    }

    /**
     * Canonical form used as the visited-set key: lower case scheme/host, strip www, drop default ports, ensure a
     * path, trim a trailing slash.
     * Query strings are kept; fragments are not included.
     */
    public URI normaliseURL(URI uri) {
        if(uri == null || uri.getHost() == null) {
            return null;
        }

        String scheme = uri.getScheme().toLowerCase();
        String host = uri.getHost().toLowerCase();

        if(host.startsWith("www")) {
            host = host.substring(4);
        }

        int port = uri.getPort();
        if((scheme.equalsIgnoreCase("http") && port == 80)  ||
                (scheme.equalsIgnoreCase("https") && port == 443)) {
            port = -1;
        }

        String path = uri.getPath();
        if((path == null) || path.isEmpty()) {
            path = "/";
        } else if(path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return URI.create(scheme + "://" + host +
                (port == -1 ? "" : ":" + port) +
                path +
                (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : ""));

    }

    public String canonicalHost(URI uri) {
        if(uri == null || uri.getHost() == null) {
            return null;
        }
        //Explicit www policy - strip only "www." not startsWith("www")
        String host = uri.getHost().toLowerCase(Locale.ROOT);
                if(host.startsWith("www.")) {
                    host = host.substring(4);
                }
        return host;
    }

    public boolean isSameHost(URI uri, URI allowed) {
        String uriGiven = canonicalHost(uri);
        String uriAllowed = canonicalHost(allowed);
        return uriGiven != null && uriGiven.equalsIgnoreCase(uriAllowed);
    }

    /**
     * True when the URI path looks like an HTML page (or has no denylisted extension).
     * Skips PDF, images, CSS, JS, and similar assets before enqueue.
     */
    public boolean isHtmlResource(URI uri) {
        return uri != null && !isNonHtmlResource(uri);
    }
    public boolean isNonHtmlResource(URI uri) {
        if (uri == null) {
            return true;
        }
        String path = uri.getPath();
        if (path == null || path.isEmpty() || path.endsWith("/")) {
            return false;
        }
        int slash = path.lastIndexOf('/');
        String fileName = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return false;
        }
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return NON_HTML_EXTENSIONS.contains(extension);
    }
}