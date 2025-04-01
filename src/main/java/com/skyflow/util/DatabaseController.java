package com.skyflow.util;

import com.skyflow.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DatabaseController {
    private Connection connection;
    private final String DB_URL = "jdbc:sqlite:skyflow.db";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Constructor
    public DatabaseController() {
        try {
            // Create a connection to the database
            connection = DriverManager.getConnection(DB_URL);

            // Create tables if they don't exist
            createTables();

            System.out.println("Database connection established successfully.");
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            // For development purposes, print stack trace
            e.printStackTrace();
        }
    }

    // Create database tables
    private void createTables() {
        String createFlightTable =
                "CREATE TABLE IF NOT EXISTS flights (" +
                        "id TEXT PRIMARY KEY, " +
                        "flightNumber TEXT, " +
                        "airline TEXT, " +
                        "aircraft TEXT, " +
                        "category TEXT, " +
                        "type TEXT, " +
                        "scheduledTime TEXT, " +
                        "actualTime TEXT, " +
                        "emergencyStatus TEXT, " +
                        "fuelLevel INTEGER, " +
                        "assignedRunway TEXT)";

        String createRunwayTable =
                "CREATE TABLE IF NOT EXISTS runways (" +
                        "id TEXT PRIMARY KEY, " +
                        "heading INTEGER, " +
                        "length INTEGER, " +
                        "nextAvailableTime TEXT, " +
                        "active INTEGER)";

        String createWeatherTable =
                "CREATE TABLE IF NOT EXISTS weather (" +
                        "id INTEGER PRIMARY KEY CHECK (id = 1), " +  // Ensure only one weather record
                        "windSpeed REAL, " +
                        "windDirection INTEGER, " +
                        "visibility REAL, " +
                        "condition TEXT)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createFlightTable);
            stmt.execute(createRunwayTable);
            stmt.execute(createWeatherTable);
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Save a flight to the database
    public void saveFlight(Flight flight) {
        String sql =
                "INSERT INTO flights (id, flightNumber, airline, aircraft, category, " +
                        "type, scheduledTime, actualTime, emergencyStatus, fuelLevel, assignedRunway) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, flight.getId());
            pstmt.setString(2, flight.getFlightNumber());
            pstmt.setString(3, flight.getAirline());
            pstmt.setString(4, flight.getAircraft());
            pstmt.setString(5, flight.getCategory().name());
            pstmt.setString(6, flight.getType().name());
            pstmt.setString(7, flight.getScheduledTime().format(DTF));

            // Handle nullable actual time
            if (flight.getActualTime() != null) {
                pstmt.setString(8, flight.getActualTime().format(DTF));
            } else {
                pstmt.setNull(8, Types.VARCHAR);
            }

            pstmt.setString(9, flight.getEmergencyStatus().name());
            pstmt.setInt(10, flight.getFuelLevel());

            // Handle nullable assigned runway
            if (flight.getAssignedRunway() != null) {
                pstmt.setString(11, flight.getAssignedRunway().getId());
            } else {
                pstmt.setNull(11, Types.VARCHAR);
            }

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving flight: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Update an existing flight
    public void updateFlight(Flight flight) {
        String sql =
                "UPDATE flights SET flightNumber = ?, airline = ?, aircraft = ?, " +
                        "category = ?, type = ?, scheduledTime = ?, actualTime = ?, " +
                        "emergencyStatus = ?, fuelLevel = ?, assignedRunway = ? " +
                        "WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, flight.getFlightNumber());
            pstmt.setString(2, flight.getAirline());
            pstmt.setString(3, flight.getAircraft());
            pstmt.setString(4, flight.getCategory().name());
            pstmt.setString(5, flight.getType().name());
            pstmt.setString(6, flight.getScheduledTime().format(DTF));

            // Handle nullable actual time
            if (flight.getActualTime() != null) {
                pstmt.setString(7, flight.getActualTime().format(DTF));
            } else {
                pstmt.setNull(7, Types.VARCHAR);
            }

            pstmt.setString(8, flight.getEmergencyStatus().name());
            pstmt.setInt(9, flight.getFuelLevel());

            // Handle nullable assigned runway
            if (flight.getAssignedRunway() != null) {
                pstmt.setString(10, flight.getAssignedRunway().getId());
            } else {
                pstmt.setNull(10, Types.VARCHAR);
            }

            pstmt.setString(11, flight.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating flight: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Delete a flight
    public void deleteFlight(Flight flight) {
        String sql = "DELETE FROM flights WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, flight.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting flight: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Load all flights
    public List<Flight> loadAllFlights() {
        List<Flight> flights = new ArrayList<>();
        String sql = "SELECT * FROM flights";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String id = rs.getString("id");
                String flightNumber = rs.getString("flightNumber");
                String airline = rs.getString("airline");
                String aircraft = rs.getString("aircraft");

                Flight.WakeTurbulenceCategory category = Flight.WakeTurbulenceCategory.valueOf(
                        rs.getString("category"));

                Flight.FlightType type = Flight.FlightType.valueOf(
                        rs.getString("type"));

                LocalDateTime scheduledTime = LocalDateTime.parse(
                        rs.getString("scheduledTime"), DTF);

                String actualTimeStr = rs.getString("actualTime");
                LocalDateTime actualTime = null;
                if (actualTimeStr != null && !actualTimeStr.isEmpty()) {
                    actualTime = LocalDateTime.parse(actualTimeStr, DTF);
                }

                Flight.EmergencyStatus emergencyStatus = Flight.EmergencyStatus.valueOf(
                        rs.getString("emergencyStatus"));

                int fuelLevel = rs.getInt("fuelLevel");

                // Create flight object (without runway for now)
                Flight flight = new Flight(id, flightNumber, airline, aircraft,
                        category, type, scheduledTime, emergencyStatus);

                flight.setFuelLevel(fuelLevel);

                if (actualTime != null) {
                    flight.setActualTime(actualTime);
                }

                // We'll set the runway separately after loading all runways
                flights.add(flight);
            }
        } catch (SQLException e) {
            System.err.println("Error loading flights: " + e.getMessage());
            e.printStackTrace();
        }

        return flights;
    }

    // Save a runway to the database
    public void saveRunway(Runway runway) {
        String sql =
                "INSERT INTO runways (id, heading, length, nextAvailableTime, active) " +
                        "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, runway.getId());
            pstmt.setInt(2, runway.getHeading());
            pstmt.setInt(3, runway.getLength());
            pstmt.setString(4, runway.getNextAvailableTime().format(DTF));
            pstmt.setInt(5, runway.isActive() ? 1 : 0);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving runway: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Update an existing runway
    public void updateRunway(Runway runway) {
        String sql =
                "UPDATE runways SET heading = ?, length = ?, " +
                        "nextAvailableTime = ?, active = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, runway.getHeading());
            pstmt.setInt(2, runway.getLength());
            pstmt.setString(3, runway.getNextAvailableTime().format(DTF));
            pstmt.setInt(4, runway.isActive() ? 1 : 0);
            pstmt.setString(5, runway.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating runway: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Delete a runway
    public void deleteRunway(Runway runway) {
        String sql = "DELETE FROM runways WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, runway.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting runway: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Load all runways
    public List<Runway> loadAllRunways() {
        List<Runway> runways = new ArrayList<>();
        String sql = "SELECT * FROM runways";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String id = rs.getString("id");
                int heading = rs.getInt("heading");
                int length = rs.getInt("length");

                LocalDateTime nextAvailableTime = LocalDateTime.parse(
                        rs.getString("nextAvailableTime"), DTF);

                boolean active = rs.getInt("active") == 1;

                // Create runway object
                Runway runway = new Runway(id, heading, length);
                runway.setNextAvailableTime(nextAvailableTime);
                runway.setActive(active);

                runways.add(runway);
            }
        } catch (SQLException e) {
            System.err.println("Error loading runways: " + e.getMessage());
            e.printStackTrace();
        }

        return runways;
    }

    // Save current weather to database
    public void saveWeather(Weather weather) {
        // First check if weather record exists
        String checkSql = "SELECT COUNT(*) FROM weather WHERE id = 1";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {

            int count = 0;
            if (rs.next()) {
                count = rs.getInt(1);
            }

            String sql;
            if (count > 0) {
                // Update existing weather
                sql = "UPDATE weather SET windSpeed = ?, windDirection = ?, " +
                        "visibility = ?, condition = ? WHERE id = 1";
            } else {
                // Insert new weather
                sql = "INSERT INTO weather (id, windSpeed, windDirection, " +
                        "visibility, condition) VALUES (1, ?, ?, ?, ?)";
            }

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setDouble(1, weather.getWindSpeed());
                pstmt.setInt(2, weather.getWindDirection());
                pstmt.setDouble(3, weather.getVisibility());
                pstmt.setString(4, weather.getCondition().name());

                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error saving weather: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Load current weather from database
    public Weather loadWeather() {
        String sql = "SELECT * FROM weather WHERE id = 1";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                double windSpeed = rs.getDouble("windSpeed");
                int windDirection = rs.getInt("windDirection");
                double visibility = rs.getDouble("visibility");
                Weather.WeatherCondition condition = Weather.WeatherCondition.valueOf(
                        rs.getString("condition"));

                return new Weather(windSpeed, windDirection, visibility, condition);
            }
        } catch (SQLException e) {
            System.err.println("Error loading weather: " + e.getMessage());
            e.printStackTrace();
        }

        // If no weather is found, return default
        return new Weather(5.0, 0, 10.0, Weather.WeatherCondition.SUNNY);
    }

    // Link flights with runways after loading
    public void linkFlightsWithRunways(List<Flight> flights, List<Runway> runways) {
        String sql = "SELECT id, assignedRunway FROM flights WHERE assignedRunway IS NOT NULL";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String flightId = rs.getString("id");
                String runwayId = rs.getString("assignedRunway");

                // Find the flight
                Flight flight = null;
                for (Flight f : flights) {
                    if (f.getId().equals(flightId)) {
                        flight = f;
                        break;
                    }
                }

                // Find the runway
                Runway runway = null;
                for (Runway r : runways) {
                    if (r.getId().equals(runwayId)) {
                        runway = r;
                        break;
                    }
                }

                // Link them if both exist
                if (flight != null && runway != null) {
                    flight.setAssignedRunway(runway);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error linking flights with runways: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Close database connection
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}