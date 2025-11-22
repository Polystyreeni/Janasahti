package com.example.wordgame.webscraper;

public interface IScrapeConfig {
    String getName();
    String getBaseUrl();
    String parseContent(String word);
}
