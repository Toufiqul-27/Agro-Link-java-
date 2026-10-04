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
        String name = cropNameField.getText();
        String qtyStr = quantityField.getText();
        String priceStr = priceField.getText();
        LocalDate date = harvestDatePicker.getValue();
        String location = pickupLocationField.getText();

        if (type == null || name.isEmpty() || qtyStr.isEmpty() || priceStr.isEmpty()) {
            errorLabel.setText("Please fill all required fields (*)!");
            return;
        }

        int qty;
        double price;
        try {
            qty = Integer.parseInt(qtyStr);
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            errorLabel.setText("Quantity and Price must be valid numbers!");
            return;
        }


        Connection conn = Database.connect();
        String sql = "INSERT INTO products (farmer_id, crop_type, crop_name, price_per_kg, stock_kg, harvest_date, pickup_location, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";


        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, UserSession.loggedInUserId);
            pstmt.setString(2, type);
            pstmt.setString(3, name);
            pstmt.setDouble(4, price);
            pstmt.setInt(5, qty);
            pstmt.setDate(6, date != null ? java.sql.Date.valueOf(date) : null);
            pstmt.setString(7, location);
            pstmt.setString(8, descriptionArea.getText());

            pstmt.executeUpdate();

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Crop listed successfully!");
            alert.showAndWait();


            clearForm();
            errorLabel.setText("");

        } catch (SQLException e) {
            errorLabel.setText("Database error occurred!");
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