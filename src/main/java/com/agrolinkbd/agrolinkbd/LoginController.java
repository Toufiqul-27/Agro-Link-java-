package com.agrolinkbd.agrolinkbd;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {
    @FXML private ComboBox<String> roleCombo;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML public void initialize() {
        roleCombo.getItems().addAll("Farmer", "Buyer", "Arotdar", "Admin");
    }

    @FXML void handleLogin() {
        String phone = phoneField.getText();
        String pass = passwordField.getText();
        String role = roleCombo.getValue();

        if (phone.isEmpty() || pass.isEmpty() || role == null) {
            errorLabel.setText("Fill all fields!");
            return;
        }

        String sql = "SELECT * FROM users WHERE phone = ? AND password_hash = ? AND role = ?";
        try (var conn = Database.connect(); var pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, phone);
            pstmt.setString(2, pass);
            pstmt.setString(3, role);

            var rs = pstmt.executeQuery();
            if (rs.next()) {

                UserSession.loggedInUserId = rs.getInt("id");

                switch (role) {
                    case "Admin" -> AgroLinkApp.sceneChange("AdminDashboard.fxml", "Admin", 1000, 700);
                    case "Farmer" -> AgroLinkApp.sceneChange("ProductForm.fxml", "Farmer", 800, 600);
                    case "Arotdar" -> AgroLinkApp.sceneChange("BuyerDashboard.fxml", "Arotdar Dashboard", 900, 700);
                    case "Buyer" -> AgroLinkApp.sceneChange("BuyerDashboard.fxml", "Buyer", 900, 700);
                }
            } else {
                errorLabel.setText("Invalid credentials!");
            }
        } catch (Exception e) {
            errorLabel.setText("Database Error!");
            e.printStackTrace();
        }
    }

    @FXML void goToRegister() {
        AgroLinkApp.sceneChange("Register.fxml", "Register Account", 400, 500);
    }
}