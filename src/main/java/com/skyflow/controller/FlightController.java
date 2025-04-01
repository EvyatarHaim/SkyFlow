package com.skyflow.controller;

import com.skyflow.model.Flight;
import com.skyflow.util.DatabaseController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class FlightController {
    private List<Flight> flights;
    private DatabaseController dbController;
    private SchedulingController schedulingController;

    // Constructor
    public FlightController(SchedulingController schedulingController,
                            DatabaseController dbController) {
        this.flights = new ArrayList<>();
        this.schedulingController = schedulingController;
        this.dbController = dbController;

        // Load flights from database if available
        if (dbController != null) {
            loadFlightsFromDatabase();
        }
    }

    // Generate a set of test flights for system validation
    public List<Flight> generateTestFlights() {
        List<Flight> testFlights = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now();

        // Create a mix of different flight types and emergency statuses
        Flight[] flightData = {
                // Emergency Flights
                createFlight("EL001", "Emergency Airlines", "Boeing 737",
                        Flight.WakeTurbulenceCategory.MEDIUM, Flight.FlightType.ARRIVAL,
                        baseTime.plusMinutes(10), Flight.EmergencyStatus.MEDICAL),

                createFlight("EL002", "State Carrier", "Airbus A320",
                        Flight.WakeTurbulenceCategory.MEDIUM, Flight.FlightType.DEPARTURE,
                        baseTime.plusMinutes(15), Flight.EmergencyStatus.LOW_FUEL),

                // Normal Flights with different categories
                createFlight("FL101", "Global Airways", "Boeing 747",
                        Flight.WakeTurbulenceCategory.HEAVY, Flight.FlightType.ARRIVAL,
                        baseTime.plusMinutes(20), Flight.EmergencyStatus.NONE),

                createFlight("FL102", "Sky Transport", "Airbus A380",
                        Flight.WakeTurbulenceCategory.SUPER, Flight.FlightType.DEPARTURE,
                        baseTime.plusMinutes(25), Flight.EmergencyStatus.NONE),

                createFlight("FL103", "Regional Express", "Embraer E175",
                        Flight.WakeTurbulenceCategory.LIGHT, Flight.FlightType.ARRIVAL,
                        baseTime.plusMinutes(30), Flight.EmergencyStatus.NONE),

                // Multiple flights close to each other
                createFlight("FL201", "International Lines", "Boeing 777",
                        Flight.WakeTurbulenceCategory.HEAVY, Flight.FlightType.ARRIVAL,
                        baseTime.plusMinutes(35), Flight.EmergencyStatus.NONE),

                createFlight("FL202", "National Carrier", "Airbus A330",
                        Flight.WakeTurbulenceCategory.HEAVY, Flight.FlightType.DEPARTURE,
                        baseTime.plusMinutes(40), Flight.EmergencyStatus.NONE),

                createFlight("FL203", "Budget Airlines", "Boeing 737",
                        Flight.WakeTurbulenceCategory.MEDIUM, Flight.FlightType.ARRIVAL,
                        baseTime.plusMinutes(45), Flight.EmergencyStatus.NONE),

                // More variety of flights
                createFlight("FL301", "Cargo Express", "Boeing 747 Freighter",
                        Flight.WakeTurbulenceCategory.HEAVY, Flight.FlightType.ARRIVAL,
                        baseTime.plusMinutes(50), Flight.EmergencyStatus.NONE),

                createFlight("FL302", "VIP Transport", "Gulfstream G650",
                        Flight.WakeTurbulenceCategory.LIGHT, Flight.FlightType.DEPARTURE,
                        baseTime.plusMinutes(55), Flight.EmergencyStatus.VIP)
        };

        // Set random fuel levels for variety
        Random random = new Random();
        for (Flight flight : flightData) {
            flight.setFuelLevel(random.nextInt(50) + 50); // Random fuel between 50-100%
            testFlights.add(flight);
        }

        return testFlights;
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

        // Save to database if available
        if (dbController != null) {
            dbController.saveFlight(flight);
        }

        return flight;
    }

    // Update an existing flight
    public void updateFlight(Flight flight) {
        // Update in database if available
        if (dbController != null) {
            dbController.updateFlight(flight);
        }

        // The flight should update its priority automatically when properties change
        // Make sure scheduling controller is aware of changes
        schedulingController.scheduleFlights();
    }

    // Delete a flight
    public void deleteFlight(Flight flight) {
        flights.remove(flight);

        // Remove from database if available
        if (dbController != null) {
            dbController.deleteFlight(flight);
        }
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

    // Load flights from database
    private void loadFlightsFromDatabase() {
        List<Flight> loadedFlights = dbController.loadAllFlights();

        if (loadedFlights != null) {
            flights.addAll(loadedFlights);

            // Add all loaded flights to scheduling controller
            for (Flight flight : loadedFlights) {
                schedulingController.addFlight(flight);
            }
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