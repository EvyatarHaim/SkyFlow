module com.skyflow.model {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires org.slf4j;

    exports com.skyflow;
    opens com.skyflow to javafx.fxml;

    exports com.skyflow.model;
    opens com.skyflow.model to javafx.fxml;
}