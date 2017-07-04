package forJson;

import com.google.gson.*;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class Deserializator {

    private static Gson gson = new Gson();
    private static List<String> regList = new ArrayList<>();

    public static List<String> deserialize(String jsonFilePath) throws FileNotFoundException {
        Reader input = new InputStreamReader(Deserializator.class.getResourceAsStream(jsonFilePath));
        WthrContainer mr = gson.fromJson(input, WthrContainer.class);
        List<Wthr> wthrList = mr.getWthr();
        for (Wthr w : wthrList) {
            regList.add(w.getCity());
        }

        return regList;
    }

}
