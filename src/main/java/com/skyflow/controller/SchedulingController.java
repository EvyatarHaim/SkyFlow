package com.skyflow.controller;

import com.skyflow.model.*;
import com.skyflow.util.FlightComparator;

import java.time.LocalDateTime;
import java.time.Duration;
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

    private boolean shouldReschedule(Flight existingFlight, Flight newFlight) {
        long timeDifference = Math.abs(Duration.between(
                existingFlight.getActualTime(),
                newFlight.getActualTime()
        ).toSeconds());

        // Only reschedule if time difference is significant (e.g., more than 30 seconds)
        return timeDifference > 30;
    }

    // Schedule flights with improved stability and conflict resolution
    public List<Flight> scheduleFlights() {
        // Prevent unnecessary rescheduling if no flights are in the queue
        if (flightQueue.isEmpty()) {
            return new ArrayList<>(scheduledFlights);
        }

        // Create a copy of the priority queue to prevent modifying the original
        PriorityQueue<Flight> workingQueue = new PriorityQueue<>(flightQueue);

        // Track flights that couldn't be scheduled
        List<Flight> unscheduledFlights = new ArrayList<>();

        // Clear previous scheduling results
        scheduledFlights.clear();

        // Maximum attempts to schedule flights to prevent infinite loops
        int maxSchedulingAttempts = workingQueue.size() * 2;
        int currentAttempt = 0;

        while (!workingQueue.isEmpty() && currentAttempt < maxSchedulingAttempts) {
            currentAttempt++;

            // Get the flight with highest priority
            Flight currentFlight = workingQueue.poll();

            // Handle emergency flights with priority
            if (currentFlight.getEmergencyStatus() != Flight.EmergencyStatus.NONE) {
                handleEmergencyFlight(currentFlight);
                continue;
            }

            // Find the best available runway for this flight
            Runway bestRunway = selectBestRunway(currentFlight);

            // Check if scheduling is necessary and possible
            if (bestRunway != null &&
                    (currentFlight.getAssignedRunway() == null ||
                            isReschedulingNecessary(currentFlight, bestRunway))) {

                // Calculate the earliest possible operation time
                LocalDateTime earliestTime = calculateEarliestTime(currentFlight, bestRunway);

                // Check for conflicts with already scheduled flights
                List<Flight> conflicts = checkForConflicts(earliestTime, bestRunway);

                if (!conflicts.isEmpty()) {
                    // Resolve conflicts and adjust timing
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
                // If no runway is available or rescheduling is not necessary
                unscheduledFlights.add(currentFlight);
            }
        }

        // Handle any unscheduled flights
        if (!unscheduledFlights.isEmpty()) {
            // Log unscheduled flights
            System.out.println("Warning: " + unscheduledFlights.size() + " flights could not be scheduled.");

            // Retry scheduling unscheduled flights
            for (Flight unscheduledFlight : unscheduledFlights) {
                unscheduledFlight.updatePriority();
                flightQueue.offer(unscheduledFlight);
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
            // Get the current time
            LocalDateTime now = LocalDateTime.now();

            // Check if there are already emergency flights assigned to this runway
            LocalDateTime earliestAvailableTime = now;

            // Look through already scheduled flights to find conflicts
            for (Flight flight : scheduledFlights) {
                if (flight.getAssignedRunway() == bestRunway &&
                        flight.getActualTime() != null) {

                    // Calculate minimum separation time required between these flights
                    int requiredSeparation = safetyMatrix.getSeparationTimeSeconds(
                            flight.getCategory(), emergency.getCategory());

                    // Adjust for weather
                    requiredSeparation = (int)(requiredSeparation * currentWeather.getWeatherFactor());

                    // If this flight is scheduled after our current time point, we need to wait
                    if (flight.getActualTime().isAfter(now) || flight.getActualTime().isEqual(now)) {
                        LocalDateTime safeTime = flight.getActualTime().plusSeconds(requiredSeparation);
                        if (safeTime.isAfter(earliestAvailableTime)) {
                            earliestAvailableTime = safeTime;
                        }
                    }
                    // If flight is before our current time, we need to check separation from it
                    else {
                        LocalDateTime safeTime = flight.getActualTime().plusSeconds(requiredSeparation);
                        if (safeTime.isAfter(now) && safeTime.isAfter(earliestAvailableTime)) {
                            earliestAvailableTime = safeTime;
                        }
                    }
                }
            }

            // Assign the runway and time to emergency flight
            emergency.setAssignedRunway(bestRunway);
            emergency.setActualTime(earliestAvailableTime);

            // Update runway's next available time
            bestRunway.updateNextAvailableTime(
                    earliestAvailableTime,
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

                // Calculate earliest available time similar to above
                LocalDateTime earliestAvailableTime = now;

                for (Flight flight : scheduledFlights) {
                    if (flight.getAssignedRunway() == anyRunway &&
                            flight.getActualTime() != null) {

                        int requiredSeparation = safetyMatrix.getSeparationTimeSeconds(
                                flight.getCategory(), emergency.getCategory());
                        requiredSeparation = (int)(requiredSeparation * currentWeather.getWeatherFactor());

                        if (flight.getActualTime().isAfter(now) || flight.getActualTime().isEqual(now)) {
                            LocalDateTime safeTime = flight.getActualTime().plusSeconds(requiredSeparation);
                            if (safeTime.isAfter(earliestAvailableTime)) {
                                earliestAvailableTime = safeTime;
                            }
                        } else {
                            LocalDateTime safeTime = flight.getActualTime().plusSeconds(requiredSeparation);
                            if (safeTime.isAfter(now) && safeTime.isAfter(earliestAvailableTime)) {
                                earliestAvailableTime = safeTime;
                            }
                        }
                    }
                }

                // Activate runway temporarily for emergency
                boolean wasActive = anyRunway.isActive();
                anyRunway.setActive(true);

                // Assign the runway to emergency flight
                emergency.setAssignedRunway(anyRunway);
                emergency.setActualTime(earliestAvailableTime);

                // Update runway's next available time
                anyRunway.updateNextAvailableTime(
                        earliestAvailableTime,
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
        // If flight already has an assigned runway, try to keep it
        if (flight.getAssignedRunway() != null) {
            Runway originalRunway = flight.getAssignedRunway();
            if (originalRunway.isActive()) {
                return originalRunway;
            }
        }

        // Find the best available runway
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
        LocalDateTime scheduledTime = flight.getScheduledTime();
        LocalDateTime runwayAvailableTime = runway.getNextAvailableTime();

        // Prefer the scheduled time if the runway is available
        if (runwayAvailableTime.isBefore(scheduledTime) || runwayAvailableTime.isEqual(scheduledTime)) {
            return scheduledTime;
        }

        // If runway is not available at scheduled time, use the next available time
        return runwayAvailableTime;
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

    // Determine if rescheduling is absolutely required
    private boolean isReschedulingNecessary(Flight flight, Runway newRunway) {
        // Emergency flights always require rescheduling
        if (flight.getEmergencyStatus() != Flight.EmergencyStatus.NONE) {
            return true;
        }

        // If the current runway is the same and available at scheduled time, no need to reschedule
        Runway currentRunway = flight.getAssignedRunway();
        if (currentRunway != null && currentRunway.equals(newRunway)) {
            return false;
        }

        // Check significant time difference or runway unavailability
        return currentRunway == null ||
                !currentRunway.getNextAvailableTime().isBefore(flight.getScheduledTime());
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
