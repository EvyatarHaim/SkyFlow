package com.skyflow.view;

import com.skyflow.controller.*;
import com.skyflow.model.*;
import com.skyflow.util.*;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ATCViewController implements Initializable {
    // Singleton instance
    private static ATCViewController instance;

    // Controllers
    private SchedulingController schedulingController;
    private FlightController flightController;
    private RunwayController runwayController;
    private WeatherController weatherController;

    // Timeline for simulation updates
    private Timeline updateTimeline;

    private OpenSkyDataImporter openSkyImporter;

    // FXML UI Components - Flights Table
    @FXML private TableView<Flight> flightsTable;
    @FXML private TableColumn<Flight, String> colFlightId;
    @FXML private TableColumn<Flight, String> colAirline;
    @FXML private TableColumn<Flight, Flight.FlightType> colType;
    @FXML private TableColumn<Flight, LocalDateTime> colScheduled;
    @FXML private TableColumn<Flight, LocalDateTime> colActual;
    @FXML private TableColumn<Flight, Flight.EmergencyStatus> colEmergency;
    @FXML private TableColumn<Flight, String> colRunway;

    // FXML UI Components - Runways Table
    @FXML private TableView<Runway> runwaysTable;
    @FXML private TableColumn<Runway, String> colRunwayId;
    @FXML private TableColumn<Runway, Integer> colHeading;
    @FXML private TableColumn<Runway, Integer> colLength;
    @FXML private TableColumn<Runway, LocalDateTime> colNextAvailable;
    @FXML private TableColumn<Runway, Boolean> colActive;

    // FXML UI Components - Flight Form
    @FXML private TextField txtFlightNumber;
    @FXML private TextField txtAirline;
    @FXML private TextField txtAircraft;
    @FXML private ComboBox<Flight.WakeTurbulenceCategory> cboCategory;
    @FXML private ComboBox<Flight.FlightType> cboFlightType;
    @FXML private DatePicker dpScheduledDate;
    @FXML private TextField txtScheduledTime;
    @FXML private ComboBox<Flight.EmergencyStatus> cboEmergencyStatus;
    @FXML private Slider sldFuelLevel;
    @FXML private Label lblFuelPercentage;

    // FXML UI Components - Runway Form
    @FXML private TextField txtRunwayId;
    @FXML private TextField txtHeading;
    @FXML private TextField txtLength;
    @FXML private CheckBox chkActive;

    // FXML UI Components - Weather Form
    @FXML private TextField txtWindSpeed;
    @FXML private TextField txtWindDirection;
    @FXML private TextField txtVisibility;
    @FXML private ComboBox<Weather.WeatherCondition> cboWeatherCondition;

    // FXML UI Components - Status
    @FXML private Label lblStatus;
    @FXML private Label lblCurrentTime;
    @FXML private Label lblWeatherStatus;

    // Observable lists for the tables
    private ObservableList<Flight> flightsData = FXCollections.observableArrayList();
    private ObservableList<Runway> runwaysData = FXCollections.observableArrayList();

    // Constructor
    public ATCViewController() {
        // Set singleton instance
        instance = this;

        // Set up controllers without database
        schedulingController = new SchedulingController();
        flightController = new FlightController(schedulingController);
        runwayController = new RunwayController(schedulingController);
        weatherController = new WeatherController(schedulingController);

        // Create update timeline for simulation
        updateTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> updateSimulation())
        );
        updateTimeline.setCycleCount(Animation.INDEFINITE);

        // Initialize OpenSky importer
        openSkyImporter = new OpenSkyDataImporter(flightController, weatherController);
    }

    // Initialize method called after FXML is loaded
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize flights table
        colFlightId.setCellValueFactory(new PropertyValueFactory<>("flightNumber"));
        colAirline.setCellValueFactory(new PropertyValueFactory<>("airline"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colScheduled.setCellValueFactory(new PropertyValueFactory<>("scheduledTime"));
        colActual.setCellValueFactory(new PropertyValueFactory<>("actualTime"));
        colEmergency.setCellValueFactory(new PropertyValueFactory<>("emergencyStatus"));

// Format date time columns to show only HH:MM
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        colScheduled.setCellFactory(column -> new TableCell<Flight, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(timeFormatter.format(item));
                }
            }
        });

        colActual.setCellFactory(column -> new TableCell<Flight, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(timeFormatter.format(item));
                }
            }
        });

//        colScheduled.setCellValueFactory(new PropertyValueFactory<>("scheduledTime"));
//        colActual.setCellValueFactory(new PropertyValueFactory<>("actualTime"));

        // Custom cell factory for runway column
        colRunway.setCellValueFactory(cellData -> {
            Runway runway = cellData.getValue().getAssignedRunway();
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> runway != null ? runway.getId() : "Not Assigned"
            );
        });

        // Initialize runways table
        colRunwayId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colHeading.setCellValueFactory(new PropertyValueFactory<>("heading"));
        colLength.setCellValueFactory(new PropertyValueFactory<>("length"));
        colNextAvailable.setCellValueFactory(new PropertyValueFactory<>("nextAvailableTime"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));

        // Initialize combo boxes
        cboCategory.setItems(FXCollections.observableArrayList(Flight.WakeTurbulenceCategory.values()));
        cboFlightType.setItems(FXCollections.observableArrayList(Flight.FlightType.values()));
        cboEmergencyStatus.setItems(FXCollections.observableArrayList(Flight.EmergencyStatus.values()));
        cboWeatherCondition.setItems(FXCollections.observableArrayList(Weather.WeatherCondition.values()));

        // Set default values
        cboCategory.setValue(Flight.WakeTurbulenceCategory.MEDIUM);
        cboFlightType.setValue(Flight.FlightType.ARRIVAL);
        cboEmergencyStatus.setValue(Flight.EmergencyStatus.NONE);
        cboWeatherCondition.setValue(Weather.WeatherCondition.SUNNY);

        // Set up fuel level slider
        sldFuelLevel.valueProperty().addListener((obs, oldVal, newVal) -> {
            int value = newVal.intValue();
            lblFuelPercentage.setText(value + "%");
        });

        // Set the tables' data sources
        flightsTable.setItems(flightsData);
        runwaysTable.setItems(runwaysData);

        // Load data
        refreshData();

        // Start update timeline
        updateTimeline.play();
    }

    // Refresh all data from controllers
    private void refreshData() {
        // Update flights table
        flightsData.clear();
        flightsData.addAll(flightController.getAllFlights());

        // Sort flights by actual time, with latest at the bottom
        flightsData.sort((flight1, flight2) -> {
            // Handle null actual times (unscheduled flights)
            if (flight1.getActualTime() == null && flight2.getActualTime() == null) {
                return 0; // Both unscheduled, keep original order
            } else if (flight1.getActualTime() == null) {
                return -1; // Unscheduled flights come first
            } else if (flight2.getActualTime() == null) {
                return 1; // Unscheduled flights come first
            } else {
                // Both have actual times, compare them
                return flight1.getActualTime().compareTo(flight2.getActualTime());
            }
        });

        // Update runways table
        runwaysData.clear();
        runwaysData.addAll(runwayController.getAllRunways());

        // Update weather display
        Weather weather = weatherController.getCurrentWeather();
        lblWeatherStatus.setText(String.format(
                "Wind: %.1f km/h at %d° - Visibility: %.1f km - Condition: %s",
                weather.getWindSpeed(), weather.getWindDirection(),
                weather.getVisibility(), weather.getCondition().name()));

        // Update weather form
        txtWindSpeed.setText(String.valueOf(weather.getWindSpeed()));
        txtWindDirection.setText(String.valueOf(weather.getWindDirection()));
        txtVisibility.setText(String.valueOf(weather.getVisibility()));
        cboWeatherCondition.setValue(weather.getCondition());
    }

    // Update simulation
    private void updateSimulation() {
        // Update current time display
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        lblCurrentTime.setText("Current Time: " + LocalDateTime.now().format(timeFormatter));

        List<Flight> scheduled = schedulingController.scheduleFlights();

        // Update status without mentioning scheduled flights
        // lblStatus.setText("Last Update: " + LocalDateTime.now().format(timeFormatter));

        // Refresh data
        Platform.runLater(this::refreshData);
    }

    @FXML
    private void handleImportRealTimeFlights() {
        try {
            // Show a dialog to get number of flights to import
            TextInputDialog dialog = new TextInputDialog("10");
            dialog.setTitle("Import Real-Time Flights");
            dialog.setHeaderText("How many flights would you like to import?");
            dialog.setContentText("Number of flights:");

            // Get the result
            dialog.showAndWait().ifPresent(numFlightsStr -> {
                try {
                    int numFlights = Integer.parseInt(numFlightsStr);

                    if (numFlights <= 0 || numFlights > 100) {
                        showAlert("Invalid Number", "Please enter a number between 1 and 100.");
                        return;
                    }

                    // Generate random weather as well
                    openSkyImporter.generateRandomWeather();

                    // Import flights
                    List<Flight> importedFlights = openSkyImporter.importRealTimeFlights(numFlights);

                    // Refresh the UI
                    refreshData();

                    // Update status
                    lblStatus.setText("Imported " + importedFlights.size() + " flights with random enhancements");

                } catch (NumberFormatException e) {
                    showAlert("Invalid Input", "Please enter a valid number.");
                }
            });
        } catch (Exception e) {
            showAlert("Import Error", "Failed to import flights: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Add a new flight to the system
    @FXML
    private void handleAddFlight() {
        try {
            // Validate inputs
            String flightNumber = txtFlightNumber.getText().trim();
            String airline = txtAirline.getText().trim();
            String aircraft = txtAircraft.getText().trim();

            // Check for empty fields
            if (flightNumber.isEmpty() || airline.isEmpty() || aircraft.isEmpty() ||
                    dpScheduledDate.getValue() == null || txtScheduledTime.getText().trim().isEmpty()) {
                showAlert("Missing Data", "Please fill in all required fields.");
                return;
            }

            // Parse scheduled time
            LocalDateTime scheduledTime;
            try {
                String timeStr = txtScheduledTime.getText().trim();
                scheduledTime = LocalDateTime.of(
                        dpScheduledDate.getValue(),
                        LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
                );
            } catch (Exception e) {
                showAlert("Invalid Time", "Please enter time in format HH:MM (e.g., 14:30).");
                return;
            }

            // Create new flight
            Flight flight = flightController.createFlight(
                    flightNumber,
                    airline,
                    aircraft,
                    cboCategory.getValue(),
                    cboFlightType.getValue(),
                    scheduledTime,
                    cboEmergencyStatus.getValue()
            );

            // Set fuel level
            flight.setFuelLevel((int) sldFuelLevel.getValue());

            // Update flight
            flightController.updateFlight(flight);

            // Clear form
            clearFlightForm();

            // Show success message
            lblStatus.setText("Flight " + flightNumber + " added successfully.");

        } catch (Exception e) {
            // Log the error
            e.printStackTrace();

            // Show error alert
            showAlert("Error", "Failed to add flight: " + e.getMessage());
        }
    }

    // Add new runway button handler
    @FXML
    private void handleAddRunway() {
        try {
            // Validate inputs
            String runwayId = txtRunwayId.getText().trim();

            if (runwayId.isEmpty() || txtHeading.getText().trim().isEmpty() ||
                    txtLength.getText().trim().isEmpty()) {
                showAlert("Missing Data", "Please fill in all required fields.");
                return;
            }

            // Parse numeric values
            int heading, length;
            try {
                heading = Integer.parseInt(txtHeading.getText().trim());
                length = Integer.parseInt(txtLength.getText().trim());

                // Validate heading
                if (heading < 0 || heading > 360) {
                    showAlert("Invalid Heading", "Heading must be between 0 and 360 degrees.");
                    return;
                }

                // Validate length
                if (length <= 0) {
                    showAlert("Invalid Length", "Runway length must be positive.");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Invalid Number", "Heading and length must be valid numbers.");
                return;
            }

            // Create new runway
            Runway runway = runwayController.createRunway(
                    runwayId, heading, length
            );

            // Set active status
            runway.setActive(chkActive.isSelected());

            // Update runway
            runwayController.updateRunway(runway);

            // Clear form
            clearRunwayForm();

            // Refresh data
            refreshData();

            // Show success message
            lblStatus.setText("Runway " + runwayId + " added successfully.");
        } catch (Exception e) {
            showAlert("Error", "Failed to add runway: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Update weather button handler
    @FXML
    private void handleUpdateWeather() {
        try {
            // Validate inputs
            if (txtWindSpeed.getText().trim().isEmpty() ||
                    txtWindDirection.getText().trim().isEmpty() ||
                    txtVisibility.getText().trim().isEmpty() ||
                    cboWeatherCondition.getValue() == null) {
                showAlert("Missing Data", "Please fill in all required fields.");
                return;
            }

            // Parse numeric values
            double windSpeed, visibility;
            int windDirection;
            try {
                windSpeed = Double.parseDouble(txtWindSpeed.getText().trim());
                windDirection = Integer.parseInt(txtWindDirection.getText().trim());
                visibility = Double.parseDouble(txtVisibility.getText().trim());

                // Validate values
                if (windSpeed < 0) {
                    showAlert("Invalid Wind Speed", "Wind speed must be non-negative.");
                    return;
                }

                if (windDirection < 0 || windDirection > 360) {
                    showAlert("Invalid Wind Direction", "Wind direction must be between 0 and 360 degrees.");
                    return;
                }

                if (visibility < 0) {
                    showAlert("Invalid Visibility", "Visibility must be non-negative.");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Invalid Number", "Please enter valid numbers for weather parameters.");
                return;
            }

            // Update weather
            weatherController.updateWeather(
                    windSpeed, windDirection, visibility, cboWeatherCondition.getValue()
            );

            // Refresh data
            refreshData();

            // Show success message
            lblStatus.setText("Weather updated successfully.");
        } catch (Exception e) {
            showAlert("Error", "Failed to update weather: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Clear flight form
    @FXML
    private void clearFlightForm() {
        txtFlightNumber.clear();
        txtAirline.clear();
        txtAircraft.clear();
        cboCategory.setValue(Flight.WakeTurbulenceCategory.MEDIUM);
        cboFlightType.setValue(Flight.FlightType.ARRIVAL);
        dpScheduledDate.setValue(java.time.LocalDate.now());
        txtScheduledTime.clear();
        cboEmergencyStatus.setValue(Flight.EmergencyStatus.NONE);
        sldFuelLevel.setValue(100);
    }

    // Clear runway form
    @FXML
    private void clearRunwayForm() {
        txtRunwayId.clear();
        txtHeading.clear();
        txtLength.clear();
        chkActive.setSelected(true);
    }

    // Delete selected flight
    @FXML
    private void handleDeleteFlight() {
        Flight selectedFlight = flightsTable.getSelectionModel().getSelectedItem();

        if (selectedFlight == null) {
            showAlert("No Selection", "Please select a flight to delete.");
            return;
        }

        // Confirm deletion
        if (showConfirmation("Delete Flight",
                "Are you sure you want to delete flight " +
                        selectedFlight.getFlightNumber() + "?")) {
            flightController.deleteFlight(selectedFlight);
            refreshData();
            lblStatus.setText("Flight deleted successfully.");
        }
    }

    // Delete selected runway
    @FXML
    private void handleDeleteRunway() {
        Runway selectedRunway = runwaysTable.getSelectionModel().getSelectedItem();

        if (selectedRunway == null) {
            showAlert("No Selection", "Please select a runway to delete.");
            return;
        }

        // Confirm deletion
        if (showConfirmation("Delete Runway",
                "Are you sure you want to delete runway " +
                        selectedRunway.getId() + "?")) {
            runwayController.deleteRunway(selectedRunway);
            refreshData();
            lblStatus.setText("Runway deleted successfully.");
        }
    }

    // Set emergency for selected flight
    @FXML
    private void handleSetEmergency() {
        Flight selectedFlight = flightsTable.getSelectionModel().getSelectedItem();

        if (selectedFlight == null) {
            showAlert("No Selection", "Please select a flight to set emergency status.");
            return;
        }

        // Open dialog to select emergency type
        ChoiceDialog<Flight.EmergencyStatus> dialog = new ChoiceDialog<>(
                Flight.EmergencyStatus.NONE, Flight.EmergencyStatus.values()
        );
        dialog.setTitle("Set Emergency Status");
        dialog.setHeaderText("Select emergency status for flight " + selectedFlight.getFlightNumber());
        dialog.setContentText("Emergency status:");

        dialog.showAndWait().ifPresent(status -> {
            flightController.setEmergencyStatus(selectedFlight, status);
            refreshData();
            lblStatus.setText("Emergency status updated for flight " + selectedFlight.getFlightNumber());
        });
    }

    // Run scheduling algorithm manually
    @FXML
    private void handleRunScheduling() {
        List<Flight> scheduled = schedulingController.scheduleFlights();
        refreshData();
        lblStatus.setText("Scheduling completed: " + scheduled.size() + " flights scheduled.");
    }

    // Activate/deactivate selected runway
    @FXML
    private void handleToggleRunwayActive() {
        Runway selectedRunway = runwaysTable.getSelectionModel().getSelectedItem();

        if (selectedRunway == null) {
            showAlert("No Selection", "Please select a runway to toggle active status.");
            return;
        }

        runwayController.setRunwayActive(selectedRunway, !selectedRunway.isActive());
        refreshData();
        lblStatus.setText("Runway " + selectedRunway.getId() + " " +
                (selectedRunway.isActive() ? "activated" : "deactivated") + ".");
    }

    // Reset simulation
    @FXML
    private void handleResetSimulation() {
        if (showConfirmation("Reset Simulation",
                "Are you sure you want to reset the simulation? " +
                        "This will clear all flights.")) {

            // Get current flights and delete them
            List<Flight> currentFlights = new ArrayList<>(flightController.getAllFlights());
            for (Flight flight : currentFlights) {
                flightController.deleteFlight(flight);
            }

            // Reset runways to available state
//            for (Runway runway : runwayController.getAllRunways()) {
//                runway.setNextAvailableTime(LocalDateTime.now());
//                runwayController.updateRunway(runway);
//            }

            // Reset the scheduling controller
            schedulingController.reset();

            // Refresh UI
            refreshData();
            lblStatus.setText("Simulation reset successfully.");
        }
    }

    // Utility method to show an alert
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Utility method to show a confirmation dialog
    private boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    // Clean up resources
    public void shutdown() {
        // Stop the update timeline
        if (updateTimeline != null) {
            updateTimeline.stop();
        }
    }

    // Get singleton instance
    public static ATCViewController getInstance() {
        return instance;
    }
    
}