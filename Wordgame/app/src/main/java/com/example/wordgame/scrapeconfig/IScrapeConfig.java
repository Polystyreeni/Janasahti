package com.example.wordgame.scrapeconfig;

public interface IScrapeConfig {
    public String getName();
    public String getBaseUrl();
    public String parseContent(String word);
}
