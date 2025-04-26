package com.skyflow.controller;

import com.skyflow.model.Runway;
import java.util.ArrayList;
import java.util.List;

public class RunwayController {
    private List<Runway> runways;
    private SchedulingController schedulingController;

    // Constructor
    public RunwayController(SchedulingController schedulingController) {
        this.runways = new ArrayList<>();
        this.schedulingController = schedulingController;

        // Create default runways
        createDefaultRunways();
    }

    // Create default runways based on Ben Gurion Airport configuration
    private void createDefaultRunways() {
        // Main Runway (12/30) - both directions
        createRunway("12", 120, 3112);
        createRunway("30", 300, 3112);

        // Extended Runway (03/21) - both directions
        createRunway("03", 30, 2772);
        createRunway("21", 210, 2772);

        // Quiet Runway (08/26) - both directions
        createRunway("08", 80, 4062);
        createRunway("26", 260, 4062);
    }

    // Create a new runway and add it to the system
    public Runway createRunway(String id, int heading, int length) {
        // Create new runway
        Runway runway = new Runway(id, heading, length);

        // Add to local list
        runways.add(runway);

        // Add to scheduling controller
        schedulingController.addRunway(runway);

        return runway;
    }

    // Update an existing runway
    public void updateRunway(Runway runway) {
        // Rescheduling might be needed if runway properties changed
        schedulingController.scheduleFlights();
    }

    // Delete a runway
    public void deleteRunway(Runway runway) {
        runways.remove(runway);
    }

    // Activate or deactivate a runway
    public void setRunwayActive(Runway runway, boolean active) {
        runway.setActive(active);

        // Update runway
        updateRunway(runway);

        // Trigger rescheduling as runway availability changed
        schedulingController.scheduleFlights();
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