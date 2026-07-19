package org.monzo.webcrawler.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.monzo.webcrawler.core.WebCrawlerService;
import org.monzo.webcrawler.exception.ClientErrorException;
import org.monzo.webcrawler.exception.HttpStatusException;
import org.monzo.webcrawler.exception.ServerErrorException;
import org.monzo.webcrawler.models.FetchResult;
import org.monzo.webcrawler.models.ParseResult;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebRequestParserUnitTest {

    @Mock
    private WebClient webClient;
    @Mock
    private WebCrawlerService webCrawlerService;

    @Test
    void run_extractsLinksAndReportsSuccess() {
        URI page = URI.create("https://monzo.com/");
        String html = """
                <html><body>
                  <a href="/about">About</a>
                  <a href="https://facebook.com/monzo">FB</a>
                </body></html>
                """;
        when(webClient.fetch(eq(page), any())).thenReturn(
                new FetchResult(page, page, List.of(page), html));

        new WebRequestParser(page, webClient, webCrawlerService).run();

        ArgumentCaptor<ParseResult> captor = ArgumentCaptor.forClass(ParseResult.class);
        verify(webCrawlerService).processResults(captor.capture());
        ParseResult result = captor.getValue();
        assertTrue(result.isSuccess());
        assertEquals(page, result.uri());
        assertEquals(2, result.links().size());
        assertTrue(result.links().contains(URI.create("https://monzo.com/about")));
        assertTrue(result.links().contains(URI.create("https://facebook.com/monzo")));
    }

    @Test
    void run_reportsFailureWhenFetchThrows() {
        URI page = URI.create("https://monzo.com/missing");
        when(webClient.fetch(eq(page), any())).thenThrow(new RuntimeException("boom"));

        new WebRequestParser(page, webClient, webCrawlerService).run();

        ArgumentCaptor<ParseResult> captor = ArgumentCaptor.forClass(ParseResult.class);
        verify(webCrawlerService).processResults(captor.capture());
        assertTrue(captor.getValue().isFailure());
        assertEquals(page, captor.getValue().uri());
    }

    @Test
    void run_reportsFailureWhenClientErrorExceptionThrown() {
        URI page = URI.create("https://monzo.com/missing");
        ClientErrorException clientError = new ClientErrorException(mockResponse(404, page));
        when(webClient.fetch(eq(page), any())).thenThrow(clientError);

        new WebRequestParser(page, webClient, webCrawlerService).run();

        ArgumentCaptor<ParseResult> captor = ArgumentCaptor.forClass(ParseResult.class);
        verify(webCrawlerService).processResults(captor.capture());
        ParseResult result = captor.getValue();
        assertTrue(result.isFailure());
        assertEquals(page, result.uri());
        assertEquals("Status code is: 404", result.error());
    }

    @Test
    void run_reportsFailureWhenServerErrorExceptionThrown() {
        URI page = URI.create("https://monzo.com/down");
        ServerErrorException serverError = new ServerErrorException(mockResponse(503, page));
        when(webClient.fetch(eq(page), any())).thenThrow(serverError);

        new WebRequestParser(page, webClient, webCrawlerService).run();

        ArgumentCaptor<ParseResult> captor = ArgumentCaptor.forClass(ParseResult.class);
        verify(webCrawlerService).processResults(captor.capture());
        ParseResult result = captor.getValue();
        assertTrue(result.isFailure());
        assertTrue(result.error().contains("503"));
    }

    @Test
    void run_reportsFailureWhenHttpStatusExceptionThrown() {
        URI page = URI.create("https://monzo.com/error");
        HttpStatusException statusError = new HttpStatusException(418, page.toString());
        when(webClient.fetch(eq(page), any())).thenThrow(statusError);

        new WebRequestParser(page, webClient, webCrawlerService).run();

        ArgumentCaptor<ParseResult> captor = ArgumentCaptor.forClass(ParseResult.class);
        verify(webCrawlerService).processResults(captor.capture());
        assertTrue(captor.getValue().isFailure());
        assertEquals("Status code is: 418", captor.getValue().error());
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> mockResponse(int status, URI uri) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.uri()).thenReturn(uri);
        return response;
    }
}

