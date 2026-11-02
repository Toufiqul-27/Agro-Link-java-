package com.agrolinkbd.agrolinkbd;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.math.BigDecimal;
import java.sql.*;

public class BuyerController {
    @FXML private ComboBox<String> filterCropType;
    @FXML private TextField filterRegion, filterMaxPrice, orderQtyField;
    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, String> colCropName, colFarmer, colLocation, colHarvestDate;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, Integer> colStock;

    private final ObservableList<Product> productList = FXCollections.observableArrayList();

    @FXML public void initialize() {
        filterCropType.getItems().addAll("All", "Vegetable", "Fruit", "Grain", "Spice");
        colCropName.setCellValueFactory(new PropertyValueFactory<>("cropName"));
        colFarmer.setCellValueFactory(new PropertyValueFactory<>("farmerName"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colHarvestDate.setCellValueFactory(new PropertyValueFactory<>("harvestDate"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        productsTable.setItems(productList);
        loadProducts(null, null, null);
    }

    private void loadProducts(String cropType, String region, BigDecimal maxPrice) {
        StringBuilder sql = new StringBuilder(
                "SELECT p.*, u.full_name AS farmer_name FROM products p "
              + "JOIN users u ON p.farmer_id = u.id "
              + "WHERE p.stock_kg > 0 AND u.status = 'Active'");
        if (cropType != null) sql.append(" AND p.crop_type = ?");
        if (region != null) sql.append(" AND p.pickup_location LIKE ?");
        if (maxPrice != null) sql.append(" AND p.price_per_kg <= ?");
        sql.append(" ORDER BY p.id DESC");

        try (Connection conn = Database.connect();
             PreparedStatement statement = conn.prepareStatement(sql.toString())) {
            int index = 1;
            if (cropType != null) statement.setString(index++, cropType);
            if (region != null) statement.setString(index++, "%" + region + "%");
            if (maxPrice != null) statement.setBigDecimal(index, maxPrice);
            try (ResultSet rs = statement.executeQuery()) {
                productList.clear();
                while (rs.next()) {
                    productList.add(new Product(
                            rs.getInt("id"), rs.getString("crop_name"),
                            rs.getString("farmer_name"), rs.getString("pickup_location"),
                            rs.getString("harvest_date"), rs.getDouble("price_per_kg"),
                            rs.getInt("stock_kg")));
                }
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Unable to load products. Check database setup.");
            e.printStackTrace();
        }
    }

    @FXML void handleFilter(ActionEvent event) {
        String type = filterCropType.getValue();
        if ("All".equals(type)) type = null;
        String region = filterRegion.getText().trim();
        BigDecimal maxPrice = null;
        if (!filterMaxPrice.getText().isBlank()) {
            try {
                maxPrice = new BigDecimal(filterMaxPrice.getText().trim());
                if (maxPrice.signum() <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Maximum price must be a positive number.");
                return;
            }
        }
        loadProducts(type, region.isBlank() ? null : region, maxPrice);
    }

    @FXML void handleClearFilter(ActionEvent event) {
        filterCropType.setValue(null);
        filterMaxPrice.clear();
        filterRegion.clear();
        loadProducts(null, null, null);
    }

    @FXML void handlePlaceOrder(ActionEvent event) {
        if (!("Buyer".equals(UserSession.loggedInRole) || "Arotdar".equals(UserSession.loggedInRole))) {
            showAlert(Alert.AlertType.ERROR, "Please sign in as a buyer.");
            return;
        }
        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Select a product first.");
            return;
        }
        int quantity;
        try {
            quantity = Integer.parseInt(orderQtyField.getText().trim());
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Quantity must be a positive whole number.");
            return;
        }

        try (Connection conn = Database.connect()) {
            BigDecimal total;
            try {
                conn.setAutoCommit(false);
                // Lock and re-check stock against current database state, not the UI.
                BigDecimal currentPrice;
                try (PreparedStatement read = conn.prepareStatement(
                        "SELECT price_per_kg FROM products WHERE id = ? AND stock_kg >= ? FOR UPDATE")) {
                    read.setInt(1, selected.getId());
                    read.setInt(2, quantity);
                    try (ResultSet rs = read.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Product is out of stock.");
                        currentPrice = rs.getBigDecimal("price_per_kg");
                    }
                }
                total = currentPrice.multiply(BigDecimal.valueOf(quantity));
                try (PreparedStatement stock = conn.prepareStatement(
                        "UPDATE products SET stock_kg = stock_kg - ? "
                      + "WHERE id = ? AND stock_kg >= ?")) {
                    stock.setInt(1, quantity);
                    stock.setInt(2, selected.getId());
                    stock.setInt(3, quantity);
                    if (stock.executeUpdate() != 1) throw new SQLException("Not enough stock.");
                }
                try (PreparedStatement order = conn.prepareStatement(
                        "INSERT INTO orders (buyer_id, product_id, quantity_kg, total_price, status) "
                      + "VALUES (?, ?, ?, ?, 'Pending')")) {
                    order.setInt(1, UserSession.loggedInUserId);
                    order.setInt(2, selected.getId());
                    order.setInt(3, quantity);
                    order.setBigDecimal(4, total);
                    order.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            showAlert(Alert.AlertType.INFORMATION, "Order placed successfully. Total: " + total + " BDT");
            orderQtyField.clear();
            handleFilter(null);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Order could not be placed. Stock may have changed.");
            e.printStackTrace();
            handleFilter(null);
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        new Alert(type, message).showAndWait();
    }

    @FXML void goToMyOrders(ActionEvent event) {
        AgroLinkApp.sceneChange("OrderManagement.fxml", "My Orders", 900, 600);
    }
    @FXML void goToTruckBooking(ActionEvent event) {
        AgroLinkApp.sceneChange("TruckBooking.fxml", "Book Truck", 900, 600);
    }
    @FXML void handleLogout(ActionEvent event) {
        UserSession.clearSession();
        AgroLinkApp.sceneChange("Login.fxml", "Login", 600, 500);
    }
}
