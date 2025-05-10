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
             ResultSet resultSet = statement.executeQuery("SELECT name, code FROM airlines ORDER BY name")) {

            while (resultSet.next()) {
                Map<String, String> airline = new HashMap<>();
                airline.put("name", resultSet.getString("name"));
                airline.put("code", resultSet.getString("code"));
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
                aircraft.put("fuelCapacity", resultSet.getInt("fuel_capacity"));
                aircraft.put("weight", resultSet.getInt("weight"));
                aircraft.put("aircraftType", resultSet.getString("aircraft_type"));
                aircraft.put("turbulenceCategory", resultSet.getString("turbulence_category"));
                return aircraft;
            }

            resultSet.close();
        } catch (SQLException e) {
            System.err.println("Error retrieving aircraft by name: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // Get airline by code
    public Map<String, String> getAirlineByCode(String code) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT icao_code, airline_name FROM airline WHERE icao_code = ?")) {

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
            e.printStackTrace();
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
                aircraftDetails.put("fuelCapacity", resultSet.getInt("fuel_capacity"));
                aircraftDetails.put("weight", resultSet.getInt("weight"));
                aircraftDetails.put("aircraftType", resultSet.getString("aircraft_type"));
                aircraftDetails.put("turbulenceCategory", resultSet.getString("turbulence_category"));
                aircraft.add(aircraftDetails);
            }

            resultSet.close();
        } catch (SQLException e) {
            System.err.println("Error retrieving aircraft by category: " + e.getMessage());
            e.printStackTrace();
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
}