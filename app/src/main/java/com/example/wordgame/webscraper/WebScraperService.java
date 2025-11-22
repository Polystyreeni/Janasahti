package com.example.wordgame.webscraper;

import android.app.Activity;

import java.util.Collections;
import java.util.List;

public class WebScraperService {
    private final List<IScrapeConfig> scrapeConfigs;

    public WebScraperService(Activity caller) {
        scrapeConfigs = Collections.singletonList(new SuomiSanakirjaConfig(caller));
    }

    public String getWordDefinition(String word) {
        StringBuilder sb = new StringBuilder();
        for (IScrapeConfig config : scrapeConfigs) {
            sb.append(config.parseContent(word));
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }
}
