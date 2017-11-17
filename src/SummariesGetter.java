import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *  To get all possible weather conditions from the site.
 */
class SummariesGetter {

    private List<String> urls = new ArrayList<>();
    Set<String> getAllSummaries(String FILE_NAME) throws IOException {

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(FILE_NAME)));

        String line;
        while ((line = reader.readLine()) != null) {
            urls.add(line);
        }


        List<Document> docs = new ArrayList<>();

        //connecting to cities' latest weather links and getting 'Document' entities;
        for (String url : urls) {
            docs.add(Jsoup.connect(url).get());
        }

        Set<String> summaries = new HashSet<>();
        List<Elements> elemsList = new ArrayList<>();


        for (Document doc : docs) {
            elemsList.add(doc.getElementsByClass("wphrase"));

        }

        for (Elements elems : elemsList) {
            for (Element element : elems) {
                summaries.add(element.getElementsByTag("b").text());
            }
        }

        return summaries;

    }

}
