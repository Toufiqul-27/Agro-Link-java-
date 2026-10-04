module com.agrolinkbd.agrolinkbd {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.agrolinkbd.agrolinkbd to javafx.fxml, javafx.base;

    exports com.agrolinkbd.agrolinkbd;
}