package com.skyflow.model;

public class Weather {
    private double windSpeed;       // in km/h
    private int windDirection;      // in degrees
    private double visibility;      // in meters
    private boolean precipitation;
    private String precipitationType; // rain, snow

    // Constructor
    public Weather(double windSpeed, int windDirection, double visibility) {
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.visibility = visibility;
        this.precipitation = false;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public int getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(int windDirection) {
        this.windDirection = windDirection;
    }

    public double getVisibility() {
        return visibility;
    }

    public void setVisibility(double visibility) {
        this.visibility = visibility;
    }

    public boolean isPrecipitation() {
        return precipitation;
    }

    public void setPrecipitation(boolean precipitation) {
        this.precipitation = precipitation;
    }

    public String getPrecipitationType() {
        return precipitationType;
    }

    public void setPrecipitationType(String precipitationType) {
        this.precipitationType = precipitationType;
        if (precipitationType != null && !precipitationType.isEmpty()) {
            this.precipitation = true;
        }
    }

    // Calculate the crosswind 
    public double calcCrosswind(int runwayHeading) {
        // Calculate the angle between wind direction and runway heading
        int angle = Math.abs(windDirection - runwayHeading);
        if (angle > 180) {
            angle = 360 - angle;
        }

        // Calculate the crosswind component using the sin of the angle
        return windSpeed * Math.sin(Math.toRadians(angle));
    }

    // Calculate the headwind
    public double calcHeadwind(int runwayHeading) {
        // Calculate the angle between wind direction and runway heading
        int angle = Math.abs(windDirection - runwayHeading);
        if (angle > 180) {
            angle = 360 - angle;
        }

        // Calculate the headwind component using the cos of the angle
        return windSpeed * Math.cos(Math.toRadians(angle));
    }



}
