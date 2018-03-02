import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.file.NoSuchFileException;
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
    private List<String>            cities;
    private List<String>            failList;
    private Element                 tBody = null;
    private Elements                maxTempRows = null;
    private Elements                minTempRows = null;
    private Element                 date = null;
    private Element                 time = null;
    private static final String     mainClass = "b-forecast__table";
    private JFrame                  mainframe;

    boolean                     parse(String[] files, JFrame mainframe, String urls_lst) throws IOException {
        int         amt = files.length;
        boolean     success;

        // new Icons(urls_lst).print(); // If you want to get the list of all summaries (Summary) from the site
        connections = 0;
        iconsList = new ArrayList<>();
        failList = new ArrayList<>();
        temperaturesList = new ArrayList<>();
        this.mainframe = mainframe;
        urls = getUrlsList(urls_lst);
        success = processUrls(urls, amt);
        if (!success) {
            writeLog();
        }
        exportToJSON(files);
        return success;
    }

    private ArrayList<String>   getUrlsList(String urls_lst) {
        ArrayList<String>   urls = new ArrayList<>();
        String              line;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(urls_lst)))) {
             while ((line = reader.readLine()) != null)
                urls.add(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return urls;
    }

    private boolean             processUrls(List<String> urls, int amt) {
        boolean     success = true;
        int         domPos = 0;
        Calendar    cal = Calendar.getInstance();
        int         dayOfMonth = cal.get(Calendar.DAY_OF_MONTH); // the day of month
        String      curDay = String.valueOf(dayOfMonth);
        String[]    split;
        String      cityFromUrl;

        if (Integer.parseInt(curDay) < 10)
            curDay = "0" + curDay;
        cities = new ArrayList<>();

        // Connecting to cities' latest weather links and getting 'Document' entities;
        for (String url : urls) {
            split = url.split("/");
            cityFromUrl = split.length > 4 ? split[4] : url;
            FileSaveDialog.curUrl.setText(cityFromUrl);
            cities.add(cityFromUrl);
            if (!(connectToUrl(url)))
                success = false;
            try {
                Element table = doc.getElementsByClass(mainClass).first();
                tBody = table.getElementsByTag("tbody").first();
                Element tHead = table.getElementsByTag("thead").first();
                maxTempRows = tBody.getElementsByClass(mainClass + "-max-temperature");
                minTempRows = tBody.getElementsByClass(mainClass + "-min-temperature");
                date = tHead.getElementsByClass(mainClass + "-days-date").first();
                time = tHead.getElementsByClass(mainClass + "-value").first();
            } catch (IndexOutOfBoundsException iobe) {
                failList.add("PARSE ERROR - " + url);
                iobe.printStackTrace();
                success = false;
            }
            // Determining the position of our current local month date in the weather-table
            String day;

            if (date != null) {
                day = date.text();
                if (day.equals(curDay)) {
                    domPos = 0;
                } else if (!day.equals(curDay))
                    domPos = 1;
            }
            else
                success = false;

            // Identifying the first cell of the table's daytime ("AM" / "PM" / "Night") row;
            String      timeStr = null;

            if (time != null)
                timeStr = time.text();
            else
                success = false;
            if (timeStr != null) {
                int dayTimeCnt;

                switch (timeStr) {
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
                for (int i = 0; i < amt; i++) {
                    int childNumber = dayTimeCnt + 3 + (i * 3) + (domPos * 3);
                    addTemps(childNumber, maxTempRows, minTempRows);
                    addIcons(childNumber, tBody.getElementsByClass(mainClass + "-summary").first());
                }
            }
            else
                success = false;
            connections++;
            float percentage = (connections * 100 / urls.size());
            FileSaveDialog.progress.setValue((int) percentage);
            domPos = 0;
        }
        /* end of 'for' loop */
        return success;
    }

    private void                writeLog() throws IOException {
        Logger          logger = Logger.getLogger("wParser errors");
        FileHandler     fh;
        String          location;

        try {
            location = URLDecoder.decode(FileSaveDialog.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI().getPath(), "UTF-8");
            location = location.substring(0, location.lastIndexOf(File.separator));
            fh = new FileHandler(location + File.separator + "wparser.log", true);
            fh.setFormatter(new LogFormatter());
            logger.addHandler(fh);
        } catch (NullPointerException | URISyntaxException | NoSuchFileException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < failList.size(); i++) {
            logger.log(Level.SEVERE,failList.get(i) + System.lineSeparator());
            i++;
        }
        try {
            logger.getHandlers()[0].close();
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    private void                exportToJSON(String[] files) {
        new JsonExporter().save(temperaturesList, iconsList, files, cities);
    }

    private boolean             connectToUrl(String url) {
        Exception           exception;
        final int           timeout = 10;
        ExecutorService     executor = Executors.newSingleThreadExecutor();
        Future<?>           future = executor.submit(() -> {
            try {
                doc = Jsoup.connect(url).get();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        });
        executor.shutdown();
        exception = null;
        try {
            future.get(timeout, TimeUnit.SECONDS);  //     <-- wait  seconds to finish
        } catch (InterruptedException e) {
            exception = e;//     <-- possible error cases
            System.out.println("Thread future.get() was interrupted");
        } catch (ExecutionException e) {
            exception = e;
            failList.add("ERROR - " + e.getCause());
        } catch (TimeoutException e) {
            exception = e;
            future.cancel(true);              //     <-- interrupt the job
            failList.add("TIMEOUT - " + url);
        } finally {
            if (exception != null) {
                JOptionPane.showMessageDialog(mainframe, exception.getCause(), mainframe.getTitle()
                        + " - " + exception.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
            }
        }
        return (exception == null);
    }

    private void                addIcons(int childNumber, Element summary) {
        String dayIcon;
        String nightIcon;
        Elements row = summary.getElementsByTag("td");
        dayIcon = row.get(childNumber - 1).text();
        nightIcon = row.get(childNumber).text();

        if (dayIcon != null && nightIcon != null)
            iconsList.add(new Icons(dayIcon, nightIcon));
    }

    private void                addTemps(int childNumber, Elements maxTempRows, Elements minTempRows) {
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
        if (maxNightT != null && minNightT != null) {
            Temperatures t = new Temperatures(minDayT, maxDayT, minNightT, maxNightT);
            t.adapt();
            temperaturesList.add(t);
        }
    }
}

class LogFormatter extends Formatter {
    // Create a DateFormat to format the logger timestamp.
    private static final DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

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
