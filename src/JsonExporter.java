import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import forJson.Deserializator;
import forJson.WthrContainer;
import forJson.Wthr;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * export
 * Created by djamp on 28.06.2017.
 */
class JsonExporter {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting().create();

    //private List<Wthr> weather = new ArrayList<>();

    WthrContainer container = new WthrContainer();

    void save(List<Temperatures> tList, List<Icons> iList, String toFile) {
        FileWriter writer = null;
        try  {
            writer = new FileWriter(toFile);
            List<String> reg = Deserializator.deserealize("./src/regs.json");
            for (int i = 0; i< tList.size(); i++) {
                Wthr w = new Wthr();
                w.setCity(reg.get(i));

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
            if (writer!=null) {
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
