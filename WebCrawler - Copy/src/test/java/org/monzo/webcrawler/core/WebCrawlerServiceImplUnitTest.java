package org.monzo.webcrawler.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.monzo.webcrawler.models.ParseResult;
import org.monzo.webcrawler.web.WebRequestParser;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebCrawlerServiceImplUnitTest {

    private static final URI ROOT = URI.create("https://monzo.com/");

    @Mock
    private ExecutorService executorService;
    @Mock
    private WebEngineObserver observer;
    @Mock
    private Future<?> future;

    private WebCrawlerServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(executorService.submit(any(Runnable.class))).thenAnswer(invocation -> future);
        service = new WebCrawlerServiceImpl(executorService, observer, ROOT, 0);
    }

    @Test
    void enqueueValidURLs_onlyEnqueuesSameHostHtmlPages() {
        ParseResult page = new ParseResult(ROOT, List.of(
                URI.create("https://monzo.com/about"),
                URI.create("https://facebook.com/monzo"),
                URI.create("https://community.monzo.com/t/hello"),
                URI.create("https://monzo.com/brand.pdf"),
                URI.create("https://www.monzo.com/careers")
        ));

        service.enqueueValidURLs(page);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService, times(2)).submit(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(WebRequestParser.class::isInstance));
        verify(observer, times(2)).incrementEnqueuedLinks();
    }

    @Test
    void claimRedirectHop_rejectsOtherSubdomainAndExternalHosts() {
        assertFalse(service.claimRedirectHop(URI.create("https://community.monzo.com/")));
        assertFalse(service.claimRedirectHop(URI.create("https://facebook.com/")));
        assertTrue(service.claimRedirectHop(URI.create("https://monzo.com/redirected")));
        assertFalse(service.claimRedirectHop(URI.create("https://monzo.com/redirected")));
    }

    @Test
    void claimRedirectHop_rejectsNonHtml() {
        assertFalse(service.claimRedirectHop(URI.create("https://monzo.com/file.pdf")));
    }

    @Test
    void maxPages_stopsFurtherEnqueues() {
        WebCrawlerServiceImpl limited =
                new WebCrawlerServiceImpl(executorService, observer, ROOT, 2);

        limited.enqueue(ROOT);
        limited.enqueue(URI.create("https://monzo.com/about"));
        limited.enqueue(URI.create("https://monzo.com/careers"));

        verify(executorService, times(2)).submit(any(Runnable.class));
        verify(observer, times(2)).incrementEnqueuedLinks();
    }

    @Test
    void processResults_doesNotEnqueueFromFailedPages() {
        service.processResults(new ParseResult(ROOT, "fetch failed"));

        verify(executorService, never()).submit(any(Runnable.class));
        verify(observer).incrementProcessedLinks();
    }

    @Test
    void processResults_enqueuesSameHostLinksFromSuccessfulPage() {
        ParseResult page = new ParseResult(ROOT, List.of(
                URI.create("https://monzo.com/about"),
                URI.create("https://facebook.com/monzo")
        ));

        service.processResults(page);

        verify(executorService, times(1)).submit(any(Runnable.class));
        verify(observer).incrementProcessedLinks();
    }

    @Test
    void start_enqueuesRoot() {
        service.start();

        verify(executorService, times(1)).submit(any(Runnable.class));
        verify(observer).incrementEnqueuedLinks();
    }

    @Test
    void enqueue_skipsDuplicates() {
        service.enqueue(URI.create("https://monzo.com/about"));
        service.enqueue(URI.create("https://www.monzo.com/about/"));

        verify(executorService, times(1)).submit(any(Runnable.class));
    }
}