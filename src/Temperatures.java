import com.google.gson.annotations.SerializedName;

/**
*   Temperatures
*/

class Temperatures {
    @SerializedName("_day")
    private String              dayT;
    @SerializedName("_night")
    private String              nightT;
    private transient String    minDayT;
    private transient String    maxDayT;
    private transient String    minNightT;
    private transient String    maxNightT;

    Temperatures(String minDayT, String maxDayT, String minNightT, String maxNightT) {
        this.minDayT = minDayT;
        this.maxDayT = maxDayT;
        this.minNightT = minNightT;
        this.maxNightT = maxNightT;
        this.parseData();
    }

    private void parseData() {
        int minDay = 0;
        int maxDay = 0;
        int minNight = 0;
        int maxNight = 0;

        try {
            minDay = Integer.parseInt(this.minDayT);
            minNight = Integer.parseInt(this.minNightT);
            maxDay = Integer.parseInt(this.maxDayT);
            maxNight = Integer.parseInt(this.maxNightT);
        } catch (NumberFormatException e) {
            System.out.println("Failed to parse temperature: " + e.getMessage());
        }

        if (minDay > maxDay) {
            minDay = minDay ^ maxDay ^ (maxDay = minDay); // Swapping (minDay, maxDay) -> (maxDay, minDay)
            this.minDayT = String.valueOf(minDay);
            this.maxDayT = String.valueOf(maxDay);
        }

        if (minNight > maxNight) {
            minNight = minNight ^ maxNight ^ (maxNight = minNight);
            this.minNightT = String.valueOf(minNight);
            this.maxNightT = String.valueOf(maxNight);
        }

        if (minDay == maxDay) {
            minDay--;
            this.minDayT = String.valueOf(minDay);
        }
        if (minNight == maxNight) {
            minNight--;
            this.minNightT = String.valueOf(minNight);
        }


        if (minDay > 0) {
            this.minDayT = "+".concat(this.minDayT).concat("°");
        } else {
            this.minDayT = this.minDayT.concat("°");
        }

        if (maxDay > 0) {
            this.maxDayT = "+".concat(this.maxDayT).concat("°");
        } else {
            this.maxDayT = this.maxDayT.concat("°");

        }
        if (minNight > 0) {
            this.minNightT = "+".concat(this.minNightT).concat("°");
        } else {
            this.minNightT = this.minNightT.concat("°");
        }
        if (maxNight > 0) {
            this.maxNightT = "+".concat(this.maxNightT).concat("°");
        } else {
            this.maxNightT = this.maxNightT.concat("°");
        }

        this.dayT = this.minDayT+".."+this.maxDayT;
        this.nightT = this.minNightT + ".." + this.maxNightT;

    }

    @SuppressWarnings("all")
    public String getDayT() {
        return dayT;
    }
    @SuppressWarnings("all")
    public String getNightT() {
        return nightT;
    }

}
