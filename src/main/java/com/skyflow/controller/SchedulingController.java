package com.skyflow.controller;

import com.skyflow.model.*;
import com.skyflow.util.FlightComparator;

import java.time.LocalDateTime;
import java.util.*;

public class SchedulingController {
    private PriorityQueue<Flight> flightQueue;
    private List<Flight> scheduledFlights;
    private List<Runway> runways;
    private Weather currentWeather;
    private SafetySeparation safetyMatrix;

    // Constructor
    public SchedulingController() {
        this.flightQueue = new PriorityQueue<>(new FlightComparator());
        this.scheduledFlights = new ArrayList<>();
        this.runways = new ArrayList<>();
        this.safetyMatrix = new SafetySeparation();

        // Initialize with default weather
        this.currentWeather = new Weather(
                5.0, 0, 10.0, Weather.WeatherCondition.SUNNY);
    }

    // Add a flight to the scheduling queue
    public void addFlight(Flight flight) {
        flightQueue.offer(flight);
    }

    // Add a runway to the available runways
    public void addRunway(Runway runway) {
        runways.add(runway);
    }

    // Update weather conditions
    public void updateWeather(Weather weather) {
        this.currentWeather = weather;
    }

    // Run the scheduling algorithm
    public List<Flight> scheduleFlights() {
        // Clear previously scheduled flights
        scheduledFlights.clear();

        // Create a copy of the priority queue to work with
        PriorityQueue<Flight> workingQueue = new PriorityQueue<>(flightQueue);

        while (!workingQueue.isEmpty()) {
            // Get the flight with highest priority
            Flight currentFlight = workingQueue.poll();

            // Handle emergency flights with special processing
            if (currentFlight.getEmergencyStatus() != Flight.EmergencyStatus.NONE) {
                handleEmergencyFlight(currentFlight);
                continue;
            }

            // Find the best runway for this flight
            Runway bestRunway = selectBestRunway(currentFlight);

            if (bestRunway != null) {
                // Calculate the earliest possible operation time
                LocalDateTime earliestTime = calculateEarliestTime(currentFlight, bestRunway);

                // Check for conflicts with already scheduled flights
                List<Flight> conflicts = checkForConflicts(earliestTime, bestRunway);

                if (!conflicts.isEmpty()) {
                    // Handle conflicts - might result in adjusting earliestTime
                    earliestTime = resolveConflicts(currentFlight, conflicts, earliestTime);
                }

                // Assign runway and time to the flight
                currentFlight.setAssignedRunway(bestRunway);
                currentFlight.setActualTime(earliestTime);

                // Update runway's next available time
                bestRunway.updateNextAvailableTime(
                        earliestTime,
                        currentFlight.getCategory(),
                        safetyMatrix,
                        currentWeather
                );

                // Add to scheduled flights
                scheduledFlights.add(currentFlight);
            } else {
                // If no runway is available, put the flight back in the queue
                // with reduced priority to avoid starvation
                workingQueue.offer(currentFlight);

                // Delay a lower priority flight to handle this scenario
                delayLowPriorityFlight(workingQueue);
            }
        }

        return new ArrayList<>(scheduledFlights);
    }

    // Handle emergency flights with priority
    private void handleEmergencyFlight(Flight emergency) {
        // For emergencies, find any available runway
        Runway bestRunway = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Runway runway : runways) {
            if (runway.isActive()) {
                double score = runway.calculateScore(currentWeather, emergency);
                if (score > bestScore) {
                    bestScore = score;
                    bestRunway = runway;
                }
            }
        }

        if (bestRunway != null) {
            // Make room for emergency by delaying other flights if needed
            LocalDateTime now = LocalDateTime.now();

            // If runway isn't immediately available, make it available
            if (bestRunway.getNextAvailableTime().isAfter(now)) {
                // Find any flight using this runway at the emergency time
                for (Flight flight : scheduledFlights) {
                    if (flight.getAssignedRunway() == bestRunway) {
                        // Only delay flights that haven't happened yet
                        if (flight.getActualTime().isAfter(now)) {
                            // Move flight back to queue for rescheduling
                            flightQueue.offer(flight);
                            scheduledFlights.remove(flight);
                            break;  // Only need to remove one to make space
                        }
                    }
                }

                // Make runway available now for emergency
                bestRunway.setNextAvailableTime(now);
            }

            // Assign the runway to emergency flight
            emergency.setAssignedRunway(bestRunway);
            emergency.setActualTime(now);

            // Update runway's next available time
            bestRunway.updateNextAvailableTime(
                    now,
                    emergency.getCategory(),
                    safetyMatrix,
                    currentWeather
            );

            // Add to scheduled flights
            scheduledFlights.add(emergency);
        } else {
            // Even with no active runways, emergencies must land
            // Find any runway, even if inactive
            if (!runways.isEmpty()) {
                Runway anyRunway = runways.get(0);
                LocalDateTime now = LocalDateTime.now();

                // Activate runway temporarily for emergency
                boolean wasActive = anyRunway.isActive();
                anyRunway.setActive(true);

                // Assign the runway to emergency flight
                emergency.setAssignedRunway(anyRunway);
                emergency.setActualTime(now);

                // Update runway's next available time
                anyRunway.updateNextAvailableTime(
                        now,
                        emergency.getCategory(),
                        safetyMatrix,
                        currentWeather
                );

                // Return runway to previous state
                anyRunway.setActive(wasActive);

                // Add to scheduled flights
                scheduledFlights.add(emergency);
            }
        }
    }

    // Select the best runway for a flight based on conditions
    private Runway selectBestRunway(Flight flight) {
        Runway bestRunway = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Runway runway : runways) {
            if (runway.isActive()) {
                double score = runway.calculateScore(currentWeather, flight);
                if (score > bestScore) {
                    bestScore = score;
                    bestRunway = runway;
                }
            }
        }

        return bestRunway;
    }

    // Calculate the earliest possible time for a flight operation
    private LocalDateTime calculateEarliestTime(Flight flight, Runway runway) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledTime = flight.getScheduledTime();
        LocalDateTime runwayAvailableTime = runway.getNextAvailableTime();

        // The earliest time is the latest of: now, scheduled time, or runway available time
        LocalDateTime earliestTime = now;

        if (scheduledTime.isAfter(earliestTime)) {
            earliestTime = scheduledTime;
        }

        if (runwayAvailableTime.isAfter(earliestTime)) {
            earliestTime = runwayAvailableTime;
        }

        return earliestTime;
    }

    // Check for conflicts with already scheduled flights
    private List<Flight> checkForConflicts(LocalDateTime proposedTime, Runway runway) {
        List<Flight> conflicts = new ArrayList<>();

        for (Flight scheduledFlight : scheduledFlights) {
            // Only consider flights on the same runway
            if (scheduledFlight.getAssignedRunway() == runway) {
                LocalDateTime scheduledTime = scheduledFlight.getActualTime();

                // Check if the time difference is too small
                long timeDifferenceSeconds = Math.abs(
                        java.time.Duration.between(proposedTime, scheduledTime).getSeconds()
                );

                // Get required separation based on aircraft categories
                int requiredSeparation = safetyMatrix.getSeparationTimeSeconds(
                        scheduledFlight.getCategory(),
                        scheduledFlight.getCategory()
                );

                // Adjust for weather conditions
                requiredSeparation = (int)(requiredSeparation * currentWeather.getWeatherFactor());

                if (timeDifferenceSeconds < requiredSeparation) {
                    conflicts.add(scheduledFlight);
                }
            }
        }

        return conflicts;
    }

    // Resolve conflicts by adjusting the proposed time
    private LocalDateTime resolveConflicts(Flight flight, List<Flight> conflicts,
                                           LocalDateTime proposedTime) {
        LocalDateTime adjustedTime = proposedTime;

        for (Flight conflict : conflicts) {
            // Get the scheduled time of the conflict
            LocalDateTime conflictTime = conflict.getActualTime();

            // Get minimum separation required
            int requiredSeparation = safetyMatrix.getSeparationTimeSeconds(
                    conflict.getCategory(),
                    flight.getCategory()
            );

            // Adjust for weather
            requiredSeparation = (int)(requiredSeparation * currentWeather.getWeatherFactor());

            // Calculate a new time that ensures proper separation
            LocalDateTime safeTime = conflictTime.plusSeconds(requiredSeparation);

            // If safe time is later than our current adjusted time, update
            if (safeTime.isAfter(adjustedTime)) {
                adjustedTime = safeTime;
            }
        }

        return adjustedTime;
    }

    // Delay a low priority flight if no runways are available
    private void delayLowPriorityFlight(Queue<Flight> queue) {
        // Find the flight with lowest priority in the scheduled list
        Flight lowestPriority = null;

        for (Flight flight : scheduledFlights) {
            // Only consider flights in the future (that haven't happened yet)
            if (flight.getActualTime().isAfter(LocalDateTime.now())) {
                // Skip emergency flights
                if (flight.getEmergencyStatus() != Flight.EmergencyStatus.NONE) {
                    continue;
                }

                if (lowestPriority == null ||
                        flight.getPriority() < lowestPriority.getPriority()) {
                    lowestPriority = flight;
                }
            }
        }

        // If we found a candidate for delay
        if (lowestPriority != null) {
            // Remove from scheduled flights
            scheduledFlights.remove(lowestPriority);

            // Return its runway to available pool by resetting next available time
            Runway runway = lowestPriority.getAssignedRunway();
            if (runway != null) {
                // Find the next flight using this runway and adjust time
                LocalDateTime nextTime = null;

                for (Flight flight : scheduledFlights) {
                    if (flight.getAssignedRunway() == runway &&
                            flight.getActualTime().isAfter(lowestPriority.getActualTime())) {
                        if (nextTime == null || flight.getActualTime().isBefore(nextTime)) {
                            nextTime = flight.getActualTime();
                        }
                    }
                }

                // If no next flight, set to now
                if (nextTime == null) {
                    runway.setNextAvailableTime(LocalDateTime.now());
                } else {
                    runway.setNextAvailableTime(nextTime);
                }
            }

            // Add back to queue for rescheduling
            queue.offer(lowestPriority);
        }
    }

    // Getters and utility methods
    public List<Flight> getScheduledFlights() {
        return scheduledFlights;
    }

    public List<Runway> getRunways() {
        return runways;
    }

    public Weather getCurrentWeather() {
        return currentWeather;
    }

    // Get all flights (both in queue and scheduled)
    public List<Flight> getAllFlights() {
        List<Flight> allFlights = new ArrayList<>(scheduledFlights);
        allFlights.addAll(flightQueue);
        return allFlights;
    }

    // Clear all flights and reset the system
    public void reset() {
        flightQueue.clear();
        scheduledFlights.clear();

        // Reset runways to be available now
        for (Runway runway : runways) {
            runway.setNextAvailableTime(LocalDateTime.now());
        }
    }
}
