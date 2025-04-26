package com.skyflow.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skyflow.controller.FlightController;
import com.skyflow.controller.WeatherController;
import com.skyflow.model.Flight;
import com.skyflow.model.Weather;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OpenSkyDataImport {
    private final FlightController flightController;
    private final WeatherController weatherController;
    private final Random random = new Random();
    private final Gson gson = new Gson();

    private static final String OPENSKY_API_URL = "https://opensky-network.org/api";

    // Constructor
    public OpenSkyDataImport(FlightController flightController, WeatherController weatherController) {
        this.flightController = flightController;
        this.weatherController = weatherController;
    }

    // Import flights from OpenSky API and enhance them with additional information
    public List<Flight> importRealTimeFlights(int numFlights) {
        List<Flight> importedFlights = new ArrayList<>();

        try {
            // Fetch states from OpenSky API
            String response = fetchFromOpenSky("/states/all");

            // Parse response
            JsonObject jsonObject = gson.fromJson(response, JsonObject.class);
            JsonArray states = jsonObject.getAsJsonArray("states");

            // Limit to requested number of flights
            int count = 0;
            int i = 0;
            boolean reachedLimit = false;

            // Iterate through states array until we reach the requested flight count
            while (i < states.size() && !reachedLimit) {
                JsonElement stateElement = states.get(i);
                JsonArray stateArray = stateElement.getAsJsonArray();

                if (stateArray != null && stateArray.size() >= 8) {
                    // Extract data from OpenSky API
                    String icao24 = stateArray.get(0).getAsString();
                    String callsign = stateArray.get(1).getAsString().trim();
                    String originCountry = stateArray.get(2).getAsString();
                    long timePosition = stateArray.get(3).isJsonNull() ? 0 : stateArray.get(3).getAsLong();

                    // Process flights with valid data
                    boolean hasValidData = !callsign.isEmpty() && timePosition != 0;

                    if (hasValidData) {
                        // Create flight with random enhancements
                        Flight flight = createEnhancedFlight(icao24, callsign, originCountry, timePosition);

                        if (flight != null) {
                            importedFlights.add(flight);
                            count++;

                            // Check if we've reached the requested number of flights
                            if (count >= numFlights) {
                                reachedLimit = true;
                            }
                        }
                    }
                }

                i++;
            }

            System.out.println("Successfully imported " + importedFlights.size() + " flights from OpenSky");

        } catch (Exception e) {
            System.err.println("Error importing flights from OpenSky: " + e.getMessage());
            e.printStackTrace();

            // If API fails or not available, generate simulated flights
            if (importedFlights.isEmpty()) {
                importedFlights = generateSimulatedFlights(numFlights);
                System.out.println("Generated " + importedFlights.size() + " simulated flights as fallback");
            }
        }

        return importedFlights;
    }
    // Create a flight with random enhancements
    private Flight createEnhancedFlight(String icao24, String callsign, String originCountry, long timePosition) {
        try {
            // Format callsign to look like a flight number
            String flightNumber = formatCallsign(callsign);

            // Generate random airline based on callsign prefix
            String airline = generateAirlineName(callsign);

            // Generate random aircraft type
            String aircraftType = generateAircraftType();

            // Random wake turbulence category (weighted toward MEDIUM)
            Flight.WakeTurbulenceCategory category = generateRandomCategory();

            // Random flight type (arrival or departure)
            Flight.FlightType flightType = random.nextBoolean() ?
                    Flight.FlightType.ARRIVAL : Flight.FlightType.DEPARTURE;

            // Convert Unix timestamp to LocalDateTime
            LocalDateTime scheduledTime = Instant.ofEpochSecond(timePosition)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            // Randomly assign emergency status (10% chance of emergency)
            Flight.EmergencyStatus emergencyStatus = generateRandomEmergencyStatus();

            // Create flight object
            Flight flight = flightController.createFlight(
                    flightNumber,
                    airline,
                    aircraftType,
                    category,
                    flightType,
                    scheduledTime,
                    emergencyStatus
            );

            // Set random fuel level
            int fuelLevel = random.nextInt(90) + 10; // 10% to 100%
            flight.setFuelLevel(fuelLevel);

            // If emergency is low fuel, set lower fuel level
            if (emergencyStatus == Flight.EmergencyStatus.LOW_FUEL) {
                flight.setFuelLevel(random.nextInt(10) + 5); // 5% to 15%
            }

            return flight;

        } catch (Exception e) {
            System.err.println("Error creating enhanced flight: " + e.getMessage());
            return null;
        }
    }

    // Generate completely simulated flights when no real data is available
    private List<Flight> generateSimulatedFlights(int numFlights) {
        List<Flight> simulatedFlights = new ArrayList<>();

        for (int i = 0; i < numFlights; i++) {
            try {
                // Default airlines if no database
                String[][] defaultAirlines = {
                        {"LY", "El Al Israel Airlines"},
                        {"BA", "British Airways"},
                        {"AA", "American Airlines"},
                        {"DL", "Delta Air Lines"},
                        {"LH", "Lufthansa"}
                };
                int idx = random.nextInt(defaultAirlines.length);
                String airlineCode = defaultAirlines[idx][0];
                String airlineName = defaultAirlines[idx][1];

                // Generate a flight number
                int flightNum = random.nextInt(900) + 100; // 100-999
                String flightNumber = airlineCode + flightNum;

                // Random wake turbulence category
                Flight.WakeTurbulenceCategory category = generateRandomCategory();

                // Random flight type
                Flight.FlightType flightType = random.nextBoolean() ?
                        Flight.FlightType.ARRIVAL : Flight.FlightType.DEPARTURE;

                // Create a scheduled time
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime scheduledTime = now.plusMinutes(random.nextInt(180) + 15);

                // Random emergency status (10% chance)
                Flight.EmergencyStatus emergencyStatus = generateRandomEmergencyStatus();

                // Fallback to generating random aircraft
                String aircraftType = generateAircraftType();
                Flight flight = flightController.createFlight(
                        flightNumber,
                        airlineName,
                        aircraftType,
                        category,
                        flightType,
                        scheduledTime,
                        emergencyStatus
                );

                // Set fuel level
                int fuelLevel = random.nextInt(90) + 10;
                flight.setFuelLevel(fuelLevel);

                if (emergencyStatus == Flight.EmergencyStatus.LOW_FUEL) {
                    flight.setFuelLevel(random.nextInt(10) + 5);
                }

                simulatedFlights.add(flight);

            } catch (Exception e) {
                System.err.println("Error generating simulated flight: " + e.getMessage());
            }
        }

        return simulatedFlights;
    }

    // Helper methods
    private String formatCallsign(String callsign) {
        if (callsign.length() <= 3) {
            return callsign + random.nextInt(1000);
        } else {
            // Extract airline code and add numbers
            String airlineCode = callsign.substring(0, 3);
            return airlineCode + random.nextInt(1000);
        }
    }

    private String generateAirlineName(String callsign) {
        String prefix = callsign.length() >= 3 ? callsign.substring(0, 3) : callsign;

        // Map common airline codes to names
        switch (prefix) {
            case "AAL": return "American Airlines";
            case "UAL": return "United Airlines";
            case "DAL": return "Delta Airlines";
            case "BAW": return "British Airways";
            case "DLH": return "Lufthansa";
            case "AFR": return "Air France";
            case "KLM": return "KLM Royal Dutch Airlines";
            case "UAE": return "Emirates";
            case "SIA": return "Singapore Airlines";
            case "QTR": return "Qatar Airways";
            default: return prefix + " Airlines";
        }
    }

    private String generateAircraftType() {
        String[] commonAircraft = {
                "Boeing 737-800",
                "Airbus A320",
                "Boeing 777-300ER",
                "Boeing 787-9",
                "Airbus A350-900",
                "Airbus A330-300",
                "Boeing 767-300",
                "Bombardier CRJ-900",
                "Embraer E190",
                "Airbus A380-800"
        };

        return commonAircraft[random.nextInt(commonAircraft.length)];
    }

    private Flight.WakeTurbulenceCategory generateRandomCategory() {
        int rand = random.nextInt(100);

        if (rand < 10) {
            return Flight.WakeTurbulenceCategory.LIGHT;
        } else if (rand < 70) {
            return Flight.WakeTurbulenceCategory.MEDIUM;
        } else if (rand < 95) {
            return Flight.WakeTurbulenceCategory.HEAVY;
        } else {
            return Flight.WakeTurbulenceCategory.SUPER;
        }
    }

    private Flight.EmergencyStatus generateRandomEmergencyStatus() {
        int rand = random.nextInt(100);

        if (rand < 90) {
            return Flight.EmergencyStatus.NONE;
        } else if (rand < 92) {
            return Flight.EmergencyStatus.MINOR_MECHANICAL;
        } else if (rand < 94) {
            return Flight.EmergencyStatus.LOW_FUEL;
        } else if (rand < 96) {
            return Flight.EmergencyStatus.MEDICAL;
        } else if (rand < 98) {
            return Flight.EmergencyStatus.MAJOR_MECHANICAL;
        } else {
            return Flight.EmergencyStatus.CRITICAL;
        }
    }

    // Random weather generation - updates the weather controller
    public void generateRandomWeather() {
        // Random wind speed between 0-30 km/h
        double windSpeed = random.nextDouble() * 30;

        // Random wind direction 0-359 degrees
        int windDirection = random.nextInt(360);

        // Random visibility 1-20 km (weighted toward good visibility)
        double visibility = 5.0 + (random.nextDouble() * 15.0);

        // Random weather condition (weighted toward good weather)
        Weather.WeatherCondition condition;
        int rand = random.nextInt(100);

        if (rand < 60) {
            condition = Weather.WeatherCondition.SUNNY;
        } else if (rand < 80) {
            condition = Weather.WeatherCondition.CLOUDY;
        } else if (rand < 90) {
            condition = Weather.WeatherCondition.RAINY;
        } else if (rand < 95) {
            condition = Weather.WeatherCondition.FOGGY;
        } else if (rand < 98) {
            condition = Weather.WeatherCondition.SNOWY;
        } else {
            condition = Weather.WeatherCondition.THUNDERSTORM;
        }

        // Update weather controller
        weatherController.updateWeather(windSpeed, windDirection, visibility, condition);
    }

    // Fetch data from OpenSky API
    private String fetchFromOpenSky(String endpoint) throws Exception {
        URL url = new URL(OPENSKY_API_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Check response code
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("Failed to fetch data from OpenSky API. Response code: " + responseCode);
        }

        // Read response
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return response.toString();
    }
}