import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Icons
 * (Precipitation class)
 */
class Icons {

    @SerializedName("_icon_day")
    private String                      dayIcon;
    @SerializedName("_icon_night")
    private String                      nightIcon;
    private transient SummariesGetter   sg = new SummariesGetter();
    private transient Set<String>       summaries = new HashSet<>();

    private void adaptNames(String origDayIcon, String origNightIcon) {

        if (origDayIcon.contains("rain")) {
            this.dayIcon = "icon_rain";
        } else
        if (origDayIcon.equals("some clouds")) {
            this.dayIcon = "icon_sun_n_clouds";
        } else
        if (origDayIcon.equals("cloudy")) {
            this.dayIcon="icon_clouds";
        } else
        if (origDayIcon.equals("risk tstorm")) {
            this.dayIcon="icon_thunderstorm";
        } else
        if (origDayIcon.equals("clear")) {
            this.dayIcon = "icon_sun";
        } else
        if (origDayIcon.equals("light snow")) {
            this.dayIcon = "icon_rain+snow";
        } else
        if (origDayIcon.contains("snow")) {
            this.dayIcon = "icon_snow";
        }
        else
            this.dayIcon = "icon_sun_n_clouds";

        if (origNightIcon.contains("rain")) {
            this.nightIcon = "icon_rain";
        } else
        if (origNightIcon.equals("some clouds")) {
            this.nightIcon = "icon_cloud_moon";
        } else
        if (origNightIcon.equals("cloudy")) {
            this.nightIcon="icon_clouds";
        } else
        if (origNightIcon.equals("risk tstorm")) {
            this.nightIcon="icon_thunderstorm";
        } else
        if (origNightIcon.equals("clear")) {
            this.nightIcon = "icon_moon";
        } else
        if (origNightIcon.equals("light snow")) {
                this.nightIcon="icon_rain+snow";
        } else
        if (origNightIcon.contains("snow")) {
            this.nightIcon = "icon_snow";
        }
        else
            this.nightIcon = "icon_cloud_moon";
    }

    private void collect(String FILE_NAME) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(FILE_NAME)));
        ArrayList<String> urls = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            urls.add(line);
        }
        summaries = sg.getAllSummaries(urls);
    }

    public void print() {
        if (!summaries.isEmpty())
            for (String s : summaries) {
                System.out.println(s);
            }
        else {
            System.out.println("empty summaries");
        }
    }

    Icons(String FILE_NAME) throws IOException {
        collect(FILE_NAME);
    }

    Icons(String dayIcon, String nightIcon) {
        adaptNames(dayIcon, nightIcon);
    }

    String getDayIcon() {
        return dayIcon;
    }

    String getNightIcon() {
        return nightIcon;
    }
}
