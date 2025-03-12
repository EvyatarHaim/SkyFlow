package com.skyflow;

import com.skyflow.model.Flight;
import com.skyflow.model.Runway;
import com.skyflow.model.Weather;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class Scheduling {
    private DepartureManager departureManager;
    private List<Flight> scheduledFlights;
    private List<Runway> runways;
    private Weather currentWeather;

    // Constructor
    public Scheduling() {
        this.runways = initRunways();
        this.currentWeather = new Weather(10, 270, 10000); // 10 km/h, west, good visibility
        this.departureManager = new DepartureManager(runways, currentWeather);
        this.scheduledFlights = new ArrayList<>();
    }


     // Creates a list of default runways for testing
    private List<Runway> initRunways() {
        List<Runway> runways = new ArrayList<>();
        runways.add(new Runway("36L", 360, 3500)); // North-facing, 3500m
        runways.add(new Runway("36R", 360, 3000)); // North-facing, 3000m
        runways.add(new Runway("09", 90, 2800));  // East-facing, 2800m
        return runways;
    }

    // Adds a flight to be scheduled
    public void addFlight(Flight flight) {
        departureManager.addFlight(flight);
    }


    // Updates the current weather conditions.
    public void updateWeather(Weather weather) {
        this.currentWeather = weather;
        departureManager.updateWeather(weather);
    }

    public List<Flight> runScheduling() {
        List<Flight> newlyScheduledFlights = departureManager.scheduleFlights();
        scheduledFlights.addAll(newlyScheduledFlights);
        return newlyScheduledFlights;
    }

    public List<Flight> getAllScheduledFlights() {
        return new ArrayList<>(scheduledFlights);
    }

    public List<Flight> getQueuedFlights() {
        return departureManager.getDepartureQueue();
    }

    public DepartureManager getDepartureManager() {
        return departureManager;
    }

    public List<Runway> getRunways() {
        return new ArrayList<>(runways);
    }

    public Weather getCurrentWeather() {
        return currentWeather;
    }

    // Adds a test flight to the system.
    public void addTestFlight(String flightId, String aircraftType, int category,
                              LocalDateTime departureTime, String destination) {
        Flight flight = new Flight(flightId, aircraftType, category, departureTime, destination);
        addFlight(flight);
    }

    public void setRunwayAvailability(String runwayId, boolean available) {
        departureManager.setRunwayAvailability(runwayId, available);
    }
}