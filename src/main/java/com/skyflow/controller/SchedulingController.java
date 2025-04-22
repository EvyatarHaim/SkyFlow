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

        // To prevent duplications, use a set to track processed flights
        Set<String> processedFlightIds = new HashSet<>();

        // Create a copy of the original queue for working
        PriorityQueue<Flight> workingQueue = new PriorityQueue<>(new FlightComparator());

        // Copy flights from the original queue to the working queue
        for (Flight flight : flightQueue) {
            workingQueue.offer(flight);
        }

        // Clear the original queue (we'll refill it with unscheduled flights)
        flightQueue.clear();

        // Track flights that couldn't be scheduled
        List<Flight> unscheduledFlights = new ArrayList<>();

        // Clear previous scheduling results
        scheduledFlights.clear();

        // Process all flights in the queue
        while (!workingQueue.isEmpty()) {
            // Get the flight with highest priority
            Flight currentFlight = workingQueue.poll();

            // Track processed flights to prevent duplicate processing
            if (processedFlightIds.contains(currentFlight.getId())) {
                continue;
            }
            processedFlightIds.add(currentFlight.getId());

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
        // For emergencies, find any available runway with highest score
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
            // Get the current time and respect scheduled time if possible
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime baseTime = emergency.getScheduledTime();
            if (baseTime.isBefore(now)) {
                // If scheduled time is in the past, use now
                baseTime = now;
            }

            // We need to reschedule ALL non-emergency flights that would conflict
            // with this emergency flight to other runways if possible
            List<Flight> conflictingFlights = new ArrayList<>();

            // Find ALL non-emergency flights on this runway that conflict
            for (Flight flight : scheduledFlights) {
                if (flight.getAssignedRunway() == bestRunway &&
                        flight.getEmergencyStatus() == Flight.EmergencyStatus.NONE &&
                        flight.getActualTime() != null) {

                    // Check for exact time conflict first (most serious)
                    if (flight.getActualTime().equals(baseTime)) {
                        conflictingFlights.add(flight);
                        continue;
                    }

                    // Also check for separation conflicts - too close in time
                    long timeDiffSeconds = Math.abs(Duration.between(baseTime, flight.getActualTime()).getSeconds());

                    // Get required separation time
                    int requiredSeparation = safetyMatrix.getSeparationTimeSeconds(
                            flight.getCategory(), emergency.getCategory());

                    // Adjust for weather
                    requiredSeparation = (int)(requiredSeparation * currentWeather.getWeatherFactor());

                    // If too close, it's a conflict
                    if (timeDiffSeconds < requiredSeparation) {
                        conflictingFlights.add(flight);
                    }
                }
            }

            // Remove conflicting flights from scheduled and try to reschedule them
            for (Flight conflict : conflictingFlights) {
                scheduledFlights.remove(conflict);

                // Try to find another runway for this flight
                Runway alternateRunway = null;
                double bestAltScore = Double.NEGATIVE_INFINITY;

                // Choose the best alternative runway, not just the first one
                for (Runway r : runways) {
                    if (r != bestRunway && r.isActive()) {
                        double score = r.calculateScore(currentWeather, conflict);
                        if (score > bestAltScore) {
                            bestAltScore = score;
                            alternateRunway = r;
                        }
                    }
                }

                if (alternateRunway != null) {
                    // We found another runway, assign it
                    conflict.setAssignedRunway(alternateRunway);

                    // Calculate new time (use original or next available, plus extra buffer)
                    LocalDateTime newTime = conflict.getScheduledTime();
                    if (alternateRunway.getNextAvailableTime().isAfter(newTime)) {
                        newTime = alternateRunway.getNextAvailableTime();
                    }

                    // Add a small buffer to ensure time conflicts don't repeat
                    newTime = newTime.plusSeconds(30);

                    conflict.setActualTime(newTime);

                    // Update runway's next available time
                    alternateRunway.updateNextAvailableTime(
                            newTime,
                            conflict.getCategory(),
                            safetyMatrix,
                            currentWeather
                    );

                    // Add back to scheduled flights
                    scheduledFlights.add(conflict);
                } else {
                    // No alternate runway, put back in queue with higher priority
                    conflict.updatePriority();
                    flightQueue.offer(conflict);
                }
            }

            // Now the runway should be clear for the emergency
            // Calculate earliest available time for emergency
            LocalDateTime earliestAvailableTime = baseTime;

            // Look through remaining scheduled flights to find any that need separation
            for (Flight flight : scheduledFlights) {
                if (flight.getAssignedRunway() == bestRunway &&
                        flight.getActualTime() != null) {

                    // Calculate minimum separation time required between these flights
                    int requiredSeparation = safetyMatrix.getSeparationTimeSeconds(
                            flight.getCategory(), emergency.getCategory());

                    // Adjust for weather
                    requiredSeparation = (int)(requiredSeparation * currentWeather.getWeatherFactor());

                    // If this flight is scheduled after our base time, we need to wait
                    if (flight.getActualTime().isAfter(baseTime) || flight.getActualTime().isEqual(baseTime)) {
                        LocalDateTime safeTime = flight.getActualTime().plusSeconds(requiredSeparation);
                        if (safeTime.isAfter(earliestAvailableTime)) {
                            earliestAvailableTime = safeTime;
                        }
                    }
                    // If flight is before our base time, we need to check separation from it
                    else {
                        LocalDateTime safeTime = flight.getActualTime().plusSeconds(requiredSeparation);
                        if (safeTime.isAfter(baseTime) && safeTime.isAfter(earliestAvailableTime)) {
                            earliestAvailableTime = safeTime;
                        }
                    }
                }
            }

            // Add final check for exact time conflicts with any remaining flights
            boolean hasExactConflict;
            do {
                hasExactConflict = false;
                for (Flight flight : scheduledFlights) {
                    if (flight.getAssignedRunway() == bestRunway &&
                            flight.getActualTime() != null &&
                            flight.getActualTime().equals(earliestAvailableTime)) {

                        // Found an exact time conflict, add 15 seconds
                        earliestAvailableTime = earliestAvailableTime.plusSeconds(15);
                        hasExactConflict = true;
                        break;
                    }
                }
            } while (hasExactConflict);

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

                // Respect scheduled time if possible
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime baseTime = emergency.getScheduledTime();
                if (baseTime.isBefore(now)) {
                    // If scheduled time is in the past, use now
                    baseTime = now;
                }

                // Calculate earliest available time similar to above
                LocalDateTime earliestAvailableTime = baseTime;

                // Check for conflicts with existing flights
                for (Flight flight : scheduledFlights) {
                    if (flight.getAssignedRunway() == anyRunway &&
                            flight.getActualTime() != null) {

                        // Skip emergency flights (don't reschedule them)
                        if (flight.getEmergencyStatus() != Flight.EmergencyStatus.NONE) {
                            continue;
                        }

                        // Check for exact time match
                        if (flight.getActualTime().equals(baseTime)) {
                            // For exact time conflict, move existing flight
                            scheduledFlights.remove(flight);
                            flight.updatePriority();
                            flightQueue.offer(flight);
                            continue;
                        }

                        int requiredSeparation = safetyMatrix.getSeparationTimeSeconds(
                                flight.getCategory(), emergency.getCategory());
                        requiredSeparation = (int)(requiredSeparation * currentWeather.getWeatherFactor());

                        if (flight.getActualTime().isAfter(baseTime) || flight.getActualTime().isEqual(baseTime)) {
                            LocalDateTime safeTime = flight.getActualTime().plusSeconds(requiredSeparation);
                            if (safeTime.isAfter(earliestAvailableTime)) {
                                earliestAvailableTime = safeTime;
                            }
                        } else {
                            LocalDateTime safeTime = flight.getActualTime().plusSeconds(requiredSeparation);
                            if (safeTime.isAfter(baseTime) && safeTime.isAfter(earliestAvailableTime)) {
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
                // Calculate base score
                double score = runway.calculateScore(currentWeather, flight);

                // Add preference for distributing flights across runways
                // Check how many flights are already assigned to this runway
                int flightsOnRunway = 0;
                for (Flight scheduledFlight : scheduledFlights) {
                    if (scheduledFlight.getAssignedRunway() == runway) {
                        flightsOnRunway++;
                    }
                }

                // Penalize runways with more flights (encourages distribution)
                score -= flightsOnRunway * 5;

                // For departures, prefer runways with headwind
                if (flight.getType() == Flight.FlightType.DEPARTURE) {
                    double headwind = currentWeather.calculateHeadwind(runway.getHeading());
                    if (headwind > 0) {
                        score += headwind * 1.5; // Bonus for headwind on takeoff
                    }
                }
                // For arrivals, less crosswind is more important
                else if (flight.getType() == Flight.FlightType.ARRIVAL) {
                    double crosswind = Math.abs(currentWeather.calculateCrosswind(runway.getHeading()));
                    score -= crosswind * 2; // Penalty for crosswind on landing
                }

                // Consider runway length based on aircraft size
                if (flight.getCategory() == Flight.WakeTurbulenceCategory.HEAVY ||
                        flight.getCategory() == Flight.WakeTurbulenceCategory.SUPER) {
                    // Larger aircraft need longer runways
                    score += runway.getLength() / 100.0; // Bonus for longer runways
                }

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

                // EXACT time match is always a conflict
                if (scheduledTime.equals(proposedTime)) {
                    conflicts.add(scheduledFlight);
                    continue;
                }

                // Is this a flight with higher priority (emergency)?
                boolean isHigherPriority = scheduledFlight.getEmergencyStatus() != Flight.EmergencyStatus.NONE &&
                        scheduledFlight.getEmergencyStatus().getPriorityLevel() >
                                Flight.EmergencyStatus.NONE.getPriorityLevel();

                // Check if the time difference is too small
                long timeDifferenceSeconds = Math.abs(
                        Duration.between(proposedTime, scheduledTime).getSeconds()
                );

                // Get required separation based on aircraft categories
                int requiredSeparation;

                // Determine leading and following aircraft based on time
                if (scheduledTime.isBefore(proposedTime)) {
                    // Scheduled flight is leading
                    requiredSeparation = safetyMatrix.getSeparationTimeSeconds(
                            scheduledFlight.getCategory(),
                            scheduledFlight.getCategory() // Using same category as placeholder (will be replaced)
                    );
                } else {
                    // New flight is leading
                    requiredSeparation = safetyMatrix.getSeparationTimeSeconds(
                            scheduledFlight.getCategory(), // Using same category as placeholder (will be replaced)
                            scheduledFlight.getCategory()
                    );
                }

                // Adjust for weather conditions
                requiredSeparation = (int)(requiredSeparation * currentWeather.getWeatherFactor());

                // If time difference is less than required separation, it's a conflict
                if (timeDifferenceSeconds < requiredSeparation) {
                    conflicts.add(scheduledFlight);
                }
            }
        }

        // Add special checks to ensure no flights share exact same time
        for (Flight scheduledFlight : scheduledFlights) {
            if (scheduledFlight.getAssignedRunway() == runway &&
                    scheduledFlight.getActualTime() != null) {

                // Check nearby times (+/- 15 seconds)
                for (int i = -15; i <= 15; i++) {
                    LocalDateTime nearbyTime = proposedTime.plusSeconds(i);
                    if (scheduledFlight.getActualTime().equals(nearbyTime)) {
                        conflicts.add(scheduledFlight);
                        break;
                    }
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
