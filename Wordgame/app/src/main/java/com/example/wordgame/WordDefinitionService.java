package com.example.wordgame;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WordDefinitionService {

    private static final String BASE_URL = "https://www.suomisanakirja.fi/";

    private static String definitionText = "";

    public static void getWordDefinition(String word) {
        Document document;

        Log.d("WordDefinitionService", "Starting fetch word definition");

        try {
            StringBuilder stringBuilder = new StringBuilder();
            String url = BASE_URL + word;
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

            stringBuilder.append(System.lineSeparator());
            stringBuilder.append(String.format("Määritelmän tarjosi: %s", BASE_URL));

            definitionText = stringBuilder.toString();
            // return stringBuilder.toString();
        }

        catch (Exception e) {
            e.printStackTrace();
            definitionText = "";
        }
    }

    public static String getDefinitionText() {
        return definitionText;
    }

}
