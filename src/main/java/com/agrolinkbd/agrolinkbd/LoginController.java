package com.agrolinkbd.agrolinkbd;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {
    @FXML private ComboBox<String> roleCombo;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML public void initialize() {
        roleCombo.getItems().addAll("Farmer", "Buyer", "Arotdar", "Admin");
    }

    @FXML void handleLogin() {
        String phone = phoneField.getText().trim();
        String password = passwordField.getText();
        String role = roleCombo.getValue();
        if (phone.isBlank() || password.isBlank() || role == null) {
            errorLabel.setText("Fill all fields.");
            return;
        }

        String sql = "SELECT id, full_name, role, status, password_hash "
                + "FROM users WHERE phone = ? AND role = ?";
        try (Connection conn = Database.connect();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, phone);
            statement.setString(2, role);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !PasswordHasher.verify(password, result.getString("password_hash"))) {
                    errorLabel.setText("Invalid credentials.");
                    return;
                }
                if (!"Active".equals(result.getString("status"))) {
                    errorLabel.setText("Your account is not active. Contact the administrator.");
                    return;
                }

                int userId = result.getInt("id");
                String stored = result.getString("password_hash");
                if (!PasswordHasher.isHashed(stored)) {
                    try (PreparedStatement upgrade = conn.prepareStatement(
                            "UPDATE users SET password_hash = ? WHERE id = ? AND password_hash = ?")) {
                        upgrade.setString(1, PasswordHasher.hash(password));
                        upgrade.setInt(2, userId);
                        upgrade.setString(3, stored);
                        upgrade.executeUpdate();
                    }
                }
                UserSession.loggedInUserId = userId;
                UserSession.loggedInUserName = result.getString("full_name");
                UserSession.loggedInRole = role;
            }

            switch (role) {
                case "Admin" -> AgroLinkApp.sceneChange("AdminDashboard.fxml", "Admin", 1000, 700);
                case "Farmer" -> AgroLinkApp.sceneChange("FarmerDashboard.fxml", "Farmer", 900, 600);
                case "Buyer", "Arotdar" ->
                        AgroLinkApp.sceneChange("BuyerDashboard.fxml", role + " Dashboard", 900, 700);
                default -> errorLabel.setText("Invalid role.");
            }
        } catch (Exception e) {
            errorLabel.setText("Unable to sign in. Check database setup.");
            e.printStackTrace();
        }
    }

    @FXML void goToRegister() {
        AgroLinkApp.sceneChange("Register.fxml", "Register Account", 500, 620);
    }
}
