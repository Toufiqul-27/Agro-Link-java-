package com.agrolinkbd.agrolinkbd;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.*;

public class OrderController {
    @FXML private ComboBox<String> filterStatusCombo;
    @FXML private Label totalOrdersLabel, pendingOrdersLabel, confirmedOrdersLabel, dispatchedOrdersLabel;

    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> colOrderId, colQuantity;
    @FXML private TableColumn<Order, String> colCropName, colBuyerName, colStatus, colOrderDate;
    @FXML private TableColumn<Order, Double> colTotalPrice;

    private ObservableList<Order> orderList = FXCollections.observableArrayList();

    @FXML public void initialize() {
        filterStatusCombo.getItems().addAll("All", "Pending", "Confirmed", "Dispatched", "Cancelled");

        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colCropName.setCellValueFactory(new PropertyValueFactory<>("cropName"));
        colBuyerName.setCellValueFactory(new PropertyValueFactory<>("buyerName"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTotalPrice.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colOrderDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));

        loadOrders();
    }

    private void loadOrders() {
        orderList.clear();
        int pending = 0, confirmed = 0, dispatched = 0, total = 0;

        String sql = "SELECT o.id, p.crop_name, b.full_name AS buyer_name, f.full_name AS farmer_name, o.status, o.order_date, o.quantity_kg, o.total_price " +
                "FROM orders o " +
                "JOIN products p ON o.product_id = p.id " +
                "JOIN users b ON o.buyer_id = b.id " +
                "JOIN users f ON p.farmer_id = f.id ";

        if ("Buyer".equals(UserSession.loggedInRole) || "Arotdar".equals(UserSession.loggedInRole)) {
            sql += "WHERE o.buyer_id = " + UserSession.loggedInUserId;
        }

        Connection conn = Database.connect();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String status = rs.getString("status");

                orderList.add(new Order(
                        rs.getInt("id"),
                        rs.getString("crop_name"),
                        rs.getString("buyer_name"),
                        rs.getString("farmer_name"),
                        status,
                        rs.getString("order_date"),
                        rs.getInt("quantity_kg"),
                        rs.getDouble("total_price")
                ));

                total++;
                if (status.equals("Pending")) pending++;
                if (status.equals("Confirmed")) confirmed++;
                if (status.equals("Dispatched")) dispatched++;
            }
            ordersTable.setItems(orderList);

            totalOrdersLabel.setText(String.valueOf(total));
            pendingOrdersLabel.setText(String.valueOf(pending));
            confirmedOrdersLabel.setText(String.valueOf(confirmed));
            dispatchedOrdersLabel.setText(String.valueOf(dispatched));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateOrderStatus(String newStatus) {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Select an order first!").showAndWait();
            return;
        }

        String sql = "UPDATE orders SET status = ? WHERE id = ?";

        Connection conn = Database.connect();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, selected.getOrderId());
            pstmt.executeUpdate();

            loadOrders();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML void handleConfirmOrder(ActionEvent event) { updateOrderStatus("Confirmed"); }
    @FXML void handleDispatchOrder(ActionEvent event) { updateOrderStatus("Dispatched"); }
    @FXML void handleCancelOrder(ActionEvent event) { updateOrderStatus("Cancelled"); }

    @FXML void goBack(ActionEvent event) {
        if ("Admin".equals(UserSession.loggedInRole)) {
            AgroLinkApp.sceneChange("AdminDashboard.fxml", "Admin", 1000, 700);
        } else {
            AgroLinkApp.sceneChange("BuyerDashboard.fxml", "Buyer Dashboard", 900, 700);
        }
    }

    @FXML void handleLogout(ActionEvent event) {
        UserSession.clearSession();
        AgroLinkApp.sceneChange("Login.fxml", "Login", 600, 500);
    }
}