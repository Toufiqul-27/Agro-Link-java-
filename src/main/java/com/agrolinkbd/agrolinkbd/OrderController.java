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

    private final ObservableList<Order> orderList = FXCollections.observableArrayList();

    @FXML public void initialize() {
        filterStatusCombo.getItems().addAll("All", "Pending", "Confirmed", "Dispatched", "Cancelled");
        filterStatusCombo.setValue("All");
        filterStatusCombo.setOnAction(event -> loadOrders());
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colCropName.setCellValueFactory(new PropertyValueFactory<>("cropName"));
        colBuyerName.setCellValueFactory(new PropertyValueFactory<>("buyerName"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colTotalPrice.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colOrderDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        ordersTable.setItems(orderList);
        loadOrders();
    }

    private void loadOrders() {
        orderList.clear();
        int pending = 0, confirmed = 0, dispatched = 0, total = 0;
        String role = UserSession.loggedInRole;
        String sql = "SELECT o.id, p.crop_name, b.full_name AS buyer_name, "
                   + "f.full_name AS farmer_name, o.status, o.order_date, "
                   + "o.quantity_kg, o.total_price FROM orders o "
                   + "JOIN products p ON o.product_id = p.id "
                   + "JOIN users b ON o.buyer_id = b.id "
                   + "JOIN users f ON p.farmer_id = f.id";
        boolean buyer = "Buyer".equals(role) || "Arotdar".equals(role);
        if (buyer) sql += " WHERE o.buyer_id = ?";
        else if (!"Admin".equals(role)) sql += " WHERE 1 = 0";
        sql += " ORDER BY o.order_date DESC";

        try (Connection conn = Database.connect();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            if (buyer) statement.setInt(1, UserSession.loggedInUserId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("status");
                    total++;
                    if ("Pending".equals(status)) pending++;
                    if ("Confirmed".equals(status)) confirmed++;
                    if ("Dispatched".equals(status)) dispatched++;
                    if ("All".equals(filterStatusCombo.getValue())
                            || status.equals(filterStatusCombo.getValue())) {
                        orderList.add(new Order(rs.getInt("id"), rs.getString("crop_name"),
                                rs.getString("buyer_name"), rs.getString("farmer_name"),
                                status, rs.getString("order_date"),
                                rs.getInt("quantity_kg"), rs.getDouble("total_price")));
                    }
                }
            }
            totalOrdersLabel.setText(Integer.toString(total));
            pendingOrdersLabel.setText(Integer.toString(pending));
            confirmedOrdersLabel.setText(Integer.toString(confirmed));
            dispatchedOrdersLabel.setText(Integer.toString(dispatched));
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Unable to load orders.").showAndWait();
            e.printStackTrace();
        }
    }

    private void updateOrderStatus(String newStatus) {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Select an order first.").showAndWait();
            return;
        }
        try (Connection conn = Database.connect()) {
            try {
                conn.setAutoCommit(false);
                int productId, quantity, buyerId;
                String oldStatus;
                try (PreparedStatement get = conn.prepareStatement(
                        "SELECT product_id, quantity_kg, buyer_id, status "
                      + "FROM orders WHERE id = ? FOR UPDATE")) {
                    get.setInt(1, selected.getOrderId());
                    try (ResultSet rs = get.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Order no longer exists.");
                        productId = rs.getInt("product_id");
                        quantity = rs.getInt("quantity_kg");
                        buyerId = rs.getInt("buyer_id");
                        oldStatus = rs.getString("status");
                    }
                }
                boolean owner = buyerId == UserSession.loggedInUserId;
                if (!OrderTransitions.allowed(UserSession.loggedInRole, owner, oldStatus, newStatus)) {
                    throw new SQLException("You cannot make this status change.");
                }
                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE orders SET status = ? WHERE id = ?")) {
                    update.setString(1, newStatus);
                    update.setInt(2, selected.getOrderId());
                    if (update.executeUpdate() != 1) throw new SQLException("Order update failed.");
                }
                if ("Cancelled".equals(newStatus)) {
                    try (PreparedStatement restore = conn.prepareStatement(
                            "UPDATE products SET stock_kg = stock_kg + ? WHERE id = ?")) {
                        restore.setInt(1, quantity);
                        restore.setInt(2, productId);
                        if (restore.executeUpdate() != 1) {
                            throw new SQLException("Unable to restore cancelled order stock.");
                        }
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                try { conn.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            loadOrders();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Order update failed or is not permitted.").showAndWait();
            e.printStackTrace();
        }
    }

    @FXML void handleConfirmOrder(ActionEvent event) { updateOrderStatus("Confirmed"); }
    @FXML void handleDispatchOrder(ActionEvent event) { updateOrderStatus("Dispatched"); }
    @FXML void handleCancelOrder(ActionEvent event) { updateOrderStatus("Cancelled"); }

    @FXML void goBack(ActionEvent event) {
        if ("Admin".equals(UserSession.loggedInRole))
            AgroLinkApp.sceneChange("AdminDashboard.fxml", "Admin", 1000, 700);
        else
            AgroLinkApp.sceneChange("BuyerDashboard.fxml", "Buyer Dashboard", 900, 700);
    }

    @FXML void handleLogout(ActionEvent event) {
        UserSession.clearSession();
        AgroLinkApp.sceneChange("Login.fxml", "Login", 600, 500);
    }
}
