package com.skyflow.db;

import java.sql.*;

public class DatabaseController {
    private static final String path = "jdbc:sqlite:skyflow.db";

    public static Connection connect() {
        Connection connection = null;
        try {
            // Connect to db
            connection = DriverManager.getConnection(path);
            System.out.println("[:] Connect to the database successfully!");
        } catch (SQLException e) {
            System.out.println("[!] Failed to connect to the database! -> " + e.getMessage());
        }
        return connection;
    }

    public static void createTables(){
        String aircrafts =  """
                CREATE TABLE IF NOT EXISTS aircrafts (
                        aircraft_id INTEGER PRIMARY KEY AUTOINCREMENT,
                        model TEXT NOT NULL,
                        manufacturer TEXT NOT NULL,
                        capacity INTEGER,
                        status TEXT
                );
            """;

        String flights = """
            CREATE TABLE IF NOT EXISTS flights (
                flight_id INTEGER PRIMARY KEY AUTOINCREMENT,
                aircraft_id INTEGER,
                departure_time TEXT,
                arrival_time TEXT,
                runway_id INTEGER,
                status TEXT,
                FOREIGN KEY (aircraft_id) REFERENCES aircrafts (aircraft_id),
                FOREIGN KEY (runway_id) REFERENCES runways (runway_id)
            );
            """;

        String runways = """
            CREATE TABLE IF NOT EXISTS runways (
                runway_id INTEGER PRIMARY KEY AUTOINCREMENT,
                code TEXT NOT NULL UNIQUE,
                length INTEGER,
                status TEXT
            );
            """;

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute(aircrafts);
            stmt.execute(flights);
            stmt.execute(runways);

            System.out.println("[:] All tables created successfully!");

        } catch (SQLException e) {
            System.out.println("[!] Failed to create tables -> " + e.getMessage());
        }
    }

    public static void insertData(){
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            stmt.execute("INSERT INTO aircrafts (model, manufacturer, capacity, status) VALUES " +
                    "('Boeing 737', 'Boeing', 180, 'Active')," +
                    "('Airbus A320', 'Airbus', 150, 'Active')," +
                    "('Embraer E195', 'Embraer', 120, 'Under Maintenance');");

            stmt.execute("INSERT INTO runways (code, length, status) VALUES " +
                    "('A1', 3000, 'Available')," +
                    "('B2', 2500, 'Under Maintenance')," +
                    "('C3', 2800, 'Available');");

            stmt.execute("INSERT INTO flights (aircraft_id, departure_time, arrival_time, runway_id, status) VALUES " +
                    "(1, '2024-03-12 14:00', '2024-03-12 16:30', 1, 'Scheduled')," +
                    "(2, '2024-03-12 15:00', '2024-03-12 18:00', 3, 'Scheduled')," +
                    "(3, '2024-03-12 16:45', '2024-03-12 19:15', 2, 'Delayed');");

            System.out.println("[:] Data inserted successfully!");

        } catch (SQLException e) {
            System.out.println("[!] Failed to insert data -> " + e.getMessage());
        }
    }

    public static void fetchAndDisplayData() {
        String query = """
                SELECT f.flight_id, a.model, r.code, f.departure_time, f.arrival_time, f.status 
                FROM flights f 
                JOIN aircrafts a ON f.aircraft_id = a.aircraft_id
                JOIN runways r ON f.runway_id = r.runway_id
                """;

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet result = stmt.executeQuery(query))  // returns set
        {
            System.out.println("\n===== Flights Data =====");
            while (result.next()) {
                System.out.println("Flight ID: " + result.getInt("flight_id") +
                        " | Aircraft: " + result.getString("model") +
                        " | Runway: " + result.getString("code") +
                        " | Departure: " + result.getString("departure_time") +
                        " | Arrival: " + result.getString("arrival_time") +
                        " | Status: " + result.getString("status"));
            }

        } catch (SQLException e) {
            System.out.println("[!] Failed to fetch data -> " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // createTables();
        // insertData();
        fetchAndDisplayData();
    }
}
