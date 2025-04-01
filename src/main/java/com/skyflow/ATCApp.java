package com.skyflow;

import com.skyflow.view.ATCViewController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class ATCApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Use getResource to load the FXML file correctly
        URL fxmlUrl = getClass().getResource("/com/skyflow/view/atc-view.fxml");

        // Check if the FXML file is found
        if (fxmlUrl == null) {
            throw new RuntimeException("Cannot find atc-view.fxml");
        }

        // Load the FXML
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        // Set up the scene
        Scene scene = new Scene(root);

        // Configure the stage
        primaryStage.setTitle("SkyFlow - Air Traffic Control System");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.show();
    }

    @Override
    public void stop() {
        // Clean up resources when application closes
        ATCViewController controller = ATCViewController.getInstance();
        if (controller != null) {
            controller.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}