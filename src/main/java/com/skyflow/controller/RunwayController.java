// Manages runway-related operations and data
package com.skyflow.controller;

import com.skyflow.model.Runway;
import com.skyflow.util.DatabaseController;

import java.util.ArrayList;
import java.util.List;

public class RunwayController {
    private List<Runway> runways;
    private DatabaseController dbController;
    private SchedulingController schedulingController;

    // Constructor
    public RunwayController(SchedulingController schedulingController,
                            DatabaseController dbController) {
        this.runways = new ArrayList<>();
        this.schedulingController = schedulingController;
        this.dbController = dbController;

        // Load runways from database if available
        if (dbController != null) {
            loadRunwaysFromDatabase();
        }
    }

    // Create a new runway
    public Runway createRunway(String id, int heading, int length) {
        Runway runway = new Runway(id, heading, length);

        // Add to local list
        runways.add(runway);

        // Add to scheduling controller
        schedulingController.addRunway(runway);

        // Save to database if available
        if (dbController != null) {
            dbController.saveRunway(runway);
        }

        return runway;
    }

    // Update an existing runway
    public void updateRunway(Runway runway) {
        // Update in database if available
        if (dbController != null) {
            dbController.updateRunway(runway);
        }

        // Rescheduling might be needed if runway properties changed
        schedulingController.scheduleFlights();
    }

    // Delete a runway
    public void deleteRunway(Runway runway) {
        runways.remove(runway);

        // Remove from database if available
        if (dbController != null) {
            dbController.deleteRunway(runway);
        }
    }

    // Activate or deactivate a runway
    public void setRunwayActive(Runway runway, boolean active) {
        runway.setActive(active);

        // Update runway
        updateRunway(runway);

        // Trigger rescheduling as runway availability changed
        schedulingController.scheduleFlights();
    }

    // Load runways from database
    private void loadRunwaysFromDatabase() {
        List<Runway> loadedRunways = dbController.loadAllRunways();

        if (loadedRunways != null) {
            runways.addAll(loadedRunways);

            // Add all loaded runways to scheduling controller
            for (Runway runway : loadedRunways) {
                schedulingController.addRunway(runway);
            }
        }
    }

    // Get all runways
    public List<Runway> getAllRunways() {
        return new ArrayList<>(runways);
    }

    // Get runway by ID
    public Runway getRunwayById(String id) {
        for (Runway runway : runways) {
            if (runway.getId().equals(id)) {
                return runway;
            }
        }
        return null;
    }
}