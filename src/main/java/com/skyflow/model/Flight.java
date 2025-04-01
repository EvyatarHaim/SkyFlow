package com.skyflow.model;

import java.time.LocalDateTime;

public class Flight {
    private String id;
    private String flightNumber;
    private String airline;
    private String aircraft;
    private WakeTurbulenceCategory category;
    private FlightType type;
    private LocalDateTime scheduledTime;
    private LocalDateTime actualTime;
    private EmergencyStatus emergencyStatus;
    private int fuelLevel; // Percentage of fuel remaining
    private Runway assignedRunway;
    private int priority; // Calculated priority value

    // Enum for wake turbulence categories
    public enum WakeTurbulenceCategory {
        LIGHT,      // Aircraft up to 7,000 kg
        MEDIUM,     // Aircraft between 7,000-136,000 kg
        HEAVY,      // Aircraft between 136,000-560,000 kg
        SUPER       // Aircraft above 560,000 kg
    }

    // Enum for flight types
    public enum FlightType {
        ARRIVAL,
        DEPARTURE
    }

    // Enum for emergency status with different levels
    public enum EmergencyStatus {
        NONE(0),
        MINOR_MECHANICAL(3),
        LOW_FUEL(4),
        MEDICAL(5),
        MAJOR_MECHANICAL(6),
        CRITICAL(7),
        VIP(2),
        GOVERNMENTAL(3);

        private final int priorityLevel;

        EmergencyStatus(int priorityLevel) {
            this.priorityLevel = priorityLevel;
        }

        public int getPriorityLevel() {
            return priorityLevel;
        }
    }

    // Constructor
    public Flight(String id, String flightNumber, String airline, String aircraft,
                  WakeTurbulenceCategory category, FlightType type,
                  LocalDateTime scheduledTime, EmergencyStatus emergencyStatus) {
        this.id = id;
        this.flightNumber = flightNumber;
        this.airline = airline;
        this.aircraft = aircraft;
        this.category = category;
        this.type = type;
        this.scheduledTime = scheduledTime;
        this.emergencyStatus = emergencyStatus;
        this.fuelLevel = 100; // Default value
        this.priority = calculatePriority();
    }

    // Calculate flight priority based on various factors
    public int calculatePriority() {
        int basePriority = 0;

        // Consider emergency status
        basePriority += emergencyStatus.getPriorityLevel() * 1000;

        // Consider fuel level for arrivals
        if (type == FlightType.ARRIVAL) {
            if (fuelLevel < 10) basePriority += 500;
            else if (fuelLevel < 20) basePriority += 300;
            else if (fuelLevel < 30) basePriority += 100;
        }

        // Consider how close the scheduled time is
        long minutesUntilScheduled = java.time.Duration.between(
                LocalDateTime.now(), scheduledTime).toMinutes();

        if (minutesUntilScheduled < 0) {
            // Flight is already delayed
            basePriority += Math.min(500, Math.abs(minutesUntilScheduled) * 10);
        } else if (minutesUntilScheduled < 30) {
            // Flight is coming up soon
            basePriority += Math.max(0, (30 - minutesUntilScheduled) * 5);
        }

        return basePriority;
    }

    // Update priority - should be called when relevant properties change
    public void updatePriority() {
        this.priority = calculatePriority();
    }

    public String getId() {
        return id;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getAirline() {
        return airline;
    }

    public String getAircraft() {
        return aircraft;
    }

    public WakeTurbulenceCategory getCategory() {
        return category;
    }

    public FlightType getType() {
        return type;
    }

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
        updatePriority();
    }

    public LocalDateTime getActualTime() {
        return actualTime;
    }

    public void setActualTime(LocalDateTime actualTime) {
        this.actualTime = actualTime;
    }

    public EmergencyStatus getEmergencyStatus() {
        return emergencyStatus;
    }

    public void setEmergencyStatus(EmergencyStatus emergencyStatus) {
        this.emergencyStatus = emergencyStatus;
        updatePriority();
    }

    public int getFuelLevel() {
        return fuelLevel;
    }

    public void setFuelLevel(int fuelLevel) {
        this.fuelLevel = Math.max(0, Math.min(100, fuelLevel)); // Keep between 0-100
        updatePriority();
    }

    public Runway getAssignedRunway() {
        return assignedRunway;
    }

    public void setAssignedRunway(Runway assignedRunway) {
        this.assignedRunway = assignedRunway;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return "Flight{" +
                "id='" + id + '\'' +
                ", flightNumber='" + flightNumber + '\'' +
                ", airline='" + airline + '\'' +
                ", type=" + type +
                ", scheduled=" + scheduledTime +
                ", actual=" + actualTime +
                ", emergency=" + emergencyStatus +
                ", runway=" + (assignedRunway != null ? assignedRunway.getId() : "none") +
                '}';
    }
}