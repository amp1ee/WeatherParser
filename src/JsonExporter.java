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
    private static Gson             GSON;
    private static final String     MAP_FILE = "cities_map.txt";
    private List<String>            citiesList;

    JsonExporter() {
        GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting().create();
        citiesList = new ArrayList<>();
    }

    void save(List<Temperatures> tempList, List<Icons> iconList, String[] files, List<String> cities) {
        final int           numFiles = files.length;
        final int           listSize = tempList.size();
        WeatherContainer    container;
        Weather             w;
        Temperatures        curTemp;
        Icons               curIcon;
        String              line;

        try (BufferedReader mapReader = new BufferedReader(new InputStreamReader
                (JsonExporter.class.getResourceAsStream(MAP_FILE)))) {
            while ((line = mapReader.readLine()) != null) {
                citiesList.add(line);
            }
        } catch (IOException ioe) {
            System.err.println("IOE on opening cities_map.txt");
        }

        for (int n = 0; n < numFiles; n++) {
            container = new WeatherContainer();
            try (FileWriter writer = new FileWriter(files[n])) {
                for (int i = 0; i < (listSize / numFiles); i++) {
                    w = new Weather();
                    for (String row : citiesList) {
                        String curCity = row.split(" :")[0];
                        if (curCity.equals(cities.get(i)))
                            w.setCity(row.split(": ")[1]);
                    }
                    if (w.getCity() == null)
                        w.setCity(cities.get(i).toLowerCase());
                    curTemp = tempList.get(n + (numFiles * i));
                    w.setDayTemp(curTemp.getDayT());
                    w.setNightTemp(curTemp.getNightT());

                    curIcon = iconList.get(n + (numFiles * i));
                    w.setDayIcon(curIcon.getDayIcon());
                    w.setNightIcon(curIcon.getNightIcon());
                    container.getWeather().add(w);
                }
                GSON.toJson(container, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
