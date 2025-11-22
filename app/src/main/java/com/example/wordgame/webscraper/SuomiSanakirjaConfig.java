package com.example.wordgame.webscraper;

import android.app.Activity;
import android.util.Log;

import com.example.wordgame.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SuomiSanakirjaConfig implements IScrapeConfig {
    private static final String name = "suomisanakirja.fi";
    private static final String baseUrl = "https://www.suomisanakirja.fi/{word}";
    private static final int timeoutMs = 8000;

    private final String resourceText;
    private final String definitionNotFoundText;
    private final String fetchErrorText;

    public SuomiSanakirjaConfig(Activity callerActivity) {
        resourceText = callerActivity.getResources().getString(R.string.word_definition_reference, name);
        definitionNotFoundText = callerActivity.getResources().getString(R.string.word_definition_not_found);
        fetchErrorText = callerActivity.getResources().getString(R.string.error_word_fetch_failed);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public String parseContent(String word) {
        Document document;

        // TODO: Need to check if words with äö work with the url

        Log.d("WordDefinitionService", "Starting fetch word definition");

        try {
            StringBuilder stringBuilder = new StringBuilder();
            String url = baseUrl.replace("{word}", word);
            document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                    .header("Accept-Language", "*")
                    .timeout(timeoutMs)
                    .get();

            final Elements elements = document.select("ol");
            for (Element elem : elements) {
                for (Element wordElem : elem.select("li")) {
                    stringBuilder.append(wordElem.selectFirst("p"));
                    final Element exampleElement = wordElem.selectFirst("em");
                    if (exampleElement != null && !exampleElement.hasText()) {
                        stringBuilder.append(exampleElement);
                    }
                }
            }

            if (stringBuilder.length() < 1) {
                stringBuilder.append(definitionNotFoundText);
            } else {
                stringBuilder.append("<br><br>");
                stringBuilder.append(resourceText);
            }

            return stringBuilder.toString();
        }

        catch (Exception e) {
            e.printStackTrace();
            return String.format(fetchErrorText, e.getMessage());
        }
    }
}
