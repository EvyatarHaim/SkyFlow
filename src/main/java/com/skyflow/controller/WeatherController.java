package com.skyflow.controller;

import com.skyflow.model.Weather;
import com.skyflow.service.DatabaseService;

import java.util.List;
import java.util.Map;


public class WeatherController {
    private Weather currentWeather;
    private SchedulingController schedulingController;
    private DatabaseService databaseService;

    // Constructor with database service
    public WeatherController(SchedulingController schedulingController, DatabaseService databaseService) {
        this.schedulingController = schedulingController;
        this.databaseService = databaseService;

        // Initialize with default weather
        this.currentWeather = new Weather(
                12.5, 135, 25.0, Weather.WeatherCondition.SUNNY);

        // Update scheduling controller with default weather
        schedulingController.updateWeather(currentWeather);
    }

    // Load weather preset from database
    public void loadWeatherPreset(String presetName) {
        Map<String, Object> preset = databaseService.getWeatherPresetByName(presetName);

        if (preset != null) {
            double windSpeed = (double) preset.get("windSpeed");
            int windDirection = (int) preset.get("windDirection");
            double visibility = (double) preset.get("visibility");
            Weather.WeatherCondition condition = Weather.WeatherCondition.valueOf((String) preset.get("condition"));

            updateWeather(windSpeed, windDirection, visibility, condition);
        }
    }

    // Get all weather presets from database
    public List<Map<String, Object>> getAllWeatherPresets() {
        return databaseService.getAllWeatherPresets();
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