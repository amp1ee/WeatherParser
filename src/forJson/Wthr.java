package forJson;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Wthr {
    @SerializedName("reg")
    @Expose
    private String city;
    @SerializedName("_day")
    @Expose
    private String dayTemp;
    @SerializedName("_night")
    @Expose
    private String nightTemp;
    @SerializedName("_icon_day")
    @Expose
    private String dayIcon;
    @SerializedName("_icon_night")
    @Expose
    private String nightIcon;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDayTemp() {
        return dayTemp;
    }

    public void setDayTemp(String dayTemp) {
        this.dayTemp = dayTemp;
    }

    public String getNightTemp() {
        return nightTemp;
    }

    public void setNightTemp(String nightTemp) {
        this.nightTemp = nightTemp;
    }

    public String getDayIcon() {
        return dayIcon;
    }

    public void setDayIcon(String dayIcon) {
        this.dayIcon = dayIcon;
    }

    public String getNightIcon() {
        return nightIcon;
    }

    public void setNightIcon(String nightIcon) {
        this.nightIcon = nightIcon;
    }
}
