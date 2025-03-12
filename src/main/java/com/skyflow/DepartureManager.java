package com.skyflow;

import com.skyflow.model.Flight;
import com.skyflow.model.Runway;
import com.skyflow.model.Weather;

import java.time.LocalDateTime;
import java.util.*;


public class DepartureManager {
    private List<Runway> runways;
    private Weather currentWeather;
    private PriorityQueue<Flight> departureQueue;
    private Map<String, LocalDateTime> runwaySchedule;

    // Constructor
    public DepartureManager(List<Runway> runways, Weather weather) {
        this.runways = runways;
        this.currentWeather = weather;
        this.runwaySchedule = new HashMap<>();

        // Init the runway schedule with the current time
        for (Runway runway : runways) {
            runwaySchedule.put(runway.getRunwayId(), LocalDateTime.now());
        }

        // Init priority queue
        this.departureQueue = new PriorityQueue<>(new FlightComparator());
    }

    public void addFlight(Flight flight) {
        departureQueue.add(flight);
    }

    public void updateWeather(Weather weather) {
        this.currentWeather = weather;
    }
    // Processes all flights in the departure queue and assigns them to runways
//    public List<Flight> scheduleFlights() {
//        List<Flight> scheduledFlights = new ArrayList<>();
//
//        while (!departureQueue.isEmpty()) {
//            Flight flight = departureQueue.poll();
//
//            // Find the best runway for this flight
//            Runway bestRunway = selectBestRunway(flight);
//
//            if (bestRunway != null) {
//                // Calculate earliest possible departure time
//                LocalDateTime earliestDeparture = calculateEarliestDeparture(flight, bestRunway);
//
//                // Assign runway and departure time to flight
//                flight.setAssignedRunway(bestRunway.getRunwayId());
//                flight.setActualDepartureTime(earliestDeparture);
//
//                // Update runway schedule
//                runwaySchedule.put(bestRunway.getRunwayId(), earliestDeparture);
//
//                scheduledFlights.add(flight);
//            } else {
//                // No suitable runway found, add the flight for later - delay it
//                departureQueue.add(flight);
//                break;
//            }
//        }
//        return scheduledFlights;
//    }

    public List<Flight> scheduleFlights() {
        List<Flight> scheduledFlights = new ArrayList<>();

        System.out.println("Starting scheduling with " + departureQueue.size() + " flights in queue");

        while (!departureQueue.isEmpty()) {
            Flight flight = departureQueue.poll();

            System.out.println("Processing flight: " + flight.getFlightId());

            // Find the best runway for this flight
            Runway bestRunway = selectBestRunway(flight);

            if (bestRunway != null) {
                // Calculate earliest possible departure time
                LocalDateTime earliestDeparture = calculateEarliestDeparture(flight, bestRunway);

                System.out.println("  Assigning runway " + bestRunway.getRunwayId() +
                        " with departure time " + earliestDeparture);

                // Assign runway and departure time to flight
                flight.setAssignedRunway(bestRunway.getRunwayId());
                flight.setActualDepartureTime(earliestDeparture);

                // Update runway schedule
                runwaySchedule.put(bestRunway.getRunwayId(), earliestDeparture);

                scheduledFlights.add(flight);
            } else {
                System.out.println("  No suitable runway found, delaying flight");
                // No suitable runway found, add the flight for later - delay it
                departureQueue.add(flight);
                break;
            }
        }

        System.out.println("Scheduled " + scheduledFlights.size() + " flights");
        return scheduledFlights;
    }

    // Selects the best runway for a flight
    private Runway selectBestRunway(Flight flight) {
        Runway bestRunway = null;
        double bestScore = Double.MIN_VALUE;

        for (Runway runway : runways) {
            if (!runway.isAvailable()) {
                continue;
            }
            double headwind = currentWeather.calcHeadwind(runway.getHeading());
            double crosswind = Math.abs(currentWeather.calcCrosswind(runway.getHeading()));

            // Prefer runways with stronger headwind and lower crosswind
            double score = headwind - (crosswind * 2);

            // Penalize runways that aren't available sooner
            if (runway.getAvailableFrom().isAfter(LocalDateTime.now())) {
                score -= 10;
            }

            if (score > bestScore) {
                bestScore = score;
                bestRunway = runway;
            }
        }

        return bestRunway;
    }

    // Calculates the earliest possible departure time for a flight on a runway
    private LocalDateTime calculateEarliestDeparture(Flight flight, Runway runway) {
        LocalDateTime nextAvailable = runwaySchedule.getOrDefault(runway.getRunwayId(), LocalDateTime.now());

        // If scheduled departure is after next available, use scheduled time
        if (flight.getScheduledDepartureTime().isAfter(nextAvailable)) {
            return flight.getScheduledDepartureTime();
        }
        else {
            return nextAvailable;
        }
    }

    public List<Flight> getDepartureQueue() {
        return new ArrayList<>(departureQueue);
    }

    public void setRunwayAvailability(String runwayId, boolean available) {
        for (Runway runway : runways) {
            if (runway.getRunwayId().equals(runwayId)) {
                runway.setAvailable(available);
                break;
            }
        }
    }

    // Compare flights for sorting in the priority queue
    private static class FlightComparator implements Comparator<Flight> {
        @Override
        public int compare(Flight f1, Flight f2) {
            // Compare by emergency status
            if (f1.isEmergency() && !f2.isEmergency()) {
                return -1;
            }
            else if (!f1.isEmergency() && f2.isEmergency()) {
                return 1;
            }

            // Compare by priority
            if (f1.getPriority() != f2.getPriority()) {
                return Integer.compare(f2.getPriority(), f1.getPriority());
            }

            // Scheduled departure time
            return f1.getScheduledDepartureTime().compareTo(f2.getScheduledDepartureTime());
        }
    }
}
