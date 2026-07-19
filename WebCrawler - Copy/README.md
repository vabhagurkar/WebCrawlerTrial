# Monzo Web crawler 

## Problem statement
We'd like you to write a simple web crawler in a programming language you're familiar with. Given a starting URL, the crawler should visit each URL it finds on the same domain. It should print each URL visited, and a list of links found on that page. The crawler should be limited to one subdomain - so when you start with https://monzo.com/, it would crawl all pages on the monzo.com website, but not follow external links, for example to facebook.com or community.monzo.com.

We would like to see your own implementation of a web crawler. Please do not use frameworks like scrapy or go-colly which handle all the crawling behind the scenes or someone else's code. You are welcome to use libraries to handle things like HTML parsing.

Ideally, write it as you would a production piece of code. This exercise is not meant to show us whether you can write code – we are more interested in how you design software. This means that we care less about a fancy UI or sitemap format, and more about how your program is structured: the trade-offs you've made, what behaviour the program exhibits, and your use of concurrency, test coverage, and so on.

## What is a web crawler?
This Web crawler is a program that traverses the web by downloading the web pages and following links from one page to another.
This web crawler starting from an initial URL will fetch and print all the URL links it can find in a webpage. Then it will enqueue and process the URLs not already visited, with the same domain 
  as the initial one.

## Notes on use of Cursor AI
While writing the code I made use of Cursor AI to review the code regularly to avoid NPE cases and infinite loop cases of redirects.
The Cursor AI was also used to ensure the code is accurate match to the requirements mentioned in the problem statement. The requirement mappings was also written with the suggestions of Cursor AI.

## Features
1. Starts from a user-supplied URL (adds "https://" if the scheme is missing)
2. If no url is provided a default URL "www.google.com" is set.
3. Fetches HTML pages concurrently using a fixed thread pool and virtual-thread workers.
4. Extracts links from "<a href>" elements (via [Jsoup](https://jsoup.org/))
5. **Follows only same-host links** (exact host after normalising `www.`) i.e. **Does not follow** other subdomains or external sites
6. Prints, for each processed page:
    - the visited URL
    - every link found on that page (including external ones that are not crawled)
7. Deduplicates URLs before enqueueing
8. Follows HTTP redirects manually, rejecting hops that leave the seed host
9. Optional **max-pages** cap to keep large sites (e.g. "abc.com") manageable.
10. Unit tests are written for URL rules, crawl enqueueing, redirects, and HTTP status errors.

## Architecture
Java 21 CLI Web Crawler: same domain Breadth First Search (BFS) over pages, virtual thread pool, observer pattern based completion.
Packages: 4
Worker concurrency: 20
Max page limit: 50
Runtime: Java 21
HTML parser: JSOUP
Please refer file: WebCrawler_architecture.png for more details

## Requirement mapping
| Requirement                            | How this project meets it |
| Start from a given URL                 | Interactive prompt in `WebCrawlerApplication` |
| Visit each URL found on the same domain| BFS-style queue in `WebCrawlerServiceImpl` |
| Print visited URL + links on that page | `printVisitedPage` in `WebCrawlerServiceImpl` |
| Stay on one subdomain                  | `URLFormatter.isSameHost` + redirect gating via `claimRedirectHop` |
| Do not follow `facebook.com`           | Filtered out at enqueue time (still listed under “Links found”) |
| Do not follow `community.monzo.com` when seed is `monzo.com` | Host must match exactly (after `www.` stripping) |
The String comparisons are case-insensitive.

## Project structure
Please refer file: WebCrawler_project_structure.md for more details

## Flowchart
1. User enters a seed URL (or presses Enter for the default).
2. The seed is validated/normalised and enqueued.
3. A worker fetches the page (following same-host redirects).
4. Links are extracted and printed with the visited URL.
5. Same-host HTML links not yet seen are enqueued.
6. When enqueued count equals processed count, the observer releases and the run completes.
Please refer file: WebCrawler_flowchart.png for more details

## Assumptions
These following assumptions are made (not all are stated explicitly in the requirement text):
1. **One subdomain means exact host match** — `monzo.com` is allowed i.e. "community.monzo.com" is not.
2. **`www.` is treated as the same host** — `www.monzo.com` and `monzo.com` are equivalent for crawling.
3. **Only `http` / `https` URLs** are accepted as seeds.
4. **Only HTML pages are crawled** — assets such as PDF, images, CSS, and JS are skipped (by path extension and/or `Content-Type`).
5. **Only `<a href>` links** are collected (not `<link>`, sitemaps, or script-discovered URLs).
6. **All links on a page are printed**; only same-host links are followed.
7. **Redirects that leave the seed host are rejected**.
8. **Visited state is in-memory** for a single process run.
9. **Max pages** (default `50`) is a safety limit for large sites. it can be set to `0` for unlimited.
10. **No politeness delay** in the current version (kept simple for this project).

## Error handling

| Situation                         | Behaviour |
| Invalid seed URL                  | Prompt again (`Invalid URL, please try again.`) |
| HTTP 4xx                          | `ClientErrorException`; page recorded as failed; crawl continues |
| HTTP 5xx                          | `ServerErrorException`; page recorded as failed; crawl continues |
| Off-host redirect                 | Fetch aborted for that chain; crawl continues with other URLs |
| Non-HTML `Content-Type`           | Skipped |
| Parse/fetch exception on one page | Logged/printed as failure; other workers keep running |

## Trade-offs and limitations
1. **No rate limiter** — parallel fetches are fast but can stress a target site; there is no per-host rate limit yet. (Concurrency vs politeness)
2. **Max pages** — The value is hardcoded to 50 practical for this project; can be set to 0 for a full same-host crawl. The other reason is : Some websites can throttle the request hence there needs 
3.    to be a limit on how many requests can be sent to the same domain.
4. **Max redirects** - The value is hardcoded to 1 but can be set to higher value like 5 for real life case.
5. **Concurrency level** - The value is hardcoded to 20 for this project demo but can be set to unlimited value for a full same-host crawl.
6. **Query strings are kept** — `?utm=...` URLs count as distinct pages.
7. **Fragments** — `#section` links may appear in printed output; normalisation does not always strip them before display.
8. **Executor lifecycle** — the pool is not shut down between interactive runs (Kept for this project i.e. for a simple CLI demo).
9. **In-memory visited set** — not suitable for distributed systems crawling.

## Potential improvements
1. For use in distributed system, data persistence in external memory can be helpful.
2. Max pages, Max redirects and concurrency level - the values can be configured to a real case value and can be separately configured in project example: puppet versus hardcoding them.
3. Strip URL fragments and de-duplicate links in printed output.
4. Align `www` stripping to a single rule everywhere (`www.` only)
5. Print successful visits only (or clearly mark failures)
6. Optional CLI flags for seed URL and max pages
7. Error handling can be improved by adding advanced, granular cases. For now, if an exception occurs the main thread is exited.
8. There are no metrics or monitoring added to this application and can be useful to have them.
9. Politeness delay / concurrency limit per host.
10. Shut down the `ExecutorService` when a crawl finishes.
11. Use of AI Cursor to add further improvements like - suggesting edge cases can help improve the project cases.

## How to run?
A. To run the application from the command line:
1. Unzip the project folder.
2. Open the command prompt.
3. Change the directory to a project path eg: C:\WebCrawler
4. On the command prompt run the command "> mvn clean install"
5. Change the directory to the target folder "> cd target"
6. Run the jar file generated by the maven "> java -jar WebCrawler-0.0.1-SNAPSHPT-jar-with-dependencies.jar"
7. On the console a welcome title "### WebCrawler 1.0 ###" and "Start - Type the initial URL (Type EXIT to stop)." will appear.
8. Enter the valid url value to work with the application.
9. Enter "Exit" to close the application.

B. To run the application from the IDE (Eg IntelliJ):
1. Unzip the project folder.
2. Open the IDE and import the project as Existing Maven Project File -> Import... -> Existing Maven Projects. Browse to the project directory.
3. Run the test cases (src\test\java\org\monzo\webcrawler) right click and 'Run tests in org.monzo.webcrawler'
4. Run the Maven sync and build -> Maven build -> specify 'clean install' as goal.
5. Run the application (src\main\java\org\monzo\webcrawler\MainApplication.java) Right click on MainApplication.java(main)
6. On the console a welcome title "### WebCrawler 1.0 ###" and "Start - Type the initial URL (Type EXIT to stop)." will appear.
7. Enter the valid url value to work with the application.
8. Enter "Exit" to close the application.