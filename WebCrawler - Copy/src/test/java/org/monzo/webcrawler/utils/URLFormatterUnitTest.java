package org.monzo.webcrawler.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class URLFormatterUnitTest {

    private URLFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new URLFormatter();
    }

    @Test
    void parseAndValidateURLAddsHttpsWhenMissing() throws Exception {
        assertEquals(URI.create("https://monzo.com"), formatter.parseAndValidateURL("monzo.com"));
    }

    @Test
    void parseAndValidateURLRejectsBlank() {
        assertThrows(MalformedURLException.class, () -> formatter.parseAndValidateURL("  "));
    }

    @Test
    void normaliseURL_stripsWwwAndTrailingSlash() {
        URI normalised = formatter.normaliseURL(URI.create("https://www.Monzo.com/about/"));
        assertEquals(URI.create("https://monzo.com/about"), normalised);
    }

    @Test
    void normaliseURLReturnsNullForRelativeUri() {
        assertNull(formatter.normaliseURL(URI.create("/relative")));
    }

    @Test
    void isSameHostAllowsWwwVariantOfSeed() {
        URI seed = URI.create("https://monzo.com/");
        assertTrue(formatter.isSameHost(URI.create("https://www.monzo.com/about"), seed));
        assertTrue(formatter.isSameHost(URI.create("https://monzo.com/careers"), seed));
    }

    @Test
    void isSameHostRejectsExternalAndOtherSubdomains() {
        URI seed = URI.create("https://monzo.com/");
        assertFalse(formatter.isSameHost(URI.create("https://facebook.com/monzo"), seed));
        assertFalse(formatter.isSameHost(URI.create("https://community.monzo.com/"), seed));
        assertFalse(formatter.isSameHost(URI.create("https://support.abc.com/"), URI.create("https://abc.com/")));
    }

    @Test
    void isHtmlResourceSkipsAssets() {
        assertTrue(formatter.isHtmlResource(URI.create("https://monzo.com/about")));
        assertTrue(formatter.isHtmlResource(URI.create("https://monzo.com/")));
        assertFalse(formatter.isHtmlResource(URI.create("https://monzo.com/logo.png")));
        assertFalse(formatter.isHtmlResource(URI.create("https://monzo.com/docs/terms.pdf")));
        assertFalse(formatter.isHtmlResource(URI.create("https://monzo.com/app.js")));
    }

    @Test
    void checkProtocolAndAddLeavesExistingScheme() {
        assertEquals("http://example.com", formatter.checkProtocolAndAdd("http://example.com"));
        assertEquals("https://example.com", formatter.checkProtocolAndAdd("example.com"));
    }
}

