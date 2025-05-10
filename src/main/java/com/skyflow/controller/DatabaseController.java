package com.skyflow.controller;

import com.skyflow.model.Flight;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseController {
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:skyflow.db";

    // Constructor
    public DatabaseController() {
        try {
            // Create a connection to the database
            connection = DriverManager.getConnection(DB_URL);

            // Create tables if they don't exist
            createTablesIfNotExist();
            createWeatherPresetsTable();

            System.out.println("Database connection established successfully.");
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Create the database tables if they don't already exist
    private void createTablesIfNotExist() {
        try (Statement statement = connection.createStatement()) {
            // Create Airlines table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS airlines (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "name TEXT NOT NULL, " +
                            "code TEXT NOT NULL UNIQUE" +
                            ")"
            );

            // Create Aircrafts table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS aircrafts (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "name TEXT NOT NULL, " +
                            "fuel_capacity INTEGER NOT NULL, " +
                            "weight INTEGER NOT NULL, " +
                            "aircraft_type TEXT NOT NULL, " +
                            "turbulence_category TEXT NOT NULL, " +
                            "CONSTRAINT unique_aircraft_name UNIQUE (name)" +
                            ")"
            );

            System.out.println("Database tables initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Get all airlines from the database
    public List<Map<String, String>> getAllAirlines() {
        List<Map<String, String>> airlines = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT icao_code, airline_name FROM airlines ORDER BY airline_name")) {

            while (resultSet.next()) {
                Map<String, String> airline = new HashMap<>();
                airline.put("code", resultSet.getString("icao_code"));
                airline.put("name", resultSet.getString("airline_name"));
                airlines.add(airline);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving airlines: " + e.getMessage());
            e.printStackTrace();
        }

        return airlines;
    }

    // Get all aircraft from the database
    public List<Map<String, Object>> getAllAircraft() {
        List<Map<String, Object>> aircraft = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM aircrafts ORDER BY name")) {

            while (resultSet.next()) {
                Map<String, Object> aircraftDetails = new HashMap<>();
                aircraftDetails.put("id", resultSet.getInt("id"));
                aircraftDetails.put("name", resultSet.getString("name"));
                aircraftDetails.put("fuelCapacity", resultSet.getInt("fuel_capacity"));
                aircraftDetails.put("weight", resultSet.getInt("weight"));
                aircraftDetails.put("aircraftType", resultSet.getString("aircraft_type"));
                aircraftDetails.put("turbulenceCategory", resultSet.getString("turbulence_category"));
                aircraft.add(aircraftDetails);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving aircraft: " + e.getMessage());
            e.printStackTrace();
        }

        return aircraft;
    }

    // Get aircraft by name
    public Map<String, Object> getAircraftByName(String name) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM aircrafts WHERE name = ?")) {

            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                Map<String, Object> aircraft = new HashMap<>();
                aircraft.put("id", resultSet.getInt("id"));
                aircraft.put("name", resultSet.getString("name"));
                aircraft.put("fuel_capacity", resultSet.getInt("fuel_capacity"));
                aircraft.put("weight", resultSet.getInt("weight"));
                aircraft.put("aircraft_type", resultSet.getString("aircraft_type"));
                aircraft.put("turbulence_category", resultSet.getString("turbulence_category"));
                return aircraft;
            }

            resultSet.close();
        } catch (SQLException e) {
            System.err.println("Error retrieving aircraft by name: " + e.getMessage());
        }

        return null;
    }

    // Get airline by code
    public Map<String, String> getAirlineByCode(String code) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT icao_code, airline_name FROM airlines WHERE icao_code = ?")) {

            statement.setString(1, code);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                Map<String, String> airline = new HashMap<>();
                airline.put("code", resultSet.getString("icao_code"));
                airline.put("name", resultSet.getString("airline_name"));
                return airline;
            }

            resultSet.close();
        } catch (SQLException e) {
            System.err.println("Error retrieving airline by code: " + e.getMessage());
        }

        return null;
    }

    // Get airline by name
    public Map<String, String> getAirlineByName(String name) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name, code FROM airlines WHERE name = ?")) {

            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                Map<String, String> airline = new HashMap<>();
                airline.put("name", resultSet.getString("name"));
                airline.put("code", resultSet.getString("code"));
                return airline;
            }

            resultSet.close();
        } catch (SQLException e) {
            System.err.println("Error retrieving airline by name: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // Get aircraft by turbulence category
    public List<Map<String, Object>> getAircraftByCategory(Flight.WakeTurbulenceCategory category) {
        List<Map<String, Object>> aircraft = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM aircrafts WHERE turbulence_category = ? ORDER BY name")) {

            statement.setString(1, category.name());
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                Map<String, Object> aircraftDetails = new HashMap<>();
                aircraftDetails.put("id", resultSet.getInt("id"));
                aircraftDetails.put("name", resultSet.getString("name"));
                aircraftDetails.put("fuel_capacity", resultSet.getInt("fuel_capacity"));
                aircraftDetails.put("weight", resultSet.getInt("weight"));
                aircraftDetails.put("aircraft_type", resultSet.getString("aircraft_type"));
                aircraftDetails.put("turbulence_category", resultSet.getString("turbulence_category"));
                aircraft.add(aircraftDetails);
            }

            resultSet.close();
        } catch (SQLException e) {
            System.err.println("Error retrieving aircraft by category: " + e.getMessage());
        }

        return aircraft;
    }

    // Get random aircraft of specific category
    public Map<String, Object> getRandomAircraftByCategory(Flight.WakeTurbulenceCategory category) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM aircrafts WHERE turbulence_category = ? ORDER BY RANDOM() LIMIT 1")) {

            statement.setString(1, category.name());
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                Map<String, Object> aircraft = new HashMap<>();
                aircraft.put("id", resultSet.getInt("id"));
                aircraft.put("name", resultSet.getString("name"));
                aircraft.put("fuelCapacity", resultSet.getInt("fuel_capacity"));
                aircraft.put("weight", resultSet.getInt("weight"));
                aircraft.put("aircraftType", resultSet.getString("aircraft_type"));
                aircraft.put("turbulenceCategory", resultSet.getString("turbulence_category"));
                return aircraft;
            }

            resultSet.close();
        } catch (SQLException e) {
            System.err.println("Error retrieving random aircraft: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // Close database connection
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[:] Closing database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // Methods for managing weather presets in the database
    public void createWeatherPresetsTable() {
        try (Statement statement = connection.createStatement()) {
            // Create Weather Presets table
            statement.execute(
                    "CREATE TABLE IF NOT EXISTS weather_presets (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "name TEXT NOT NULL UNIQUE, " +
                            "wind_speed REAL NOT NULL, " +
                            "wind_direction INTEGER NOT NULL, " +
                            "visibility REAL NOT NULL, " +
                            "condition TEXT NOT NULL" +
                            ")"
            );

            // Check if table is empty and populate with default presets
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM weather_presets");
            if (resultSet.next() && resultSet.getInt(1) == 0) {
                populateDefaultWeatherPresets();
            }
            resultSet.close();

            System.out.println("Weather presets table initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Error creating weather presets table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Populate default weather presets
    private void populateDefaultWeatherPresets() {
        try (Statement statement = connection.createStatement()) {
            // Insert default weather presets
            statement.execute("INSERT INTO weather_presets (name, wind_speed, wind_direction, visibility, condition) " +
                    "VALUES ('Clear Day', 5.0, 90, 25.0, 'SUNNY')");
            statement.execute("INSERT INTO weather_presets (name, wind_speed, wind_direction, visibility, condition) " +
                    "VALUES ('Light Breeze', 10.0, 180, 20.0, 'SUNNY')");
            statement.execute("INSERT INTO weather_presets (name, wind_speed, wind_direction, visibility, condition) " +
                    "VALUES ('Cloudy Morning', 7.5, 270, 15.0, 'CLOUDY')");
            statement.execute("INSERT INTO weather_presets (name, wind_speed, wind_direction, visibility, condition) " +
                    "VALUES ('Rainy Afternoon', 12.0, 45, 8.0, 'RAINY')");
            statement.execute("INSERT INTO weather_presets (name, wind_speed, wind_direction, visibility, condition) " +
                    "VALUES ('Heavy Fog', 3.0, 135, 2.5, 'FOGGY')");
            statement.execute("INSERT INTO weather_presets (name, wind_speed, wind_direction, visibility, condition) " +
                    "VALUES ('Winter Storm', 15.0, 315, 5.0, 'SNOWY')");
            statement.execute("INSERT INTO weather_presets (name, wind_speed, wind_direction, visibility, condition) " +
                    "VALUES ('Summer Storm', 25.0, 225, 4.0, 'THUNDERSTORM')");

            System.out.println("Default weather presets added successfully.");
        } catch (SQLException e) {
            System.err.println("Error populating weather presets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Get all weather presets from the database
    public List<Map<String, Object>> getAllWeatherPresets() {
        List<Map<String, Object>> presets = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM weather_presets ORDER BY name")) {

            while (resultSet.next()) {
                Map<String, Object> preset = new HashMap<>();
                preset.put("id", resultSet.getInt("id"));
                preset.put("name", resultSet.getString("name"));
                preset.put("windSpeed", resultSet.getDouble("wind_speed"));
                preset.put("windDirection", resultSet.getInt("wind_direction"));
                preset.put("visibility", resultSet.getDouble("visibility"));
                preset.put("condition", resultSet.getString("condition"));
                presets.add(preset);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving weather presets: " + e.getMessage());
            e.printStackTrace();
        }

        return presets;
    }

    // Get weather preset by name
    public Map<String, Object> getWeatherPresetByName(String name) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM weather_presets WHERE name = ?")) {

            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                Map<String, Object> preset = new HashMap<>();
                preset.put("id", resultSet.getInt("id"));
                preset.put("name", resultSet.getString("name"));
                preset.put("windSpeed", resultSet.getDouble("wind_speed"));
                preset.put("windDirection", resultSet.getInt("wind_direction"));
                preset.put("visibility", resultSet.getDouble("visibility"));
                preset.put("condition", resultSet.getString("condition"));
                return preset;
            }

            resultSet.close();
        } catch (SQLException e) {
            System.err.println("Error retrieving weather preset by name: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}