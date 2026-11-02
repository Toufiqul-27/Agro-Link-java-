package com.agrolinkbd.agrolinkbd;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;

public class AddTruckController {
    @FXML private TextField routeField, capacityField, costField;
    @FXML private DatePicker datePicker;

    @FXML void handleSave() {
        String route = routeField.getText().trim();
        LocalDate departure = datePicker.getValue();
        if (route.isBlank() || departure == null || departure.isBefore(LocalDate.now())) {
            new Alert(Alert.AlertType.WARNING, "Enter a route and a future departure date.").showAndWait();
            return;
        }
        int capacity;
        double cost;
        try {
            capacity = Integer.parseInt(capacityField.getText().trim());
            cost = Double.parseDouble(costField.getText().trim());
            if (capacity <= 0 || !Double.isFinite(cost) || cost <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Capacity and cost must be positive numbers.").showAndWait();
            return;
        }
        if (!"Admin".equals(UserSession.loggedInRole)) {
            new Alert(Alert.AlertType.ERROR, "Admin access is required.").showAndWait();
            return;
        }

        String sql = "INSERT INTO trucks (route, capacity_kg, booked_weight_kg, "
                   + "cost_per_kg, departure_date, status) VALUES (?, ?, 0, ?, ?, 'Available')";
        try (Connection conn = Database.connect();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, route);
            statement.setInt(2, capacity);
            statement.setDouble(3, cost);
            statement.setDate(4, Date.valueOf(departure));
            statement.executeUpdate();
            // This view is on the primary stage: do not close the entire app.
            AgroLinkApp.sceneChange("AdminDashboard.fxml", "Admin", 1000, 700);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Unable to save truck.").showAndWait();
            e.printStackTrace();
        }
    }
}
