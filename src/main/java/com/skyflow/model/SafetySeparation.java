package com.skyflow.model;

import java.util.HashMap;
import java.util.Map;

public class SafetySeparation {
    // Matrix for wake turbulence separation time in seconds
    private final Map<Flight.WakeTurbulenceCategory, Map<Flight.WakeTurbulenceCategory, Integer>> separationMatrix;

    // Constructor - initializes the separation matrix with standard values
    public SafetySeparation() {
        separationMatrix = new HashMap<>();

        // Initialize for SUPER aircraft as leading
        Map<Flight.WakeTurbulenceCategory, Integer> superLeading = new HashMap<>();
        superLeading.put(Flight.WakeTurbulenceCategory.SUPER, 120);
        superLeading.put(Flight.WakeTurbulenceCategory.HEAVY, 180);
        superLeading.put(Flight.WakeTurbulenceCategory.MEDIUM, 240);
        superLeading.put(Flight.WakeTurbulenceCategory.LIGHT, 300);
        separationMatrix.put(Flight.WakeTurbulenceCategory.SUPER, superLeading);

        // Initialize for HEAVY aircraft as leading
        Map<Flight.WakeTurbulenceCategory, Integer> heavyLeading = new HashMap<>();
        heavyLeading.put(Flight.WakeTurbulenceCategory.SUPER, 100);
        heavyLeading.put(Flight.WakeTurbulenceCategory.HEAVY, 120);
        heavyLeading.put(Flight.WakeTurbulenceCategory.MEDIUM, 180);
        heavyLeading.put(Flight.WakeTurbulenceCategory.LIGHT, 240);
        separationMatrix.put(Flight.WakeTurbulenceCategory.HEAVY, heavyLeading);

        // Initialize for MEDIUM aircraft as leading
        Map<Flight.WakeTurbulenceCategory, Integer> mediumLeading = new HashMap<>();
        mediumLeading.put(Flight.WakeTurbulenceCategory.SUPER, 80);
        mediumLeading.put(Flight.WakeTurbulenceCategory.HEAVY, 100);
        mediumLeading.put(Flight.WakeTurbulenceCategory.MEDIUM, 120);
        mediumLeading.put(Flight.WakeTurbulenceCategory.LIGHT, 180);
        separationMatrix.put(Flight.WakeTurbulenceCategory.MEDIUM, mediumLeading);

        // Initialize for LIGHT aircraft as leading
        Map<Flight.WakeTurbulenceCategory, Integer> lightLeading = new HashMap<>();
        lightLeading.put(Flight.WakeTurbulenceCategory.SUPER, 60);
        lightLeading.put(Flight.WakeTurbulenceCategory.HEAVY, 80);
        lightLeading.put(Flight.WakeTurbulenceCategory.MEDIUM, 100);
        lightLeading.put(Flight.WakeTurbulenceCategory.LIGHT, 120);
        separationMatrix.put(Flight.WakeTurbulenceCategory.LIGHT, lightLeading);
    }

    // Get separation time between leading and following aircraft
    public int getSeparationTimeSeconds(Flight.WakeTurbulenceCategory leading,
                                        Flight.WakeTurbulenceCategory following) {
        return separationMatrix.get(leading).get(following);
    }

    // Get base separation time for a specific category
    public int getSeparationTimeSeconds(Flight.WakeTurbulenceCategory category) {
        // For simplicity, use the same-category separation as base value
        return separationMatrix.get(category).get(category);
    }

    // Adjust separation time based on weather factor
    public int getAdjustedSeparationTime(Flight.WakeTurbulenceCategory leading,
                                         Flight.WakeTurbulenceCategory following,
                                         double weatherFactor) {
        int baseTime = getSeparationTimeSeconds(leading, following);
        return (int)(baseTime * weatherFactor);
    }

    // Custom method to update specific separation values if needed
    public void updateSeparationTime(Flight.WakeTurbulenceCategory leading,
                                     Flight.WakeTurbulenceCategory following,
                                     int seconds) {
        separationMatrix.get(leading).put(following, seconds);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SafetySeparation Matrix (seconds):\n");

        for (Flight.WakeTurbulenceCategory leading : Flight.WakeTurbulenceCategory.values()) {
            for (Flight.WakeTurbulenceCategory following : Flight.WakeTurbulenceCategory.values()) {
                sb.append(leading).append(" → ").append(following)
                        .append(": ").append(getSeparationTimeSeconds(leading, following))
                        .append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}