package forJson;

import com.google.gson.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class Deserializator {

    private static Gson gson = new Gson();
    private static List<String> regList = new ArrayList<>();

    public static void main(String[] args) {

    }

    public static List<String> deserealize(String jsonFilePath) throws FileNotFoundException {
        FileReader input = new FileReader(jsonFilePath);
        WthrContainer mr = gson.fromJson(input, WthrContainer.class);
        List<Wthr> wthrList = mr.getWthr();
        for (Wthr w : wthrList) {
            regList.add(w.getCity());
        }

        return regList;
    }

}
