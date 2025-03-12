package com.skyflow.model;

import java.time.LocalDateTime;

public class Runway {
    private String runwayId;
    private int heading; // in degrees
    private int length ; // in meters
    private boolean isAvailable;
    private LocalDateTime availableFrom; // Next available time

    // Constructor
    public Runway(String runwayId, int heading, int length) {
        this.runwayId = runwayId;
        this.heading = heading;
        this.length = length;
        this.isAvailable = true;
        this.availableFrom = LocalDateTime.now();
    }

    public String getRunwayId() {
        return runwayId;
    }

    public void setRunwayId(String runwayId) {
        this.runwayId = runwayId;
    }

    public int getHeading() {
        return heading;
    }

    public void setHeading(int heading) {
        this.heading = heading;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public LocalDateTime getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalDateTime availableFrom) {
        this.availableFrom = availableFrom;
    }

    @Override
    public String toString() {
        return "Runway{" +
                "runwayId='" + runwayId + '\'' +
                ", heading=" + heading +
                ", length=" + length +
                ", isAvailable=" + isAvailable +
                ", availableFrom=" + availableFrom +
                '}';
    }
}
