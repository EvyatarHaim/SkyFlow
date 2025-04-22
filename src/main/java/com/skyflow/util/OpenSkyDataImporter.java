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

public class OpenSkyDataImporter {
    private final FlightController flightController;
    private final WeatherController weatherController;
    private final Random random = new Random();
    private final Gson gson = new Gson();

    private static final String OPENSKY_API_URL = "https://opensky-network.org/api";

    // Constructor
    public OpenSkyDataImporter(FlightController flightController, WeatherController weatherController) {
        this.flightController = flightController;
        this.weatherController = weatherController;
    }

    // Import flights from OpenSky with random enhancements
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
            for (JsonElement stateElement : states) {
                if (count >= numFlights) break;

                JsonArray stateArray = stateElement.getAsJsonArray();
                if (stateArray != null && stateArray.size() >= 8) {
                    // Extract data from OpenSky API
                    String icao24 = stateArray.get(0).getAsString();
                    String callsign = stateArray.get(1).getAsString().trim();
                    String originCountry = stateArray.get(2).getAsString();
                    long timePosition = stateArray.get(3).isJsonNull() ? 0 : stateArray.get(3).getAsLong();

                    // Skip flights with missing data
                    if (callsign.isEmpty() || timePosition == 0) continue;

                    // Create flight with random enhancements
                    Flight flight = createEnhancedFlight(icao24, callsign, originCountry, timePosition);
                    if (flight != null) {
                        importedFlights.add(flight);
                        count++;
                    }
                }
            }

            System.out.println("Successfully imported " + importedFlights.size() + " flights from OpenSky");

        } catch (Exception e) {
            System.err.println("Error importing flights from OpenSky: " + e.getMessage());
            e.printStackTrace();
        }

        return importedFlights;
    }


//    // Import flights from OpenSky focused on TLV airport area
//    public List<Flight> importRealTimeFlights(int numFlights) {
//        List<Flight> importedFlights = new ArrayList<>();
//
//        try {
//            // TLV coordinates: approximately 32.01° N, 34.88° E
//            // Create a bounding box around TLV (roughly 150km square)
//            double minLat = 31.5;  // South boundary
//            double maxLat = 32.5;  // North boundary
//            double minLon = 34.4;  // West boundary
//            double maxLon = 35.4;  // East boundary
//
//            // Create the API query with bounding box
//            String endpoint = String.format("/states/all?lamin=%f&lomin=%f&lamax=%f&lomax=%f",
//                    minLat, minLon, maxLat, maxLon);
//
//            // Fetch states from OpenSky API
//            String response = fetchFromOpenSky(endpoint);
//
//            // Parse response
//            JsonObject jsonObject = gson.fromJson(response, JsonObject.class);
//            JsonArray states = jsonObject.getAsJsonArray("states");
//
//            // If no flights in the area, generate simulated TLV flights
//            if (states == null || states.size() == 0) {
//                System.out.println("No flights found in TLV area, generating simulated flights");
//                return generateSimulatedTLVFlights(numFlights);
//            }
//
//            // Limit to requested number of flights
//            int count = 0;
//            for (JsonElement stateElement : states) {
//                if (count >= numFlights) break;
//
//                JsonArray stateArray = stateElement.getAsJsonArray();
//                if (stateArray != null && stateArray.size() >= 8) {
//                    // Extract data from OpenSky API
//                    String icao24 = stateArray.get(0).getAsString();
//                    String callsign = stateArray.get(1).getAsString().trim();
//                    String originCountry = stateArray.get(2).getAsString();
//                    long timePosition = stateArray.get(3).isJsonNull() ? 0 : stateArray.get(3).getAsLong();
//
//                    // Extract position data (if we want to use it)
//                    double latitude = stateArray.get(6).isJsonNull() ? 0 : stateArray.get(6).getAsDouble();
//                    double longitude = stateArray.get(5).isJsonNull() ? 0 : stateArray.get(5).getAsDouble();
//
//                    // Skip flights with missing data
//                    if (callsign.isEmpty() || timePosition == 0) continue;
//
//                    // Enhance with TLV-specific information
//                    Flight flight = createTLVEnhancedFlight(icao24, callsign, originCountry, timePosition);
//                    if (flight != null) {
//                        importedFlights.add(flight);
//                        count++;
//                    }
//                }
//            }
//
//            System.out.println("Successfully imported " + importedFlights.size() + " flights from TLV area");
//
//            // If we found fewer flights than requested, add some simulated ones
//            if (importedFlights.size() < numFlights) {
//                int remainingFlights = numFlights - importedFlights.size();
//                List<Flight> simulatedFlights = generateSimulatedTLVFlights(remainingFlights);
//                importedFlights.addAll(simulatedFlights);
//                System.out.println("Added " + simulatedFlights.size() + " simulated TLV flights to reach requested count");
//            }
//
//        } catch (Exception e) {
//            System.err.println("Error importing flights from OpenSky: " + e.getMessage());
//            e.printStackTrace();
//
//            // If API fails, generate simulated flights as a fallback
//            importedFlights = generateSimulatedTLVFlights(numFlights);
//            System.out.println("Generated " + importedFlights.size() + " simulated TLV flights after API error");
//        }
//
//        return importedFlights;
//    }
//
//    // Helper method to create a TLV-enhanced flight
//    private Flight createTLVEnhancedFlight(String icao24, String callsign, String originCountry, long timePosition) {
//        try {
//            // Replace with TLV-specific flight number and airline if needed
//            String flightNumber = formatCallsign(callsign);
//            String airline = generateAirlineName(callsign);
//
//            // For Israeli carriers, keep original callsign but ensure proper airline names
//            if (originCountry.equals("Israel") || callsign.startsWith("ELY") ||
//                    callsign.startsWith("ISR") || callsign.startsWith("AIZ")) {
//
//                if (callsign.startsWith("ELY")) {
//                    airline = "El Al Israel Airlines";
//                } else if (callsign.startsWith("ISR")) {
//                    airline = "Israir Airlines";
//                } else if (callsign.startsWith("AIZ")) {
//                    airline = "Arkia Israeli Airlines";
//                } else {
//                    airline = callsign.substring(0, Math.min(3, callsign.length())) + " Israeli Airline";
//                }
//            }
//
//            // Generate random aircraft type
//            String aircraftType = generateAircraftType();
//
//            // Random wake turbulence category (weighted toward MEDIUM)
//            Flight.WakeTurbulenceCategory category = generateRandomCategory();
//
//            // Random flight type (arrival or departure)
//            Flight.FlightType flightType = random.nextBoolean() ?
//                    Flight.FlightType.ARRIVAL : Flight.FlightType.DEPARTURE;
//
//            // Create a scheduled time based on current time
//            LocalDateTime now = LocalDateTime.now();
//            LocalDateTime scheduledTime = now.plusMinutes(random.nextInt(180) + 15); // 15-195 minutes from now
//
//            // Randomly assign emergency status (10% chance of emergency)
//            Flight.EmergencyStatus emergencyStatus = generateRandomEmergencyStatus();
//
//            // Create flight object
//            Flight flight = flightController.createFlight(
//                    flightNumber,
//                    airline,
//                    aircraftType,
//                    category,
//                    flightType,
//                    scheduledTime,
//                    emergencyStatus
//            );
//
//            // Set random fuel level
//            int fuelLevel = random.nextInt(90) + 10; // 10% to 100%
//            flight.setFuelLevel(fuelLevel);
//
//            // If emergency is low fuel, set lower fuel level
//            if (emergencyStatus == Flight.EmergencyStatus.LOW_FUEL) {
//                flight.setFuelLevel(random.nextInt(10) + 5); // 5% to 15%
//            }
//
//            return flight;
//
//        } catch (Exception e) {
//            System.err.println("Error creating TLV-enhanced flight: " + e.getMessage());
//            return null;
//        }
//    }
//
//    // Generate completely simulated TLV flights when no real data is available
//    private List<Flight> generateSimulatedTLVFlights(int numFlights) {
//        List<Flight> simulatedFlights = new ArrayList<>();
//
//        // TLV-specific airlines
//        String[][] tlvAirlines = {
//                {"ELY", "El Al Israel Airlines"},
//                {"ISR", "Israir Airlines"},
//                {"AIZ", "Arkia Israeli Airlines"},
//                {"LY", "El Al"},
//                {"6H", "Israir"},
//                {"IZ", "Arkia"},
//                {"LH", "Lufthansa"},
//                {"OS", "Austrian Airlines"},
//                {"BA", "British Airways"},
//                {"AF", "Air France"},
//                {"TK", "Turkish Airlines"},
//                {"SU", "Aeroflot"},
//                {"DL", "Delta Airlines"},
//                {"UA", "United Airlines"},
//                {"MS", "Egypt Air"},
//                {"RJ", "Royal Jordanian"}
//        };
//
//        for (int i = 0; i < numFlights; i++) {
//            try {
//                // Select a random airline
//                int airlineIndex = random.nextInt(tlvAirlines.length);
//                String airlineCode = tlvAirlines[airlineIndex][0];
//                String airlineName = tlvAirlines[airlineIndex][1];
//
//                // Generate a flight number
//                int flightNum = random.nextInt(900) + 100; // 100-999
//                String flightNumber = airlineCode + flightNum;
//
//                // Generate random aircraft type
//                String aircraftType = generateAircraftType();
//
//                // Random wake turbulence category
//                Flight.WakeTurbulenceCategory category = generateRandomCategory();
//
//                // Random flight type (arrival or departure)
//                Flight.FlightType flightType = random.nextBoolean() ?
//                        Flight.FlightType.ARRIVAL : Flight.FlightType.DEPARTURE;
//
//                // Create a scheduled time
//                LocalDateTime now = LocalDateTime.now();
//                LocalDateTime scheduledTime = now.plusMinutes(random.nextInt(180) + 15);
//
//                // Random emergency status (10% chance)
//                Flight.EmergencyStatus emergencyStatus = generateRandomEmergencyStatus();
//
//                // Create flight
//                Flight flight = flightController.createFlight(
//                        flightNumber,
//                        airlineName,
//                        aircraftType,
//                        category,
//                        flightType,
//                        scheduledTime,
//                        emergencyStatus
//                );
//
//                // Set fuel level
//                int fuelLevel = random.nextInt(90) + 10;
//                flight.setFuelLevel(fuelLevel);
//
//                if (emergencyStatus == Flight.EmergencyStatus.LOW_FUEL) {
//                    flight.setFuelLevel(random.nextInt(10) + 5);
//                }
//
//                simulatedFlights.add(flight);
//
//            } catch (Exception e) {
//                System.err.println("Error generating simulated TLV flight: " + e.getMessage());
//            }
//        }
//
//        return simulatedFlights;
//    }

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