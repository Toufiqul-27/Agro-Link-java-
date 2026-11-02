package com.agrolinkbd.agrolinkbd;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RegisterController {
    @FXML private TextField nameField, nidField, phoneField, locationField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private Label errorLabel;

    @FXML public void initialize() {
        roleCombo.getItems().addAll("Farmer", "Buyer", "Arotdar");
    }

    @FXML void handleRegister(ActionEvent event) {
        String name = nameField.getText().trim();
        String nid = nidField.getText().trim();
        String phone = phoneField.getText().trim();
        String location = locationField.getText().trim();
        String role = roleCombo.getValue();
        String password = passwordField.getText();

        if (name.isBlank() || nid.isBlank() || phone.isBlank()
                || location.isBlank() || role == null || password.isBlank()) {
            errorLabel.setText("Please complete all fields.");
            return;
        }
        if (!phone.matches("01[3-9][0-9]{8}")) {
            errorLabel.setText("Enter a valid 11-digit Bangladeshi phone number.");
            return;
        }
        if (password.length() < 8) {
            errorLabel.setText("Password must be at least 8 characters.");
            return;
        }
        if (!password.equals(confirmPasswordField.getText())) {
            errorLabel.setText("Passwords do not match.");
            return;
        }

        String sql = "INSERT INTO users (full_name, nid, phone, location, role, password_hash, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'Active')";
        try (Connection conn = Database.connect();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, nid);
            statement.setString(3, phone);
            statement.setString(4, location);
            statement.setString(5, role);
            statement.setString(6, PasswordHasher.hash(password));
            statement.executeUpdate();

            new Alert(Alert.AlertType.INFORMATION,
                    "Registration successful. Please sign in.").showAndWait();
            goToLogin(event);
        } catch (SQLException e) {
            errorLabel.setText("Registration failed. Phone or NID may already be registered.");
            e.printStackTrace();
        } catch (IllegalStateException e) {
            errorLabel.setText("Database is not configured. See README.");
            e.printStackTrace();
        }
    }

    @FXML void goToLogin(ActionEvent event) {
        AgroLinkApp.sceneChange("Login.fxml", "Login", 600, 500);
    }
}
