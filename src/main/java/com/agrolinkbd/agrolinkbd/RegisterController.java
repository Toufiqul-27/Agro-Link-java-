package com.agrolinkbd.agrolinkbd;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.*;

public class RegisterController {

    @FXML private TextField nameField, nidField, phoneField, locationField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {

        roleCombo.getItems().addAll("Farmer", "Buyer", "Arotdar");
    }

    @FXML
    void handleRegister(ActionEvent event) {

        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            errorLabel.setText("Passwords do not match!");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/agrolink_db", "root", "ovi@2327")) {

            String sql = "INSERT INTO users (full_name, nid, phone, location, role, password_hash, status) VALUES (?, ?, ?, ?, ?, ?, 'Active')";
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, nameField.getText());
            pstmt.setString(2, nidField.getText());
            pstmt.setString(3, phoneField.getText());
            pstmt.setString(4, locationField.getText());
            pstmt.setString(5, roleCombo.getValue());
            pstmt.setString(6, passwordField.getText());

            pstmt.executeUpdate();


            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Registration Successful! Please login.");
            alert.showAndWait();


            goToLogin(event);

        } catch (SQLException e) {
            errorLabel.setText("Error: Phone or NID might already exist.");
            e.printStackTrace();
        }
    }

    @FXML
    void goToLogin(ActionEvent event) {
        AgroLinkApp.sceneChange("Login.fxml", "Login", 600, 500);
    }
}