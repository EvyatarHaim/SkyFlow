package com.skyflow.model;

public class SafetySeparation {

    // Gets the minimum separation time in seconds between two aircraft
    public static int getMinimumSeparationTime(int leadingCategory, int followingCategory) {

        //  L M H S  <- Leading aircraft
        int[][] separationMatrix = {
                {60, 60, 60, 60},  // Light following
                {120, 60, 60, 120}, // Medium following
                {180, 120, 60, 120}, // Heavy following
                {240, 180, 120, 120}  // Super following
        };

        // Matrix start at 0
        int leadingIndex = leadingCategory - 1;
        int followingIndex = followingCategory - 1;

        // If invalid category is provided set time as the maximum time
        if (leadingIndex < 0 || leadingIndex > 3 || followingIndex < 0 || followingIndex > 3) {
            return 240;
        }

        return separationMatrix[followingIndex][leadingIndex];
    }


    // Adjust the separation time based on weather conditions
    public static int adjustSeparationForWeather(int baseSeparation, double visibility, boolean precipitation) {
        double factor = 1.0;

        if (visibility < 5000) {
            factor += 0.5;
        }
        else if (visibility < 8000) {
            factor += 0.2;
        }
        if (precipitation) {
            factor += 0.3;
        }

        return (int) (baseSeparation * factor);
    }
}
