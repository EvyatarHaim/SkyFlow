package com.skyflow.model;

public class Weather {
    private double windSpeed; // In km/h
    private int windDirection; // In degrees (0-360)
    private double visibility; // In kilometers
    private WeatherCondition condition;

    // Enum for different weather conditions
    public enum WeatherCondition {
        SUNNY,
        CLOUDY,
        RAINY,
        SNOWY,
        FOGGY,
        THUNDERSTORM
    }

    // Constructor
    public Weather(double windSpeed, int windDirection, double visibility, WeatherCondition condition) {
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.visibility = visibility;
        this.condition = condition;
    }

    // Calculate headwind component for a given runway heading
    public double calculateHeadwind(int runwayHeading) {
        // Convert runway heading to radians
        double runwayRadians = Math.toRadians(runwayHeading);

        // Calculate the angle between wind direction and runway heading
        // Wind direction is where the wind is coming FROM
        double windRadians = Math.toRadians((windDirection + 180) % 360);

        // Calculate the component of wind along the runway
        return windSpeed * Math.cos(windRadians - runwayRadians);
    }

    // Calculate crosswind component for a given runway heading
    public double calculateCrosswind(int runwayHeading) {
        // Convert runway heading to radians
        double runwayRadians = Math.toRadians(runwayHeading);

        // Calculate the angle between wind direction and runway heading
        // Wind direction is where the wind is coming FROM
        double windRadians = Math.toRadians((windDirection + 180) % 360);

        // Calculate the component of wind perpendicular to the runway
        return windSpeed * Math.sin(windRadians - runwayRadians);
    }

    // Get a weather factor that affects separation times
    public double getWeatherFactor() {
        double factor = 1.0;

        // Adjust for visibility
        if (visibility < 1.0) {
            factor *= 2.0; // Severe visibility issues
        } else if (visibility < 3.0) {
            factor *= 1.5; // Moderate visibility issues
        } else if (visibility < 5.0) {
            factor *= 1.2; // Slight visibility issues
        }

        // Adjust for wind speed
        if (windSpeed > 30) {
            factor *= 1.5; // Strong winds
        } else if (windSpeed > 20) {
            factor *= 1.3; // Moderate winds
        } else if (windSpeed > 10) {
            factor *= 1.1; // Light winds
        }

        // Adjust for weather condition
        switch (condition) {
            case SUNNY:
                break;
            case CLOUDY:
                factor *= 1.1;
                break;
            case RAINY:
                factor *= 1.3;
                break;
            case SNOWY:
                factor *= 1.8;
                break;
            case FOGGY:
                factor *= 1.6;
                break;
            case THUNDERSTORM:
                factor *= 2.0;
                break;
        }

        return factor;
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

    public WeatherCondition getCondition() {
        return condition;
    }

    public void setCondition(WeatherCondition condition) {
        this.condition = condition;
    }

    @Override
    public String toString() {
        return "Weather{" +
                "wind=" + windSpeed + " km/h at " + windDirection + "°" +
                ", visibility=" + visibility + " km" +
                ", condition=" + condition +
                '}';
    }
}