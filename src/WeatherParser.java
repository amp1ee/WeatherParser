import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.*;

/**
 * Parsing
 * Created by djamp on 27.06.2017.
 */
public class WeatherParser {

    int connections;
    List<String> urls;

    public void parse(String toFile) throws IOException {
        List<Temperatures> temperaturesList = new ArrayList<>();
        List<Icons> iconsList = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);// the day of month
        String curDay = String.valueOf(dayOfMonth);

        if (Integer.parseInt(curDay) < 10) {
            curDay = "0" + curDay;
        }

        System.out.println("today " + curDay);

        String FILE_NAME = "Weather-links.txt";

        //new Icons("D:/Weather-links.txt"); // -- получить список названий осадков (Summary) на сайте

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(WeatherParser.class.getResourceAsStream(FILE_NAME)));
        String line;

        urls = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            urls.add(line);

        }
        reader.close();

        //connecting to cities' latest weather links and getting 'Document' entities;
        connections = 0;
        float percentage;
        String[] split;
        String maxDayT = null;
        String maxNightT = null;
        String minDayT = null;
        String minNightT = null;
        boolean catched;
        String dayIcon = null;
        String nightIcon = null;
        int domPos = 0;

        for (String url : urls) {
            split = url.split("/");
            FileSaveDialog.cururl.setText(split[4]);
            Document doc = Jsoup.connect(url).get();
            Element forecast = doc.getElementsByClass("forecasts").get(0); //1 - я таблица
            catched = false;
            Elements maxTempRows = forecast.children().get(0).getElementsByClass("max-temp-row");
            Elements minTempRows = forecast.children().get(0).getElementsByClass("min-temp-row");

            Elements daysOfMonth = forecast.getElementsByClass("dom")
                    .first().getElementsByClass("dom"); //число месяца в таблице на первом месте
            Elements tBody = forecast.getElementsByTag("tbody");
            Elements dayTime;
            try {
                dayTime = forecast.getElementsByClass("pname")
                        .first().getElementsByClass("pname"); //название времени суток в таблице на первом месте
            } catch (NullPointerException e) {
                dayTime = null;
                catched = true;
            }
            finally {
                if (catched) {
                    forecast = doc.getElementsByClass("long-range").get(0);
                    maxTempRows = forecast.children().get(0).getElementsByClass("max-temp-row");
                    minTempRows = forecast.children().get(0).getElementsByClass("min-temp-row");
                    daysOfMonth = forecast.getElementsByClass("dom")
                            .first().getElementsByClass("dom"); //число месяца в таблице на первом месте
                    dayTime = forecast.getElementsByClass("pname")
                            .first().getElementsByClass("pname");
                    tBody = forecast.getElementsByTag("tbody");
                }
            }

            int dayTimeCnt;
            int childNumber = 0;

            //смотрим позицию сегодняшней колонки в таблице
            for (Element dom : daysOfMonth) {
                String day = dom.text();
                if (day.equals(curDay)) {
                    domPos = 0;
                } else if (!day.equalsIgnoreCase(curDay))
                    domPos = 1;
            }
            //смотрим первый элемент с временем суток в таблице
            String time;
            if (dayTime != null)
                time = dayTime.text();
            else
                time = "Night";
            System.out.println(time);
            switch (time) {
                case "AM":
                    dayTimeCnt = 2;
                    break;
                case "PM":
                    dayTimeCnt = 1;
                    break;
                default:
                    dayTimeCnt = 0; //"Night"
                    break;
            }

            //определяем необходимый номер элемента в строке таблицы
            if (domPos == 0) {
                childNumber = dayTimeCnt + 3;
            }
            if (domPos == 1) {
                childNumber = dayTimeCnt + 6;
            }
            for (int k = 0; k < maxTempRows.size(); k++) {
                maxDayT = maxTempRows.get(k).child(childNumber)
                        .child(0).text();
                maxNightT = maxTempRows.get(k).child(childNumber + 1)
                        .child(0).text();
                minDayT = maxTempRows.get(k).child(childNumber - 1)
                        .child(0).text();
                minNightT = minTempRows.get(k).child(childNumber + 1)
                        .child(0).text();

            }

            if (maxNightT != null && minNightT != null) {
                Temperatures t = new Temperatures(minDayT, maxDayT, minNightT, maxNightT);
                t.adapt();
                temperaturesList.add(t);
            }

            for (Element row : tBody) {
                dayIcon = row.child(catched ? 6 : 5).child(childNumber).text();
                System.out.println(dayIcon);
                nightIcon = row.child(catched ? 6 : 5).child(childNumber + 1).text();
            }

            if (dayIcon != null && nightIcon != null)
                try {
                    iconsList.add(new Icons(dayIcon, nightIcon));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            connections++;
            percentage = (connections * 100 / urls.size());
            FileSaveDialog.progress.setValue((int) percentage);
            System.out.println("connected: " + url + " (" + connections + ")");
            domPos = 0;
        }

        new JsonExporter().save(temperaturesList, iconsList, toFile);

    }
}
