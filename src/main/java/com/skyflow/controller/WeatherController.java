// Manages weather-related operations and updates
package com.skyflow.controller;

import com.skyflow.model.Weather;
import com.skyflow.util.DatabaseController;

public class WeatherController {
    private Weather currentWeather;
    private DatabaseController dbController;
    private SchedulingController schedulingController;

    // Constructor
    public WeatherController(SchedulingController schedulingController,
                             DatabaseController dbController) {
        this.schedulingController = schedulingController;
        this.dbController = dbController;

        // Initialize with default weather
        this.currentWeather = new Weather(
                5.0, 0, 10.0, Weather.WeatherCondition.SUNNY);

        // Load weather from database if available
        if (dbController != null) {
            loadWeatherFromDatabase();
        } else {
            // Update scheduling controller with default weather
            schedulingController.updateWeather(currentWeather);
        }
    }

    // Update weather conditions
    public void updateWeather(double windSpeed, int windDirection,
                              double visibility, Weather.WeatherCondition condition) {

        Weather newWeather = new Weather(windSpeed, windDirection, visibility, condition);
        this.currentWeather = newWeather;

        // Update in scheduling controller
        schedulingController.updateWeather(newWeather);

        // Save to database if available
        if (dbController != null) {
            dbController.saveWeather(newWeather);
        }

        // Trigger rescheduling as weather conditions changed
        schedulingController.scheduleFlights();
    }

    // Update specific aspects of weather
    public void updateWindSpeed(double windSpeed) {
        currentWeather.setWindSpeed(windSpeed);

        // Update in scheduling controller
        schedulingController.updateWeather(currentWeather);

        // Save to database if available
        if (dbController != null) {
            dbController.saveWeather(currentWeather);
        }

        // Trigger rescheduling
        schedulingController.scheduleFlights();
    }

    public void updateWindDirection(int windDirection) {
        currentWeather.setWindDirection(windDirection);

        // Update in scheduling controller
        schedulingController.updateWeather(currentWeather);

        // Save to database if available
        if (dbController != null) {
            dbController.saveWeather(currentWeather);
        }

        // Trigger rescheduling
        schedulingController.scheduleFlights();
    }

    public void updateVisibility(double visibility) {
        currentWeather.setVisibility(visibility);

        // Update in scheduling controller
        schedulingController.updateWeather(currentWeather);

        // Save to database if available
        if (dbController != null) {
            dbController.saveWeather(currentWeather);
        }

        // Trigger rescheduling
        schedulingController.scheduleFlights();
    }

    public void updateWeatherCondition(Weather.WeatherCondition condition) {
        currentWeather.setCondition(condition);

        // Update in scheduling controller
        schedulingController.updateWeather(currentWeather);

        // Save to database if available
        if (dbController != null) {
            dbController.saveWeather(currentWeather);
        }

        // Trigger rescheduling
        schedulingController.scheduleFlights();
    }

    // Load weather from database
    private void loadWeatherFromDatabase() {
        Weather loadedWeather = dbController.loadWeather();

        if (loadedWeather != null) {
            this.currentWeather = loadedWeather;

            // Update scheduling controller
            schedulingController.updateWeather(currentWeather);
        }
    }

    // Get current weather
    public Weather getCurrentWeather() {
        return currentWeather;
    }
}