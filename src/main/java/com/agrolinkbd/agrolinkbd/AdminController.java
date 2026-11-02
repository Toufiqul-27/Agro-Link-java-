package com.agrolinkbd.agrolinkbd;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.*;

public class AdminController {

    @FXML private Label totalFarmersLabel, totalBuyersLabel, pendingApprovalsLabel, totalOrdersLabel;
    @FXML private ComboBox<String> filterUserRole;

    @FXML private TableView<User> usersTable, pendingFarmersTable;
    @FXML private TableColumn<User, Integer> colUserId, colFarmerId;
    @FXML private TableColumn<User, String> colUserName, colUserPhone, colUserRole, colUserLocation, colUserStatus;


    @FXML private TableView<Order> allOrdersTable;
    @FXML private TableColumn<Order, Integer> colOrderId, colOrderQty;
    @FXML private TableColumn<Order, Double> colOrderTotal;
    @FXML private TableColumn<Order, String> colOrderCrop, colOrderBuyer, colOrderFarmer, colOrderStatus, colOrderDate;

    @FXML private TableView<Truck> trucksTable;
    @FXML private TableColumn<Truck, Integer> colTruckId, colTruckCapacity;
    @FXML private TableColumn<Truck, String> colTruckRoute, colTruckStatus;
    @FXML private TableColumn<Truck, Double> colTruckCost;

    @FXML
    public void initialize() {
        filterUserRole.getItems().setAll("All Roles", "Farmer", "Buyer", "Arotdar");
        filterUserRole.setValue("All Roles");

        setupBindings();
        loadAllData();
    }

    private void setupBindings() {

        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUserName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colUserPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colUserLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));


        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colOrderCrop.setCellValueFactory(new PropertyValueFactory<>("cropName"));
        colOrderBuyer.setCellValueFactory(new PropertyValueFactory<>("buyerName"));
        colOrderFarmer.setCellValueFactory(new PropertyValueFactory<>("farmerName"));
        colOrderQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colOrderTotal.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        colOrderStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colOrderDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));


        colTruckId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTruckRoute.setCellValueFactory(new PropertyValueFactory<>("route"));
        colTruckCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colTruckCost.setCellValueFactory(new PropertyValueFactory<>("costPerKg"));
        colTruckStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadAllData() {
        loadUsers("");
        loadOrders();
        loadTrucks();
        updateStats();
    }

    private void loadUsers(String role) {
        ObservableList<User> list = FXCollections.observableArrayList();
        String sql = (role.isEmpty() || role.equals("All Roles")) ? "SELECT * FROM users" : "SELECT * FROM users WHERE role = '" + role + "'";
        try (Connection conn = Database.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new User(rs.getInt("id"), rs.getString("full_name"), rs.getString("nid"), rs.getString("phone"), rs.getString("location"), rs.getString("role"), rs.getString("status"), rs.getString("registered_on")));
            }
            usersTable.setItems(list);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadOrders() {
        ObservableList<Order> list = FXCollections.observableArrayList();
        String sql = "SELECT o.id, p.crop_name, b.full_name AS buyer_name, f.full_name AS farmer_name, o.status, o.order_date, o.quantity_kg, o.total_price " +
                "FROM orders o JOIN products p ON o.product_id = p.id JOIN users b ON o.buyer_id = b.id JOIN users f ON p.farmer_id = f.id";
        try (Connection conn = Database.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Order(rs.getInt("id"), rs.getString("crop_name"), rs.getString("buyer_name"), rs.getString("farmer_name"), rs.getString("status"), rs.getString("order_date"), rs.getInt("quantity_kg"), rs.getDouble("total_price")));
            }
            allOrdersTable.setItems(list);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadTrucks() {
        ObservableList<Truck> list = FXCollections.observableArrayList();
        try (Connection conn = Database.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM trucks")) {
            while (rs.next()) {
                list.add(new Truck(rs.getInt("id"), rs.getString("route"), rs.getInt("capacity_kg"), rs.getDouble("cost_per_kg"), rs.getInt("booked_weight_kg"), rs.getString("departure_date"), rs.getString("status")));
            }
            trucksTable.setItems(list);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateStats() {
        try (Connection conn = Database.connect(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT (SELECT COUNT(*) FROM users WHERE role='Farmer') as f, (SELECT COUNT(*) FROM users WHERE role='Buyer') as b, (SELECT COUNT(*) FROM orders) as o")) {
            if (rs.next()) {
                totalFarmersLabel.setText(String.valueOf(rs.getInt("f")));
                totalBuyersLabel.setText(String.valueOf(rs.getInt("b")));
                totalOrdersLabel.setText(String.valueOf(rs.getInt("o")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML void handleManageOrders(ActionEvent event) { AgroLinkApp.sceneChange("OrderManagement.fxml", "Manage Orders", 900, 600); }
    @FXML void handleFilterUsers(ActionEvent event) { loadUsers(filterUserRole.getValue()); }
    @FXML void handleLogout(ActionEvent event) { UserSession.clearSession(); AgroLinkApp.sceneChange("Login.fxml", "Login", 600, 500); }
    @FXML void handleAddTruck(ActionEvent event) { AgroLinkApp.sceneChange("AddTruck.fxml", "Add Truck", 400, 300); }


    @FXML void handleDeactivateUser(ActionEvent event) {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET status = 'Deactivated' WHERE id = ?")) {
                pstmt.setInt(1, selected.getId());
                pstmt.executeUpdate();
                loadUsers("");
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    @FXML
    void handleActivateUser(ActionEvent event) {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {

            return;
        }

        String sql = "UPDATE users SET status = 'Active' WHERE id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, selected.getId());
            pstmt.executeUpdate();


            loadUsers(filterUserRole.getValue());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML void handleRemoveTruck(ActionEvent event) {
        Truck selected = trucksTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement("DELETE FROM trucks WHERE id = ?")) {
                pstmt.setInt(1, selected.getId());
                pstmt.executeUpdate();
                loadTrucks();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    @FXML
    void handleApproveFarmer(ActionEvent event) {
        User selected = pendingFarmersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String sql = "UPDATE users SET status = 'Active' WHERE id = ?";
        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, selected.getId());
            pstmt.executeUpdate();
            loadUsers("");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    void handleRejectFarmer(ActionEvent event) {
        User selected = pendingFarmersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, selected.getId());
            pstmt.executeUpdate();
            loadUsers("");
        } catch (SQLException e) { e.printStackTrace(); }
    }
}