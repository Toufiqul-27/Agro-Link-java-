package com.agrolinkbd.agrolinkbd;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.*;

public class FarmerDashboardController {
    @FXML private TableView<Product> myProductsTable;
    @FXML private TableColumn<Product, String> colCropType, colCropName, colHarvestDate;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, Integer> colStock;

    private final ObservableList<Product> myProductsList = FXCollections.observableArrayList();

    @FXML public void initialize() {
        colCropType.setCellValueFactory(new PropertyValueFactory<>("cropType"));
        colCropName.setCellValueFactory(new PropertyValueFactory<>("cropName"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colHarvestDate.setCellValueFactory(new PropertyValueFactory<>("harvestDate"));
        myProductsTable.setItems(myProductsList);
        loadMyProducts();
    }

    private void loadMyProducts() {
        myProductsList.clear();
        String sql = "SELECT * FROM products WHERE farmer_id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, UserSession.loggedInUserId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Product product = new Product(rs.getInt("id"), rs.getString("crop_name"),
                            "", rs.getString("pickup_location"),
                            rs.getString("harvest_date"), rs.getDouble("price_per_kg"),
                            rs.getInt("stock_kg"));
                    product.setCropType(rs.getString("crop_type"));
                    myProductsList.add(product);
                }
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load crops.").showAndWait();
            e.printStackTrace();
        }
    }

    @FXML void handleDeleteCrop(ActionEvent event) {
        Product selected = myProductsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a crop.").showAndWait();
            return;
        }
        if (new Alert(Alert.AlertType.CONFIRMATION,
                "Delete '" + selected.getCropName() + "'?").showAndWait()
                .orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        String sql = "DELETE FROM products WHERE id = ? AND farmer_id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, selected.getId());
            statement.setInt(2, UserSession.loggedInUserId);
            if (statement.executeUpdate() == 0) {
                new Alert(Alert.AlertType.WARNING, "Crop not found or not owned by you.").showAndWait();
            }
            loadMyProducts();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Cannot delete a crop that has existing orders.").showAndWait();
            e.printStackTrace();
        }
    }

    @FXML void goToAddCrop(ActionEvent event) {
        AgroLinkApp.sceneChange("ProductForm.fxml", "Add New Crop", 800, 600);
    }
    @FXML void handleLogout(ActionEvent event) {
        UserSession.clearSession();
        AgroLinkApp.sceneChange("Login.fxml", "Login", 600, 500);
    }
}
