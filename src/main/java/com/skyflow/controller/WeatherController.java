package com.skyflow.controller;

import com.skyflow.model.Weather;

public class WeatherController {
    private Weather currentWeather;
    private SchedulingController schedulingController;

    // Constructor
    public WeatherController(SchedulingController schedulingController) {
        this.schedulingController = schedulingController;

        // Initialize with default weather
        this.currentWeather = new Weather(
                12.5, 135, 25.0, Weather.WeatherCondition.SUNNY);

        // Update scheduling controller with default weather
        schedulingController.updateWeather(currentWeather);
    }

    // Update weather conditions
    public void updateWeather(double windSpeed, int windDirection,
                              double visibility, Weather.WeatherCondition condition) {
        Weather newWeather = new Weather(windSpeed, windDirection, visibility, condition);
        this.currentWeather = newWeather;

        // Update in scheduling controller
        schedulingController.updateWeather(newWeather);

        // Trigger rescheduling as weather conditions changed
        schedulingController.scheduleFlights();
    }

    // Update specific aspects of weather
    public void updateWindSpeed(double windSpeed) {
        currentWeather.setWindSpeed(windSpeed);

        // Update in scheduling controller
        schedulingController.updateWeather(currentWeather);

        // Trigger rescheduling
        schedulingController.scheduleFlights();
    }

    public void updateWindDirection(int windDirection) {
        currentWeather.setWindDirection(windDirection);

        // Update in scheduling controller
        schedulingController.updateWeather(currentWeather);

        // Trigger rescheduling
        schedulingController.scheduleFlights();
    }

    public void updateVisibility(double visibility) {
        currentWeather.setVisibility(visibility);

        // Update in scheduling controller
        schedulingController.updateWeather(currentWeather);

        // Trigger rescheduling
        schedulingController.scheduleFlights();
    }

    public void updateWeatherCondition(Weather.WeatherCondition condition) {
        currentWeather.setCondition(condition);

        // Update in scheduling controller
        schedulingController.updateWeather(currentWeather);

        // Trigger rescheduling
        schedulingController.scheduleFlights();
    }

    // Get current weather
    public Weather getCurrentWeather() {
        return currentWeather;
    }
}