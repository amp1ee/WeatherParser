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

        System.out.println("today "+curDay);

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

        List<Document> docs = new ArrayList<>();

        //connecting to cities' latest weather links and getting 'Document' entities;
        connections = 0;
        float percentage;
        String[] split;

        for (String url : urls) {
            split = url.split("/");
            FileSaveDialog.cururl.setText(split[4]);
            docs.add(Jsoup.connect(url).get());
            connections++;
            percentage = (connections * 100/ urls.size());
            System.out.println(percentage);
            FileSaveDialog.progress.setValue((int)percentage);
            System.out.println("connected: " + url + " ("+connections+")");
        }

        List<Element> elementsList = new ArrayList<>();

        for (Document doc : docs) {

            Element forecast = doc.getElementsByClass("forecasts").first();
            elementsList.add(forecast);

        }

        String maxDayT = null;
        String maxNightT = null;
        String minDayT = null;
        String minNightT = null;

        String dayIcon = null;
        String nightIcon = null;

        int domPos = 0;

        for (Element elements : elementsList) {    //elementsList -- список html-элементов с классом "forecasts" из ссылок

            for (Element element : elements.children()) {      //elements -- элементы на html-странице

                Elements maxEs = element.getElementsByClass("max-temp-row");
                Elements minEs = element.getElementsByClass("min-temp-row");
                Elements daysOfMonth = element.getElementsByClass("dom")
                        .first().getElementsByClass("dom"); //число месяца в таблице на первом месте

                Elements dayTime;
                dayTime = element.getElementsByClass("pname")
                            .first().getElementsByClass("pname"); //название времени суток в таблице на первом месте

                Elements tBody = element.getElementsByTag("tbody");


                int dayTimeCnt;
                int childNumber = 0;

                //смотрим позицию сегодняшней колонки в таблице
                for (Element dom : daysOfMonth) {
                    String day = dom.text();
                    System.out.println("Date: "+day);
                    if (day.equals(curDay)) {
                        domPos = 0;
                    } else if (!day.equalsIgnoreCase(curDay))
                        domPos = 1;

                }
                //смотрим первый элемент с временем суток в таблице
                String time = dayTime.text();
                //System.out.println(time);
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
                    System.out.println("childNumber "+ childNumber);
                }
                if (domPos == 1) {
                    childNumber = dayTimeCnt + 6;
                }


                //System.out.println("childNumber "+childNumber + ": weekCnt " + weekCnt);

                    for (int i = 0; i < maxEs.size(); i++) {
                            maxDayT = maxEs.get(i).child(childNumber)
                                    .child(0).text();
                            maxNightT = maxEs.get(i).child(childNumber + 1)
                                    .child(0).text();
                            minDayT = maxEs.get(i).child(childNumber -1)
                                .child(0).text();
                            minNightT = minEs.get(i).child(childNumber + 1)
                                .child(0).text();
                    }

                    if (maxNightT != null && minNightT != null) {
                        Temperatures t = new Temperatures(minDayT, maxDayT, minNightT, maxNightT);
                        t.adapt();
                        temperaturesList.add(t);
                    }


                    for (Element row : tBody) {
                        dayIcon = row.child(5).child(childNumber).text();
                        nightIcon = row.child(5).child(childNumber + 1).text();
                    }

                if (dayIcon != null && nightIcon != null)
                    iconsList.add(new Icons(dayIcon, nightIcon));
            }

            domPos = 0;
        }

        /*for (Temperatures tmps : temperaturesList) {
            System.out.println(tmps.getDayT() + " " + tmps.getNightT());
        }

        for (Icons icons : iconsList) {
            System.out.println(icons.getDayIcon()+ " " + icons.getNightIcon());
        }*/

        JsonExporter exporter = new JsonExporter();

        exporter.save(temperaturesList, iconsList, toFile);

    }

}


