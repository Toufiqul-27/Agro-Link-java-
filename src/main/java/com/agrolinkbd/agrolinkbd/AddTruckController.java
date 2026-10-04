package com.agrolinkbd.agrolinkbd;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.*;

public class AddTruckController {
    @FXML private TextField routeField, capacityField, costField;
    @FXML private DatePicker datePicker;

    @FXML void handleSave() {
        String sql = "INSERT INTO trucks (route, capacity_kg, cost_per_kg, departure_date, status) VALUES (?, ?, ?, ?, 'Available')";
        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, routeField.getText());
            pstmt.setInt(2, Integer.parseInt(capacityField.getText()));
            pstmt.setDouble(3, Double.parseDouble(costField.getText()));
            pstmt.setDate(4, Date.valueOf(datePicker.getValue()));
            pstmt.executeUpdate();

            ((Stage) routeField.getScene().getWindow()).close();
        } catch (Exception e) { e.printStackTrace(); }
    }
}