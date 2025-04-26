package com.skyflow.model;

public class Weather {
    private double windSpeed; // km/h
    private int windDirection; // degrees
    private double visibility; // kilometers
    private WeatherCondition condition;

    // Enum for different weather conditions
    public enum WeatherCondition {
        SUNNY(1.0),
        CLOUDY(1.1),
        RAINY(1.3),
        SNOWY(1.8),
        FOGGY(1.6),
        THUNDERSTORM(2.0);

        private final double separationMultiplier;

        WeatherCondition(double separationMultiplier) {
            this.separationMultiplier = separationMultiplier;
        }

        public double getSeparationMultiplier() {
            return separationMultiplier;
        }
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
    // Calculate a combined weather factor for separation times
    public double getWeatherFactor() {
        return calculateVisibilityFactor()
                * calculateWindSpeedFactor()
                * condition.getSeparationMultiplier();
    }

    // Determine visibility-based multiplier
    private double calculateVisibilityFactor() {
        if (visibility < 1.0) return 2.0;
        if (visibility < 3.0) return 1.5;
        if (visibility < 5.0) return 1.2;
        return 1.0;
    }

    // Determine wind-speed–based multiplier
    private double calculateWindSpeedFactor() {
        if (windSpeed > 30) return 1.5;
        if (windSpeed > 20) return 1.3;
        if (windSpeed > 10) return 1.1;
        return 1.0;
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