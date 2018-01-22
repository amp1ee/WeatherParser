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

class WeatherParser {

    int                             connections;
    List<String>                    urls;
    private Document                doc;
    private List<Temperatures>      temperaturesList;
    private List<Icons>             iconsList;
    private final String            mainClass = "b-forecast__table";

    boolean parse(String[] files, String urls_lst) throws IOException {
        Elements dayTime;
        Element table;
        Element tBody = null;
        Element tHead;
        Elements maxTempRows = null;
        Elements minTempRows = null;
        Element date; //число месяца в таблице на первом месте
        Element time = null;

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

        boolean failed          = false;
        List<String> failList   = new ArrayList<>();

        //connecting to cities' latest weather links and getting 'Document' entities;
        connections = 0;
        int domPos = 0; // Позиция текущего числа месяца в таблице
        List<String> cities = new ArrayList<>();
        iconsList = new ArrayList<>();
        temperaturesList = new ArrayList<>();
        for (String url : urls) {

            String[] split = url.split("/");
            String cityFromUrl = split.length > 4 ? split[4] : url;
            FileSaveDialog.curUrl.setText(cityFromUrl);
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
                future.get(3, TimeUnit.SECONDS);  //     <-- wait  seconds to finish
            } catch (InterruptedException e) {    //     <-- possible error cases
                System.out.println("job was interrupted");
            } catch (ExecutionException e) {
                System.out.println("caught exception: " + e.getCause());
            } catch (TimeoutException e) {
                future.cancel(true);              //     <-- interrupt the job
                failed = true;
                failList.add("TIMEOUT - " + url);
            }

            try {
                table = doc.getElementsByClass(mainClass).first(); // 1-я таблица
                tBody = table.getElementsByTag("tbody").first();
                tHead = table.getElementsByTag("thead").first();
                maxTempRows = tBody.getElementsByClass(mainClass + "-max-temperature");
                minTempRows = tBody.getElementsByClass(mainClass + "-min-temperature");
                date = tHead.getElementsByClass(mainClass + "-days-date").first(); //число месяца в таблице на первом месте
                time = tHead.getElementsByClass(mainClass + "-value").first();
            } catch (IndexOutOfBoundsException iobe) {
                failed = true;
                failList.add("PARSE ERROR - " + url);
                iobe.printStackTrace();
                return false;
            }
            //смотрим позицию сегодняшней колонки в таблице
            String day = date.text();
            if (day.equals(curDay)) {
                domPos = 0;
            } else if (!day.equalsIgnoreCase(curDay))
                domPos = 1;
            //смотрим первый элемент с временем суток в таблице
            String timeS;
            if (time != null)
                timeS = time.text();
            else
                return false;
            int dayTimeCnt;
            switch (timeS) {
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

            for (int i = 0; i < 4; i++) {
                int childNumber = dayTimeCnt + 3 + (i * 3) + (domPos * 3);                                              /* определяем необходимый номер ячейки в строке таблицы (maxNightT) */
                addTemps(childNumber, maxTempRows, minTempRows);
                addIcons(childNumber, tBody.getElementsByClass(mainClass + "-summary").first());
            }
            connections++;
            float percentage = (connections * 100 / urls.size());
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

    private void addIcons(int childNumber, Element summary) {
        String dayIcon;
        String nightIcon;
        Elements row = summary.getElementsByTag("td");
        dayIcon = row.get(childNumber - 1).text();
        nightIcon = row.get(childNumber).text();

        if (dayIcon != null && nightIcon != null)
            try {
                iconsList.add(new Icons(dayIcon, nightIcon));
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private void addTemps(int childNumber, Elements maxTempRows, Elements minTempRows) {
        String maxDayT;
        String minDayT;
        String maxNightT;
        String minNightT;

        maxDayT = maxTempRows.first().child(childNumber)
                .child(0).text();
        maxNightT = maxTempRows.first().child(childNumber + 1)
                .child(0).text();
        minDayT = maxTempRows.first().child(childNumber - 1)
                .child(0).text();
        minNightT = minTempRows.first().child(childNumber + 1)
                .child(0).text();

//                System.out.println("Weather for tomorrow + " + i);
//                System.out.println(cityFromUrl + "'s maxDayT: " + maxDayT);
//                System.out.println(cityFromUrl + "'s minDayT: " + minDayT);
//                System.out.println(cityFromUrl + "'s maxNightT: " + maxNightT);
//                System.out.println(cityFromUrl + "'s minNightT: " + minNightT);
        if (maxNightT != null && minNightT != null) {
            Temperatures t = new Temperatures(minDayT, maxDayT, minNightT, maxNightT);
            t.adapt();
            temperaturesList.add(t);
        }
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
