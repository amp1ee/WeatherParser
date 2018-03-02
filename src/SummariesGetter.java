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

    Set<String> getAllSummaries(ArrayList<String> urls) throws IOException {
        String      mainClass = "b-forecast__table";
        List<Document> docs = new ArrayList<>();

        //connecting to cities' latest weather links and getting 'Document' entities;
        for (String url : urls) {
            docs.add(Jsoup.connect(url).get());
        }

        Set<String> summaries = new HashSet<>();
        List<Elements> elemsList = new ArrayList<>();


        for (Document doc : docs) {
            elemsList.add(doc.getElementsByClass( mainClass + "-summary"));

        }

        for (Elements elems : elemsList) {
            for (Element element : elems) {
                summaries.add(element.getElementsByTag("td").text());
            }
        }

        return summaries;

    }

}
