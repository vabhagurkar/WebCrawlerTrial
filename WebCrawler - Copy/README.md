# Monzo Web crawler 

## What is a web crawler?
A Web crawler is a program that traverses the web by downloading the web pages and following links from one page to another.
This web crawler starting from an initial URL will fetch and print all the URL links it can find in a webpage. Then it will enqueue and process the URLs not already visited, with the same domain as the initial one.

## What does the aim of this code? Features

## Architecture

## Setup and run the service
This service requires Java 21 to run.

Install Java 21
<<TO DO>>

## Usage and behavior
Once the application is started, it will ask for the initial URL.
If nothing is entered, the default URL -> https://www.google.com is used.
If the protocol is not specified, the default https is used.
If exit is typed, the application will terminate the execution.

## Components
Which pattern - observer and factory
Use of Cursor for reviews and exceptions 

## Assumptions
URL format

## Trade-offs



## Getting started

## How to run?

## Project status



## Noted and Improvements
The code is written in Java. Java 21 comes with Virtual threads which can be helpful in developing this application. 
Everything is persisted in the memory and it works as it is a single instance. For distributed system, to store information external persistence will be used.
To avoid concurrency the processing of parsed uses **synchornized** keyword on the method  which can be mitigated by using ExecutorService framweork and having more granular locks.
The HTTPClient uses the same threads of the ExecutorService. Some websites can throttle the request hence there needs to be a limit on how many requests can be sent to the same domain.
Error handling can be improved by adding advanced, granular cases. For now, if an exception occurs the correct error is printed but the main thread is exited
There are no metrics or monitoring added to this application and can be useful to have them.

