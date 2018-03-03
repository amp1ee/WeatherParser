package weather;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class WeatherContainer {
    @SerializedName("weather")
    @Expose
    private List<Weather> weatherList = new ArrayList<>();
    public List<Weather> getWeather() {
        return weatherList;
    }

}
