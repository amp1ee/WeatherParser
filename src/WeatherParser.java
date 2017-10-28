import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.logging.Formatter;

public class WeatherParser {

    int connections;
    List<String> urls;
    private Document doc;

    public boolean parse(String[] files, String urls_lst) throws IOException {
        List<Temperatures> temperaturesList = new ArrayList<>();
        List<Icons> iconsList = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);// the day of month
        String curDay = String.valueOf(dayOfMonth);

        if (Integer.parseInt(curDay) < 10)
            curDay = "0" + curDay;
        //new Icons("D:/Weather-links.txt"); // -- получить список названий осадков (Summary) на сайте
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(urls_lst)));
        String line;

        urls = new ArrayList<>();
        while ((line = reader.readLine()) != null)
            urls.add(line);
        reader.close();

        //connecting to cities' latest weather links and getting 'Document' entities;
        connections = 0;
        float percentage;
        String[] split;
        String maxDayT = null;
        String maxNightT = null;
        String minDayT = null;
        String minNightT = null;
        boolean caught;
        boolean failed = false;
        List<String> failList = new ArrayList<>();
        String dayIcon = null;
        String nightIcon = null;
        int domPos = 0; // Позиция текущего числа месяца в таблице
        List<String> cities = new ArrayList<>();
        for (String url : urls) {
            split = url.split("/");
            String cityFromUrl = split.length > 4 ? split[4] : url;
            FileSaveDialog.cururl.setText(cityFromUrl);
            cities.add(cityFromUrl);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> future = executor.submit(() -> {
                try {
                    doc = Jsoup.connect(url).get();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            });
            executor.shutdown();
            try {
                future.get(15, TimeUnit.SECONDS);  //     <-- wait  seconds to finish
            } catch (InterruptedException e) {    //     <-- possible error cases
                System.out.println("job was interrupted");
            } catch (ExecutionException e) {
                System.out.println("caught exception: " + e.getCause());
            } catch (TimeoutException e) {
                future.cancel(true);              //     <-- interrupt the job
                failed = true;
                failList.add("TIMEOUT - " + url);
            }

            Element forecast0 = doc.getElementsByClass("forecasts").get(0); // 1-я таблица
            Element forecast1 = doc.getElementsByClass("forecasts").get(1); // 2-я таблица
            caught = false;
            Elements maxTempRows0 = forecast0.children().get(0).getElementsByClass("max-temp-row");
            Elements minTempRows0 = forecast0.children().get(0).getElementsByClass("min-temp-row");
            Elements maxTempRows1 = forecast1.children().get(0).getElementsByClass("max-temp-row");
            Elements minTempRows1 = forecast1.children().get(0).getElementsByClass("min-temp-row");

            Elements daysOfMonth0 = forecast0.getElementsByClass("dom")
                    .first().getElementsByClass("dom"); //число месяца в таблице на первом месте
            Elements daysOfMonth1 = forecast1.getElementsByClass("dom")
                    .first().getElementsByClass("dom"); //число месяца в таблице на первом месте
            Elements tBody0 = forecast0.getElementsByTag("tbody");
            Elements tBody1 = forecast1.getElementsByTag("tbody");
            Elements dayTime0;
            Elements dayTime1;
            try {
                dayTime0 = forecast0.getElementsByClass("pname")
                        .first().getElementsByClass("pname"); //название времени суток в таблице на первом месте
            } catch (NullPointerException e) {
                dayTime0 = null;
                caught = true;
            }
            /* //finally
            finally {
                if (caught) {
                    try {
                        forecast = doc.getElementsByClass("long-range").get(0);
                        maxTempRows = forecast.children().get(0).getElementsByClass("max-temp-row");
                        minTempRows = forecast.children().get(0).getElementsByClass("min-temp-row");
                        daysOfMonth = forecast.getElementsByClass("dom")
                                .first().getElementsByClass("dom"); //число месяца в таблице на первом месте
                        dayTime = forecast.getElementsByClass("pname")
                                .first().getElementsByClass("pname");
                        tBody = forecast.getElementsByTag("tbody");
                    } catch (IndexOutOfBoundsException iobe) {
                        failed = true;
                        failList.add("PARSE ERROR - " + url);
                    }
                }
            }
            */
            int dayTimeCnt;
            int childNumber = 0;
            //смотрим позицию сегодняшней колонки в таблице
            for (Element dom : daysOfMonth0) {
                String day = dom.text();
                if (day.equals(curDay)) {
                    domPos = 0;
                } else if (!day.equalsIgnoreCase(curDay))
                    domPos = 1;
            }
            //смотрим первый элемент с временем суток в таблице
            String time;
            if (dayTime0 != null)
                time = dayTime0.text();
            else
                time = "Night";
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
            //Завтра, послезавтра, ...
            for (int i = 0; i < 3; i++) {
                //определяем необходимый номер ячейки в строке таблицы (maxNightT)
                childNumber = dayTimeCnt + 3 + (i * 3) + (domPos * 3);

                maxDayT = (childNumber <= 9) ? maxTempRows0.get(0).child(childNumber)
                        .child(0).text()     : maxTempRows1.get(0).child(childNumber % 9)
                        .child(0).text();
                maxNightT = ((childNumber + 1) <= 9) ? maxTempRows0.get(0).child(childNumber + 1)
                        .child(0).text()             : maxTempRows1.get(0).child((childNumber + 1) % 9)
                        .child(0).text();
                minDayT = ((childNumber - 1) <= 9) ? maxTempRows0.get(0).child(childNumber - 1)
                        .child(0).text()           : maxTempRows1.get(0).child((childNumber - 1) % 9)
                        .child(0).text();
                minNightT = ((childNumber + 1) <= 9) ? minTempRows0.get(0).child(childNumber + 1)
                        .child(0).text()             : minTempRows1.get(0).child((childNumber + 1) % 9)
                        .child(0).text();

                /*
                System.out.println("Weather for tomorrow + " + i);
                System.out.println(cityFromUrl + "'s maxDayT: " + maxDayT);
                System.out.println(cityFromUrl + "'s minDayT: " + minDayT);
                System.out.println(cityFromUrl + "'s maxNightT: " + maxNightT);
                System.out.println(cityFromUrl + "'s minNightT: " + minNightT);
                */
                if (maxNightT != null && minNightT != null) {
                    Temperatures t = new Temperatures(minDayT, maxDayT, minNightT, maxNightT);
                    t.adapt();
                    temperaturesList.add(t);
                }

                Element row0 = tBody0.last();
                Element row1 = tBody1.last();

                dayIcon = (childNumber <= 9) ? row0.child(caught ? 6 : 5).child(childNumber).text()
                                             : row1.child(caught ? 6 : 5).child(childNumber % 9).text();
                nightIcon = ((childNumber + 1) <= 9) ? row0.child(caught ? 6 : 5).child(childNumber + 1).text()
                                                     : row1.child(caught ? 6 : 5).child((childNumber + 1) % 9).text();

                if (dayIcon != null && nightIcon != null)
                    try {
                        iconsList.add(new Icons(dayIcon, nightIcon));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }

            connections++;
            percentage = (connections * 100 / urls.size());
            FileSaveDialog.progress.setValue((int) percentage);
            domPos = 0;
        }
        /* end of 'for' loop */

        if (failed) {
            Logger logger = Logger.getLogger("WParser errors");
            FileHandler fh;
            try {
                String location = FileSaveDialog.class.getProtectionDomain().getCodeSource()
                        .getLocation().toURI().getPath();
                fh = new FileHandler(location.substring(1, location.length()) + ".log", true);
                fh.setFormatter(new LogFormatter());
                logger.addHandler(fh);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            int i = 0;
            while (i < failList.size()) {
                logger.log(Level.SEVERE,failList.get(i) + System.lineSeparator());
                i++;
            }
            logger.getHandlers()[0].close();
        }
        new JsonExporter().save(temperaturesList, iconsList, files, cities);
        return failed;
    }
}

class LogFormatter extends Formatter {
    // Create a DateFormat to format the logger timestamp.
    private static final DateFormat df = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder(1000);
        builder.append(df.format(new Date(record.getMillis()))).append(" - ");
        builder.append(formatMessage(record));
        builder.append("\n");
        return builder.toString();
    }

    public String getHead(Handler h) {
        return super.getHead(h);
    }

    public String getTail(Handler h) {
        return super.getTail(h);
    }
}
