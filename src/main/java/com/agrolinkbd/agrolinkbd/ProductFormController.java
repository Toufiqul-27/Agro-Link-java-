package com.agrolinkbd.agrolinkbd;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

public class ProductFormController {
    @FXML private Label formTitle, errorLabel;
    @FXML private ComboBox<String> cropTypeCombo;
    @FXML private TextField cropNameField, quantityField, priceField, pickupLocationField;
    @FXML private DatePicker harvestDatePicker;
    @FXML private TextArea descriptionArea;

    @FXML public void initialize() {
        cropTypeCombo.getItems().addAll("Vegetable", "Fruit", "Grain", "Spice");
    }

    @FXML void handleSave(ActionEvent event) {
        String type = cropTypeCombo.getValue();
        String name = cropNameField.getText().trim();
        String location = pickupLocationField.getText().trim();
        LocalDate date = harvestDatePicker.getValue();
        if (type == null || name.isBlank() || location.isBlank() || date == null) {
            errorLabel.setText("Crop, type, pickup location and harvest date are required.");
            return;
        }
        if (UserSession.loggedInUserId <= 0 || !"Farmer".equals(UserSession.loggedInRole)) {
            errorLabel.setText("Please sign in as a farmer.");
            return;
        }
        int quantity;
        double price;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
            price = Double.parseDouble(priceField.getText().trim());
            if (quantity <= 0 || !Double.isFinite(price) || price <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            errorLabel.setText("Quantity and price must be positive numbers.");
            return;
        }

        String sql = "INSERT INTO products "
                   + "(farmer_id, crop_type, crop_name, price_per_kg, stock_kg, "
                   + "harvest_date, pickup_location, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.connect();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, UserSession.loggedInUserId);
            statement.setString(2, type);
            statement.setString(3, name);
            statement.setDouble(4, price);
            statement.setInt(5, quantity);
            statement.setDate(6, java.sql.Date.valueOf(date));
            statement.setString(7, location);
            statement.setString(8, descriptionArea.getText().trim());
            statement.executeUpdate();

            new Alert(Alert.AlertType.INFORMATION, "Crop listed successfully!").showAndWait();
            clearForm();
            errorLabel.setText("");
        } catch (Exception e) {
            errorLabel.setText("Cannot save crop. Check the database connection.");
            e.printStackTrace();
        }
    }

    private void clearForm() {
        cropTypeCombo.getSelectionModel().clearSelection();
        cropNameField.clear();
        quantityField.clear();
        priceField.clear();
        harvestDatePicker.setValue(null);
        pickupLocationField.clear();
        descriptionArea.clear();
    }

    @FXML void handleCancel(ActionEvent event) {
        AgroLinkApp.sceneChange("FarmerDashboard.fxml", "Farmer Dashboard", 900, 600);
    }
}
