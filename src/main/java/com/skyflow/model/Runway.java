package com.skyflow.model;

import java.time.LocalDateTime;

public class Runway {
    private String id;
    private int heading; // Magnetic heading in degrees (0-360)
    private int length; // Length in meters
    private LocalDateTime nextAvailableTime; // When runway is next available
    private boolean active; // If the runway is operational

    // Constructor
    public Runway(String id, int heading, int length) {
        this.id = id;
        this.heading = heading;
        this.length = length;
        this.nextAvailableTime = LocalDateTime.now();
        this.active = true;
    }

    // Calculate runway score for a flight based on wind conditions
    public double calculateScore(Weather weather, Flight flight) {
        // Calculate headwind and crosswind components
        double headwindComponent = weather.calculateHeadwind(heading);
        double crosswindComponent = Math.abs(weather.calculateCrosswind(heading));

        // Base score starting point
        double score = 100.0;

        // Headwind is good (helps planes slow down/take off)
        score += headwindComponent * 2;

        // Crosswind is bad (makes landing/takeoff harder)
        score -= crosswindComponent * 3;

        // Consider runway length based on aircraft category
        // Larger aircraft need longer runways
        if (flight.getCategory() == Flight.WakeTurbulenceCategory.HEAVY ||
                flight.getCategory() == Flight.WakeTurbulenceCategory.SUPER) {
            if (length < 3000) {
                score -= 50; // Heavy penalty for short runway with large aircraft
            }
        }

        // Consider when the runway becomes available
        long minutesUntilAvailable = java.time.Duration.between(
                LocalDateTime.now(), nextAvailableTime).toMinutes();

        if (minutesUntilAvailable > 0) {
            score -= minutesUntilAvailable * 5; // Penalty for waiting
        }

        // If runway is not active, make score extremely negative
        if (!active) {
            score = -1000;
        }

        return score;
    }

    // Update the next available time based on a flight's operation
    public void updateNextAvailableTime(LocalDateTime operationTime,
                                        Flight.WakeTurbulenceCategory category,
                                        SafetySeparation safetyMatrix,
                                        Weather weather) {
        // Calculate base separation time from the safety matrix
        int separationTimeSeconds = safetyMatrix.getSeparationTimeSeconds(category);

        // Adjust for weather conditions
        double weatherFactor = 1.0;

        // Poor visibility increases separation time
        if (weather.getVisibility() < 5) {
            weatherFactor *= 1.5;
        }

        // Strong crosswinds increase separation time
        double crosswind = Math.abs(weather.calculateCrosswind(heading));
        if (crosswind > 15) {
            weatherFactor *= 1.3;
        }

        // Apply the weather factor to the separation time
        int adjustedSeparationSeconds = (int)(separationTimeSeconds * weatherFactor);

        // Set the next available time
        this.nextAvailableTime = operationTime.plusSeconds(adjustedSeparationSeconds);
    }

    public String getId() {
        return id;
    }

    public int getHeading() {
        return heading;
    }

    public int getLength() {
        return length;
    }

    public LocalDateTime getNextAvailableTime() {
        return nextAvailableTime;
    }

    public void setNextAvailableTime(LocalDateTime nextAvailableTime) {
        this.nextAvailableTime = nextAvailableTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "Runway{" +
                "id='" + id + '\'' +
                ", heading=" + heading +
                ", length=" + length +
                ", nextAvailable=" + nextAvailableTime +
                ", active=" + active +
                '}';
    }
}