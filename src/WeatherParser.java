import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.logging.Formatter;

/**
 * Parsing
 * Created by djamp on 27.06.2017.
 */
public class WeatherParser {

    int connections;
    List<String> urls;
    private Document doc;

    public boolean parse(String toFile, String urls_lst) throws IOException {
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
        boolean catched;
        boolean failed = false;
        List<String> failList = new ArrayList<>();
        String dayIcon = null;
        String nightIcon = null;
        int domPos = 0;
        List<String> cities = new ArrayList<>();
        for (String url : urls) {
            split = url.split("/");
            String cityFromUrl = split.length > 4 ? split[4] : url;
            FileSaveDialog.cururl.setText(cityFromUrl);
            cities.add(cityFromUrl);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> future = executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        doc = Jsoup.connect(url).get();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            });

            executor.shutdown();
            try {
                future.get(11, TimeUnit.SECONDS);  //     <-- wait 8 seconds to finish
            } catch (InterruptedException e) {    //     <-- possible error cases
                System.out.println("job was interrupted");
            } catch (ExecutionException e) {
                System.out.println("caught exception: " + e.getCause());
            } catch (TimeoutException e) {
                future.cancel(true);              //     <-- interrupt the job
                failed = true;
                failList.add(url + " - [TIMEOUT]");
            }

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
                        failList.add(url + " - [PARSE ERROR]");
                    }
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
            domPos = 0;
        }
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
        new JsonExporter().save(temperaturesList, iconsList, toFile, cities);
        return failed;
    }
}

class LogFormatter extends Formatter {
    // Create a DateFormat to format the logger timestamp.
    private static final DateFormat df = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder(1000);
        builder.append(df.format(new Date(record.getMillis()))).append(" - ");
        builder.append("[").append(record.getLevel()).append("] - ");
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
