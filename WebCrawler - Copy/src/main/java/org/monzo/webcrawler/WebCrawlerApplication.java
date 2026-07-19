package org.monzo.webcrawler;

import org.monzo.webcrawler.core.WebCrawlerService;
import org.monzo.webcrawler.core.WebCrawlerServiceImpl;
import org.monzo.webcrawler.core.WebEngineObserver;
import org.monzo.webcrawler.utils.URLFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Scanner;

/**
 * The interactive CLI for WebCrawler.
 * Prompts for a seed URL, starts and runs the same-host crawls and loop until the user types
 * "Exit"
 */
public class WebCrawlerApplication {

    private static final Logger log = LoggerFactory.getLogger(WebCrawlerApplication.class);
    //The DEFAULT_URL is used when user inputs blank line.
    private static final String DEFAULT_URL = "https://www.google.com";

    //Factory style constructor
    public static WebCrawlerApplication instance() {
        return new WebCrawlerApplication();
    }

    public void start() {
        displayTitleMessage();

        while(true) {
            displayStartMessage();
            Scanner scanner = new Scanner(System.in);

            String input = scanner.nextLine();
            log.debug(input);

            if("exit".equalsIgnoreCase(input)) {
                displayGoodByeMessage();
                return;
            }

            try {
                URI uri = formatInput(input);
                System.out.println("URI formed is: " + uri.toString());
                Instant startTime = Instant.now();

                //Creates the crawler for the seed and starts the first enqueue
                WebCrawlerService webCrawlerService = WebCrawlerServiceImpl.create(uri);
                WebEngineObserver observer = webCrawlerService.start();

                //Blocks until enqueued==processed
                observer.awaitTermination();

                Instant endTime = Instant.now();
                System.out.println();
                System.out.printf("Completed the task in %d seconds.", Duration.between(startTime, endTime).getSeconds());

            } catch(MalformedURLException mfe) {
                System.out.println("Invalid URL, please try again.");

            } catch (InterruptedException ie) {
                log.error("Thread interrupted: {}", ie.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    private URI formatInput(String input) throws MalformedURLException {
        if(input == null || input.isBlank()) {
            System.out.println("Falling back to default url: " + DEFAULT_URL);
            return URI.create(DEFAULT_URL);
        }
        return new URLFormatter().parseAndValidateURL(input);
    }

    private void displayTitleMessage() {
        System.out.printf("%n");
        System.out.println("### WebCrawler 1.0 ###");
    }

    private void displayStartMessage() {
        System.out.printf("%n");
        System.out.println("Start - Type the initial URL (Type EXIT to stop).");
    }

    private void displayGoodByeMessage() {
        System.out.printf("%n");
        System.out.println("Goodbye.");
    }
}