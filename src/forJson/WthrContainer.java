package forJson;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class WthrContainer {
    @SerializedName("weather")
    @Expose
    private List<Wthr> wthrList = new ArrayList<>();
    public List<Wthr> getWthr() {
        return wthrList;
    }

}
