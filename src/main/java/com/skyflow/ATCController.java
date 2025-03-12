package com.skyflow;

import com.skyflow.model.Flight;
import com.skyflow.model.Runway;
import com.skyflow.model.Weather;
import com.skyflow.Scheduling; // Match your existing import
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ATCController {

    @FXML
    private TableView<Flight> flightTable;

    @FXML
    private TableColumn<Flight, String> flightIdColumn;

    @FXML
    private TableColumn<Flight, String> aircraftTypeColumn;

    @FXML
    private TableColumn<Flight, LocalDateTime> scheduledTimeColumn;

    @FXML
    private TableColumn<Flight, LocalDateTime> actualTimeColumn;

    @FXML
    private TableColumn<Flight, String> runwayColumn;

    @FXML
    private TextField flightIdField;

    @FXML
    private TextField aircraftTypeField;

    @FXML
    private TextField categoryField;

    @FXML
    private TextField destinationField;

    @FXML
    private Button addFlightButton;

    @FXML
    private Button scheduleButton;

    @FXML
    private Label weatherLabel;

    @FXML
    private TextField windSpeedField;

    @FXML
    private TextField windDirectionField;

    @FXML
    private TextField visibilityField;

    @FXML
    private Button updateWeatherButton;

    private Scheduling scheduling; // Changed to match your class name
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    public void initialize() {
        scheduling = new Scheduling();

        // Set up the flight table columns
        flightIdColumn.setCellValueFactory(new PropertyValueFactory<>("flightId"));
        aircraftTypeColumn.setCellValueFactory(new PropertyValueFactory<>("aircraftType"));
        scheduledTimeColumn.setCellValueFactory(new PropertyValueFactory<>("scheduledDepartureTime"));
        actualTimeColumn.setCellValueFactory(new PropertyValueFactory<>("actualDepartureTime"));
        runwayColumn.setCellValueFactory(new PropertyValueFactory<>("assignedRunway"));

        // Format the scheduled time column
        scheduledTimeColumn.setCellFactory(column -> {
            return new TableCell<Flight, LocalDateTime>() {
                @Override
                protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                    } else {
                        setText(item.format(timeFormatter));
                    }
                }
            };
        });

        // Format the actual time column
        actualTimeColumn.setCellFactory(column -> {
            return new TableCell<Flight, LocalDateTime>() {
                @Override
                protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                    } else {
                        setText(item.format(timeFormatter));
                    }
                }
            };
        });

        // Update weather display
        updateWeatherDisplay();

        // Add some test data for demonstration
        addTestData();
    }

    private void addTestData() {
        // Add test flights at 5-minute intervals starting now
        LocalDateTime now = LocalDateTime.now();

        scheduling.addTestFlight("AA123", "B737", 2, now.plusMinutes(5), "JFK");
        scheduling.addTestFlight("DL456", "A320", 2, now.plusMinutes(10), "ATL");
        scheduling.addTestFlight("UA789", "B787", 3, now.plusMinutes(15), "SFO");
        scheduling.addTestFlight("LH101", "A380", 4, now.plusMinutes(20), "FRA");
        scheduling.addTestFlight("BA202", "B777", 3, now.plusMinutes(25), "LHR");

        updateFlightTable();
    }

    private void updateFlightTable() {
        flightTable.getItems().clear();

        List<Flight> allFlights = new ArrayList<>();

        // Get queued flights
        List<Flight> queuedFlights = scheduling.getQueuedFlights();
        allFlights.addAll(queuedFlights);

        // Get scheduled flights
        List<Flight> scheduledFlights = scheduling.getAllScheduledFlights();
        allFlights.addAll(scheduledFlights);

        // Add all flights to the table
        flightTable.getItems().addAll(allFlights);

        flightTable.refresh();
    }

    private void updateWeatherDisplay() {
        Weather weather = scheduling.getCurrentWeather();
        weatherLabel.setText(String.format(
                "Wind: %.1f km/h at %d° | Visibility: %.0f m",
                weather.getWindSpeed(),
                weather.getWindDirection(),
                weather.getVisibility()
        ));
    }

    // Handles the add flight button click
    @FXML
    private void handleAddFlight() {
        try {
            String flightId = flightIdField.getText();
            String aircraftType = aircraftTypeField.getText();
            int category = Integer.parseInt(categoryField.getText());
            String destination = destinationField.getText();

            // Create a new flight with departure time 30 minutes from now
            LocalDateTime departureTime = LocalDateTime.now().plusMinutes(30);

            // Add the flight to scheduling
            scheduling.addTestFlight(flightId, aircraftType, category, departureTime, destination);

            updateFlightTable();
            clearFlightInputFields();
        } catch (NumberFormatException e) {
            // Handle invalid input
            System.err.println("Invalid input: " + e.getMessage());
        }
    }

    // Clears all flight input fields
    private void clearFlightInputFields() {
        flightIdField.clear();
        aircraftTypeField.clear();
        categoryField.clear();
        destinationField.clear();
    }

    // Handles the schedule button click.
    @FXML
    private void handleScheduleFlights() {
        List<Flight> newlyScheduledFlights = scheduling.runScheduling();

        System.out.println("Scheduled " + newlyScheduledFlights.size() + " flights");
        for (Flight flight : newlyScheduledFlights) {
            System.out.println("Flight: " + flight.getFlightId() +
                    ", Departure: " + flight.getActualDepartureTime() +
                    ", Runway: " + flight.getAssignedRunway());
        }
        updateFlightTable();
    }

    // Handles the update weather button click.
    @FXML
    private void handleUpdateWeather() {
        try {
            double windSpeed = Double.parseDouble(windSpeedField.getText());
            int windDirection = Integer.parseInt(windDirectionField.getText());
            double visibility = Double.parseDouble(visibilityField.getText());

            Weather newWeather = new Weather(windSpeed, windDirection, visibility);
            scheduling.updateWeather(newWeather);
            updateWeatherDisplay();
            clearWeatherInputFields();
        } catch (NumberFormatException e) {
            System.err.println("Invalid weather input: " + e.getMessage());
        }
    }

    // Clears all weather input fields
    private void clearWeatherInputFields() {
        windSpeedField.clear();
        windDirectionField.clear();
        visibilityField.clear();
    }
}