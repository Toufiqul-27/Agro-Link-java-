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
    @FXML private TableColumn<Product, String> colCropType;
    @FXML private TableColumn<Product, String> colCropName;
    @FXML private TableColumn<Product, String> colHarvestDate;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, Integer> colStock;

    private ObservableList<Product> myProductsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        colCropType.setCellValueFactory(new PropertyValueFactory<>("cropType"));
        colCropName.setCellValueFactory(new PropertyValueFactory<>("cropName"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colHarvestDate.setCellValueFactory(new PropertyValueFactory<>("harvestDate"));

        loadMyProducts();
    }

    private void loadMyProducts() {
        myProductsList.clear();


        String sql = "SELECT * FROM products WHERE farmer_id = ?";

        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, UserSession.loggedInUserId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    myProductsList.add(new Product(
                            rs.getInt("id"),
                            rs.getString("crop_name"),
                            rs.getString("crop_type"),
                            rs.getString("pickup_location"),
                            rs.getString("harvest_date"),
                            rs.getDouble("price_per_kg"),
                            rs.getInt("stock_kg")
                    ));
                }
            }
            myProductsTable.setItems(myProductsList);

        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to load products from database.").showAndWait();
        }
    }

    @FXML
    void handleDeleteCrop(ActionEvent event) {
        Product selected = myProductsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a crop to delete.").showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete '" + selected.getCropName() + "'?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            String sql = "DELETE FROM products WHERE id = ?";

            try (Connection conn = Database.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, selected.getId());
                pstmt.executeUpdate();


                loadMyProducts();

            } catch (SQLException e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Cannot delete: This crop has existing orders or references.").showAndWait();
            }
        }
    }

    @FXML
    void goToAddCrop(ActionEvent event) {
        AgroLinkApp.sceneChange("ProductForm.fxml", "Add New Crop", 800, 600);
    }

    @FXML
    void handleLogout(ActionEvent event) {
        UserSession.clearSession();
        AgroLinkApp.sceneChange("Login.fxml", "Login", 600, 500);
    }
}