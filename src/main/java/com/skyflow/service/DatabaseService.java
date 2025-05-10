package com.skyflow.service;

import com.skyflow.controller.DatabaseController;
import com.skyflow.model.Flight;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DatabaseService {
    private final DatabaseController databaseController;
    private final Random random = new Random();
    private Map<String, Map<String, String>> airlineCache;
    private Map<String, Map<String, Object>> aircraftCache;

    // Constructor
    public DatabaseService(DatabaseController databaseController) {
        this.databaseController = databaseController;
        this.airlineCache = new HashMap<>();
        this.aircraftCache = new HashMap<>();

        // Pre-load some data for faster access
        preloadCommonData();
    }

    // Preload common data to improve performance
    private void preloadCommonData() {
        // Cache some common airlines by code
        List<Map<String, String>> airlines = databaseController.getAllAirlines();
        for (Map<String, String> airline : airlines) {
            String code = airline.get("code");
            airlineCache.put(code, airline);
        }

        System.out.println("Preloaded " + airlineCache.size() + " airlines into cache");
    }

    // Get airline name by code
    public String getAirlineNameByCode(String code) {
        // Check cache first
        if (airlineCache.containsKey(code)) {
            return airlineCache.get(code).get("name");
        }

        // Query database if not in cache
        Map<String, String> airline = databaseController.getAirlineByCode(code);
        if (airline != null) {
            // Add to cache
            airlineCache.put(code, airline);
            return airline.get("name");
        }

        return code + " Airlines"; // Default fallback
    }

    // Get airline by ICAO code from db
    public Map<String, String> getAirlineByCode(String icaoCode) {
        // Check cache first
        if (airlineCache.containsKey(icaoCode)) {
            return airlineCache.get(icaoCode);
        }

        // Query database if not in cache
        Map<String, String> airline = databaseController.getAirlineByCode(icaoCode);
        if (airline != null) {
            // Add to cache
            airlineCache.put(icaoCode, airline);
            return airline;
        }

        return null;
    }

    // Get aircraft details by name with caching
    public Map<String, Object> getAircraftByName(String name) {
        // Check cache first
        if (aircraftCache.containsKey(name)) {
            return aircraftCache.get(name);
        }

        // Query database if not in cache
        Map<String, Object> aircraft = databaseController.getAircraftByName(name);
        if (aircraft != null) {
            // Add to cache
            aircraftCache.put(name, aircraft);
            return aircraft;
        }

        return null;
    }

    // Get fuel capacity for an aircraft
    public int getAircraftFuelCapacity(String aircraftName) {
        Map<String, Object> aircraft = getAircraftByName(aircraftName);
        if (aircraft != null) {
            return (int) aircraft.get("fuelCapacity");
        }
        return 5000; // Default value if not found
    }

    // Get all aircraft of a specific category
    public List<Map<String, Object>> getAircraftByCategory(Flight.WakeTurbulenceCategory category) {
        return databaseController.getAircraftByCategory(category);
    }

    // Get a random aircraft from a specific category
    public Map<String, Object> getRandomAircraftByCategory(Flight.WakeTurbulenceCategory category) {
        List<Map<String, Object>> aircraftList = getAircraftByCategory(category);

        if (aircraftList != null && !aircraftList.isEmpty()) {
            // Pick a random aircraft from the list
            int randomIndex = random.nextInt(aircraftList.size());
            return aircraftList.get(randomIndex);
        }

        return null;
    }

}