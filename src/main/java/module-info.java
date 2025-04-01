module com.skyflow {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires org.slf4j;

    exports com.skyflow;
    exports com.skyflow.model;
    exports com.skyflow.controller;
    exports com.skyflow.view;
    exports com.skyflow.util;

    opens com.skyflow to javafx.fxml;
    opens com.skyflow.model to javafx.fxml;
    opens com.skyflow.controller to javafx.fxml;
    opens com.skyflow.view to javafx.fxml;
}