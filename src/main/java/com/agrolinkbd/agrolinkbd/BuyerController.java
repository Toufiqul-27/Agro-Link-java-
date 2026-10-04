package com.agrolinkbd.agrolinkbd;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.*;

public class BuyerController {
    @FXML private ComboBox<String> filterCropType;
    @FXML private TextField filterRegion, filterMaxPrice, orderQtyField;

    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, String> colCropName, colFarmer, colLocation, colHarvestDate;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, Integer> colStock;

    private ObservableList<Product> productList = FXCollections.observableArrayList();

    @FXML public void initialize() {
        filterCropType.getItems().addAll("All", "Vegetable", "Fruit", "Grain", "Spice");

        colCropName.setCellValueFactory(new PropertyValueFactory<>("cropName"));
        colFarmer.setCellValueFactory(new PropertyValueFactory<>("farmerName"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colHarvestDate.setCellValueFactory(new PropertyValueFactory<>("harvestDate"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        loadProducts("");
    }

    private void loadProducts(String filterCondition) {
        productList.clear();
        String sql = "SELECT p.*, u.full_name AS farmer_name FROM products p JOIN users u ON p.farmer_id = u.id WHERE p.stock_kg > 0 " + filterCondition;


        Connection conn = Database.connect();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            int count = 0;
            while (rs.next()) {
                count++;
                productList.add(new Product(
                        rs.getInt("id"),
                        rs.getString("crop_name"),
                        rs.getString("farmer_name"),
                        rs.getString("pickup_location"),
                        rs.getString("harvest_date"),
                        rs.getDouble("price_per_kg"),
                        rs.getInt("stock_kg")
                ));
            }


            System.out.println("SUCCESS: Found " + count + " products for the Buyer Dashboard");

            productsTable.setItems(productList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML void handleFilter(ActionEvent event) {
        StringBuilder condition = new StringBuilder();
        if (filterCropType.getValue() != null && !filterCropType.getValue().equals("All")) {
            condition.append(" AND p.crop_type = '").append(filterCropType.getValue()).append("'");
        }
        if (!filterMaxPrice.getText().isEmpty()) {
            condition.append(" AND p.price_per_kg <= ").append(filterMaxPrice.getText());
        }
        loadProducts(condition.toString());
    }

    @FXML void handleClearFilter(ActionEvent event) {
        filterCropType.setValue(null);
        filterMaxPrice.clear();
        loadProducts("");
    }


    @FXML void handlePlaceOrder(ActionEvent event) {
        Product selected = productsTable.getSelectionModel().getSelectedItem();
        String qtyStr = orderQtyField.getText();

        if (selected == null || qtyStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Please select a product and enter quantity.");
            return;
        }

        try {
            int orderQty = Integer.parseInt(qtyStr);
            if (orderQty <= 0 || orderQty > selected.getStock()) {
                showAlert(Alert.AlertType.ERROR, "Invalid quantity. Check available stock.");
                return;
            }

            double totalCost = orderQty * selected.getPrice();


            Connection conn = Database.connect();
            try {
                conn.setAutoCommit(false);

                String insertOrder = "INSERT INTO orders (buyer_id, product_id, quantity_kg, total_price, status) VALUES (?, ?, ?, ?, 'Pending')";
                try(PreparedStatement pstmtOrder = conn.prepareStatement(insertOrder)) {
                    pstmtOrder.setInt(1, UserSession.loggedInUserId);
                    pstmtOrder.setInt(2, selected.getId());
                    pstmtOrder.setInt(3, orderQty);
                    pstmtOrder.setDouble(4, totalCost);
                    pstmtOrder.executeUpdate();
                }

                String updateStock = "UPDATE products SET stock_kg = stock_kg - ? WHERE id = ?";
                try(PreparedStatement pstmtStock = conn.prepareStatement(updateStock)) {
                    pstmtStock.setInt(1, orderQty);
                    pstmtStock.setInt(2, selected.getId());
                    pstmtStock.executeUpdate();
                }

                conn.commit();
                conn.setAutoCommit(true);

                showAlert(Alert.AlertType.INFORMATION, "Order placed successfully! Total: " + totalCost + " BDT");
                loadProducts("");
                orderQtyField.clear();

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Database transaction failed.");
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Quantity must be a valid number.");
        }
    }

    private void showAlert(Alert.AlertType type, String msg) {
        new Alert(type, msg).showAndWait();
    }

    @FXML void goToMyOrders(ActionEvent event) { AgroLinkApp.sceneChange("OrderManagement.fxml", "My Orders", 900, 600); }
    @FXML void goToTruckBooking(ActionEvent event) { AgroLinkApp.sceneChange("TruckBooking.fxml", "Book Truck", 900, 600); }
    @FXML void handleLogout(ActionEvent event) { UserSession.clearSession(); AgroLinkApp.sceneChange("Login.fxml", "Login", 600, 500); }
}