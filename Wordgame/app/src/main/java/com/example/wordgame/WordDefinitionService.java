package com.example.wordgame;

import android.util.Log;

import com.example.wordgame.scrapeconfig.IScrapeConfig;
import com.example.wordgame.scrapeconfig.SuomiSanakirjaConfig;

import java.util.Collections;
import java.util.List;

public class WordDefinitionService {
    private static String definitionText = "";

    private static List<IScrapeConfig> scraperConfigs = Collections.singletonList(
            new SuomiSanakirjaConfig()
    );

    public static void getWordDefinition(String word) {
        StringBuilder sb = new StringBuilder();
        for(IScrapeConfig config : scraperConfigs) {
            sb.append(config.parseContent(word));
            sb.append(System.lineSeparator());
        }

        definitionText = sb.toString();
    }

    public static String getDefinitionText() {
        return definitionText;
    }

}
