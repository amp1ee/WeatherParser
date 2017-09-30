import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import forJson.Deserializator;
import forJson.WthrContainer;
import forJson.Wthr;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * export
 */

class JsonExporter {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting().create();
    WthrContainer container = new WthrContainer();
    private String MAP_FILE = "cities_map.txt";
    private BufferedReader mapReader = new BufferedReader(
        new InputStreamReader(JsonExporter.class.getResourceAsStream(MAP_FILE)));
    private List<String> citiesList = new ArrayList<>();

    void save(List<Temperatures> tList, List<Icons> iList, String toFile, List<String> cities) {
        FileWriter writer = null;
        String ln = null;
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
        try  {
            writer = new FileWriter(toFile);
            for (int i = 0; i < tList.size(); i++) {
                Wthr w = new Wthr();
                for (int j = 0; j < citiesList.size(); j++)
                {
                    String row = citiesList.get(j);
                    String curCity = row.split(" :")[0];
                    if (curCity.equals(cities.get(i)))
                        w.setCity(row.split(": ")[1]);
                }
                if (w.getCity() == null)
                    w.setCity(cities.get(i).toLowerCase());
                Temperatures curTmp = tList.get(i);
                w.setDayTemp(curTmp.getDayT());
                w.setNightTemp(curTmp.getNightT());

                Icons curIco = iList.get(i);
                w.setDayIcon(curIco.getDayIcon());
                w.setNightIcon(curIco.getNightIcon());
                container.getWthr().add(w);
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

    JsonExporter() {

    }
}
