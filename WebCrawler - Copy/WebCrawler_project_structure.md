## Project structure
```text
src/main/java/org/monzo/webcrawler/
├── MainApplication.java
├── WebCrawlerApplication.java
├── core/
│   ├── WebCrawlerService.java
│   ├── WebCrawlerServiceImpl.java
│   └── WebEngineObserver.java
├── web/
│   ├── WebClient.java
│   └── WebRequestParser.java
├── utils/
│   └── URLFormatter.java
├── models/
│   ├── FetchResult.java
│   └── ParseResult.java
└── exception/
    ├── WebClientException.java
    ├── HttpStatusException.java
    ├── ClientErrorException.java
    ├── ServerErrorException.java
    └── InvalidInputURLException.java

src/test/java/org/monzo/webcrawler/
├── core/WebCrawlerServiceImplTest.java
├── utils/URLFormatterTest.java
├── web/WebClientTest.java
├── web/WebRequestParserTest.java
└── exception/