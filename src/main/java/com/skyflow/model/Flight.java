package com.skyflow.model;

import java.time.LocalDateTime;

public class Flight {
    private String flightId;
    private String aircraftType;
    private int wakeTurbulenceCategory;  // 1-Light, 2-Medium, 3-Heavy, 4-Super
    private LocalDateTime scheduledDepartureTime;
    private LocalDateTime actualDepartureTime;
    private String destination;
    private boolean isEmergency;
    private int priority; // 0-Normal, 1-High, 2-Emergency
    private String assignedRunway;

    // Constructor
    public Flight(String flightId, String aircraftType, int wakeTurbulenceCategory,
                  LocalDateTime scheduledDepartureTime, String destination) {
        this.flightId = flightId;
        this.aircraftType = aircraftType;
        this.wakeTurbulenceCategory = wakeTurbulenceCategory;
        this.scheduledDepartureTime = scheduledDepartureTime;
        this.destination = destination;
        this.isEmergency = false;
        this.priority = 0;
    }

    public String getFlightId() {
        return flightId;
    }

    public void setFlightId(String flightId) {
        this.flightId = flightId;
    }

    public String getAircraftType() {
        return aircraftType;
    }

    public void setAircraftType(String aircraftType) {
        this.aircraftType = aircraftType;
    }

    public int getWakeTurbulenceCategory() {
        return wakeTurbulenceCategory;
    }

    public void setWakeTurbulenceCategory(int wakeTurbulenceCategory) {
        this.wakeTurbulenceCategory = wakeTurbulenceCategory;
    }

    public LocalDateTime getScheduledDepartureTime() {
        return scheduledDepartureTime;
    }

    public void setScheduledDepartureTime(LocalDateTime scheduledDepartureTime) {
        this.scheduledDepartureTime = scheduledDepartureTime;
    }

    public LocalDateTime getActualDepartureTime() {
        return actualDepartureTime;
    }

    public void setActualDepartureTime(LocalDateTime actualDepartureTime) {
        this.actualDepartureTime = actualDepartureTime;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public boolean isEmergency() {
        return isEmergency;
    }

    public void setEmergency(boolean emergency) {
        isEmergency = emergency;
        if (emergency) {
            this.priority = 2;
        }
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getAssignedRunway() {
        return assignedRunway;
    }

    public void setAssignedRunway(String assignedRunway) {
        this.assignedRunway = assignedRunway;
    }

    @Override
    public String toString() {
        return "Flight{" +
                "flightId='" + flightId + '\'' +
                ", aircraftType='" + aircraftType + '\'' +
                ", scheduledDepartureTime=" + scheduledDepartureTime +
                ", actualDepartureTime=" + actualDepartureTime +
                ", assignedRunway='" + assignedRunway + '\'' +
                '}';
    }
}
