module com.skyflow.skyflow {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.skyflow.skyflow to javafx.fxml;
    exports com.skyflow.skyflow;
}