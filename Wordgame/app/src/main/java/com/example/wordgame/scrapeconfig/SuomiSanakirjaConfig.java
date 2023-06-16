package com.example.wordgame.scrapeconfig;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SuomiSanakirjaConfig implements IScrapeConfig {

    private String name = "suomisanakirja.fi";
    private String baseUrl = "https://www.suomisanakirja.fi/{word}";

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

        Log.d("WordDefinitionService", "Starting fetch word definition");

        try {
            StringBuilder stringBuilder = new StringBuilder();
            String url = baseUrl.replace("{word}", word);//baseUrl + word;
            document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                    .header("Accept-Language", "*")
                    .get();

            Elements elements = document.select("ol.fst");
            for(Element elem : elements) {
                for(Element wordElem : elem.select("li")) {
                    stringBuilder.append(wordElem.selectFirst("p"));
                }
            }

            stringBuilder.append("<br><br>");
            stringBuilder.append(String.format("Määritelmän tarjosi: %s", name));
            return stringBuilder.toString();
        }

        catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
