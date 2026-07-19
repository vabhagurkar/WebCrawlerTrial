package org.monzo.webcrawler;

/**
 * The entry point of the WebCrawler project.
 * Delegates to {@WebCrawlerApplication}, which prompts for a seed URL, runs the crawls and loop until the user types
 * "Exit"
 */
public class MainApplication {
    public static void main(String[] args) {
        WebCrawlerApplication.instance().start();
    }
}