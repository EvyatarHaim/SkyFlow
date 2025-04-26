package com.skyflow.controller;

import com.skyflow.model.Flight;
import com.skyflow.service.DatabaseService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FlightController {
    private List<Flight> flights;
    private SchedulingController schedulingController;
    private DatabaseService databaseService;

    // Constructor with database service
    public FlightController(SchedulingController schedulingController, DatabaseService databaseService) {
        this.flights = new ArrayList<>();
        this.schedulingController = schedulingController;
        this.databaseService = databaseService;
    }

    // Create a new flight and add it to the system
    public Flight createFlight(String flightNumber, String airline, String aircraft,
                               Flight.WakeTurbulenceCategory category,
                               Flight.FlightType type,
                               LocalDateTime scheduledTime,
                               Flight.EmergencyStatus emergencyStatus) {
        // Generate a unique ID for the flight
        String id = UUID.randomUUID().toString();

        // Create new flight
        Flight flight = new Flight(id, flightNumber, airline, aircraft,
                category, type, scheduledTime, emergencyStatus);

        // Add to local list
        flights.add(flight);

        // Add to scheduling queue
        schedulingController.addFlight(flight);

        return flight;
    }


    // Update an existing flight
    public void updateFlight(Flight flight) {
        // The flight should update its priority automatically when properties change
        // Make sure scheduling controller is aware of changes
        schedulingController.scheduleFlights();
    }

    // Delete a flight
    public void deleteFlight(Flight flight) {
        flights.remove(flight);
    }

    // Set emergency status for a flight
    public void setEmergencyStatus(Flight flight, Flight.EmergencyStatus status) {
        flight.setEmergencyStatus(status);

        // Update flight
        updateFlight(flight);

        // If setting to emergency, trigger immediate rescheduling
        if (status != Flight.EmergencyStatus.NONE) {
            schedulingController.scheduleFlights();
        }
    }

    // Update fuel level for a flight
    public void updateFuelLevel(Flight flight, int fuelLevel) {
        flight.setFuelLevel(fuelLevel);

        // Update flight
        updateFlight(flight);

        // Low fuel might change priority significantly
        if (fuelLevel < 20) {
            schedulingController.scheduleFlights();
        }
    }

    // Get all flights
    public List<Flight> getAllFlights() {
        return new ArrayList<>(flights);
    }

    // Get flight by ID
    public Flight getFlightById(String id) {
        for (Flight flight : flights) {
            if (flight.getId().equals(id)) {
                return flight;
            }
        }
        return null;
    }

}