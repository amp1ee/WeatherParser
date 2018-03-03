import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import weather.WeatherContainer;
import weather.Weather;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Export to .json
 */

class JsonExporter {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting().create();
    private String MAP_FILE = "cities_map.txt";
    private BufferedReader mapReader = new BufferedReader(
        new InputStreamReader(JsonExporter.class.getResourceAsStream(MAP_FILE)));
    private List<String> citiesList = new ArrayList<>();

    void save(List<Temperatures> tList, List<Icons> iList, String[] files, List<String> cities) {
        FileWriter writer = null;
        String ln;
        int amt = files.length;
        try {
            while ((ln = mapReader.readLine()) != null) {
                citiesList.add(ln);
            }
        } catch (IOException ioe) {
            System.err.println("IOE on opening cities_map.txt");
        } finally {
            try {
                mapReader.close();
            } catch (IOException ioe) {
                System.err.println("IOE on closing mapReader");
            }
        }
        int tListSz = tList.size();

        for (int j = 0; j < amt; j++) {
            WeatherContainer container = new WeatherContainer();
            try  {
                writer = new FileWriter(files[j]);
                for (int i = 0; i < (tListSz / amt); i++) {
                    Weather w = new Weather();
                    for (String row : citiesList) {
                        String curCity = row.split(" :")[0];
                        if (curCity.equals(cities.get(i)))
                            w.setCity(row.split(": ")[1]);
                    }
                    if (w.getCity() == null)
                        w.setCity(cities.get(i).toLowerCase());
                    Temperatures curTmp = tList.get(j + (amt * i));
                    w.setDayTemp(curTmp.getDayT());
                    w.setNightTemp(curTmp.getNightT());

                    Icons curIco = iList.get(j + (amt * i));
                    w.setDayIcon(curIco.getDayIcon());
                    w.setNightIcon(curIco.getNightIcon());
                    container.getWeather().add(w);
                }
                GSON.toJson(container, writer);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        writer.flush();
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    JsonExporter() {

    }
}
