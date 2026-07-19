package org.monzo.webcrawler.web;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monzo.webcrawler.exception.WebClientException;
import org.monzo.webcrawler.models.FetchResult;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebClientUnitTest {

    private HttpServer server;
    private URI baseUri;
    private WebClient webClient;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        webClient = new WebClient();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void fetchReturnsBodyForHtmlPage() {
        server.createContext("/page", exchange -> {
            byte[] body = "<html><body>ok</body></html>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        FetchResult result = webClient.fetch(baseUri.resolve("/page"));

        assertEquals(baseUri.resolve("/page").toString(), result.finalUri().toString());
        assertTrue(result.body().contains("ok"));
    }

    @Test
    void fetchFollowsSameHostRedirectWhenAccepted() {
        server.createContext("/start", exchange -> {
            exchange.getResponseHeaders().add("Location", "/landing");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/landing", exchange -> {
            byte[] body = "<html>landed</html>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        AtomicInteger claims = new AtomicInteger();
        FetchResult result = webClient.fetch(baseUri.resolve("/start"), hop -> {
            claims.incrementAndGet();
            return true;
        });

        assertEquals(1, claims.get());
        assertTrue(result.body().contains("landed"));
        assertEquals(2, result.redirectChain().size());
    }

    @Test
    void fetchRejectsRedirectWhenPredicateReturnsFalse() {
        server.createContext("/start", exchange -> {
            exchange.getResponseHeaders().add("Location", "/offhost");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        WebClientException ex = assertThrows(WebClientException.class,
                () -> webClient.fetch(baseUri.resolve("/start"), hop -> false));

        assertTrue(ex.getMessage().contains("not crawlable"));
    }

    @Test
    void fetchDetectsRedirectLoop() {
        server.createContext("/a", exchange -> {
            exchange.getResponseHeaders().add("Location", "/b");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/b", exchange -> {
            exchange.getResponseHeaders().add("Location", "/a");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        assertThrows(WebClientException.class,
                () -> webClient.fetch(baseUri.resolve("/a"), hop -> true));
    }
}

